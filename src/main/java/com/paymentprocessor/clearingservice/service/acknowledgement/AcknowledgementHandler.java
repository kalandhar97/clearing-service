package com.paymentprocessor.clearingservice.service.acknowledgement;

import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.domain.enums.AckStatus;
import com.paymentprocessor.clearingservice.dto.AcknowledgementRequest;

/**
 * Strategy that processes one type of inbound acknowledgement. Implementations
 * apply the state transition and any side effects (audit, events, transaction
 * updates) for a single acknowledgement status.
 */
public interface AcknowledgementHandler {

    AckStatus supportedStatus();

    void handle(ClearingBatch batch, AcknowledgementRequest request, AcknowledgementHandlerContext context);
}
