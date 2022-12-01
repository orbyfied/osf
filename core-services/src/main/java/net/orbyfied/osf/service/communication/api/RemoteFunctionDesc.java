package net.orbyfied.osf.service.communication.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Descriptor for fields which should
 * have a remote function constructed.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RemoteFunctionDesc {

    /**
     * The name of the function.
     */
    String name();

    /**
     * If the call is expected to
     * give a response.
     */
    boolean response();

    /**
     * The return type.
     * Void if no response is expected.
     */
    Class<?> returnType() default Void.class;

}
