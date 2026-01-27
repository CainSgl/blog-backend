package com.cainsgl.common.exception

import com.cainsgl.common.dto.response.ResultCode

class BusinessException(message: String) : RuntimeException(message)
{
    var resultCode:ResultCode?=null
    constructor(resultCode: ResultCode) : this(resultCode.message)
}
