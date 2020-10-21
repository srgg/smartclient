package org.srg.smartclient.utils;

import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.Map;

public class ContextualRuntimeException extends RuntimeException {
    private final Object context;

    public ContextualRuntimeException(String message, Throwable cause, Object context) {
        super(message, cause);
        this.context = context;
    }

    public Object getContext() {
        return context;
    }

    /**
     * This method  does not use Json serialization on purpose to achieve better readability.
     */
    @SuppressWarnings("UnusedReturnValue")
    public StringWriter dumpContext_ifAny(StringWriter sw, String baseIndent, ObjectWriter objectWriter){
        final Object ctx = this.getContext();
//        final Field[] fields = FieldUtils.getAllFields(ctx.getClass());
        final Field[] fields = ctx.getClass().getDeclaredFields();

        for (Field f: fields) {

            final String fieldName = f.getName();

            Object v;
            try {
                v = FieldUtils.readField(f, ctx, true);
            } catch (Throwable tt) {
                v = tt.getMessage();
            }

            if (v instanceof String str) {
                writeField(sw, objectWriter, baseIndent, fieldName, str);
            } else if (v == null) {
                writeField(sw, objectWriter, baseIndent, fieldName, "null");
            } else if (v instanceof Map map) {
                map.entrySet().stream()
                    .forEach( e -> {
                        if (e instanceof Map.Entry entry) {
                            writeField(sw, objectWriter,
                                baseIndent + "  ",
                                entry.getKey().toString(),
                                entry.getValue()
                            );
                        } else {
                            // TODO: rework!
                            throw new IllegalStateException();
                        }
                    });
            } else {
                writeField(sw, objectWriter, baseIndent, fieldName, v);
            }
        }

        return sw;
    }

    @SuppressWarnings("UnusedReturnValue")
    private static StringWriter writeField(StringWriter sw, ObjectWriter ow, String baseIndent, String name, Object value) {
        sw.write("" + baseIndent + name.toUpperCase().trim() + ":");

        if (value instanceof String str) {
            final boolean isMultiline = str.indexOf('\n') != -1;

            if (isMultiline) {
                sw.write("\n\n");
                sw.write(
                        str.indent(baseIndent.length() + 2)
                );
            } else {
                sw.write("  ");
                sw.write(str.trim());
            }

            if (!str.endsWith("\n")) {
                sw.write("\n");
            }
        } else if (value instanceof Iterable) {
            final int i[]= {0};

            ((Iterable<?>) value).forEach( vv-> {
                sw.append('\n');
                writeField(sw, ow, baseIndent + "  ", "[%d]".formatted(i[0]++), vv);
            });
        } else {
            sw.append("  ");
            try {
                ow.writeValue(sw, value);
            } catch (IOException e) {
                sw.append("  ");
//                sw.write(e.getMessage());
                sw.write( value.toString());
            }
        }

        sw.write('\n');
        return sw;
    }
}
