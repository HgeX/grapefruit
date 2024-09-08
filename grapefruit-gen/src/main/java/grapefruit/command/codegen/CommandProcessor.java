package grapefruit.command.codegen;

import com.google.auto.common.MoreElements;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import grapefruit.command.annotation.CommandDefinition;
import grapefruit.command.codegen.generator.ContainerGenerator;
import grapefruit.command.codegen.generator.GeneratorContext;
import grapefruit.command.codegen.model.CommandDescriptor;
import grapefruit.command.codegen.model.FactoryDescriptor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.groupingBy;

@AutoService(Processor.class)
@SupportedAnnotationTypes("grapefruit.command.annotation.CommandDefinition")
public class CommandProcessor extends AbstractProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    // @Override
    public boolean _process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> commandMethods = roundEnv.getElementsAnnotatedWith(CommandDefinition.class);
        Map<TypeElement, List<CommandDescriptor>> knownCommands = commandMethods.stream()
                .map(CommandDescriptor::create)
                .collect(groupingBy(CommandDescriptor::parent));

        for (Map.Entry<TypeElement, List<CommandDescriptor>> entry : knownCommands.entrySet()) {
            FactoryDescriptor descriptor = new FactoryDescriptor(entry.getKey(), entry.getValue(), getClass());
            JavaFile file = descriptor.generateFile();

            try {
                file.writeTo(this.processingEnv.getFiler());
            } catch (IOException ex) {
                this.processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Failed to write Java file to: '%s'".formatted(file)
                );
            }
        }

        return true;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> commandMethods = roundEnv.getElementsAnnotatedWith(CommandDefinition.class);
        Map<TypeElement, List<ExecutableElement>> commandsByContainer = commandMethods.stream()
                .map(MoreElements::asExecutable) // Since @CommandDefinition is only allowed on methods, this is a safe cast
                .collect(groupingBy(x -> (TypeElement) x.getEnclosingElement())); // And that also means that the enclosing element is a class

        for (Map.Entry<TypeElement, List<ExecutableElement>> entry : commandsByContainer.entrySet()) {
            GeneratorContext context = new GeneratorContext(getClass().getCanonicalName());
            ContainerGenerator generator = new ContainerGenerator(entry.getKey(), entry.getValue());

            JavaFile file = generator.generate(context);
            try {
                file.writeTo(this.processingEnv.getFiler());
            } catch (IOException ex) {
                this.processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Failed to write Java file to '%s'.".formatted(file)
                );

                ex.printStackTrace();
            }
        }

        return true;
    }
}
