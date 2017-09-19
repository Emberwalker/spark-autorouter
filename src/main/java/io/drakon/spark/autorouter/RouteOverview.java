package io.drakon.spark.autorouter;

import io.drakon.spark.autorouter.Utils.Pair;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.ResponseTransformer;
import spark.template.mustache.MustacheTemplateEngine;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

import static io.drakon.spark.autorouter.Routes.NULL_STR;
import static spark.Spark.halt;

/**
 * Implementation of route overview page.
 */
class RouteOverview {

    private final Autorouter router;

    RouteOverview(Autorouter router) {
        this.router = router;
    }

    Object route(Request req, Response res) {
        Autorouter.SearchResult result = router.lastSearch;
        if (result == null) throw halt(400, "Router not routed yet.");

        List<Row> routes = new ArrayList<>();
        result.routes.forEach((ann, info) -> {
            String verb = ann.getSimpleName();
            info.forEach(pair -> {
                String classMethod = pair.first.getDeclaringClass().getCanonicalName() + "#" + pair.first.getName();
                String path = pair.second.path;
                String accept = pair.second.acceptType;
                if (accept == null) accept = "*/*";
                ResponseTransformer transformer = pair.second.transformer;
                String transformerStr = "None";
                if (transformer != null) transformerStr = transformer.getClass().getCanonicalName();
                routes.add(new Row(classMethod, verb, path, accept, transformerStr));
            });
        });

        List<Row> beforeFilters = filtersToList(result.beforeFilters);
        List<Row> afterFilters = filtersToList(result.afterFilters);

        List<Row> afterAfterFilters = new ArrayList<>();
        result.afterAfterFilters.forEach(pair -> {
            String classMethod = pair.first.getDeclaringClass().getCanonicalName() + "#" + pair.first.getName();
            String path = "*";
            String annPath = pair.second.path();
            if (!annPath.equals(NULL_STR)) path = annPath;
            afterAfterFilters.add(new Row(classMethod, null, path, null, null));
        });

        List<Row> exceptionHandlers = new ArrayList<>();
        result.exceptionHandlers.forEach(pair -> {
            String classMethod = pair.first.getDeclaringClass().getCanonicalName() + "#" + pair.first.getName();
            String exceptionType = pair.second.exceptionType().getCanonicalName();
            exceptionHandlers.add(new Row(classMethod, exceptionType, null, null, null));
        });

        Map<String, Object> model = new HashMap<>();
        model.put("beforeFilters", beforeFilters);
        model.put("beforeFilters?", beforeFilters.size() != 0);
        model.put("afterFilters", afterFilters);
        model.put("afterFilters?", afterFilters.size() != 0);
        model.put("afterAfterFilters", afterAfterFilters);
        model.put("afterAfterFilters?", afterAfterFilters.size() != 0);
        model.put("exceptionHandlers", exceptionHandlers);
        model.put("exceptionHandlers?", exceptionHandlers.size() != 0);
        model.put("routes", routes);
        return new MustacheTemplateEngine("autorouter/templates")
                .render(new ModelAndView(model, "routeOverview.mustache"));
    }

    private static <T extends Annotation> List<Row> filtersToList(Set<Pair<Method, T>> src) {
        List<Row> filters = new ArrayList<>();
        src.forEach(pair -> {
            String classMethod = pair.first.getDeclaringClass().getCanonicalName() + "#" + pair.first.getName();
            String path = "*";
            String annPath = Utils.getRoutePathFromAnnotation(pair.second);
            if (!annPath.equals(NULL_STR)) path = annPath;
            String accept = "*/*";
            String annAccept = Utils.getRouteAcceptTypeFromAnnotation(pair.second);
            if (!annAccept.equals(NULL_STR)) accept = annAccept;
            filters.add(new Row(classMethod, null, path, accept, null));
        });
        return filters;
    }

    private static class Row {
        public final String classMethod;
        public final String verb;
        public final String path;
        public final String accept;
        public final String transformer;

        public Row(String classMethod, String verb, String path, String accept, String transformer) {
            this.classMethod = classMethod;
            this.verb = verb;
            this.path = path;
            this.accept = accept;
            this.transformer = transformer;
        }
    }

}
