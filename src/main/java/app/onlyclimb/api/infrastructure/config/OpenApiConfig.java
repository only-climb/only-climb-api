package app.onlyclimb.api.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI onlyClimbOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Only Climb API")
                        .description("AI-powered climbing training platform — REST API")
                        .version("v1")
                        .contact(new Contact().name("Only Climb"))
                        .license(new License().name("Proprietary")));
    }
}
