package com.paymentprocessor.clearingservice.service.acknowledgement;

import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.domain.enums.AckStatus;
import com.paymentprocessor.clearingservice.domain.enums.BatchStatus;
import com.paymentprocessor.clearingservice.dto.AcknowledgementRequest;
import com.paymentprocessor.clearingservice.web.exception.ConflictException;
import org.springframework.stereotype.Component;

/** Handler for acknowledgements that only confirm receipt by the participant. */
@Component
public class AcknowledgedHandler implements AcknowledgementHandler {

    @Override
    public AckStatus supportedStatus() {
        return AckStatus.ACKNOWLEDGED;
    }

    @Override
    public void handle(ClearingBatch batch, AcknowledgementRequest request, AcknowledgementHandlerContext context) {
        if (batch.getStatus() == BatchStatus.SENT) {
            batch.transitionTo(BatchStatus.ACKNOWLEDGED);
        } else if (batch.getStatus() != BatchStatus.ACKNOWLEDGED) {
            throw new ConflictException("Cannot acknowledge batch " + batch.getReference()
                    + " in state " + batch.getStatus());
        }
    }
}
