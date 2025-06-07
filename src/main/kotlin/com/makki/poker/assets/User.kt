package com.makki.poker.assets

/**
 * Internal use only
 */
data class User(
    val id: String,
    val name: String,
    val created: Long
) {
    fun toRef(): UserRef = UserRef(id = id, name = name)
}

data class UserRef(
    val id: String,
    val name: String,
)

class UserNotInSessionException : IllegalStateException("The user is not part of the session")

class UnauthenticatedException : IllegalStateException("The user is not authenticated")
