package grapefruit.command.dispatcher;

import grapefruit.command.util.Registry;
import grapefruit.command.util.key.Key;

import java.util.Optional;

public class CommandContext {
    private final Registry<Key<?>, Object> internalStore = Registry.create();

    // TODO
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getSafe(Key<T> key) {
        return (Optional<T>) this.internalStore.get(key) ;
    }

    // TODO
    public <T> T get(Key<T> key) {
        return getSafe(key).orElseThrow();
    }

    public boolean has(Key<?> key) {
        return this.internalStore.has(key);
    }

    public <T> void store(Key<T> key, T instance) {
        if (this.internalStore.has(key)) throw new IllegalStateException("Key '%s' is already stored in this context".formatted(key));
        this.internalStore.store(key, instance);
    }
}
