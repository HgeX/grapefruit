package grapefruit.command.argument;

import grapefruit.command.argument.condition.CommandCondition;
import grapefruit.command.argument.mapper.ArgumentMapper;
import grapefruit.command.util.key.Keyed;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface CommandArgument<S, T> extends Keyed<T> {

    String name();

    List<CommandCondition<S>> conditions();

    interface Builder<S, T, C extends CommandArgument<S, T>, B extends Builder<S, T, C, B>> {

        B expect(final CommandCondition<S> condition);

        B expect(final Collection<CommandCondition<S>> conditions);

        C build();
    }

    interface Literal<S> extends CommandArgument<S, String> {

        Set<String> aliases();

        interface Builder<S> extends CommandArgument.Builder<S, String, Literal<S>, Builder<S>> {

            Builder<S> aliases(final String... aliases);
        }
    }

    interface Dynamic<S, T> extends CommandArgument<S, T> {

        ArgumentMapper<S, T> mapper();

        boolean isFlag();

        Flag<S, T> asFlag();
    }

    interface Required<S, T> extends Dynamic<S, T> {

        interface Builder<S, T> extends CommandArgument.Builder<S, T, Required<S, T>, Builder<S, T>> {

            Builder<S, T> mapWith(final ArgumentMapper<S, T> mapper);
        }
    }

    interface Flag<S, T> extends Dynamic<S, T> {

        char shorthand();

        ArgumentMapper<S, T> mapper();

        boolean isPresence();

        interface Builder<S, T, B extends Builder<S, T, B>> extends CommandArgument.Builder<S, T, Flag<S, T>, Builder<S, T, B>> {

            B shorthand(final char shorthand);

            B assumeShorthand();
        }

        interface PresenceBuilder<S> extends Builder<S, Boolean, PresenceBuilder<S>> {}

        interface ValueBuilder<S, T> extends Builder<S, T, ValueBuilder<S, T>> {

            ValueBuilder<S, T> mapWith(final ArgumentMapper<S, T> mapper);
        }
    }
}
