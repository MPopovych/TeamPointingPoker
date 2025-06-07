package com.makki.poker.web

import com.makki.poker.assets.User
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class SessionInterceptor : HandlerInterceptor {
    @Throws(Exception::class)
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val session = request.getSession(false)
        val isLoggedIn = session != null && session.getUser() != null

        if (!isLoggedIn && !request.requestURI.startsWith("/login")) {
            response.sendRedirect("/login?r=${request.requestURI}")
            return false
        }

        return true
    }
}

fun HttpSession.getUser(): User? {
    return getAttribute("user") as? User
}
