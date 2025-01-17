package grapefruit.command.argument.condition;

import grapefruit.command.dispatcher.CommandContext;

import java.util.List;

final class OrCondition<S> extends AbstractMultiCondition<S> {

    OrCondition(final List<CommandCondition<S>> conditions) {
        super(conditions);
    }

    @Override
    public void test(final CommandContext<S> context) throws UnfulfilledConditionException {
        UnfulfilledConditionException captured = null;
        boolean success = false;
        for (final CommandCondition<S> condition : this.conditions) {
            try {
                condition.test(context);
                success = true;
            } catch (final UnfulfilledConditionException ex) {
                captured = ex;
            }
        }

        if (!success && captured != null) throw captured;
    }
}
