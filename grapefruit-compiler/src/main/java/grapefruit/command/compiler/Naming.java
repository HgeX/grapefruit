package grapefruit.command.compiler;

import javax.lang.model.element.ExecutableElement;
import java.util.function.Function;

public final class Naming {
    public static final Function<ExecutableElement, String> ARGUMENTS_METHOD_SUFFIX = method -> "%s$arguments".formatted(method.getSimpleName());
    public static final String COMMANDS_METHOD = "commands";
    public static final Function<String, String> CONTAINER_CLASS_SUFFIX = "%s_Container"::formatted;
    public static final String CONTEXT_PARAM = "context";
    public static final Function<ExecutableElement, String> HANDLER_METHOD_SUFFIX = method -> "%s$handler".formatted(method.getSimpleName());
    public static final String INTERNAL_COMMANDS_FIELD = "internalCommands";
    public static final String KEY_FIELD_SUFFIX = "_key";
    public static final String REFERENCE_PARAM = "reference";

    private Naming() {}
}