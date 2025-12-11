package com.cainsgl.common.config.excption

import com.cainsgl.common.exception.BSystemException
import com.cainsgl.common.exception.BusinessException
import io.grpc.*
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * gRPC 全局异常处理拦截器
 * 捕获 gRPC 服务中的所有异常，记录日志并返回合适的错误状态
 */
@GrpcGlobalServerInterceptor
@Component
@Order(Int.MIN_VALUE) // 确保最先执行，这样能捕获所有异常
class GrpcExceptionInterceptor : ServerInterceptor
{
    private val log = LoggerFactory.getLogger(GrpcExceptionInterceptor::class.java)

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT>
    {
        // 包装 ServerCall 以捕获响应阶段的异常
        val wrappedCall = ExceptionHandlingServerCall(call)
        
        return try
        {
            // 包装 Listener 以捕获请求处理阶段的异常
            ExceptionHandlingServerCallListener(
                next.startCall(wrappedCall, headers),
                wrappedCall,
                call.methodDescriptor.fullMethodName
            )
        } catch (e: Exception)
        {
            handleException(e, wrappedCall, call.methodDescriptor.fullMethodName)
            object : ServerCall.Listener<ReqT>() {}
        }
    }

    /**
     * 包装 ServerCall，用于处理响应阶段的异常
     */
    private inner class ExceptionHandlingServerCall<ReqT, RespT>(
        delegate: ServerCall<ReqT, RespT>
    ) : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(delegate)
    {
        @Volatile
        private var closed = false

        override fun close(status: Status, trailers: Metadata)
        {
            if (closed) return
            closed = true
            super.close(status, trailers)
        }

        fun closeWithException(status: Status, trailers: Metadata)
        {
            if (closed) return
            closed = true
            super.close(status, trailers)
        }

        fun isClosed(): Boolean = closed
    }

    /**
     * 包装 Listener，用于捕获请求处理阶段的异常
     */
    private inner class ExceptionHandlingServerCallListener<ReqT, RespT>(
        private val delegate: ServerCall.Listener<ReqT>,
        private val call: ExceptionHandlingServerCall<ReqT, RespT>,
        private val methodName: String
    ) : ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(delegate)
    {
        override fun onMessage(message: ReqT)
        {
            try
            {
                super.onMessage(message)
            } catch (e: Exception)
            {
                handleException(e, call, methodName)
            }
        }

        override fun onHalfClose()
        {
            try
            {
                super.onHalfClose()
            } catch (e: Exception)
            {
                handleException(e, call, methodName)
            }
        }

        override fun onCancel()
        {
            try
            {
                super.onCancel()
            } catch (e: Exception)
            {
                // onCancel 的异常只记录日志，不需要返回错误
                log.warn("[gRPC] {} onCancel异常: {}", methodName, e.message, e)
            }
        }

        override fun onComplete()
        {
            try
            {
                super.onComplete()
            } catch (e: Exception)
            {
                log.warn("[gRPC] {} onComplete异常: {}", methodName, e.message, e)
            }
        }

        override fun onReady()
        {
            try
            {
                super.onReady()
            } catch (e: Exception)
            {
                handleException(e, call, methodName)
            }
        }
    }

    /**
     * 统一异常处理：记录日志 + 返回错误状态
     */
    private fun <ReqT, RespT> handleException(
        e: Exception,
        call: ExceptionHandlingServerCall<ReqT, RespT>,
        methodName: String
    )
    {
        val (status, logLevel) = when (e)
        {
            is BusinessException ->
            {
                log.warn("[gRPC] {} 业务异常: {}", methodName, e.message)
                Status.INVALID_ARGUMENT.withDescription(e.message) to "WARN"
            }

            is BSystemException ->
            {
                log.error("[gRPC] {} 系统异常: {}", methodName, e.message, e)
                Status.INTERNAL.withDescription(e.message) to "ERROR"
            }

            is IllegalArgumentException ->
            {
                log.warn("[gRPC] {} 参数异常: {}", methodName, e.message)
                Status.INVALID_ARGUMENT.withDescription(e.message) to "WARN"
            }

            is IllegalStateException ->
            {
                log.warn("[gRPC] {} 状态异常: {}", methodName, e.message)
                Status.FAILED_PRECONDITION.withDescription(e.message) to "WARN"
            }

            is StatusRuntimeException ->
            {
                // gRPC 自身的异常，直接透传
                log.error("[gRPC] {} gRPC状态异常: {}", methodName, e.status, e)
                e.status to "ERROR"
            }

            else ->
            {
                // 未知异常，记录完整堆栈
                log.error("[gRPC] {} 未捕获异常: {}", methodName, e.message, e)
                Status.INTERNAL.withDescription("内部服务错误: ${e.message}") to "ERROR"
            }
        }

        // 返回错误给客户端
        if (!call.isClosed())
        {
            call.closeWithException(status, Metadata())
        }
    }
}
