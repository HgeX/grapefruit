package grapefruit.command.dispatcher;

import grapefruit.command.argument.CommandArgument;
import grapefruit.command.argument.CommandChain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface CommandParseResult<S> {

    /*
     * The last input that was being parsed (whether the
     * parsing was successful does not matter here).
     */
    Optional<String> lastInput();

    /*
     * The last argument that was being parsed.
     */
    Optional<CommandArgument.Dynamic<S, ?>> lastArgument();

    /*
     * A list of arguments that were not consumed yet.
     */
    List<CommandArgument.Required<S, ?>> remainingArguments();

    /*
     * A list of flags that were not consumed yet.
     */
    List<CommandArgument.Flag<S, ?>> remainingFlags();

    static <S> Builder<S> createBuilder(final CommandChain<S> chain) {
        // Make mutable copies
        return new CommandParseResultImpl.Builder<>(new ArrayList<>(chain.arguments()), new ArrayList<>(chain.flags()));
    }

    interface Builder<S> {

        void begin(final CommandArgument.Dynamic<S, ?> argument, final String input);

        void end();

        CommandParseResult<S> build();
    }
}
