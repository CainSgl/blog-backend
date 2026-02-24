package com.cainsgl.common.mq


interface MessagePublisher {
    fun publish(topic: String, message: Any)
}
