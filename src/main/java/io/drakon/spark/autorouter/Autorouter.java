package io.drakon.spark.autorouter;

import io.drakon.spark.autorouter.dispatch.BytecodeDispatch;
import io.drakon.spark.autorouter.dispatch.IExceptionDispatch;
import io.drakon.spark.autorouter.dispatch.IRouteDispatch;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.*;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.ParametersAreNullableByDefault;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static spark.Spark.*;
import static io.drakon.spark.autorouter.Utils.*;
import static io.drakon.spark.autorouter.Routes.NULL_STR;
import static io.drakon.spark.autorouter.Routes.NULL_TRANSFORMER;

@ParametersAreNonnullByDefault
@SuppressWarnings("unused")
public class Autorouter {

    private static final Logger log = LoggerFactory.getLogger(Autorouter.class);
    private final String pkg;
    private boolean routingComplete = false;
    private boolean debugPageAdded = false;
    SearchResult lastSearch = null;

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

    enum RouteHandler {
        GET(Routes.GET.class, Spark::get, Spark::get, Spark::get, Spark::get),
        POST(Routes.POST.class, Spark::post, Spark::post, Spark::post, Spark::post),
        PATCH(Routes.PATCH.class, Spark::patch, Spark::patch, Spark::patch, Spark::patch),
        PUT(Routes.PUT.class, Spark::put, Spark::put, Spark::put, Spark::put),
        HEAD(Routes.HEAD.class, Spark::head, Spark::head, Spark::head, Spark::head),
        OPTIONS(Routes.OPTIONS.class, Spark::options, Spark::options, Spark::options, Spark::options),
        DELETE(Routes.DELETE.class, Spark::delete, Spark::delete, Spark::delete, Spark::delete),
        CONNECT(Routes.CONNECT.class, Spark::connect, Spark::connect, Spark::connect, Spark::connect),
        TRACE(Routes.TRACE.class, Spark::trace, Spark::trace, Spark::trace, Spark::trace);

        public final Class<? extends Annotation> annotation;
        public final BiConsumer<String, Route> routePath;
        public final TriConsumer<String, String, Route> routePathAndAccept;
        public final TriConsumer<String, Route, ResponseTransformer> routePathAndTransform;
        public final QuadConsumer<String, String, Route, ResponseTransformer> routeAll;

        RouteHandler(Class<? extends Annotation> annotation, BiConsumer<String, Route> routePath,
                      TriConsumer<String, String, Route> routePathAndAccept,
                      TriConsumer<String, Route, ResponseTransformer> routePathAndTransform,
                      QuadConsumer<String, String, Route, ResponseTransformer> routeAll) {
            this.annotation = annotation;
            this.routePath = routePath;
            this.routePathAndAccept = routePathAndAccept;
            this.routePathAndTransform = routePathAndTransform;
            this.routeAll = routeAll;
        }
    }

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

