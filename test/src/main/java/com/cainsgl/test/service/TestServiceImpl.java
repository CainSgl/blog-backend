package com.cainsgl.test.service;

import com.cainsgl.common.service.TestService;
import org.springframework.stereotype.Service;

@Service
public class TestServiceImpl implements TestService
{
    @Override
    public String sayHello(String who)
    {
        return "Hello " + who;
    }
}
