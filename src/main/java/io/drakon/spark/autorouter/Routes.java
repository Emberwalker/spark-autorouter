package io.drakon.spark.autorouter;

import spark.ResponseTransformer;

import java.lang.annotation.*;

public class Routes {

    static final String NULL_STR = "null";
    static final Class<? extends ResponseTransformer> NULL_TRANSFORMER = ResponseTransformer.class;

    private Routes() {} // Static only

    /*
     * ========== Spark Helpers ============
     */

    /**
     * Marks all routes in this class as being in this path. Equivalent to the path() call in Spark. See
     * http://sparkjava.com/documentation#routes Path Groups.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface PathGroup {
        /** The prefix for routes in this class. */
        String prefix();
    }

    /*
     * ========== Filters/Events ===========
     */

    /** Define a filter which runs before routes. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Before {
        /** Optional: Restrict this filter to this path. */
        String path() default NULL_STR;
        /** Optional: Accept Type to restrict this filter to. */
        String acceptType() default NULL_STR;
    }

    /** Define a filter which runs after routes. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface After {
        /** Optional: Restrict this filter to this path. */
        String path() default NULL_STR;
        /** Optional: Accept Type to restrict this filter to. */
        String acceptType() default NULL_STR;
    }

    /** Define a filter which runs after after filters (yep, really). */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface AfterAfter {
        /** Optional: Restrict this filter to this path. */
        String path() default NULL_STR;
    }

    /** Define an exception handler. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ExceptionHandler {
        /** The exception type to handle with this route. */
        Class<? extends Exception> exceptionType();
    }


    /*
     * ========== HTTP Verbs ===============
     */

    /** Define this method as a GET route. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface GET {
        /** The path this route accepts in standard Spark notation. */
        String path();
        /** Optional: the accept type for the route. */
        String acceptType() default NULL_STR;
        /** Optional: The transformer class to apply. Must be a class to work around an annotations restriction. */
        Class<? extends ResponseTransformer> transformer() default ResponseTransformer.class;
    }

    /** Define this method as a POST route. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface POST {
        /** The path this route accepts in standard Spark notation. */
        String path();
        /** Optional: the accept type for the route. */
        String acceptType() default NULL_STR;
        /** Optional: The transformer class to apply. Must be a class to work around an annotations restriction. */
        Class<? extends ResponseTransformer> transformer() default ResponseTransformer.class;
    }

    /** Define this method as a PATCH route. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface PATCH {
        /** The path this route accepts in standard Spark notation. */
        String path();
        /** Optional: the accept type for the route. */
        String acceptType() default NULL_STR;
        /** Optional: The transformer class to apply. Must be a class to work around an annotations restriction. */
        Class<? extends ResponseTransformer> transformer() default ResponseTransformer.class;
    }

    /** Define this method as a PUT route. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface PUT {
        /** The path this route accepts in standard Spark notation. */
        String path();
        /** Optional: the accept type for the route. */
        String acceptType() default NULL_STR;
        /** Optional: The transformer class to apply. Must be a class to work around an annotations restriction. */
        Class<? extends ResponseTransformer> transformer() default ResponseTransformer.class;
    }

    /** Define this method as a HEAD route. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface HEAD {
        /** The path this route accepts in standard Spark notation. */
        String path();
        /** Optional: the accept type for the route. */
        String acceptType() default NULL_STR;
        /** Optional: The transformer class to apply. Must be a class to work around an annotations restriction. */
        Class<? extends ResponseTransformer> transformer() default ResponseTransformer.class;
    }

    /** Define this method as a OPTIONS route. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface OPTIONS {
        /** The path this route accepts in standard Spark notation. */
        String path();
        /** Optional: the accept type for the route. */
        String acceptType() default NULL_STR;
        /** Optional: The transformer class to apply. Must be a class to work around an annotations restriction. */
        Class<? extends ResponseTransformer> transformer() default ResponseTransformer.class;
    }

    /** Define this method as a DELETE route. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface DELETE {
        /** The path this route accepts in standard Spark notation. */
        String path();
        /** Optional: the accept type for the route. */
        String acceptType() default NULL_STR;
        /** Optional: The transformer class to apply. Must be a class to work around an annotations restriction. */
        Class<? extends ResponseTransformer> transformer() default ResponseTransformer.class;
    }

    /** Define this method as a CONNECT route. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface CONNECT {
        /** The path this route accepts in standard Spark notation. */
        String path();
        /** Optional: the accept type for the route. */
        String acceptType() default NULL_STR;
        /** Optional: The transformer class to apply. Must be a class to work around an annotations restriction. */
        Class<? extends ResponseTransformer> transformer() default ResponseTransformer.class;
    }

    /** Define this method as a TRACE route. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface TRACE {
        /** The path this route accepts in standard Spark notation. */
        String path();
        /** Optional: the accept type for the route. */
        String acceptType() default NULL_STR;
        /** Optional: The transformer class to apply. Must be a class to work around an annotations restriction. */
        Class<? extends ResponseTransformer> transformer() default ResponseTransformer.class;
    }

}
