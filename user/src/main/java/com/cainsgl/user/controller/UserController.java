package com.cainsgl.user.controller;

import com.cainsgl.common.service.TestService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController
{
    @Resource
    TestService testService;

    @GetMapping("/test")
    public Object test() {
        try{
            return testService.sayHello("test");
        }catch (Exception e){
            System.out.println(e.getMessage());
            System.out.println(e.getCause());
            e.printStackTrace();
            return "error";
        }

    }

}
