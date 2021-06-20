package grapefruit.command.parameter.resolver;

import grapefruit.command.parameter.CommandParameter;
import io.leangen.geantyref.TypeToken;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Queue;

public interface ParameterResolver<S, T> {

    @NotNull TypeToken<T> type();

    @NotNull T resolve(final @NotNull S source,
                       final @NotNull Queue<String> args,
                       final @NotNull CommandParameter param)
            throws NoInputProvidedException, ParameterResolutionException;

    default @NotNull List<String> listSuggestions(final @NotNull S source,
                                                  final @NotNull String currentArg,
                                                  final @NotNull CommandParameter param) {
        return List.of();
    }
}
