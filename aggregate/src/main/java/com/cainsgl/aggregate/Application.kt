package com.cainsgl.aggregate

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType

/**
 * 聚合模块启动类
 * 扫描所有业务模块的包，实现单体架构
 */
@SpringBootApplication
@ComponentScan(
    basePackages = ["com.cainsgl"],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE, classes = [
                com.cainsgl.user.Application::class,
                com.cainsgl.user.Application.AppNamePrinter::class,
                com.cainsgl.article.Application::class,
                com.cainsgl.article.Application.AppNamePrinter::class,
                com.cainsgl.test.Application::class,
                com.cainsgl.test.Application.AppNamePrinter::class,
            ]
        )
    ]
)
class Application {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(Application::class.java, *args)
        }
    }
}
