package grapefruit.command.dispatcher;

import grapefruit.command.dispatcher.registration.CommandRegistration;
import grapefruit.command.parameter.CommandParameter;
import grapefruit.command.parameter.ParameterNode;
import grapefruit.command.parameter.StandardParameter;
import grapefruit.command.parameter.resolver.ParameterResolutionException;
import grapefruit.command.util.Miscellaneous;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static grapefruit.command.parameter.ParameterNode.FLAG_PATTERN;
import static java.util.Objects.requireNonNull;

final class CommandGraph<S> {
    protected static final String ALIAS_SEPARATOR = "\\|";
    private static final UnaryOperator<String> AS_REQUIRED = arg -> '<' + arg + '>';
    private static final UnaryOperator<String> AS_OPTIONAL = arg -> '[' + arg + ']';
    private final CommandNode<S> rootNode = new CommandNode<>("__ROOT__", Set.of(), null);
    private final CommandAuthorizer<S> authorizer;

    CommandGraph(final @NotNull CommandAuthorizer<S> authorizer) {
        this.authorizer = requireNonNull(authorizer, "authorizer cannot be null");
    }

    public void registerCommand(final @NotNull String route, final @NotNull CommandRegistration<S> reg) {
        final List<RouteFragment> parts = Arrays.stream(route.split(" "))
                .map(String::trim)
                .map(x -> x.split(ALIAS_SEPARATOR))
                .map(x -> {
                    final String primary = x[0];
                    final String[] aliases = x.length > 1
                            ? Arrays.copyOfRange(x, 1, x.length)
                            : new String[0];
                    return new RouteFragment(primary, aliases);
                })
                .collect(Collectors.toList());

        if (parts.isEmpty()) {
            throw new IllegalArgumentException("Empty command tree detected");
        }

        CommandNode<S> node = this.rootNode;
        for (final Iterator<RouteFragment> iter = parts.iterator(); iter.hasNext();) {
            final RouteFragment current = iter.next();
            final boolean last = !iter.hasNext();
            final CommandNode<S> childNode = new CommandNode<>(current.primary(), current.aliases(), !last ? null : reg);
            final Optional<CommandNode<S>> possibleChild = node.findChild(childNode.primary());

            if (possibleChild.isPresent()) {
                if (last) {
                    throw new IllegalStateException("Ambigious command tree");
                }

                final CommandNode<S> realChildNode = possibleChild.get();
                realChildNode.mergeAliases(childNode.aliases());
                node = realChildNode;
            } else {
                node.addChild(childNode);
                node = childNode;
            }
        }
    }

    public @NotNull RouteResult<S> routeCommand(final @NotNull Queue<CommandArgument> args) {
        CommandNode<S> commandNode = this.rootNode;
        CommandArgument arg;
        boolean firstRun = true;
        while ((arg = args.poll()) != null) {
            final String rawInput = arg.rawArg();
            // If we don't have a child with this name, throw NoSuchCommand
            if (firstRun) {
                if (this.rootNode.findChild(rawInput).isEmpty()) {
                    return RouteResult.failure(RouteResult.Failure.Reason.NO_SUCH_COMMAND);
                }

                firstRun = false;
            }

            Optional<CommandNode<S>> possibleChild = commandNode.findChild(rawInput);
            if (possibleChild.isEmpty()) {
                for (final CommandNode<S> each : commandNode.children()) {
                    if (each.aliases().contains(rawInput)) {
                        possibleChild = Optional.of(each);
                    }
                }

                if (possibleChild.isEmpty()) {
                    return RouteResult.failure(RouteResult.Failure.Reason.INVALID_SYNTAX);
                }
            }

            final CommandNode<S> child = possibleChild.get();
            if (child.children().isEmpty()) {
                final Optional<CommandRegistration<S>> registration = child.registration();
                return registration.map(RouteResult::success).orElseGet(() ->
                        RouteResult.failure(RouteResult.Failure.Reason.INVALID_SYNTAX));
            } else {
                commandNode = child;
            }
        }

        return RouteResult.failure(RouteResult.Failure.Reason.INVALID_SYNTAX);
    }

