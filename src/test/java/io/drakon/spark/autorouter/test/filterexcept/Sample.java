package io.drakon.spark.autorouter.test.filterexcept;

import io.drakon.spark.autorouter.Routes;
import spark.Request;
import spark.Response;

import static spark.Spark.*;

public class Sample {

    @Routes.Before
    public static Object beforeOne(Request req, Response res) {
        throw halt(500);
    }

    @Routes.After
    public static Object afterOne(Request req, Response res) {
        throw halt(500);
    }

    @Routes.AfterAfter
    public static Object afterTwo(Request req, Response res) {
        throw halt(500);
    }

    @Routes.ExceptionHandler(exceptionType = Exception.class)
    public static Object except(Exception ex, Request req, Response res) {
        throw halt(500);
    }

}
