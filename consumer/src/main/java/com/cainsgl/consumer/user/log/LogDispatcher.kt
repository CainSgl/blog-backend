package com.cainsgl.consumer.user.log

import com.cainsgl.common.entity.user.UserLogEntity
import com.cainsgl.common.exception.BSystemException
import com.cainsgl.consumer.user.log.context.LogProcessContext
import com.cainsgl.consumer.user.log.context.PostProcessor
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Collections.emptyList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Component
class LogDispatcher
{
    @Resource
    private lateinit var logPipelineManager: LogPipelineManager

    fun batchDispatch(logs: List<UserLogEntity>): List<UserLogEntity>
    {
        val logPipeline = logPipelineManager.accumulate(logs)
        return logPipelineManager.tryStartPipeline(logPipeline)
    }
}

private val log = KotlinLogging.logger {}

@Component
class LogPipelineManager(handlers: List<LogHandler>)
{
    private val handlerMap: Map<String, LogHandler> = mutableMapOf()

    init
    {
        require(handlers.isNotEmpty()) { "日志处理器为空" }
        val handlerMutableMap = handlerMap as MutableMap<String, LogHandler>
        var logActionCount = 0
        for (entry in UserLogEntity.ACTIONS_SET)
        {
            logActionCount += entry.value.size
        }
        require(handlers.size >= logActionCount) {
            //看下到底是缺少哪些日志处理器
            val actionList = mutableListOf<String>()
            for (entry in UserLogEntity.ACTIONS_SET)
            {
                for (action in entry.value)
                {
                    actionList.add("${entry.key}.${action}")
                }
            }
            handlers.forEach { handler -> actionList.remove(handler.supportType()) }
            "缺少以下日志处理器: $actionList"
        }
        handlers.forEach { handler ->
            val supportType = handler.supportType()
            require(UserLogEntity.validAction(supportType)) { "日志行为类型无效，你应该在UserLogEntity里添加对应的行为类型" }
            handlerMutableMap[supportType] = handler
        }
    }
    @Value("\${userLog.batchNumber}")
    var batchNumber:Int=20
//    companion object
//    {
//        //TODO ，目前设置为20条
//        const val MAX_PIPELINE_SIZE = 20
//    }

    private val backlogLogs: MutableList<UserLogEntity> = mutableListOf()
    private val lock = ReentrantLock()


    fun accumulate(items: List<UserLogEntity>): LogPipeline?
    {
        lock.withLock {
            backlogLogs.addAll(items)
            if (backlogLogs.size > batchNumber)
            {
                //开始处理
                val logs: List<UserLogEntity> = ArrayList(backlogLogs)
                val logPipeline = LogPipeline(logs, handlerMap)
                backlogLogs.clear()
                return logPipeline
            } else
            {
                return null
            }
        }
    }

    fun tryStartPipeline(logPipeline: LogPipeline?): List<UserLogEntity>
    {
        if (logPipeline != null)
        {
            val context = logPipeline.start()
            //处理异常
            if (context.hasFailure())
            {
                val failures = context.getFailure()
                val failureInfos = context.getFailureInfo()
                val errors = context.getError()
                for ((index, failure) in failures.withIndex())
                {
                    log.error(errors[index]) { "日志处理失败:${failure.id} ${failure.action} ${failure.userId} \n因为: ${failureInfos[index]}" }
                }
                return failures
            }
            return emptyList()
        }
        return emptyList()
    }


}

class LogPipeline(private val logs: List<UserLogEntity>, private val handlerMap: Map<String, LogHandler>)
{
    fun start(): LogProcessContext
    {
        val context = LogProcessContext(logs)
        for (log in logs)
        {
            val handler = handlerMap[log.action]
            if (handler != null)
            {
                doProcess(context, handler)
            } else
            {
                context.addFailureItem("日志行为类型无效", BSystemException("${context.current()}找不到对应的日志处理器"))
            }
        }
        context.abort()
        //进行收尾工作，合并所有可合并的PostProcessor
        for (processor in mergePostProcessors(context))
        {
            processor.doProcess(context)
        }
        return context
    }

    /**
     * 不断尝试合并所有PostProcessor，直到没有任何处理器可以合并为止
     */
    private fun mergePostProcessors(context: LogProcessContext): List<PostProcessor>
    {
        val processors = context.getPostProcessors().toMutableList()
        while (tryMergeOnce(processors, context))
        {
            // 继续合并直到无法合并
        }
        return processors
    }

    /**
     * 尝试合并一次，返回是否有合并发生
     */
    private fun tryMergeOnce(processors: MutableList<PostProcessor>, context: LogProcessContext): Boolean
    {
        for (i in processors.indices)
        {
            for (j in i + 1 until processors.size)
            {
                val combined = processors[i].tryCombine(processors[j], context)
                if (combined != null)
                {
                    processors.removeAt(j)
                    processors.removeAt(i)
                    processors.add(combined)
                    return true
                }
            }
        }
        return false
    }

    private fun doProcess(context: LogProcessContext, handler: LogHandler)
    {
        if (context.isAborted())
        {
            throw BSystemException("日志处理上下文已经被中断")
        }
        if (context.hasNext())
        {
            context.next()
            try
            {
                handler.process(context)
            } catch (e: Exception)
            {
                context.addFailureItem("日志处理器抛出异常", e)
            }
        }
    }

}