package grapefruit.command.parameter.mapper.builtin;

import grapefruit.command.dispatcher.CommandArgument;
import grapefruit.command.message.Message;
import grapefruit.command.message.MessageKeys;
import grapefruit.command.message.Template;
import grapefruit.command.parameter.CommandParameter;
import grapefruit.command.parameter.mapper.AbstractParamterMapper;
import grapefruit.command.parameter.mapper.ParameterMappingException;
import io.leangen.geantyref.TypeToken;
import org.jetbrains.annotations.NotNull;

import java.util.Queue;

public class CharacterMapper<S> extends AbstractParamterMapper<S, Character> {

    public CharacterMapper() {
        super(TypeToken.get(Character.class));
    }

    @Override
    public @NotNull Character map(final @NotNull S source,
                                  final @NotNull Queue<CommandArgument> args,
                                  final @NotNull CommandParameter param) throws ParameterMappingException {
        final String input = args.element().rawArg();
        if (input.length() != 1) {
            throw new ParameterMappingException(Message.of(
                    MessageKeys.INVALID_CHARACTER_VALUE,
                    Template.of("{input}", input)
            ), param);
        }

        return input.charAt(0);
    }
}
