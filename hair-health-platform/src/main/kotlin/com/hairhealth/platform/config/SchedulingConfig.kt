package com.hairhealth.platform.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
class SchedulingConfig {
    // Spring will automatically detect @Scheduled methods
}