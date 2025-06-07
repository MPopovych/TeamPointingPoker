package com.makki.poker.web

import com.makki.poker.service.UserService
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/login")
class LoginController(
    private val userService: UserService,
) {
    companion object {
        val USER_NAME_REGEX = "^[a-zA-Z _]{1,16}$".toRegex()
    }

    @PostMapping()
    fun login(
        @RequestParam displayName: String?,
        @RequestParam("r") returnTo: String?,
        session: HttpSession
    ): String {
        check(displayName != null) { "User name must be specified!" }
        check(displayName.matches(USER_NAME_REGEX)) {
            "User name must match regex!"
        }
        val user = userService.createAndStoreNewUser(displayName)
        session.setAttribute("user", user)
        return "redirect:${returnTo ?: "/"}" // redirect to home or voting session
    }

    @GetMapping()
    fun loginPage(
        model: Model,
        session: HttpSession,
        @RequestParam("r") returnTo: String?
    ): String {
        session.setAttribute("user", null)

        model.addAttribute("returnTo", returnTo ?: "/")
        model.addAttribute("title", "Please login!")
        model.addAttribute("content", "fragments/login/login")
        return "fragments/main"
    }
}
