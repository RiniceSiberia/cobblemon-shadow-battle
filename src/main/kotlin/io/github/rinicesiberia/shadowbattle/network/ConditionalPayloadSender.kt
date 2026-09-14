package io.github.rinicesiberia.shadowbattle.network

/** 只在接收方支持指定消息时创建并发送消息。 */
internal object ConditionalPayloadSender {
    fun <R, P> deliver(
        recipient: R,
        payloadFactory: () -> P,
        supports: (R) -> Boolean,
        send: (R, P) -> Unit,
    ): Boolean {
        if (!supports(recipient)) return false
        send(recipient, payloadFactory())
        return true
    }
}
