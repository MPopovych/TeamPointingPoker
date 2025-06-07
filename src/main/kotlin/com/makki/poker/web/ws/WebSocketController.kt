package com.makki.poker.web.ws

import com.makki.poker.assets.MissingSessionException
import com.makki.poker.assets.PokerSession
import com.makki.poker.assets.UnauthenticatedException
import com.makki.poker.assets.User
import com.makki.poker.service.SessionService
import com.makki.poker.web.cmd.NewSessionRoundCmd
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class WebSocketController(
    private val sessionService: SessionService,
    private val messagingTemplate: SimpMessagingTemplate
) {
    @MessageMapping("{sessionId}/observe")
    fun observeSession(
        @DestinationVariable sessionId: String,
        headerAccessor: SimpMessageHeaderAccessor
    ) {
        val (user, session) = getUserAndSession(sessionId, headerAccessor)
        session.addObserver(user.toRef())

        // On observe - return the full data for the user
        messagingTemplate.convertAndSendToUser(
            user.id,
            "/topic/session/$sessionId/sync",
            ServerSyncMessage.fromSession(session)
        )
        messagingTemplate.convertAndSend(
            "/topic/session/$sessionId/sync",
            ServerUserListMessage.from(session)
        )
    }

    @MessageMapping("{sessionId}/newRound")
    fun newRoundInSession(
        @DestinationVariable sessionId: String,
        headerAccessor: SimpMessageHeaderAccessor
    ) {
        val (user, session) = getUserAndSession(sessionId, headerAccessor)

        val updatedSession = sessionService.startNewRound(
            session.id,
            user.toRef(),
            NewSessionRoundCmd(null, null)
        )
        messagingTemplate.convertAndSend(
            "/topic/session/$sessionId/sync",
            ServerSyncMessage.fromSession(updatedSession)
        )
    }

    @MessageMapping("{sessionId}/vote")
    fun voteInSession(
        @DestinationVariable sessionId: String,
        headerAccessor: SimpMessageHeaderAccessor,
        @Payload payload: ClientVoteMessage
    ) {
        val (user, session) = getUserAndSession(sessionId, headerAccessor)

        val updatedSession = sessionService.placeVote(
            session.id,
            user.toRef(),
            roundId = payload.data.roundId,
            teamId = payload.data.teamId,
            voteId = payload.data.voteId
        )
        messagingTemplate.convertAndSend(
            "/topic/session/$sessionId/sync",
            ServerSyncMessage.fromSession(updatedSession)
        )
    }

    @MessageMapping("{sessionId}/toggleReveal")
    fun toggleRevealInSessionRound(
        @DestinationVariable sessionId: String,
        headerAccessor: SimpMessageHeaderAccessor,
        @Payload payload: ClientVoteRevealMessage
    ) {
        val (user, session) = getUserAndSession(sessionId, headerAccessor)

        val updatedSession = sessionService.toggleVoteReveal(
            session.id,
            user.toRef(),
            roundId = payload.data.roundId
        )
        messagingTemplate.convertAndSend(
            "/topic/session/$sessionId/sync",
            ServerSyncMessage.fromSession(updatedSession)
        )
    }

    @MessageMapping("{sessionId}/editSession")
    fun editSessionMeta(
        @DestinationVariable sessionId: String,
        headerAccessor: SimpMessageHeaderAccessor,
        @Payload payload: ClientEditSessionMessage
    ) {
        val (user, session) = getUserAndSession(sessionId, headerAccessor)

        val updatedSession = sessionService.editSession(
            session.id,
            user.toRef(),
            title = payload.data.title,
            description = payload.data.description
        )
        messagingTemplate.convertAndSend(
            "/topic/session/$sessionId/sync",
            ServerSyncMetaMessage.from(updatedSession)
        )
    }

    private fun getUserAndSession(
        sessionId: String,
        headerAccessor: SimpMessageHeaderAccessor
    ): Pair<User, PokerSession> {
        // User will join the session via REST, this endpoint is for observing it
        val user = headerAccessor.sessionAttributes?.get("user") as? User
            ?: throw UnauthenticatedException()

        val session = try {
            sessionService.getSessionForUser(user.toRef(), sessionId)
        } catch (ex: Exception) {
            messagingTemplate.convertAndSendToUser(
                user.id,
                "/topic/errors",
                ex.message ?: "Session with ID $sessionId not found."
            )
            throw MissingSessionException()
        }

        return Pair(user, session)
    }
}