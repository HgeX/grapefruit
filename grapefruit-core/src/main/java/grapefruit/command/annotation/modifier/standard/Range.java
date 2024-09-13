package grapefruit.command.annotation.modifier.standard;

import grapefruit.command.annotation.modifier.Modifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// TODO
// @Modifier
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Range {

    double min();

    double max();
}
