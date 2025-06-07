package com.makki.poker.assets

/**
 * Mutable session object
 */
data class PokerSession(
    val id: String,
    var title: String?,
    var description: String?,
    val config: SessionConfig,
    val rounds: LinkedHashMap<String, PokerSessionRound>,
    val members: LinkedHashMap<String, UserRef>,
    val observers: LinkedHashMap<String, UserRef>,
    val creator: UserRef,
    val created: Long,
    var updated: Long,
) {
    fun setTitleChecked(title: String?) {
        this.title = title?.takeIf { it.isNotBlank() }
        update()
    }

    fun setDescriptionChecked(description: String?) {
        this.description = description?.takeIf { it.isNotBlank() }
        update()
    }

    fun addNewRound(round: PokerSessionRound) {
        if (rounds.size > 20) {
            throw IllegalStateException("There are more than 20 rounds in the session. You've reached the limit.")
        }
        rounds[round.id] = round
        update()
    }

    fun addUser(userRef: UserRef) {
        if (members.size > 20) {
            throw IllegalStateException("There are more than 20 users in the session. You've reached the limit.")
        }
        members[userRef.id] = userRef
        update()
    }

    fun addObserver(userRef: UserRef) {
        if (observers.size > 20) {
            throw IllegalStateException("There are more than 20 users in the session. You've reached the limit.")
        }
        observers[userRef.id] = userRef
        update()
    }

    fun removeObserver(userRef: UserRef) {
        observers.remove(userRef.id)
        update()
    }

    fun checkUserOrThrow(userRef: UserRef) {
        if (userRef.id !in members.keys) {
            throw UserNotInSessionException()
        }
    }

    fun isMember(userRef: UserRef) = userRef.id in members.keys

    fun isObserving(userRef: UserRef): Boolean = userRef.id in observers.keys

    fun update() {
        updated = System.currentTimeMillis()
    }
}

data class SessionConfig(
    val points: List<SessionPoint>,
    val teams: List<String>,
    val joinPassword: String?,
)

data class SessionPoint(
    val id: String,
    val fScore: Float?,
    val label: String
)

data class PokerSessionRound(
    val id: String,
    val title: String?,
    val comment: String?,
    val created: Long,
    var forceOpen: Boolean,
    val teamStateMap: Map<String, SessionRoundTeamState>,
)

data class SessionRoundTeamState(
    val usersByScore: LinkedHashMap<String, SessionPoint>
)

class MissingSessionException : IllegalStateException("The session has expired or wrong id provided")
