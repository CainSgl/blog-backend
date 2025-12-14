package com.cainsgl.common.dto.response

import com.cainsgl.common.dto.response.ResultCode.*
import com.cainsgl.common.exception.BSystemException
import com.cainsgl.common.exception.BusinessException
import com.cainsgl.common.util.TraceIdUtils

data class Result(
    var code: Int = 0,
    var msg: String? = null,
    var data: Any? = null,
    var traceId: String? = null,
    var debug:String?=null,
)
{

    companion object
    {
        private val SUCCESS_DEFAULT_RESULT = Result(SUCCESS.code, SUCCESS.message, null, null)
        @JvmStatic
        fun success(body: Any?): Result
        {
            return Result(SUCCESS.code, SUCCESS.message, body, null)
        }

        @JvmStatic
        fun success(): Result
        {
            return SUCCESS_DEFAULT_RESULT
        }
        //未捕获的异常
        @JvmStatic
        fun error(exception: Exception): Result
        {
            return Result(SYSTEM_ERROR.code, "fail", null, TraceIdUtils.getTraceId(),exception.message)
        }
        //业务异常
        @JvmStatic
        fun error(exception: BusinessException): Result
        {
            if(exception.resultCode!=null)
            {
                return Result(exception.resultCode!!.code,exception.message )
            }
            return Result(BUSINESS_ERROR.code, exception.message)
        }

        //系统异常
        @JvmStatic
        fun error(exception: BSystemException): Result
        {
            return Result(SYSTEM_ERROR.code, exception.message, null, TraceIdUtils.getTraceId())
        }
    }
}
