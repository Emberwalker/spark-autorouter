package io.drakon.spark.autorouter.dispatch;

import spark.Request;
import spark.Response;

public interface IExceptionDispatch {

    Object dispatch(Exception ex, Request req, Response res);

}
