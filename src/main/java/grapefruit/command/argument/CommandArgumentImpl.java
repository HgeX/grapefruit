package grapefruit.command.argument;

import grapefruit.command.argument.condition.CommandCondition;
import grapefruit.command.argument.mapper.AbstractArgumentMapper;
import grapefruit.command.argument.mapper.ArgumentMapper;
import grapefruit.command.dispatcher.CommandContext;
import grapefruit.command.dispatcher.input.CommandInputTokenizer;
import grapefruit.command.util.ToStringer;
import grapefruit.command.util.key.Key;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public abstract class CommandArgumentImpl<S, T> implements CommandArgument<S, T> {
    private final Key<T> key;
    private final List<CommandCondition<S>> conditions;

    protected CommandArgumentImpl(final Key<T> key, final List<CommandCondition<S>> conditions) {
        this.key = requireNonNull(key, "key cannot be null");
        this.conditions = requireNonNull(conditions, "conditions cannot be null");
    }

    @Override
    public Key<T> key() {
        return this.key;
    }

    @Override
    public String name() {
        return this.key.name();
    }

    @Override
    public List<CommandCondition<S>> conditions() {
        return List.copyOf(this.conditions);
    }

    static final class Literal<S> extends CommandArgumentImpl<S, String> implements CommandArgument.Literal<S> {
        private final Set<String> aliases;

        Literal(final Key<String> key, final List<CommandCondition<S>> conditions, final Set<String> aliases) {
            super(key, conditions);
            this.aliases = requireNonNull(aliases, "aliases cannot be null");
        }

        @Override
        public Set<String> aliases() {
            return Set.copyOf(this.aliases);
        }

        @Override
        public String toString() {
            return ToStringer.create(this)
                    .append("key", key())
                    .append("permission", conditions())
                    .append("aliases", this.aliases)
                    .toString();
        }
    }

    static abstract class Dynamic<S, T> extends CommandArgumentImpl<S, T> implements CommandArgument.Dynamic<S, T> {
        private final ArgumentMapper<S, T> mapper;

        Dynamic(final Key<T> key, final List<CommandCondition<S>> conditions, final ArgumentMapper<S, T> mapper) {
            super(key, conditions);
            this.mapper = requireNonNull(mapper, "mapper cannot be null");
        }

        @Override
        public ArgumentMapper<S, T> mapper() {
            return this.mapper;
        }
    }

    static final class Required<S, T> extends Dynamic<S, T> implements CommandArgument.Required<S, T> {

        Required(final Key<T> key, List<CommandCondition<S>> conditions, final ArgumentMapper<S, T> mapper) {
            super(key, conditions, mapper);
        }

        @Override
        public boolean isFlag() {
            return false;
        }

        @Override
        public CommandArgument.Flag<S, T> asFlag() {
            throw new UnsupportedOperationException("Attempted to cast required argument to flag");
        }

        @Override
        public String toString() {
            return ToStringer.create(this)
                    .append("key", key())
                    .append("permission", conditions())
                    .append("mapper", mapper())
                    .toString();
        }
    }

    static final class Flag<S, T> extends Dynamic<S, T> implements CommandArgument.Flag<S, T> {
        private final char shorthand;
        private final boolean isPresence;

        Flag(final Key<T> key, List<CommandCondition<S>> conditions, final ArgumentMapper<S, T> mapper, final char shorthand, final boolean isPresence) {
            super(key, conditions, mapper);
            this.shorthand = shorthand;
            this.isPresence = isPresence;
        }

        @Override
        public boolean isFlag() {
            return true;
        }

        @Override
        public CommandArgument.Flag<S, T> asFlag() {
            return this;
        }

        @Override
        public char shorthand() {
            return this.shorthand;
        }

        @Override
        public boolean isPresence() {
            return this.isPresence;
        }

        @Override
        public String toString() {
            return ToStringer.create(this)
                    .append("key", key())
                    .append("permission", conditions())
                    .append("mapper", mapper())
                    .append("shorthand", this.shorthand)
                    .append("presence", this.isPresence)
                    .toString();
        }
    }

    static abstract class Builder<S, T, C extends CommandArgument<S, T>, B extends CommandArgument.Builder<S, T, C, B>> implements CommandArgument.Builder<S, T, C, B> {
        protected final Key<T> key;
        protected final List<CommandCondition<S>> conditions = new ArrayList<>();

        Builder(final Key<T> key) {
            this.key = requireNonNull(key, "key cannot be null");
        }

        protected abstract B self();

        @Override
        public B expect(final CommandCondition<S> condition) {
            requireNonNull(condition, "condition cannot be null");
            this.conditions.add(condition);
            return self();
        }

        @Override
        public B expect(final Collection<CommandCondition<S>> conditions) {
            requireNonNull(conditions, "conditions cannot be null");
            this.conditions.addAll(conditions);
            return self();
        }
    }

    static final class LiteralBuilder<S> extends Builder<S, String, CommandArgument.Literal<S>, CommandArgument.Literal.Builder<S>> implements CommandArgument.Literal.Builder<S> {
        private final Set<String> aliases = new HashSet<>();

        LiteralBuilder(final Key<String> key) {
            super(key);
        }

        @Override
        protected LiteralBuilder<S> self() {
            return this;
        }

        @Override
        public CommandArgument.Literal.Builder<S> aliases(final String... aliases) {
            this.aliases.addAll(Arrays.asList(aliases));
            return this;
        }

        @Override
        public CommandArgument.Literal<S> build() {
            return new Literal<>(this.key, this.conditions, this.aliases);
        }
    }

    static final class RequiredBuilder<S, T> extends Builder<S, T, CommandArgument.Required<S, T>, CommandArgument.Required.Builder<S, T>> implements CommandArgument.Required.Builder<S, T> {
        private ArgumentMapper<S, T> mapper;

        RequiredBuilder(final Key<T> key) {
            super(key);
        }

        @Override
        protected CommandArgument.Required.Builder<S, T> self() {
            return this;
        }

        @Override
        public CommandArgument.Required.Builder<S, T> mapWith(final ArgumentMapper<S, T> mapper) {
            this.mapper = requireNonNull(mapper, "mapper cannot be null");
            return this;
        }

        @Override
        public CommandArgument.Required<S, T> build() {
            return new Required<>(this.key, this.conditions, this.mapper);
        }
    }

    static abstract class FlagBuilder<S, T, B extends CommandArgument.Flag.Builder<S, T, B>>
            extends Builder<S, T, CommandArgument.Flag<S, T>, CommandArgument.Flag.Builder<S, T, B>>
            implements CommandArgument.Flag.Builder<S, T, B> {
        private final boolean isPresence;
        private char shorthand;
        protected ArgumentMapper<S, T> mapper;

        FlagBuilder(final Key<T> key, final boolean isPresence) {
            super(key);
            this.isPresence = isPresence;
        }

        @Override
        protected abstract B self();

        @Override
        public B shorthand(final char shorthand) {
            this.shorthand = shorthand;
            return self();
        }

        @Override
        public B assumeShorthand() {
            this.shorthand = this.key.name().charAt(0);
            return self();
        }

        @Override
        public CommandArgument.Flag<S, T> build() {
            return new Flag<>(this.key, this.conditions, this.mapper, this.shorthand, this.isPresence);
        }
    }

    static final class ValueFlagBuilder<S, T> extends FlagBuilder<S, T, CommandArgument.Flag.ValueBuilder<S, T>> implements CommandArgument.Flag.ValueBuilder<S, T> {

        ValueFlagBuilder(final Key<T> key) {
            super(key, false);
        }

        @Override
        protected CommandArgument.Flag.ValueBuilder<S, T> self() {
            return this;
        }

        @Override
        public CommandArgument.Flag.ValueBuilder<S, T> mapWith(final ArgumentMapper<S, T> mapper) {
            this.mapper = requireNonNull(mapper, "mapper cannot be null");
            return this;
        }
    }

    static final class PresenceFlagBuilder<S> extends FlagBuilder<S, Boolean, CommandArgument.Flag.PresenceBuilder<S>> implements CommandArgument.Flag.PresenceBuilder<S> {

        PresenceFlagBuilder(final Key<Boolean> key) {
            super(key, true);
            this.mapper = new PresenceFlagMapper<>();
        }

        @Override
        protected CommandArgument.Flag.PresenceBuilder<S> self() {
            return this;
        }
    }

    private static final class PresenceFlagMapper<S> extends AbstractArgumentMapper<S, Boolean> {
        private PresenceFlagMapper() {
            super(Boolean.class, false);
        }

        @Override
        public Boolean tryMap(final CommandContext<S> context, final CommandInputTokenizer input) {
            // Presence flags always return true if set.
            return true;
        }
    }
}
