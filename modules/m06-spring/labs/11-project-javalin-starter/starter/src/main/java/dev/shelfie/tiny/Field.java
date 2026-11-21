package dev.shelfie.tiny;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The method resolves field `name` (default: the method's name) of type `type`. For Query and Mutation
 * fields every parameter is an @Arg or @Context; for other types the first parameter is the parent object.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Field {
    String type();
    String name() default "";
}
