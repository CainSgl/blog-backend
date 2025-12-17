package com.cainsgl.common.config.excption

import cn.dev33.satoken.exception.NotLoginException
import cn.dev33.satoken.exception.NotPermissionException
import cn.dev33.satoken.exception.NotRoleException
import cn.dev33.satoken.exception.SaTokenException
import com.cainsgl.common.dto.response.Result
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.exception.BSystemException
import com.cainsgl.common.exception.BusinessException
import com.cainsgl.common.util.TraceIdUtils
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import jakarta.servlet.ServletException
import org.slf4j.LoggerFactory
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException


@RestControllerAdvice
class GlobalExceptionHandler
{
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): Result
    {
        log.error("系统异常", e)
        return Result.error(e)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArg(e: IllegalArgumentException): Any
    {
        log.debug("参数检验出现异常", e)
        return Result.error(e)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException): Any
    {
        //一般是前端参数传字段有问题
        log.debug("调用方法的参数有问题", e)
        return Result(ResultCode.DATA_FORMAT_ERROR.code, ResultCode.DATA_FORMAT_ERROR.message, null, TraceIdUtils.getTraceId(), debug = e.message)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(e: HttpMessageNotReadableException): Any
    {
        val traceId = TraceIdUtils.getTraceId()
        log.warn("JSON解析失败，traceId: {},{}", traceId, e.message)
        log.debug("请求出错", e)
        return when (val rootCause = e.rootCause)
        {
            is NullPointerException        -> Result(ResultCode.MISSING_PARAM.code, ResultCode.MISSING_PARAM.message, null, debug = rootCause.message)
            is ValueInstantiationException -> Result(ResultCode.DESERIALIZATION_FAILED.code, ResultCode.DESERIALIZATION_FAILED.message, null, debug = rootCause.message)
            //参数格式问题
            is InvalidFormatException      -> Result(ResultCode.DATA_FORMAT_ERROR.code, ResultCode.DATA_FORMAT_ERROR.message, null, debug = rootCause.message)
            else                           -> Result(ResultCode.PARAM_FORMAT_ERROR.code, ResultCode.PARAM_FORMAT_ERROR.message, null, debug = rootCause?.message)
        }
    }

    @ExceptionHandler(ServletException::class)
    fun handleException(e: ServletException): Result
    {
        return Result.error(e)
    }

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): Result
    {
        log.warn("业务异常 {}", e.message)
        return Result.error(e)
    }

    @ExceptionHandler(BSystemException::class)
    fun handleSystemExceptionException(e: BSystemException): Result
    {
        return Result.error(e)
    }

    @ExceptionHandler(SaTokenException::class)
    fun handleNotLoginException(e: SaTokenException): Any
    {
        if (e is NotLoginException)
        {
            return ResultCode.USER_NOT_LOGIN
        }
        if (e is NotRoleException)
        {
            return ResultCode.NOT_ROLE
        }
        if (e is NotPermissionException)
        {
            return ResultCode.PERMISSION_DENIED
        }
        log.error("未知的Satoken异常", e)
        return ResultCode.UNKOWN_SATOKEN_ERROR
    }
    //这是一样的，一般是post或者其他复杂请求体的检验出错
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException::class)
    fun handleValidationError(ex: org.springframework.web.bind.MethodArgumentNotValidException): Any
    {

        val transform: (FieldError) -> CharSequence =
            { error -> "`${error.field}`:${error.defaultMessage ?: "参数校验失败\n"}" }
        val errMsg =
            ex.bindingResult.fieldErrors.joinToString(prefix = "[", postfix = "]", transform = transform)
        return Result(ResultCode.PARAM_INVALID.code, "fail", null, traceId = TraceIdUtils.getTraceId(), debug = errMsg)
    }
    //这个是方法传参错误，一般是前端传的get请求
    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleValidationError(ex: HandlerMethodValidationException): Any
    {
        val errorMessages = ex.allValidationResults.flatMap { validationResult ->
            validationResult.resolvableErrors.map { error ->
                val paramName = validationResult.methodParameter.parameterName ?: "unknown"
                val message = error.defaultMessage ?: "参数校验失败"
                "`$paramName` because $message"
            }
        }
        val errMsg = errorMessages.joinToString(prefix = "[", postfix = "]")
        return Result(ResultCode.PARAM_INVALID.code, "fail", null, traceId = TraceIdUtils.getTraceId(), debug = errMsg)
    }

}
