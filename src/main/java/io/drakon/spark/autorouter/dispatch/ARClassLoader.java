package io.drakon.spark.autorouter.dispatch;

/**
 * Classloader with a public defineClass for ASM class loading. Based on the ASM 4.0 guidebook.
 */
public class ARClassLoader extends ClassLoader {

    /** Override to make defineClass public for ASM'ing. */
    public Class defineClass(String name, byte[] b) {
        return defineClass(name, b, 0, b.length);
    }

}
