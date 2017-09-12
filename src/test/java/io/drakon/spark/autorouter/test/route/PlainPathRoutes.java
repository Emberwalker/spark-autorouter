package io.drakon.spark.autorouter.test.route;

import io.drakon.spark.autorouter.Routes;
import spark.Request;
import spark.Response;

import static spark.Spark.halt;

public class PlainPathRoutes {

    @Routes.GET(path = "/get")
    @Routes.POST(path = "/post")
    @Routes.PATCH(path = "/patch")
    @Routes.PUT(path = "/put")
    @Routes.HEAD(path = "/head")
    @Routes.OPTIONS(path = "/options")
    @Routes.DELETE(path = "/delete")
    @Routes.CONNECT(path = "/connect")
    @Routes.TRACE(path = "/trace")
    public static Object theThing(Request req, Response res) {
        throw halt(500);
    }

}
