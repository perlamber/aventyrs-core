package org.aventyrs.core.modifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a no-arg, int-returning method as a source of a {@link ModifierType} bonus.
 * Any class can carry one of these — an ability, a feat, a title, an item — without
 * implementing any shared interface: {@link ModifierResolver} discovers and sums them
 * purely by scanning for this annotation, so a new bonus source never requires changing
 * whatever service consumes that ModifierType.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Modifier {
    ModifierType value();
}
