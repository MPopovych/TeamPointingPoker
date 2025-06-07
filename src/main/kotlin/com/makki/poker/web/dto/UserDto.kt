package com.makki.poker.web.dto

import com.makki.poker.assets.UserRef

data class UserDto(
    val id: String,
    val name: String
) {
    companion object {
        fun from(value: UserRef): UserDto = UserDto(value.id, value.name)
    }
}
