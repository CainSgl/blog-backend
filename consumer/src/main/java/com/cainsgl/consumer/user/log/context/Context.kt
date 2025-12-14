package com.cainsgl.consumer.user.log.context

import com.cainsgl.common.entity.user.UserLogEntity
import com.cainsgl.common.exception.BSystemException

interface ProcessContext
{
    fun current(): Any
    fun getAttribute(key: String): Any?
    fun setAttribute(key: String, value: Any)
    fun addPostProcessor(postProcessor: PostProcessor)
}

interface PostProcessor
{
    fun tryCombine(other: PostProcessor, context: ProcessContext): PostProcessor?
    fun doProcess(context: ProcessContext)
}


class LogPostProcessor(private val key:String,private val doProcessFunc:(LogProcessContext)->Unit) : PostProcessor
{
    override fun tryCombine(other: PostProcessor, context: ProcessContext): PostProcessor?
    {
        if(other !is LogPostProcessor)
        {
            return null
        }
        //尝试合并，只要key相同，那么直接合并
        if(other.key == this.key)
        {
            return this
        }
        return null
    }

    override fun doProcess(context: ProcessContext)
    {
        val logProcessContext= context as? LogProcessContext
        if(logProcessContext != null)
        {
            doProcessFunc(logProcessContext)
        }else
        {
            throw BSystemException("日志上下文传递有问题，类型错误,期望的类型是LogProcessContext，实际上是"+context::class.simpleName)
        }
    }


}

/**
 * 日志处理上下文
 */
class LogProcessContext(private var logList: List<UserLogEntity>) : ProcessContext
{
    private val attributes: MutableMap<String, Any> = mutableMapOf()
    private lateinit var currentLog: UserLogEntity

    //开放给处理器用的
    override fun current(): UserLogEntity
    {
        return currentLog
    }

    override fun getAttribute(key: String): Any?
    {
        return attributes[key]
    }
    fun getAttributes(): Map<String, Any>
    {
        return attributes
    }
    override fun setAttribute(key: String, value: Any)
    {
        attributes[key] = value
    }
    private val postProcessors: MutableList<PostProcessor> = mutableListOf()
    override fun addPostProcessor(postProcessor: PostProcessor)
    {
        postProcessors.add(postProcessor)
    }
    //收尾处理
    fun getPostProcessors(): List<PostProcessor>
    {
        return postProcessors
    }

    //异常处理
    private val failures: MutableList<UserLogEntity> = mutableListOf()
    private val failureInfos: MutableList<String> = mutableListOf()
    private val errors: MutableList<Exception> = mutableListOf()
    fun addFailureItem(info: String, error: Exception)
    {
        failures.add(currentLog)
        failureInfos.add(info)
        errors.add(error)
    }

    fun hasFailure(): Boolean
    {
        return failures.isNotEmpty()
    }

    fun getFailure(): List<UserLogEntity>
    {
        return failures
    }

    fun getFailureInfo(): List<String>
    {
        return failureInfos
    }

    fun getError(): List<Exception>
    {
        return errors
    }

    //流程控制
    private var index = 0
    fun next()
    {
        if (index >= logList.size)
        {
            throw NoSuchElementException()
        }
        currentLog = logList[index]
        index++
    }

    fun hasNext(): Boolean
    {
        return index < logList.size
    }

    private var isAbort: Boolean = false
    fun abort()
    {
        isAbort = true
    }

    fun isAborted(): Boolean
    {
        return isAbort
    }
}