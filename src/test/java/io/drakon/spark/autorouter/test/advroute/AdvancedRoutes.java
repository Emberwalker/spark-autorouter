package io.drakon.spark.autorouter.test.advroute;

import io.drakon.spark.autorouter.Routes;
import spark.Request;
import spark.Response;
import spark.ResponseTransformer;

import static spark.Spark.halt;

public class AdvancedRoutes {

    @Routes.GET(path = "/get", acceptType = "application/json", transformer = CustomTransformer.class)
    @Routes.POST(path = "/post", acceptType = "application/json", transformer = CustomTransformer.class)
    @Routes.PATCH(path = "/patch", acceptType = "application/json", transformer = CustomTransformer.class)
    @Routes.PUT(path = "/put", acceptType = "application/json", transformer = CustomTransformer.class)
    @Routes.HEAD(path = "/head", acceptType = "application/json", transformer = CustomTransformer.class)
    @Routes.OPTIONS(path = "/options", acceptType = "application/json", transformer = CustomTransformer.class)
    @Routes.DELETE(path = "/delete", acceptType = "application/json", transformer = CustomTransformer.class)
    @Routes.CONNECT(path = "/connect", acceptType = "application/json", transformer = CustomTransformer.class)
    @Routes.TRACE(path = "/trace", acceptType = "application/json", transformer = CustomTransformer.class)
    public static Object theThing(Request req, Response res) {
        throw halt(500);
    }

    public static class CustomTransformer implements ResponseTransformer {
        @Override
        public String render(Object model) throws Exception {
            return model.toString().toUpperCase();
        }
    }

}
