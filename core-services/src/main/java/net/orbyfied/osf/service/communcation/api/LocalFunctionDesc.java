package net.orbyfied.osf.service.communcation.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Descriptor for methods which handle
 * a specific local function.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LocalFunctionDesc {

    /**
     * The function name.
     */
    String name();

}
