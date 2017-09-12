package io.drakon.spark.autorouter.tmp;

import io.drakon.spark.autorouter.Autorouter;
import io.drakon.spark.autorouter.Routes;
import spark.Request;
import spark.Response;
import spark.Spark;

public class Main {

    public static void main(String[] argv) {
        Autorouter autorouter = new Autorouter("io.drakon.spark.autorouter.tmp");
        autorouter.route();
        Spark.awaitInitialization();
    }

    @Routes.ExceptionHandler(exceptionType = Exception.class)
    public static Object exceptional(Exception ex, Request req, Response res) {
        System.out.println("Ping!");
        return null;
    }

    @Routes.GET(path = "/")
    public static String root(Request req, Response res) {
        return "Hullo world!";
    }

}
