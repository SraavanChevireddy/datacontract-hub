package com.sraavan.dchub.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dataContractHubOpenApi() {
        return new OpenAPI().info(new Info()
                .title("datacontract-hub API")
                .version("0.1.0")
                .description("Data-contract governance: versioned schemas, compatibility "
                        + "enforcement, approval workflow, and audit trail.")
                .license(new License().name("MIT")));
    }
}
