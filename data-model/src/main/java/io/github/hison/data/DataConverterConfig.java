package io.github.hison.data;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataConverterConfig {
    @Bean
    @ConditionalOnMissingBean(DataConverter.class)
    public DataConverter dataConverter() {
        return new DataConverterDefault();
    }
}
