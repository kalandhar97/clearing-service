package com.paymentprocessor.clearingservice.service.transport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.clearingservice.config.ClearingProperties;
import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.domain.ClearingFile;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Submits clearing files to the external clearing application over HTTPS/REST
 * using {@link WebClient}. Active when {@code clearing.transport.mock=false}.
 */
@Component
@ConditionalOnProperty(name = "clearing.transport.mock", havingValue = "false")
public class RestClearingTransport implements ClearingTransport {

    private static final Logger log = LoggerFactory.getLogger(RestClearingTransport.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String submitPath;
    private final String apiKey;

    public RestClearingTransport(WebClient clearingWebClient,
                                 ObjectMapper objectMapper,
                                 ClearingProperties properties) {
        this.webClient = clearingWebClient;
        this.objectMapper = objectMapper;
        this.submitPath = properties.transport().submitPath();
        this.apiKey = properties.transport().apiKey();
    }

    @Override
    public SubmissionReceipt submit(ClearingBatch batch, ClearingFile file, byte[] content) {
        RestSubmissionPayload payload = new RestSubmissionPayload(
                batch.getReference(),
                batch.getNetwork().name(),
                batch.getCurrency(),
                batch.getSettlementDate().toString(),
                batch.getFormat().name(),
                file.getRecordCount(),
                file.getControlTotalMinor(),
                file.getContentHash(),
                file.getFileName(),
                Base64.getEncoder().encodeToString(content));

        try {
            String body = webClient.post()
                    .uri(submitPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> {
                        if (apiKey != null && !apiKey.isBlank()) {
                            h.set("X-Api-Key", apiKey);
                        }
                    })
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            String receiptReference = extractReceiptReference(body, batch.getReference());
            log.info("Submitted batch {} to clearing application; receipt {}",
                    batch.getReference(), receiptReference);
            return new SubmissionReceipt(receiptReference, 200, body);

        } catch (WebClientResponseException e) {
            int status = e.getStatusCode().value();
            boolean retryable = status >= 500 || status == 429;
            throw new TransportException(
                    "Clearing application returned HTTP " + status + " for batch " + batch.getReference(),
                    retryable, e);
        } catch (TransportException e) {
            throw e;
        } catch (Exception e) {
            // Connection resets, timeouts, DNS failures, etc. are transient.
            throw new TransportException(
                    "Transport failure submitting batch " + batch.getReference() + ": " + e.getMessage(),
                    true, e);
        }
    }

    private String extractReceiptReference(String body, String fallback) {
        if (body == null || body.isBlank()) {
            return fallback;
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            for (String field : new String[]{"receiptReference", "receipt", "id", "reference"}) {
                JsonNode v = node.get(field);
                if (v != null && !v.isNull()) {
                    return v.asText();
                }
            }
        } catch (Exception ex) {
            log.debug("Non-JSON clearing response; using batch reference as receipt", ex);
        }
        return fallback;
    }
}
