package grapefruit.command.argument.condition;

import java.util.List;

import static java.util.Objects.requireNonNull;

abstract class AbstractMultiCondition<S> implements CommandCondition<S> {
    protected final List<CommandCondition<S>> conditions;
    private final boolean contextFree;

    protected AbstractMultiCondition(final List<CommandCondition<S>> conditions) {
        this.conditions = requireNonNull(conditions, "conditions cannot be null");
        this.contextFree = requireSameContextDependency(conditions);
    }

    private static <S> boolean requireSameContextDependency(final List<CommandCondition<S>> conditions) {
        if (conditions.isEmpty()) throw new IllegalArgumentException("No conditions were provided");

        final boolean contextFree = conditions.getFirst().isContextFree();
        for (final CommandCondition<S> condition : conditions) {
            if (condition.isContextFree() != contextFree) {
                throw new IllegalArgumentException("Cannot combine context free and context dependent conditions ('%s')".formatted(condition));
            }
        }

        return contextFree;
    }

    @Override
    public boolean isContextFree() {
        return this.contextFree;
    }
}
