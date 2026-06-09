package com.deadlock.hellocs.common.web.config;

import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(MeterFilter.class)
public class MetricsConfig {

    @Bean
    public MeterFilter denyJvmBuffer() {
        return MeterFilter.denyNameStartsWith("jvm.buffer");
    }

    @Bean
    public MeterFilter limitUriTags() {
        return MeterFilter.maximumAllowableTags(
                "http.server.requests", "uri", 100, MeterFilter.deny()
        );
    }
}
