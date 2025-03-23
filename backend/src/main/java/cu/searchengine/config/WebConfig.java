package cu.searchengine.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")  // Adjust this to specific origins in production @TODO: CHANGE THIS TO DEPLOYMENT URI
                .allowedMethods("GET", "POST")
                .allowedHeaders("*");
    }
}
