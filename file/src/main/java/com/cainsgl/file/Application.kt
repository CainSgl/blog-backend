package com.cainsgl.file

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.stereotype.Component

@SpringBootApplication
@ComponentScan(basePackages = ["com.cainsgl.*"])
class Application {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(Application::class.java, *args)
        }
    }

    @Component
    class AppNamePrinter : CommandLineRunner {
        @Value("\${spring.application.name}")
        private lateinit var appName: String

        override fun run(vararg args: String) {
            println("\n\n\nspring.app.name: $appName")
        }
    }
}
