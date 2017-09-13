package io.drakon.spark.autorouter;

import java.util.stream.Stream;

import io.drakon.spark.autorouter.dispatch.BytecodeDispatch;
import io.drakon.spark.autorouter.dispatch.IExceptionDispatch;
import io.drakon.spark.autorouter.dispatch.IRouteDispatch;
import io.drakon.spark.autorouter.test.dispatch.route.Sample;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import spark.Request;
import spark.Response;

@DisplayName("Dispatch code generation")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestDispatch {

    @Test
    @DisplayName("generates standard route")
    public void testRouteGen() {
        BytecodeDispatch dispatchGen = new BytecodeDispatch();
        Class<IRouteDispatch> cls1, cls2;
        try {
            cls1 = dispatchGen.generateRouteStub(Sample.class.getMethod("sample",
                    Request.class, Response.class));
            cls2 = dispatchGen.generateRouteStub(Sample.class.getMethod("sampleTwo",
                    Request.class, Response.class));
        } catch (ReflectiveOperationException ex) {
            fail("Exception thrown by reflection.");
            return;
        }
        IRouteDispatch dispatch1 = Utils.dispatchClassToObj(cls1);
        IRouteDispatch dispatch2 = Utils.dispatchClassToObj(cls2);
        Stream.of(dispatch1, dispatch2).forEach(dispatch -> {
            Sample.tripped = false;
            assertNotNull(dispatch, "Dispatch object created.");
            dispatch.dispatch(null, null);
            assertTrue(Sample.tripped, "Sample class now reports true.");
        });
    }

    @Test
    @DisplayName("generates except route")
    public void testExceptGen() {
        io.drakon.spark.autorouter.test.dispatch.except.Sample.tripped = false;
        BytecodeDispatch dispatchGen = new BytecodeDispatch();
        Class<IExceptionDispatch> cls;
        try {
            cls = dispatchGen.generateExceptionStub(io.drakon.spark.autorouter.test.dispatch.except.Sample.class.getMethod(
                    "sample", Exception.class, Request.class, Response.class), Exception.class);
        } catch (ReflectiveOperationException ex) {
            fail("Exception thrown by reflection.");
            return;
        }
        IExceptionDispatch dispatch = Utils.dispatchClassToObj(cls);
        assertNotNull(dispatch, "Dispatch object created.");
        dispatch.dispatch(null, null, null);
        assertTrue(io.drakon.spark.autorouter.test.dispatch.except.Sample.tripped, "Sample class now reports true.");
    }

}
