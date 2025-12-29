package com.appalanche.backend.applications.business.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.time.ZoneId;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The annotated element can be {@code null} but if it isn't, it must be a recognized timezone TZ identifier (e.g. America/Toronto).
 *
 * @author Andrej Zivkovic
 * @see ZoneId#getAvailableZoneIds()
 * @since 1.0
 */
@Documented
@Constraint(validatedBy = TimezoneValidator.class)
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
public @interface ValidTimezone {
    String message() default "Invalid Timezone TZ identifier";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
