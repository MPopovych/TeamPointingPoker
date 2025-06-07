package com.makki.poker.web.cmd

data class CreateSessionCmd(
    val title: String?,
    val description: String?,
    val config: SessionConfigCmd
) {
    fun validate() {
        check(title == null || title.length <= 45) {
            "Title must be less than 45 characters."
        }
        check(description == null || description.length <= 500) {
            "Description must be less than 500 characters."
        }
        config.validate()
    }
}

data class SessionConfigCmd(
    val points: List<SessionPointCmd>,
    val teams: List<String>,
    val joinPassword: String?,
) {
    fun validate() {
        check(joinPassword == null || joinPassword.length <= 16) {
            "Password must be less than 16 characters."
        }
        check(points.isNotEmpty()) {
            "Points must not be empty."
        }
        check(teams.isNotEmpty()) {
            "Teams must not be empty."
        }
    }
}

data class SessionPointCmd(
    val value: Float?,
    val label: String
)

data class NewSessionRoundCmd(
    val title: String?,
    val comment: String?,
)