        public RouteInfo(@Nonnull String path, String acceptType, ResponseTransformer transformer) {
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
            Class<?> clsParent = cls;
            while (clsParent != null) {
                Routes.PathGroup group = clsParent.getAnnotation(Routes.PathGroup.class);
                if (group != null) { path.insert(0, group.prefix()); }
                clsParent = clsParent.getDeclaringClass();
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
     * Searches the classpath and wires up annotated methods to the current Spark singleton. Will silently cancel if
     * called multiple times.
     */
    public void route() {
        if (routingComplete) return;
        routingComplete = true;

        SearchResult searchResult = search();
        lastSearch = searchResult;

        // Setup filters and exception handlers
        searchResult.exceptionHandlers.forEach(this::registerExceptionHandler);
        searchResult.beforeFilters.forEach(pair -> {
            Routes.Before ann = pair.second;
            registerBeforeOrAfterFilter(pair.first, ann.path(), ann.acceptType(), Spark::before, Spark::before,
                    Spark::before);
        });
        searchResult.afterFilters.forEach(pair -> {
            Routes.After ann = pair.second;
            registerBeforeOrAfterFilter(pair.first, ann.path(), ann.acceptType(), Spark::after, Spark::after,
                    Spark::after);
        });
        searchResult.afterAfterFilters.forEach(pair -> {
            IRouteDispatch d = generateRouteDispatcher(pair.first);
            if (pair.second.path().equals(NULL_STR)) afterAfter(d::dispatch);
            else afterAfter(pair.second.path(), d::dispatch);
        });

        // Setup routes
        searchResult.routes.forEach(this::registerRoutes);
    }

    /**
     * Enables a Route Overview page inspired by older versions of Spark.
     *
     * @param path Path to mount the overview route on.
     */
    public void enableRouteOverview(String path) {
        if (debugPageAdded) return;
        RouteOverview overview = new RouteOverview(this);
        get(path, overview::route);
        debugPageAdded = true;
    }

    /**
     * Handles registration for an exception handler entry.
     *
     * @param pair A pair from the Search pile.
     */
    private void registerExceptionHandler(Pair<Method, Routes.ExceptionHandler> pair) {
        Method m = pair.first;
        Class<? extends Exception> exType= pair.second.exceptionType();

        Class<IExceptionDispatch> dispatchClass = new BytecodeDispatch().generateExceptionStub(m, exType);
        IExceptionDispatch dispatch = Utils.dispatchClassToObj(dispatchClass);
        if (dispatch == null) throw new RuntimeException("Dispatcher is null!");

        exception(exType, dispatch::dispatch);
    }

    /**
     * Generates a standard route dispatch object from the code gen.
     *
     * @param m Target method to call with dispatcher.
     * @return The finished dispatcher.
     */
    private IRouteDispatch generateRouteDispatcher(Method m) {
        Class<IRouteDispatch> dispatchClass = new BytecodeDispatch().generateRouteStub(m);
        IRouteDispatch dispatch = Utils.dispatchClassToObj(dispatchClass);
        if (dispatch == null) throw new RuntimeException("Dispatcher is null!");

        return dispatch;
    }

    /**
     * Convenience method for registering the Before and After filter routes.
     *
     * @param m Target method to invoke.
     * @param path The path this filter affects from annotation.
     * @param acceptType The accept type of this filter from annotation.
     * @param a The Filter-only Spark method.
     * @param b The Path-and-Filter Spark method.
     * @param c The Path-Accept-and-Filter Spark method.
     */
    private void registerBeforeOrAfterFilter(Method m, String path, String acceptType, Consumer<Filter> a,
                                             BiConsumer<String, Filter> b, TriConsumer<String, String, Filter> c) {
        IRouteDispatch d = generateRouteDispatcher(m);
        if (path.equals(NULL_STR) && acceptType.equals(NULL_STR)) a.accept(d::dispatch);
        else if (!path.equals(NULL_STR) && acceptType.equals(NULL_STR)) b.accept(path, d::dispatch);
        else if (!path.equals(NULL_STR) && !acceptType.equals(NULL_STR))
            c.apply(path, acceptType, d::dispatch);
        else log.warn("Invalid @Before or @After handler {}#{} - acceptType must be accompanied by a path! Skipping.",
                    m.getDeclaringClass().getName(), m.getName());
    }

    /**
     * Registers all types of standard HTTP verb routes with Spark.
     *
     * @param cls The specific verb annotation type for this route set.
     * @param set The set generated by search() for this route set.
     */
    private void registerRoutes(Class<? extends Annotation> cls, Set<Pair<Method, RouteInfo>> set) {
        @SuppressWarnings("ConstantConditions") // We know all values are mapped, thing.
        RouteHandler rh = Arrays.stream(RouteHandler.values()).filter(h -> h.annotation == cls).findFirst().get();
        set.forEach(pair -> {
            IRouteDispatch d = generateRouteDispatcher(pair.first);
            RouteInfo info = pair.second;
            boolean hasAccept = info.acceptType != null;
            boolean hasTransform = info.transformer != null;
            if (hasAccept && hasTransform) rh.routeAll.apply(info.path, info.acceptType, d::dispatch, info.transformer);
            else if (hasAccept) rh.routePathAndAccept.apply(info.path, info.acceptType, d::dispatch);
            else if (hasTransform) rh.routePathAndTransform.apply(info.path, d::dispatch, info.transformer);
            else rh.routePath.accept(info.path, d::dispatch);
        });
    }

}
