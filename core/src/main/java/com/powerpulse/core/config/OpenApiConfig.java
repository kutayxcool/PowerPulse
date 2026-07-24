package com.powerpulse.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI powerPulseOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("PowerPulse Core API")
                        .description(
                                "Gerçek zamanlı enerji tüketimi, kota, fatura "
                                        + "ve anomali yönetimi API'si."
                        )
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("PowerPulse Team")));
    }
}