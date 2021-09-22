package grapefruit.command.dispatcher.registration;

import grapefruit.command.parameter.ParameterNode0;
import io.leangen.geantyref.TypeToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;

public record CommandRegistration<S>(@NotNull Object holder,
                                     @NotNull Method method,
                                     @NotNull List<ParameterNode0<S>> parameters,
                                     @Nullable String permission,
                                     @Nullable TypeToken<?> commandSourceType,
                                     boolean runAsync) {
}
