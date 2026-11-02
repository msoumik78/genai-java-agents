package com.genai.agents.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.agents.store.JdbcDurableStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DurableStoreConfig {

    @Bean
    JdbcDurableStore durableStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        JdbcDurableStore store = new JdbcDurableStore(jdbcTemplate, objectMapper);
        store.init();
        return store;
    }
}
