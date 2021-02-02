package org.srg.smartclient.utils;

import org.jboss.jandex.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class AnnotationUtils {
    private static Logger logger = LoggerFactory.getLogger(AnnotationUtils.class);

    private AnnotationUtils() {
    }

    public static <A extends Annotation> A getAnnotation(Class<?> clazz, String name, Class<A> annotationClass) {
        final String canonicalName = clazz.getCanonicalName();
        final String cname = canonicalName.replaceAll("\\.", "/") + ".class";

        Indexer indexer = new Indexer();
        InputStream stream = annotationClass
                .getClassLoader()
                .getResourceAsStream(cname);

        try {
            indexer.index(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Index index = indexer.complete();

        final DotName dotCName = DotName.createSimple(canonicalName);
        final ClassInfo ci = index.getClassByName(dotCName);

        final DotName dotAnnotation = DotName.createSimple(annotationClass.getCanonicalName());
        List<AnnotationInstance> annotations = index.getAnnotations(dotAnnotation);

        final BeanInfo bi;
        try {
            bi = Introspector.getBeanInfo(clazz);
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }

        PropertyDescriptor propertyDescriptor = null;
        for (PropertyDescriptor pd : bi.getPropertyDescriptors()) {
            if (name.equals(pd.getName())) {
                propertyDescriptor = pd;
                break;
            }
        }

        if (propertyDescriptor == null) {
            /**
             * If property descriptor is null that probably means that it is not a bean property and there is no
             * accessors for it. In most cases it will be a field introduced by a various proxies introduced by
             * Hibernate, java assist, etc.
             */

            logger.debug("Property Description is empty {}.{}, this will be treated as no {} annotation was found for the field.",
                    clazz.getSimpleName(), name, annotationClass.getSimpleName());
            return null;
        }

        final List<AnnotationInstance> effectiveAnnotations = new LinkedList<>();

        for (AnnotationInstance annotation : annotations) {
            switch (annotation.target().kind()) {
                case METHOD:
                    final String methodName = annotation.target().asMethod().name();
                    if ( (propertyDescriptor.getReadMethod() != null &&  methodName.equals(propertyDescriptor.getReadMethod().getName()))
                            || ( propertyDescriptor.getWriteMethod() != null && methodName.equals(propertyDescriptor.getWriteMethod().getName()) )
                    ) {
                        effectiveAnnotations.add(annotation);
                    }
                    break;

                case FIELD:
                    final String fieldName = annotation.target().asField().name();
                    if (fieldName.equals(propertyDescriptor.getName())) {
                        effectiveAnnotations.add(annotation);
                    }
                    break;
            }
        }

        final A ai = effectiveAnnotations.isEmpty() ? null : mergeJoinColumns(annotationClass, effectiveAnnotations);
        return ai;
    }


    public static <A extends Annotation> A mergeJoinColumns(Class<A> annotationClass, Iterable<AnnotationInstance> annotationInstances) {

        final Map<String, Object> effectiveValues = new HashMap<>();

        // -- populate map with default values
        final BeanInfo bi;
        try {
            bi = Introspector.getBeanInfo(annotationClass);
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }

        assert bi.getPropertyDescriptors().length == 0;

        final MethodDescriptor mds[] = bi.getMethodDescriptors();

        for (MethodDescriptor md: mds) {
            final Object dv = md.getMethod().getDefaultValue();
            effectiveValues.put(md.getName(), dv);
        }

        // -- populate map with values given from annotations
        for (AnnotationInstance ai: annotationInstances) {
            for (AnnotationValue av: ai.values()) {
                switch (av.kind()) {
                    case ENUM:
                        final DotName enumClassName = av.asEnumType();
                        try {
                            final Class enumClass = Class.forName(enumClassName.toString());
                            final Object o = Enum.valueOf(enumClass, av.asEnum());
                            effectiveValues.put(av.name(), o);

                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                        break;

                    default:
                        effectiveValues.put(av.name(), av.value());
                }

            }
        }

        return RuntimeAnnotations.annotationForMap(annotationClass, effectiveValues);
    }
}
