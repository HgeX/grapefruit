package grapefruit.command.dispatcher;

import grapefruit.command.dispatcher.input.StringReader;
import grapefruit.command.dispatcher.input.StringReaderImpl;
import grapefruit.command.util.Registry;
import grapefruit.command.util.key.Key;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static grapefruit.command.dispatcher.InternalContextKeys.INPUT;
import static grapefruit.command.dispatcher.InternalContextKeys.PARSE_INFO;
import static java.util.Objects.requireNonNull;

/**
 * Stores data related to the current command execution. This data
 * mainly consists of parsed values of command arguments, but
 * any value can be stored with a {@link Key key} instance. Command
 * handler functions are able to retrieve these values.
 */
public class CommandContext {
    private final Registry<Key<?>, Object> internalStore = Registry.create(Registry.DuplicateStrategy.replace());

    /**
     * Finds and returns the value associated with the
     * provided key, wrapped in an {@link Optional}. If
     * no value is mapped to the key, {@link Optional#empty()}
     * is returned.
     *
     * @param <T> The expected argument type
     * @param key The key
     * @return The found value, or an empty optional
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(Key<T> key) {
        requireNonNull(key, "key cannot be null");
        return (Optional<T>) this.internalStore.get(key);
    }

    /**
     * Finds and returns the value associated with the
     * provided key, or throws an {@link IllegalArgumentException},
     * if nothing is mapped to the key.
     *
     * @param <T> The expected argument type
     * @param key The key
     * @return The found value
     * @throws IllegalArgumentException If nothing is mapped to the key
     */
    public <T> T require(Key<T> key) {
        requireNonNull(key, "key cannot be null");
        // TODO probably need to throw a different exception. Maybe
        return get(key).orElseThrow(() -> new IllegalArgumentException("Nothing is mapped to key '%s'".formatted(key)));
    }

    /**
     * Find and returns the value associated with the
     * provided key, or null if nothing is mapped to
     * the key.
     *
     * @param <T> The expected argument type
     * @param key The key
     * @return The value, or null
     */
    public <T> @Nullable T nullable(Key<T> key) {
        requireNonNull(key, "key cannot be null");
        return get(key).orElse(null);
    }

    /**
     * Checks whether the provided key is contained in
     * {@link this#internalStore}.
     *
     * @param key The key to check
     * @return Whether the key is contained by this
     * context
     */
    public boolean contains(Key<?> key) {
        return this.internalStore.has(requireNonNull(key, "key cannot be null"));
    }

    /**
     * Stores the provided value in the internal map
     * by the provided key.
     *
     * @param <T> The type of the value
     * @param key The key
     * @param value The value
     * @param replace Whether to replace potentially
     *                existin values.
     * @throws IllegalStateException If the key is already
     * in the map and 'replace' is set to false
     */
    public <T> void put(Key<T> key, T value, boolean replace) {
        if (contains(key) && !replace) throw new IllegalStateException("A value is already associated with key '%s'".formatted(key));
        this.internalStore.store(key, value);
    }

    /**
     * @see this#put(Key, Object, boolean)
     */
    public <T> void put(Key<T> key, T value) {
        put(key, value, false);
    }

    /**
     * Removes and returns the value mapped to the
     * provided key, or null, if no value was mapped
     * to the key.
     *
     * @param <T> The expected type
     * @param key The key
     * @return The remove value, or null
     */
    @SuppressWarnings("unchecked")
    public <T> T remove(Key<T> key) {
        return (T) this.internalStore.remove(key);
    }

    /**
     * Returns an immutable map view of {@link this#internalStore}.
     *
     * @return The map view of the stored arguments
     */
    public Map<Key<?>, Object> asMap() {
        return this.internalStore.asImmutableMap();
    }

    /**
     * Creates a new {@link StringReader} wrapping the provided
     * user input, stores it in {@link this#internalStore} mapped to
     * {@link InternalContextKeys#INPUT} and returns it.
     * @see this#create(Key, Supplier)
     *
     * @param commandLine The command line to wrap
     * @return The created reader
     */
    public StringReader createReader(String commandLine) {
        return create(INPUT, () -> new StringReaderImpl(commandLine, this));
    }

    /**
     * Creates a new {@link ParseInfo} instance, stores if
     * in {@link this#internalStore} mapped to {@link InternalContextKeys#PARSE_INFO}
     * and returns it.
     * @see this#create(Key, Supplier)
     *
     * @return The created parse info
     */
    public ParseInfo createParseInfo() {
        return create(PARSE_INFO, ParseInfo::new);
    }

    /**
     * Creates and stores an instance generated by the provided
     * supplier mapped to the provided key.
     *
     * @param <T> The type of the instance to sore
     * @param key The key to map the instance to
     * @param factory The factory to generate the instance from
     * @return The generated instance
     */
    private <T> T create(Key<T> key, Supplier<T> factory) {
        T instance = factory.get();
        this.internalStore.store(key, instance);
        return instance;
    }
}