    public @NotNull List<String> listSuggestions(final @NotNull S source, final @NotNull Deque<CommandArgument> args) {
        CommandNode<S> commandNode = this.rootNode;
        CommandArgument part;
        while ((part = args.peek()) != null) {
            if (commandNode.registration().isPresent()) {
                break;

            } else {
                final String rawArg = part.rawArg();
                Optional<CommandNode<S>> childNode = commandNode.findChild(rawArg);
                if (childNode.isPresent()) {
                    commandNode = childNode.get();

                } else {
                    for (final CommandNode<S> each : commandNode.children()) {
                        if (each.aliases().contains(rawArg)) {
                            childNode = Optional.of(each);
                        }
                    }
                }

                if (childNode.isEmpty()) {
                    break;
                }
            }

            args.remove();
        }

        final Optional<CommandRegistration<S>> registrationOpt = commandNode.registration();
        final Deque<CommandArgument> argsCopy = new ConcurrentLinkedDeque<>(args);
        if (registrationOpt.isPresent()) {
            final CommandRegistration<S> registration = registrationOpt.orElseThrow();
            if (!Miscellaneous.checkAuthorized(source, registration.permission(), this.authorizer)) {
                return List.of();
            }

            for (final ParameterNode<S> param : registration.parameters()) {
                if (args.isEmpty()) {
                    return List.of();
                }

                CommandArgument currentArg = args.element();
                final Matcher flagPatternMatcher = FLAG_PATTERN.matcher(currentArg.rawArg());
                if (flagPatternMatcher.matches()) {
                    args.remove();
                    if (args.isEmpty()) {
                        return suggestFor(source, param, args, currentArg.rawArg());
                    }

                    currentArg = args.element();
                }

                if (currentArg.rawArg().equals("") || args.size() < 2) {
                    return suggestFor(source, param, argsCopy, currentArg.rawArg());
                }

                try {
                    param.resolver().resolve(source, argsCopy, param.unwrap());
                    args.remove();
                } catch (final ParameterResolutionException ex) {
                    return List.of();
                }
            }

            return List.of();

        } else {
            return commandNode.children().stream()
                    .map(x -> {
                        final List<String> suggestions = new ArrayList<>();
                        suggestions.add(x.primary());
                        suggestions.addAll(x.aliases());
                        return suggestions;
                    })
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        }
    }

    private @NotNull List<String> suggestFor(final @NotNull S source,
                                             final @NotNull ParameterNode<S> parameter,
                                             final @NotNull Deque<CommandArgument> previousArgs,
                                             final @NotNull String currentArg) {
        if (parameter instanceof StandardParameter.ValueFlag) {
            if (previousArgs.stream().anyMatch(arg -> arg.rawArg().equalsIgnoreCase(Miscellaneous.formatFlag(parameter.name())))) {
                return parameter.resolver().listSuggestions(source, currentArg, parameter.unwrap());
            }

            return List.of(Miscellaneous.formatFlag(parameter.name()));
        }

        return parameter.resolver().listSuggestions(source, currentArg, parameter.unwrap());
    }

    public @NotNull String generateSyntaxFor(final @NotNull String commandLine) {
        final String[] path = commandLine.split(" ");
        final StringJoiner joiner = new StringJoiner(" ");
        CommandNode<S> node = this.rootNode;

        for (final String pathPart : path) {
            final Optional<CommandNode<S>> child = node.findChild(pathPart);
            if (child.isPresent()) {
                joiner.add(pathPart);
                node = child.get();

            } else {
                break;
            }
        }

        final Optional<CommandRegistration<S>> registration = node.registration();
        if (registration.isPresent()) {
            for (final ParameterNode<S> parameter : registration.get().parameters()) {
                final CommandParameter unwrapped = parameter.unwrap();
                joiner.add(unwrapped.isOptional()
                        ? AS_OPTIONAL.apply(parameter.name())
                        : AS_REQUIRED.apply(parameter.name()));
            }

        } else {
            final String children = node.children().stream()
                    .map(CommandNode::primary)
                    .collect(Collectors.joining("|"));
            joiner.add(AS_REQUIRED.apply(children));
        }

        return joiner.toString();
    }

    private static final record RouteFragment (@NotNull String primary, @NotNull String[] aliases) {}

    interface RouteResult<S> {

        static <S> @NotNull RouteResult<S> success(final @NotNull CommandRegistration<S> registration) {
            return new Success<>(registration);
        }

        static <S> @NotNull RouteResult<S> failure(final @NotNull Failure.Reason reason) {
            return new Failure<>(reason);
        }

        record Success<S>(@NotNull CommandRegistration<S> registration) implements RouteResult<S> {}

        record Failure<S>(@NotNull RouteResult.Failure.Reason reason) implements RouteResult<S> {

            enum Reason {
                NO_SUCH_COMMAND, INVALID_SYNTAX
            }
        }
    }
}
