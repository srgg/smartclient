package org.srg.smartclient.utils;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;

public class Utils {
    private Utils() {}

    public static <T> T throw_it(String message, Object...args) throws IllegalStateException {
        throw new IllegalStateException(String.format(message, args));
    }

    public static <T> T throw_it_runtime(String message, Object...args) throws RuntimeException {
        throw new RuntimeException(String.format(message, args));
    }

    @FunctionalInterface
    public interface CheckedFunction<T,R> {
        R apply(T arg) throws Exception;
    }

    public static URL getResource(Path resource) {
        return getResource(resource.toString());
    }

    public static InputStream getResourceAsStream(String resource) {
        final ClassLoader classLoader = getDefaultClassLoader();
        return classLoader != null ? classLoader.getResourceAsStream(resource) : ClassLoader.getSystemResourceAsStream(resource);
    }

    public static URL getResource(String resource) {
        final ClassLoader classLoader = getDefaultClassLoader();
        return classLoader != null ? classLoader.getResource(resource) : ClassLoader.getSystemResource(resource);

//        if (url != null) {
//            return url;
//        }
//
//        final URL systemResource = ClassLoader.getSystemResource(resource);
//        if (systemResource != null) {
//            logger.warn("return system resource {}", url);
//            return systemResource;
//        } else {
//            try {
//                final String s = new File(resource).toURI().toURL().toString().replace("file:", "classpath:");
//                final URL rv = new URL(s);
//
//                logger.warn("file resource {}", rv);
//                return rv;
//            } catch (MalformedURLException e) {
//                logger.warn("resource not found {}", resource);
//                return null;
//            }
//        }
    }

    public static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = null;

        try {
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable var3) {
        }

        if (cl == null) {
            cl = Utils.class.getClassLoader();
            if (cl == null) {
                try {
                    cl = ClassLoader.getSystemClassLoader();
                } catch (Throwable var2) {
                }
            }
        }

        return cl;
    }
}
