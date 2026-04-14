package br.com.reservasala.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// CORS removido daqui — gerenciado exclusivamente pelo SecurityConfig.corsConfigurationSource()
// para evitar conflito entre múltiplas fontes de CORS.
@Configuration
public class WebConfig implements WebMvcConfigurer {
}
