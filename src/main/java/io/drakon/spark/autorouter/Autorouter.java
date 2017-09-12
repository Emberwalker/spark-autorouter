package io.drakon.spark.autorouter;

import javafx.util.Pair;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

@ParametersAreNonnullByDefault
@SuppressWarnings("unused")
public class Autorouter {

    private static final Logger log = LoggerFactory.getLogger(Autorouter.class);
    private final String pkg;
    private boolean routingComplete = false;
    private boolean searchComplete = false;

    static final List<Class<? extends Annotation>> ALL_ROUTE_ANNOTATIONS = Arrays.asList(
            Route.GET.class,
            Route.POST.class,
            Route.PATCH.class,
            Route.PUT.class,
            Route.HEAD.class,
            Route.OPTIONS.class,
            Route.DELETE.class,
            Route.CONNECT.class,
            Route.TRACE.class
    );

    /**
     * Exception thrown when route() is called more than once.
     */
    public static class AlreadyRoutedException extends Exception {}

    /** Search results container. Yes it's ugly. */
    static class SearchResult {
        public final Map<Class<?>, String> pathClasses;

        public final Set<Pair<Method, Route.Before>> beforeFilters;
        public final Set<Pair<Method, Route.After>> afterFilters;
        public final Set<Pair<Method, Route.AfterAfter>> afterAfterFilters;
        public final Set<Pair<Method, Route.ExceptionHandler>> exceptionHandlers;

        public final Map<Class<? extends Annotation>, Set<Pair<Method, Route.GET>>> routes;

        public SearchResult(Map<Class<?>, String> pathClasses,
                            Set<Pair<Method, Route.Before>> beforeFilters,
                            Set<Pair<Method, Route.After>> afterFilters,
                            Set<Pair<Method, Route.AfterAfter>> afterAfterFilters,
                            Set<Pair<Method, Route.ExceptionHandler>> exceptionHandlers,
                            Map<Class<? extends Annotation>, Set<Pair<Method, Route.GET>>> routes) {
            this.pathClasses = pathClasses;
            this.beforeFilters = beforeFilters;
            this.afterFilters = afterFilters;
            this.afterAfterFilters = afterAfterFilters;
            this.exceptionHandlers = exceptionHandlers;
            this.routes = routes;
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
    void search() {
        Reflections ref = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(pkg))
                .setScanners(new MethodAnnotationsScanner(), new TypeAnnotationsScanner())
                .filterInputsBy(new FilterBuilder.Include(pkg)));

        log.debug("Beginning search for path classes.");
        Set<Class<?>> pathClasses = ref.getTypesAnnotatedWith(Route.PathGroup.class);
        HashMap<Class<?>, String> pathClassMap = new HashMap<>();
        for (Class<?> cls : pathClasses) {
            StringBuilder path = new StringBuilder();
            Class<?> clsSuper = cls;
            while (clsSuper != Object.class) {
                Route.PathGroup group = cls.getAnnotation(Route.PathGroup.class);
                if (group != null) { path.insert(0, group.prefix()); }
                clsSuper = clsSuper.getSuperclass();
            }
            pathClassMap.put(cls, path.toString());
        }
        log.debug("Found {} class-route path mappings.", pathClassMap.size());

        Map<Class<? extends Annotation>, Set<Pair<Method, ?>>> routes = new HashMap<>();

        searchComplete = true;
    }

    /**
     * Searches the classpath and wires up annotated methods to the current Spark singleton.
     *
     * @throws AlreadyRoutedException if called multiple times.
     */
    void route() throws AlreadyRoutedException {
        if (routingComplete) throw new AlreadyRoutedException();
        routingComplete = true;

        // TODO
    }
}
