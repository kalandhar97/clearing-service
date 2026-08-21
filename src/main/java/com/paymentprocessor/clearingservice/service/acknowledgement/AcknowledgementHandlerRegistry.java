package com.paymentprocessor.clearingservice.service.acknowledgement;

import com.paymentprocessor.clearingservice.domain.enums.AckStatus;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Resolves the acknowledgement handler for a given inbound status. */
@Component
public class AcknowledgementHandlerRegistry {

    private final Map<AckStatus, AcknowledgementHandler> handlers = new EnumMap<>(AckStatus.class);

    public AcknowledgementHandlerRegistry(List<AcknowledgementHandler> discovered) {
        for (AcknowledgementHandler handler : discovered) {
            handlers.put(handler.supportedStatus(), handler);
        }
    }

    public AcknowledgementHandler get(AckStatus status) {
        AcknowledgementHandler handler = handlers.get(status);
        if (handler == null) {
            throw new IllegalStateException("No handler registered for acknowledgement status " + status);
        }
        return handler;
    }
}
