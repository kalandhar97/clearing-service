package com.paymentprocessor.clearingservice.service.format;

import com.paymentprocessor.clearingservice.domain.enums.ClearingFormat;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Resolves the {@link ClearingMessageFormatter} for a given format. */
@Component
public class FormatterRegistry {

    private final Map<ClearingFormat, ClearingMessageFormatter> formatters =
            new EnumMap<>(ClearingFormat.class);

    public FormatterRegistry(List<ClearingMessageFormatter> discovered) {
        for (ClearingMessageFormatter f : discovered) {
            formatters.put(f.format(), f);
        }
    }

    public ClearingMessageFormatter get(ClearingFormat format) {
        ClearingMessageFormatter f = formatters.get(format);
        if (f == null) {
            throw new IllegalStateException("No formatter registered for format " + format);
        }
        return f;
    }
}
