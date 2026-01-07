package com.cainsgl.aggregate

import org.mybatis.spring.annotation.MapperScan
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * 聚合模块启动类
 * 扫描所有业务模块的包，实现单体架构
 */
@EnableScheduling
//@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
@SpringBootApplication
@ComponentScan(
    basePackages = ["com.cainsgl"],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = [
                "com\\.cainsgl\\.user\\.Application",
                "com\\.cainsgl\\.user\\.Application\\\$AppNamePrinter",
                "com\\.cainsgl\\.article\\.Application",
                "com\\.cainsgl\\.article\\.Application\\\$AppNamePrinter",
                "com\\.cainsgl\\.ai\\.Application",
                "com\\.cainsgl\\.ai\\.Application\\\$AppNamePrinter",
                "com\\.cainsgl\\.consumer\\.Application",
                "com\\.cainsgl\\.consumer\\.Application\\\$AppNamePrinter",
                "com\\.cainsgl\\.scheduler\\.Application",
                "com\\.cainsgl\\.scheduler\\.Application\\\$AppNamePrinter",
                "com\\.cainsgl\\.file\\.Application",
                "com\\.cainsgl\\.file\\.Application\\\$AppNamePrinter",
                "com\\.cainsgl\\.comment\\.Application",
                "com\\.cainsgl\\.comment\\.Application\\\$AppNamePrinter"
            ]
        )
    ]
)
@MapperScan("com.cainsgl.**.repository")
class Application
{
    companion object
    {
        @JvmStatic
        fun main(args: Array<String>)
        {
            SpringApplication.run(Application::class.java, *args)
        }
    }
}
