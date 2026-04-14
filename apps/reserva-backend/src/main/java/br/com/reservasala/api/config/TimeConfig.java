package br.com.reservasala.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        // Mantemos toda a aplicação em UTC para evitar deslocamentos de horário
        return Clock.systemUTC();
    }
}
