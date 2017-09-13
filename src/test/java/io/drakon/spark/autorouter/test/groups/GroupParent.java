package io.drakon.spark.autorouter.test.groups;

import io.drakon.spark.autorouter.Routes;
import spark.Request;
import spark.Response;

@Routes.PathGroup(prefix = "/parent")
public class GroupParent {

    @Routes.PathGroup(prefix = "/child")
    public static class GroupChild {

        @Routes.GET(path = "/")
        public static Object childRoute(Request req, Response res) {
            return null;
        }

    }

    @Routes.GET(path = "/")
    public static Object parentRoute(Request req, Response res) {
        return null;
    }

}
