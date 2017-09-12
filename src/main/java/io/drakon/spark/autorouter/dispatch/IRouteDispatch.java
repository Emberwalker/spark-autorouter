package io.drakon.spark.autorouter.dispatch;

import spark.Request;
import spark.Response;

public interface IRouteDispatch {

    Object dispatch(Request req, Response res);

}
