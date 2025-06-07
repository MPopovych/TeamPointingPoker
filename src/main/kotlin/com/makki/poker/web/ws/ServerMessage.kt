package com.makki.poker.web.ws

import com.makki.poker.assets.PokerSession
import com.makki.poker.web.dto.PokerSessionDto
import com.makki.poker.web.dto.UserDto

enum class ServerMessageType {
    NOTIFICATION,
    ERROR, // personalised
    SYNC, // personalised
    SYNC_META,
    USER_LIST_UPDATE,
}

abstract class ServerMessage<T>(
    val type: ServerMessageType,
) {
    abstract val data: T
}

data class ServerNotificationMessage(
    override val data: String
) : ServerMessage<String>(ServerMessageType.NOTIFICATION) {
    companion object {
        fun from(msg: String): ServerNotificationMessage {
            return ServerNotificationMessage(msg)
        }
    }
}

data class ServerErrorMessage(
    override val data: String
) : ServerMessage<String>(ServerMessageType.ERROR) {
    companion object {
        fun from(msg: String): ServerErrorMessage {
            return ServerErrorMessage(msg)
        }
    }
}

data class ServerSyncMessage(
    override val data: PokerSessionDto
) : ServerMessage<PokerSessionDto>(ServerMessageType.SYNC) {
    companion object {
        fun fromSession(session: PokerSession): ServerSyncMessage {
            return ServerSyncMessage(PokerSessionDto.from(session))
        }
    }
}

data class ServerUserListMessage(
    override val data: UserList
) : ServerMessage<UserList>(ServerMessageType.USER_LIST_UPDATE) {
    companion object {
        fun from(pokerSession: PokerSession): ServerUserListMessage {
            return ServerUserListMessage(
                UserList(
                    members = pokerSession.members.mapValues { UserDto.from(it.value) },
                    observers = pokerSession.observers.mapValues { UserDto.from(it.value) }
                )
            )
        }
    }
}

data class UserList(
    val members: Map<String, UserDto>,
    val observers: Map<String, UserDto>
)

data class ServerSyncMetaMessage(
    override val data: SessionMeta
) : ServerMessage<SessionMeta>(ServerMessageType.SYNC_META) {
    companion object {
        fun from(pokerSession: PokerSession): ServerSyncMetaMessage {
            return ServerSyncMetaMessage(
                SessionMeta(
                    title = pokerSession.title,
                    description = pokerSession.description
                )
            )
        }
    }
}

data class SessionMeta(
    val title: String?,
    val description: String?,
)
