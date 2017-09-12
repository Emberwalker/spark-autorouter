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

    private static <T, S> S getValueFromMethod(T obj, String method) {
        try {
            return (S) obj.getClass().getMethod(method).invoke(obj);
        } catch (ReflectiveOperationException ex) {
            log.error("Passed annotation is not a valid Route annotation! Class {}, object {}.", obj.getClass(), obj);
            return null;
        } catch (ClassCastException ex) {
            log.error("Wrong type given for return value from method {} on {}", method, obj);
            return null;
        }
    }

}
