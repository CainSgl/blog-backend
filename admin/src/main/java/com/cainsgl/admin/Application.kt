package com.cainsgl.admin

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["com.cainsgl.admin", "com.cainsgl.common"])
class Application

fun main(args: Array<String>) {
    SpringApplication.run(Application::class.java, *args)
}
