package io.drakon.spark.autorouter.dispatch;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Response;
import spark.Request;

import static org.objectweb.asm.Opcodes.*;

/**
 * Internal bytecode-generating dispatcher to preserve performance even on hot paths, instead of reflection invocation.
 */
@ParametersAreNonnullByDefault
public class BytecodeDispatch {

    private static final Logger log = LoggerFactory.getLogger(BytecodeDispatch.class);
    private static final ARClassLoader classLoader = new ARClassLoader();

    private enum StubType {
        Route(IRouteDispatch.class, 2),
        Exception(IExceptionDispatch.class, 3);

        public final Type iface;
        public final int params;
        StubType(Class iface, int params) {
            this.iface = Type.getType(iface);
            this.params = params;
        }
    }

    /**
     * Generates a IRouteDispatch subclass for the given target method.
     *
     * @param targetMethod The method the stub class should invoke.
     * @return An IRouteDispatch subclass or null for invalid methods.
     */
    @SuppressWarnings("unchecked")
    public Class<IRouteDispatch> generateRouteStub(Method targetMethod) {
        if (!isValidTarget(StubType.Route, targetMethod)) return null;
        return (Class<IRouteDispatch>) generateClass(StubType.Route, targetMethod, null);
    }

    /**
     * Generates a IExceptionDispatch subclass for the given target method.
     *
     * @param targetMethod The method the stub class should invoke.
     * @return An IExceptionDispatch subclass or null for invalid methods.
     */
    @SuppressWarnings("unchecked")
    public Class<IExceptionDispatch> generateExceptionStub(Method targetMethod, Class<? extends Exception> exType) {
        if (!isValidTarget(StubType.Exception, targetMethod)) return null;
        return (Class<IExceptionDispatch>) generateClass(StubType.Exception, targetMethod, exType);
    }

    /**
     * Determines if a target method is valid for a given dispatch type.
     *
     * @param type The dispatcher type.
     * @param m Target method.
     * @return True if valid, false otherwise.
     */
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

    /**
     * The meat of the bytecode dispatcher. Takes a type, target method and optionally an Exception type and spits out
     * a shiny new dispatcher class of the appropriate dispatch interface.
     *
     * @param type The type of dispatcher.
     * @param targetMethod Target method the new class will invoke.
     * @param exType The Exception type, if making an Exception dispatcher.
     * @return A new dispatcher subclass.
     */
    private Class generateClass(StubType type, Method targetMethod,
                                @Nullable Class<? extends Exception> exType) {
        Class targetClass = targetMethod.getDeclaringClass();
        String basename = "$Generated" + type.name() + "Dispatch_"
                + targetClass.getCanonicalName().replace('.', '_') + "$" + targetMethod.getName();

        if (type == StubType.Exception && exType == null)
            throw new RuntimeException("exType must not be null when StubType == Exception");

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        // Visit the class
        String asmBasename = "io/drakon/spark/autorouter/dispatch/gen/routes" + basename;
        writer.visit(Opcodes.V1_8, ACC_PUBLIC + ACC_FINAL, asmBasename, null, "java/lang/Object",
                new String[]{ type.iface.getInternalName() });

        // Constructor
        // from https://coderwall.com/p/k9uusw/generate-default-constructor-using-asm-5-bytecode-manipulation
        MethodVisitor ctorMv = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        ctorMv.visitCode();
        ctorMv.visitVarInsn(ALOAD, 0);
        ctorMv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        ctorMv.visitInsn(RETURN);
        ctorMv.visitMaxs(1,1);
        ctorMv.visitEnd();

        // Visit the dispatch method
        String mDescript = "(" +
                (type == StubType.Exception ? "L" + Type.getType(exType).getInternalName() + ";" : "") +
                "Lspark/Request;Lspark/Response;)Ljava/lang/Object;";
        MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "dispatch",
                mDescript,
                null, null);
        int base = 1;
        if (type == StubType.Exception) {
            mv.visitVarInsn(ALOAD, 1);
            base += 1;
        }
        mv.visitVarInsn(ALOAD, base);
        mv.visitVarInsn(ALOAD, base + 1);
        mv.visitMethodInsn(INVOKESTATIC, Type.getType(targetClass).getInternalName(),
                targetMethod.getName(), Type.getMethodDescriptor(targetMethod), false);
        mv.visitMaxs(base + 1, 0);
        mv.visitInsn(ARETURN);
        mv.visitEnd();

        // End visitations
        writer.visitEnd();
        byte[] b = writer.toByteArray();

        return classLoader.defineClass("io.drakon.spark.autorouter.dispatch.gen.routes" + basename, b);
    }

}
