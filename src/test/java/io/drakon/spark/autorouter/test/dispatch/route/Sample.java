package io.drakon.spark.autorouter.test.dispatch.route;

import io.drakon.spark.autorouter.Routes;
import spark.Request;
import spark.Response;

public class Sample {

    public static boolean tripped = false;

    @Routes.GET(path = "/")
    public static Object sample(Request req, Response res) {
        tripped = true;
        return null;
    }

    @Routes.GET(path = "/")
    public static Object sampleTwo(Request req, Response res) {
        tripped = true;
        return null;
    }

}
