package com.paymentprocessor.clearingservice.config;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/** WebClient used by the REST transport to reach the external clearing application. */
@Configuration
@ConditionalOnProperty(name = "clearing.transport.mock", havingValue = "false")
public class WebClientConfig {

    @Bean
    public WebClient clearingWebClient(ClearingProperties properties) {
        ClearingProperties.Transport t = properties.transport();

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, t.connectTimeoutMs())
                .responseTimeout(Duration.ofMillis(t.responseTimeoutMs()));

        // Allow larger clearing payloads (default buffer is 256 KB).
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();

        return WebClient.builder()
                .baseUrl(t.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .build();
    }
}
