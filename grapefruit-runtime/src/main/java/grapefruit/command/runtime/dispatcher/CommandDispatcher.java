package grapefruit.command.runtime.dispatcher;

import grapefruit.command.runtime.CommandException;
import grapefruit.command.runtime.dispatcher.config.DispatcherConfigurer;
import grapefruit.command.runtime.generated.CommandContainer;
import grapefruit.command.runtime.generated.CommandMirror;

import java.util.List;

public interface CommandDispatcher {

    void register(Iterable<CommandMirror> commands);

    default void register(CommandContainer container) {
        register(container.commands());
    }

    void unregister(Iterable<CommandMirror> commands);

    default void unregister(CommandContainer container) {
        unregister(container.commands());
    }

    void dispatch(CommandContext context, String commandLine) throws CommandException;

    List<String> complete(CommandContext context, String commandLine);

    static CommandDispatcher using(DispatcherConfigurer... configurers) {
        return new CommandDispatcherImpl(DispatcherConfigurer.merge(configurers));
    }
}