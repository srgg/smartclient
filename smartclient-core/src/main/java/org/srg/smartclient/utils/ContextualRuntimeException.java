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

    @SuppressWarnings("UnusedReturnValue")
    public StringWriter dumpContext_ifAny(StringWriter sw, String baseIndent, ObjectWriter objectWriter){
        final Object ctx = this.getContext();
        final Field[] fields = FieldUtils.getAllFields(ctx.getClass());

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
        sw.write(   "" + baseIndent + name.toUpperCase().trim() + ":");

        if (value instanceof String str) {
            final boolean isMultiline = str.indexOf('\n') != -1;

            if (isMultiline) {
                sw.write("\n\n");
                sw.write(
                        str.indent(baseIndent.length()+2)
                );
            } else {
                sw.write("  ");
                sw.write(str.trim());
            }

            if (!str.endsWith("\n")) {
                sw.write("\n");
            }
        } else {
            try {
                ow.writeValue(sw, value);
            } catch (IOException e) {
                sw.write(e.getMessage());
            }
        }

        sw.write('\n');
        return sw;
    }
}
