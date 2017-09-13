package io.drakon.spark.autorouter.test.dispatch.except;

import io.drakon.spark.autorouter.Routes;
import spark.Request;
import spark.Response;

public class Sample {

    public static boolean tripped = false;

    @Routes.ExceptionHandler(exceptionType = Exception.class)
    public static Object sample(Exception ex, Request req, Response res) {
        tripped = true;
        return null;
    }

}
