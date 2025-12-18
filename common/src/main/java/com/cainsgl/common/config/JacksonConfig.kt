package com.cainsgl.common.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean

@AutoConfiguration
class JacksonConfig {
    
    @Bean
    fun kotlinModule(): KotlinModule {
        return KotlinModule.Builder().build()
    }
}