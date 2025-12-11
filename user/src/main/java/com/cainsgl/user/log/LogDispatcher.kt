package com.cainsgl.user.log

import com.cainsgl.common.entity.user.UserLogEntity
import org.springframework.stereotype.Component

@Component
class LogDispatcher(handlers: List<LogHandler>)
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
            val actionList =mutableListOf<String>()
            for (entry in UserLogEntity.ACTIONS_SET)
            {
                for(action in entry.value)
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

    fun dispatch(log: UserLogEntity):Boolean
    {
       return  handlerMap[log.action]?.handle(log)
            ?: throw IllegalArgumentException("未知日志行为类型")
    }
}