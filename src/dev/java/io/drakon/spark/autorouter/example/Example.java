package io.drakon.spark.autorouter.example;

import io.drakon.spark.autorouter.Autorouter;
import io.drakon.spark.autorouter.Routes;
import spark.Request;
import spark.Response;

@Routes.PathGroup(prefix = "/example")
public class Example {

    public static void main(String[] argv) {
        Autorouter autorouter = new Autorouter("io.drakon.spark.autorouter.example");
        autorouter.route();
        autorouter.enableRouteOverview("/debug");
    }

    @Routes.GET(path = "/get")
    public static Object get(Request req, Response res) {
        return null;
    }

    @Routes.Before
    public static Object before(Request req, Response res) {
        return null;
    }

    @Routes.After
    public static Object after(Request req, Response res) {
        return null;
    }

    @Routes.AfterAfter
    public static Object afterAfter(Request req, Response res) {
        return null;
    }

    @Routes.ExceptionHandler(exceptionType = Exception.class)
    public static Object except(Exception ex, Request req, Response res) {
        return null;
    }

}
