package com.makki.poker.web.ws

import java.security.Principal

data class StompPrincipal(private val name: String) : Principal {
    override fun getName(): String = name
}
