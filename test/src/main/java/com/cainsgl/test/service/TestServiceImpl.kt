package com.cainsgl.test.service


import org.springframework.stereotype.Service

@Service
class TestServiceImpl
{
    fun sayHello(who: String): String
    {
        return "Hello $who"
    }
}
