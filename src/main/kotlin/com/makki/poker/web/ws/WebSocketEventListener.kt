package com.makki.poker.web.ws

import com.makki.poker.assets.User
import com.makki.poker.service.SessionService
import com.makki.poker.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionConnectedEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent

@Component
class WebSocketEventListener(
    private val sessionService: SessionService,
    private val messagingTemplate: SimpMessageSendingOperations
) {
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    @EventListener
    fun handleWebSocketConnectListener(event: SessionConnectedEvent) {
        logger.info("Received a new connection event")
    }

    @EventListener
    fun handleWebSocketDisconnectListener(event: SessionDisconnectEvent) {
        logger.info("Received a disconnect event")

        val headerAccessor = SimpMessageHeaderAccessor.wrap(event.message)
        val userId = headerAccessor.sessionAttributes?.get("user") as? User?
            ?: return

        val userRef = userId.toRef()

        sessionService.getUserObservedSessions(userRef).forEach { session ->
            session.removeObserver(userRef)
            messagingTemplate.convertAndSend(
                "/topic/session/${session.id}/sync",
                ServerUserListMessage.from(session)
            )
        }
    }
}