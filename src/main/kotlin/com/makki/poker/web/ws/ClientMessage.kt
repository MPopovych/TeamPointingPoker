package com.makki.poker.web.ws

enum class ClientMessageType {
    VOTE,
    REVEAL,
    EDIT_SESSION,
}

abstract class ClientMessage<T>(
    val type: ClientMessageType,
) {
    abstract val data: T
}

data class ClientVoteMessage(
    override val data: VoteDataCmd
) : ClientMessage<VoteDataCmd>(ClientMessageType.VOTE)

data class VoteDataCmd(
    val roundId: String,
    val teamId: String,
    val voteId: String
)

data class ClientVoteRevealMessage(
    override val data: RevealDataCmd
) : ClientMessage<RevealDataCmd>(ClientMessageType.REVEAL)

data class RevealDataCmd(
    val roundId: String
)

data class ClientEditSessionMessage(
    override val data: EditSessionCmd
) : ClientMessage<EditSessionCmd>(ClientMessageType.EDIT_SESSION)

data class EditSessionCmd(
    val title: String?,
    val description: String?,
)