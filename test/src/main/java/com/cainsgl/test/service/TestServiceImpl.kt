package com.cainsgl.test.service

import com.cainsgl.common.service.test.TestService
import org.springframework.stereotype.Service

@Service
class TestServiceImpl : TestService
{
    override fun sayHello(who: String): String
    {
        return "Hello $who"
    }
}
