package io.drakon.spark.autorouter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Stream;

import io.drakon.spark.autorouter.test.advroute.AdvancedRoutes;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.drakon.spark.autorouter.Utils.Pair;

@DisplayName("Search system")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class TestSearch {

    @Nested
    @DisplayName("Filter and Exception search")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class FilterExceptSearch {

        Autorouter.SearchResult searchResult;

        @BeforeAll
        void setup() {
            Autorouter router = new Autorouter("io.drakon.spark.autorouter.test.filterexcept");
            searchResult = router.search();
        }

        @Test
        @DisplayName("finds Before filter")
        void testBeforeFilter() {
            assertEquals(1, searchResult.beforeFilters.size());
        }

        @Test
        @DisplayName("finds After filter")
        void testAfterFilter() {
            assertEquals(1, searchResult.afterFilters.size());
        }

        @Test
        @DisplayName("finds AfterAfter filter")
        void testAfterAfterFilter() {
            assertEquals(1, searchResult.afterAfterFilters.size());
        }

        @Test
        @DisplayName("finds exception handler")
        void testExceptHandler() {
            assertEquals(1, searchResult.exceptionHandlers.size());
            Pair<Method, Routes.ExceptionHandler> pair = new ArrayList<>(searchResult.exceptionHandlers).get(0);
            assertEquals(Exception.class, pair.second.exceptionType());
        }

        @AfterAll
        void teardown() {
            searchResult = null;
        }
    }

    @Nested
    @DisplayName("Basic routes search")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class RouteSearch {

        Autorouter.SearchResult searchResult;

        @BeforeAll
        void setup() {
            Autorouter router = new Autorouter("io.drakon.spark.autorouter.test.route");
            searchResult = router.search();
        }

        @TestFactory
        @DisplayName("Basic Routes")
        Stream<DynamicTest> testRoutes() {
            return Stream.of(
                    new Pair<>(Routes.GET.class, "get"),
                    new Pair<>(Routes.POST.class, "post"),
                    new Pair<>(Routes.PATCH.class, "patch"),
                    new Pair<>(Routes.PUT.class, "put"),
                    new Pair<>(Routes.HEAD.class, "head"),
                    new Pair<>(Routes.OPTIONS.class, "options"),
                    new Pair<>(Routes.DELETE.class, "delete"),
                    new Pair<>(Routes.CONNECT.class, "connect"),
                    new Pair<>(Routes.TRACE.class, "trace")
            ).map(pair -> dynamicTest(pair.second + " route", () -> {
                Set<Pair<Method, Autorouter.RouteInfo>> data = searchResult.routes.get(pair.first);
                assertNotNull(data);
                assertEquals(1, data.size());
                Autorouter.RouteInfo info = new ArrayList<>(data).get(0).second;
                assertEquals("/" + pair.second, info.path);
                assertNull(info.acceptType);
                assertNull(info.transformer);
            }));
        }

        @AfterAll
        void teardown() {
            searchResult = null;
        }
    }

    @Nested
    @DisplayName("Advanced routes search")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class AdvRouteSearch {

        Autorouter.SearchResult searchResult;

        @BeforeAll
        void setup() {
            Autorouter router = new Autorouter("io.drakon.spark.autorouter.test.advroute");
            searchResult = router.search();
        }

        @TestFactory
        @DisplayName("Advanced Routes")
        Stream<DynamicTest> testRoutes() {
            return Stream.of(
                    new Pair<>(Routes.GET.class, "get"),
                    new Pair<>(Routes.POST.class, "post"),
                    new Pair<>(Routes.PATCH.class, "patch"),
                    new Pair<>(Routes.PUT.class, "put"),
                    new Pair<>(Routes.HEAD.class, "head"),
                    new Pair<>(Routes.OPTIONS.class, "options"),
                    new Pair<>(Routes.DELETE.class, "delete"),
                    new Pair<>(Routes.CONNECT.class, "connect"),
                    new Pair<>(Routes.TRACE.class, "trace")
            ).map(pair -> dynamicTest(pair.second + " adv. route", () -> {
                Set<Pair<Method, Autorouter.RouteInfo>> data = searchResult.routes.get(pair.first);
                assertNotNull(data);
                assertEquals(1, data.size());
                Autorouter.RouteInfo info = new ArrayList<>(data).get(0).second;
                assertEquals("/" + pair.second, info.path);
                assertEquals("application/json", info.acceptType);
                assertTrue(info.transformer instanceof AdvancedRoutes.CustomTransformer,
                        "transformer constructed correctly");
            }));
        }

        @AfterAll
        void teardown() {
            searchResult = null;
        }
    }

}
