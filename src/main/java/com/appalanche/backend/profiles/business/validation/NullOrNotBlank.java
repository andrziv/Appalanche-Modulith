package com.appalanche.backend.profiles.business.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The annotated element can be {@code null} but if it isn't, it must contain at least one non-whitespace character.
 *
 * @author Andrej Zivkovic
 * @see Character#isWhitespace(char)
 * @since 1.0
 */
@Documented
@Constraint(validatedBy = NullOrNotBlankValidator.class)
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface NullOrNotBlank {
    String message() default "Must not be blank";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
