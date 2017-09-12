package io.drakon.spark.autorouter;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.ResponseTransformer;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.ParametersAreNullableByDefault;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

import static io.drakon.spark.autorouter.Utils.*;
import static io.drakon.spark.autorouter.Routes.NULL_STR;
import static io.drakon.spark.autorouter.Routes.NULL_TRANSFORMER;

@ParametersAreNonnullByDefault
@SuppressWarnings("unused")
public class Autorouter {

    private static final Logger log = LoggerFactory.getLogger(Autorouter.class);
    private final String pkg;
    private boolean routingComplete = false;

    static final List<Class<? extends Annotation>> ALL_ROUTE_ANNOTATIONS = Arrays.asList(
            Routes.GET.class,
            Routes.POST.class,
            Routes.PATCH.class,
            Routes.PUT.class,
            Routes.HEAD.class,
            Routes.OPTIONS.class,
            Routes.DELETE.class,
            Routes.CONNECT.class,
            Routes.TRACE.class
    );

    /**
     * Exception thrown when route() is called more than once.
     */
    public static class AlreadyRoutedException extends Exception {}

    /** Search results container. Yes it's ugly. */
    static class SearchResult {
        public final Map<Class<?>, String> pathClasses;

        public final Set<Pair<Method, Routes.Before>> beforeFilters;
        public final Set<Pair<Method, Routes.After>> afterFilters;
        public final Set<Pair<Method, Routes.AfterAfter>> afterAfterFilters;
        public final Set<Pair<Method, Routes.ExceptionHandler>> exceptionHandlers;

        public final Map<Class<? extends Annotation>, Set<Pair<Method, RouteInfo>>> routes;

        public SearchResult(Map<Class<?>, String> pathClasses,
                            Set<Pair<Method, Routes.Before>> beforeFilters,
                            Set<Pair<Method, Routes.After>> afterFilters,
                            Set<Pair<Method, Routes.AfterAfter>> afterAfterFilters,
                            Set<Pair<Method, Routes.ExceptionHandler>> exceptionHandlers,
                            Map<Class<? extends Annotation>, Set<Pair<Method, RouteInfo>>> routes) {
            this.pathClasses = pathClasses;
            this.beforeFilters = beforeFilters;
            this.afterFilters = afterFilters;
            this.afterAfterFilters = afterAfterFilters;
            this.exceptionHandlers = exceptionHandlers;
            this.routes = routes;
        }
    }

    /** Route info container. Deals with the lack of inheritance in Java Annotations (FU Java). */
    @ParametersAreNullableByDefault
    static class RouteInfo {
        public final String path;
        public final String acceptType;
        public final ResponseTransformer transformer;

        public RouteInfo(String path, String acceptType, ResponseTransformer transformer) {
            this.path = path;
            this.acceptType = acceptType;
            this.transformer = transformer;
        }
    }

    /**
     * Creates a new Autorouter that restricts its search to a specific package.
     *
     * @param pkg The package in standard Java notation (e.g. io.drakon.spark)
     */
    public Autorouter(String pkg) {
        this.pkg = pkg;
    }

    /**
     * Searches the classpath for all the annotated things.
     */
    SearchResult search() {
        Reflections ref = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(pkg))
                .setScanners(new MethodAnnotationsScanner(), new TypeAnnotationsScanner(), new SubTypesScanner())
                .filterInputsBy(new FilterBuilder().includePackage(pkg)));

        log.debug("Beginning search for path classes.");
        HashMap<Class<?>, String> pathClassMap = new HashMap<>();
        for (Class<?> cls : ref.getTypesAnnotatedWith(Routes.PathGroup.class)) {
            StringBuilder path = new StringBuilder();
            Class<?> clsSuper = cls;
            while (clsSuper != Object.class) {
                Routes.PathGroup group = cls.getAnnotation(Routes.PathGroup.class);
                if (group != null) { path.insert(0, group.prefix()); }
                clsSuper = clsSuper.getSuperclass();
            }
            pathClassMap.put(cls, path.toString());
        }
        log.debug("Found {} class-route path mappings.", pathClassMap.size());

        log.debug("Beginning search for route methods.");
        HashMap<Class<? extends Annotation>, Set<Pair<Method, RouteInfo>>> results = new HashMap<>();
        for (Class<? extends Annotation> routeAnnotation : ALL_ROUTE_ANNOTATIONS) {
            Set<Method> methods = ref.getMethodsAnnotatedWith(routeAnnotation);
            for (Method m : methods) {
                // Pull fields from annotation
                Annotation ann = m.getAnnotation(routeAnnotation);
                String path = pathClassMap.getOrDefault(m.getDeclaringClass(), "") + getRoutePathFromAnnotation(ann);
                String acceptType = getRouteAcceptTypeFromAnnotation(ann);
                Class<? extends ResponseTransformer> transformerCls = getRouteTransformerFromAnnotation(ann);

                // Get rid of placeholder values (more Java baggage...)
                ResponseTransformer transformer = null;
                if (acceptType.equals(NULL_STR)) acceptType = null;
                if (transformerCls != NULL_TRANSFORMER) {
                    try {
                        transformer = transformerCls.newInstance();
                    } catch (ReflectiveOperationException ex) {
                        log.error("Invalid transformer {} - must have param-less constructor!", transformerCls);
                        log.error("Skipping route {}", path);
                        continue;
                    }
                }
                log.trace("Adding path '{}' (accept {}, transformer {})", path, acceptType, transformer);

                // Attach info to route annotation
                RouteInfo info = new RouteInfo(path, acceptType, transformer);
                Set<Pair<Method, RouteInfo>> resSet = results.getOrDefault(routeAnnotation, new HashSet<>());
                resSet.add(new Pair<>(m, info));
                results.put(routeAnnotation, resSet);
            }
        }
        log.debug("Route search complete.");

        log.debug("Beginning search for filters/event handlers.");
        Set<Pair<Method, Routes.Before>> beforeFilters = new HashSet<>();
        for (Method m : ref.getMethodsAnnotatedWith(Routes.Before.class))
            beforeFilters.add(new Pair<>(m, m.getAnnotation(Routes.Before.class)));

        Set<Pair<Method, Routes.After>> afterFilters = new HashSet<>();
        for (Method m : ref.getMethodsAnnotatedWith(Routes.After.class))
            afterFilters.add(new Pair<>(m, m.getAnnotation(Routes.After.class)));

        Set<Pair<Method, Routes.AfterAfter>> afterAfterFilters = new HashSet<>();
        for (Method m : ref.getMethodsAnnotatedWith(Routes.AfterAfter.class))
            afterAfterFilters.add(new Pair<>(m, m.getAnnotation(Routes.AfterAfter.class)));

        Set<Pair<Method, Routes.ExceptionHandler>> exceptionHandlers = new HashSet<>();
        for (Method m : ref.getMethodsAnnotatedWith(Routes.ExceptionHandler.class))
            exceptionHandlers.add(new Pair<>(m, m.getAnnotation(Routes.ExceptionHandler.class)));

        return new SearchResult(pathClassMap, beforeFilters, afterFilters, afterAfterFilters, exceptionHandlers,
                results);
    }

    /**
     * Searches the classpath and wires up annotated methods to the current Spark singleton.
     *
     * @throws AlreadyRoutedException if called multiple times.
     */
    void route() throws AlreadyRoutedException {
        if (routingComplete) throw new AlreadyRoutedException();
        routingComplete = true;

        SearchResult searchResult = search();
    }
}
