package com.cainsgl.user.controller

import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.user.service.UserExtraInfoServiceImpl
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/user/hotInfo")
class HotInfoController
{
    @Resource
    lateinit var userExtraInfoServiceImpl: UserExtraInfoServiceImpl

    @GetMapping
    fun getHotInfo(@RequestParam id:Long): Any?
    {
        if(id<0)
        {
            return ResultCode.RESOURCE_NOT_FOUND
        }
        return userExtraInfoServiceImpl.getBySaveOnNull(id)
    }

}