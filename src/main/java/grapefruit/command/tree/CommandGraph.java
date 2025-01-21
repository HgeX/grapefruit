package grapefruit.command.tree;

import grapefruit.command.CommandException;
import grapefruit.command.CommandModule;
import grapefruit.command.argument.CommandArgument;
import grapefruit.command.argument.CommandChain;
import grapefruit.command.dispatcher.input.CommandInputTokenizer;
import grapefruit.command.dispatcher.input.MissingInputException;
import grapefruit.command.tree.node.CommandNode;
import grapefruit.command.tree.node.InternalCommandNode;
import grapefruit.command.util.Tuple2;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public class CommandGraph<S> {
    private final InternalCommandNode<S> rootNode = InternalCommandNode.of("__ROOT__", Set.of(), null);

    public void insert(final CommandChain<S> chain, final CommandModule<S> command) {
        requireNonNull(chain, "chain cannot be null");
        requireNonNull(command, "command cannot be null");

        if (chain.route().isEmpty()) {
            throw new IllegalStateException("Cannot register a command handler directly on the root node");
        }

        InternalCommandNode<S> node = this.rootNode;
        for (final Iterator<CommandArgument.Literal<S>> iter = chain.route().iterator(); iter.hasNext();) {
            final CommandArgument.Literal<S> literal = iter.next();
            final boolean isLast = !iter.hasNext();

            final Optional<InternalCommandNode<S>> childCandidate = queryChildOf(node, literal);
            if (childCandidate.isPresent()) {
                final InternalCommandNode<S> child = childCandidate.orElseThrow();
                if (isLast) {
                    /*
                     * We throw an exception if a child node exists, and we don't have
                     * any route parts left, because either this child has child nodes
                     * or already has a command handler attached to it (otherwise the
                     * chain would be invalid). In any case, we cannot proceed from here.
                     */
                    throw new IllegalStateException("Command node '%s' already exists in the command tree".formatted(child));
                }

                child.mergeAliases(literal.aliases());
                node = child;
            } else {
                if (node.command().isPresent()) {
                    throw new IllegalStateException("Command node '%s' already has a command attached to it, thus it cannot have children.");
                }

                final InternalCommandNode<S> child = InternalCommandNode.of(literal.name(), literal.aliases(), node);
                node.addChild(child);
                node = child;
                // Register command if we're at the end of the chain
                if (isLast) node.command(command);
            }
        }
    }

    public void delete(final CommandChain<S> chain) {
        requireNonNull(chain, "chain cannot be null");
        if (chain.route().isEmpty()) return;
        if (this.rootNode.isLeaf()) throw new IllegalStateException("Root node is leaf");

        InternalCommandNode<S> node = this.rootNode;
        for (final CommandArgument.Literal<S> literal : chain.route()) {
            final Optional<InternalCommandNode<S>> childCandidate = queryChildOf(node, literal);
            if (childCandidate.isEmpty()) {
                throw new IllegalStateException("Command node '%s' does not have a suitable child".formatted(node));
            }

            node = childCandidate.orElseThrow();
        }

        // Check just in case
        if (!node.isLeaf()) throw new IllegalStateException("Attempting to delete non-leaf command node");
        while (!node.equals(this.rootNode)) {
            final InternalCommandNode<S> parent = node.parent().orElseThrow();
            // If the node is a leaf node, we can safely delete it from its parent
            if (node.isLeaf()) {
                parent.removeChild(node);
                node = parent;
            } else {
                // If the node is not a leaf node, we can't delete more nodes.
                break;
            }
        }
    }

    public CommandModule<S> query(final CommandInputTokenizer input) throws CommandException {
        requireNonNull(input, "input cannot be null");
        final InternalCommandNode<S> node = query0(input);
        final Optional<CommandModule<S>> command = node.command();
        if (command.isPresent()) return command.orElseThrow();

        throw generateNoSuchCommand(node, input, "");
    }

    private InternalCommandNode<S> query0(final CommandInputTokenizer input) throws NoSuchCommandException {
        InternalCommandNode<S> node = this.rootNode;
        try {
            while (input.canReadNonWhitespace()) {
                final String name = input.readWord();
                final Optional<InternalCommandNode<S>> childCandidate = node.queryChild(name);
                if (childCandidate.isEmpty()) throw generateNoSuchCommand(node, input, name);

                node = childCandidate.orElseThrow();
                if (node.isLeaf()) {
                    final Optional<CommandModule<S>> commandCandidate = node.command();
                    if (commandCandidate.isPresent()) {
                        return node;
                    }

                    /*
                     * If the node is a leaf node, we assume this is the command handler
                     * we're looking for. Technically it should always exist, because
                     * leaf nodes must have a command attached to them, and this#insert
                     * makes sure of that. Just to be safe though, if the command handler
                     * still happens to be missing.
                     */
                    throw new IllegalStateException("CommandNode '%s' is a leaf node, but has no command attached to it.".formatted(node));
                }
            }
        } catch (final MissingInputException ex) {
            throw generateNoSuchCommand(node, input, "");
        }

        return node;
    }

    public Tuple2<List<String>, CommandModule<S>> complete(final CommandInputTokenizer input) {
        requireNonNull(input, "input cannot be null");

        try {
            if (!input.canReadNonWhitespace()) {
                // The input is empty, complete the direct children of the root node
                return new Tuple2<>(completeChildren(this.rootNode), null);

            }

            final InternalCommandNode<S> node = query0(input);
            final Optional<CommandModule<S>> command = node.command();

            if (command.isPresent() && input.canRead()) {
                /*
                 * We have found a command. If we there are more arguments in the queue, we
                 * want to return this command (thus passing all subsequent arguments for
                 * completion to it rather than handling them here).
                 */
                return new Tuple2<>(null, command.orElseThrow());
            }
            /*
             * If `canRead()` returns true at this stage, it means that all
             * command names have been valid so far and the input ends with
             * a whitespace. In this case, we want to complete child nodes
             * with an empty input string. Otherwise, complete the current
             * node with the current input.
             */
            final List<String> completions = input.canRead()
                    ? completeChildren(node)
                    : completeNode(node);

            return new Tuple2<>(completions, null);
        } catch (final NoSuchCommandException ex) {
            final List<String> completions;
            if (input.canRead()) {
                /*
                 * If we have more input to read, that means that the invalid
                 * node name is not the last argument. Return an empty list in
                 * such cases.
                 */
                completions = List.of();
            } else {
                // Otherwise, we collect completions for the current node
                completions = ex.alternatives().stream()
                        .flatMap(CommandGraph::collectAliases)
                        .toList();
            }

            return new Tuple2<>(completions, null);
        }
    }

    private static Stream<String> collectAliases(final CommandNode node) {
        return Stream.concat(Stream.of(node.name()), node.aliases().stream());
    }

    private static List<String> completeNode(final CommandNode node) {
        return collectAliases(node).toList();
    }

    private static <S> List<String> completeChildren(final InternalCommandNode<S> node) {
        return node.children().stream()
                .flatMap(CommandGraph::collectAliases)
                .toList();
    }

    private static <S> Optional<InternalCommandNode<S>> queryChildOf(final InternalCommandNode<S> parent, final CommandArgument.Literal<S> literal) {
        Optional<InternalCommandNode<S>> candidate = parent.queryChild(literal.name());
        if (candidate.isPresent()) return candidate;

        for (String alias : literal.aliases()) {
            candidate = parent.queryChild(alias);
            if (candidate.isPresent()) return candidate;
        }

        return Optional.empty();
    }

    private static <S> NoSuchCommandException generateNoSuchCommand(final InternalCommandNode<S> node, final CommandInputTokenizer input, final String argument) {
        final Set<CommandNode> alternatives = node.children().stream()
                .map(InternalCommandNode::asImmutable)
                .collect(Collectors.toSet());

        return input.internal().gen(
                argument,
                (consumed, arg, remaining) -> new NoSuchCommandException(consumed, arg, remaining, alternatives)
        );
    }
}
