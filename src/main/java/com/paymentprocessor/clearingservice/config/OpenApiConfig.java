package com.paymentprocessor.clearingservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI clearingOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Clearing Service API")
                .version("1.0.0")
                .description("Prepares, validates, batches and transmits clearing files to the "
                        + "external clearing application, and tracks acknowledgements.")
                .license(new License().name("Proprietary")));
    }
}
