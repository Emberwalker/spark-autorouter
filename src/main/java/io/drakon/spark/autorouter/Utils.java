package io.drakon.spark.autorouter;

import java.lang.annotation.Annotation;
import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.ResponseTransformer;

/** Misc. utilities. */
@ParametersAreNonnullByDefault
class Utils {

    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    private Utils() {} // Statics

    /**
     * Convenience class to handle paired data without resorting to JavaFX.
     *
     * @param <A> Type of first entry
     * @param <B> Type of second entry
     */
    static class Pair<A, B> {
        public final A first;
        public final B second;

        public Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }
    }

    /**
     * Extracts the 'path' value from a route annotation.
     * @param ann The annotation.
     * @return The 'path' field or null on error.
     */
    static String getRoutePathFromAnnotation(Annotation ann) {
        return getValueFromMethod(ann, "path");
    }

    /**
     * Extracts the 'acceptType' value from a route annotation.
     * @param ann The annotation.
     * @return The 'acceptType' field or null on error.
     */
    static String getRouteAcceptTypeFromAnnotation(Annotation ann) {
        return getValueFromMethod(ann, "acceptType");
    }

    /**
     * Extracts the 'transformer' value from a route annotation.
     * @param ann The annotation.
     * @return The 'transformer' field or null on error.
     */
    static Class<? extends ResponseTransformer> getRouteTransformerFromAnnotation(Annotation ann) {
        return getValueFromMethod(ann, "transformer");
    }

    /**
     * Generates a dispatcher object from a template class (from the code generator).
     *
     * @param cls The dispatcher class object from BytecodeDispatch.
     * @param <T> The dispatcher interface type.
     * @return The created object (or null in case of error)
     */
    static <T> T dispatchClassToObj(Class<T> cls) {
        try {
            return cls.newInstance();
        } catch (InstantiationException|IllegalAccessException ex) {
            log.error("Error instantiating dispatcher object.", ex);
            return null;
        }
    }

    /**
     * Pulls values from an annotation by method name.
     *
     * @param obj The annotation itself.
     * @param method The method/field name to pull.
     * @param <T> The annotation type.
     * @param <S> The return value type.
     * @return The return value of the given method/field.
     */
    private static <T extends Annotation, S> S getValueFromMethod(T obj, String method) {
        try {
            //noinspection unchecked
            return (S) obj.getClass().getMethod(method).invoke(obj);
        } catch (ReflectiveOperationException ex) {
            log.error("Passed annotation is not a valid Route annotation! Class {}, object {}.", obj.getClass(), obj);
            return null;
        } catch (ClassCastException ex) {
            log.error("Wrong type given for return value from method {} on {}", method, obj);
            return null;
        }
    }

    // For dealing with routing/before/after
    /**
     * A minimal implementation of the Functional consumer for three parameters.
     *
     * @param <A> First param type.
     * @param <B> Second param type.
     * @param <C> Third param type.
     */
    @FunctionalInterface
    public interface TriConsumer<A,B,C> {
        void apply(A a, B b, C c);
    }

    /**
     * A minimal implementation of the Functional consumer for four parameters.
     *
     * @param <A> First param type.
     * @param <B> Second param type.
     * @param <C> Third param type.
     * @param <D> Fourth param type.
     */
    @FunctionalInterface
    public interface QuadConsumer<A,B,C,D> {
        void apply(A a, B b, C c, D d);
    }

}
