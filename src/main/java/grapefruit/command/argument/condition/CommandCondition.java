package grapefruit.command.argument.condition;

import grapefruit.command.dispatcher.CommandContext;

import java.util.List;

public interface CommandCondition<S> {

    void test(final CommandContext<S> context) throws UnfulfilledConditionException;

    default boolean isContextFree() {
        return false;
    }

    @SafeVarargs
    static <S> CommandCondition<S> and(final CommandCondition<S>... conditions) {
        return new AndCondition<>(List.of(conditions));
    }

    @SafeVarargs
    static <S> CommandCondition<S> or(final CommandCondition<S>... conditions) {
        return new OrCondition<>(List.of(conditions));
    }
}
