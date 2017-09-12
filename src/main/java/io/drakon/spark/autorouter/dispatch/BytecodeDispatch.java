package io.drakon.spark.autorouter.dispatch;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import javassist.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Response;
import spark.Request;

/**
 * Internal bytecode-generating dispatcher to preserve performance even on hot paths, instead of reflection invocation.
 *
 * Based heavily on Flightpath's bytecode generation:
 * https://github.com/Emberwalker/Flightpath/tree/master/src/main/java/io/drakon/flightpath/dispatch
 */
@ParametersAreNonnullByDefault
public class BytecodeDispatch {

    private static final Logger log = LoggerFactory.getLogger(BytecodeDispatch.class);

    private static final String TWO_DISPATCH_BODY = "{ return $TARGET.$METHOD($1, $2); }";
    private static final String THREE_DISPATCH_BODY = "{ return $TARGET.$METHOD($1, $2, $3); }";

    private enum StubType {
        Route(TWO_DISPATCH_BODY, "IRouteDispatch", 2),
        Exception(THREE_DISPATCH_BODY, "IExceptionDispatch", 3);

        public final String template;
        public final String iface;
        public final int params;
        StubType(String template, String iface, int params) {
            this.template = template;
            this.iface = "io.drakon.spark.autorouter.dispatch." + iface;
            this.params = params;
        }
    }

    @SuppressWarnings("unchecked")
    public Class<IRouteDispatch> generateRouteStub(Method targetMethod) {
        if (!isValidTarget(StubType.Route, targetMethod)) return null;
        return (Class<IRouteDispatch>) generateClass(StubType.Route, targetMethod, null);
    }

    @SuppressWarnings("unchecked")
    public Class<IExceptionDispatch> generateExceptionStub(Method targetMethod, Class<? extends Exception> exType) {
        if (!isValidTarget(StubType.Exception, targetMethod)) return null;
        return (Class<IExceptionDispatch>) generateClass(StubType.Exception, targetMethod, exType);
    }

    private boolean isValidTarget(StubType type, Method m) {
        try {
            if (m.getReturnType() == void.class || m.getParameterCount() != type.params
                    || !Modifier.isStatic(m.getModifiers()) || !Modifier.isPublic(m.getModifiers())) {
                throw new Exception("The signature must match the Spark standard and be public static.");
            }

            Class p1 = null, p2 = null, p3 = null;

            switch (type.params) {
                case 3:
                    p3 = m.getParameterTypes()[2];
                case 2:
                    p2 = m.getParameterTypes()[1];
                case 1:
                    p1 = m.getParameterTypes()[0];
                    break;
            }

            if (type.params > 2 && p3 != Response.class) throw new Exception("Wrong third param type.");
            if (type.params > 1 && p2 != (type == StubType.Route ? Response.class : Request.class))
                throw new Exception("Wrong second param type.");
            if (type.params > 0 && (p1 != Request.class && type == StubType.Route) ||
                    (p1 == null || !Exception.class.isAssignableFrom(p1) && type == StubType.Exception))
                throw new Exception("Wrong first param type.");

            return true;
        } catch (Exception ex) {
            log.warn("The {} handler {}#{} will be skipped! {}", type.name(), m.getDeclaringClass().getCanonicalName(),
                    m.getName(), ex.getMessage());
            return false;
        }
    }

    private Class generateClass(StubType type, Method targetMethod,
                                @Nullable Class<? extends Exception> exType) {
        Class targetClass = targetMethod.getDeclaringClass();
        // Generate unique name
        String _basename = "io.drakon.spark.autorouter.dispatch.gen.routes$Generated" + type.name() + "Dispatch_"
                + targetClass.getCanonicalName();
        ClassPool classPool = ClassPool.getDefault();
        String basename = _basename;
        int nameAttempt = 0;
        while (classPool.getOrNull(basename) != null) {
            basename = _basename + "_" + nameAttempt;
            nameAttempt += 1;
        }

        try {
            // Get CtClass objects and prep
            CtClass ctClass = classPool.makeClass(basename);
            CtClass iface = classPool.get(type.iface);
            ctClass.setInterfaces(new CtClass[]{iface});
            CtClass objectCtClass = classPool.get(Object.class.getName());
            CtClass exceptionCtClass = classPool.get((exType == null ? Exception.class : exType).getName());
            CtClass requestCtClass = classPool.get(Request.class.getName());
            CtClass responseCtClass = classPool.get(Response.class.getName());
            CtClass targetCtClass = classPool.get(targetClass.getName());

            // Create ctor.
            //CtConstructor constr = CtNewConstructor.skeleton(new CtClass[]{targetCtClass}, null, ctClass);
            CtConstructor constr = CtNewConstructor.skeleton(new CtClass[]{}, null, ctClass);
            ctClass.addConstructor(constr);

            // Create fields.
            /*CtField.Initializer init = CtField.Initializer.byParameter(0);
            CtField field = new CtField(targetCtClass, "target", ctClass);
            ctClass.addField(field, init);*/

            // Generate dispatch method
            CtMethod dispatchMethod;
            switch (type) {
                case Route:
                    dispatchMethod = new CtMethod(objectCtClass, "dispatch",
                            new CtClass[]{requestCtClass, responseCtClass}, ctClass);
                    break;
                case Exception:
                    dispatchMethod = new CtMethod(objectCtClass, "dispatch",
                            new CtClass[]{exceptionCtClass, requestCtClass, responseCtClass}, ctClass);
                    break;
                default:
                    throw new RuntimeException("Impossible!");
            }
            dispatchMethod.setBody(type.template.replace("$TARGET", targetCtClass.getName())
                    .replace("$METHOD", targetMethod.getName()));
            ctClass.addMethod(dispatchMethod);

            // Dump finished class and release from the ClassPool (as we don't edit existing generated classes)
            Class out = ctClass.toClass();
            ctClass.detach();

            return out;
        } catch (CannotCompileException ex) {
            throw new RuntimeException("Unable to compile; make sure you're running a JDK, not a JRE!", ex);
        } catch (NotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

}
