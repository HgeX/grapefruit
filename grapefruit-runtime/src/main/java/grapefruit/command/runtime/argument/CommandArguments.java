package grapefruit.command.runtime.argument;

import grapefruit.command.runtime.argument.modifier.ModifierChain;
import grapefruit.command.runtime.util.key.Key;

import java.util.List;

/**
 * Utility class to easily create command arguments.
 */
public final class CommandArguments {

    private CommandArguments() {}

    /**
     * Creates a required command arguments.
     *
     * @param name The name of the argument
     * @param key The Key of the argument
     * @param mapperKey The mapperKey of the argument
     * @return The created argument
     */
    public static <T> CommandArgument<T> required(String name, Key<T> key, Key<T> mapperKey, ModifierChain<T> modifierChain) {
        return new CommandArgumentImpl.Required<>(name, key, mapperKey, modifierChain);
    }

    /**
     * Creates a new presence flag argument. Presence flags
     * always represent boolean values; the flag being set
     * equals to true, while omitting the flag results in
     * its value being set to false.
     *
     * @param name The name of the argument
     * @param shorthand The flag shorthand
     * @param key The key of the argument
     * @return the created argument
     */
    public static CommandArgument.Flag<Boolean> presenceFlag(String name, char shorthand, Key<Boolean> key) {
        // Modifiers aren't supported on presence flags
        return new CommandArgumentImpl.PresenceFlag(name, key, shorthand, ModifierChain.of(List.of()));
    }

    /**
     * Creates a new value flag argument.
     *
     * @param name The name of the argument
     * @param shorthand The flag shorthand
     * @param key The key of the argument
     * @param mapperKey The mapper key
     * @return The created argument
     */
    public static <T> CommandArgument.Flag<T> valueFlag(String name, char shorthand, Key<T> key, Key<T> mapperKey, ModifierChain<T> modifierChain) {
        return new CommandArgumentImpl.ValueFlag<>(name, key, mapperKey, shorthand, modifierChain);
    }
}
