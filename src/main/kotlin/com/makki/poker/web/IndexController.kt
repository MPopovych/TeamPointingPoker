package com.makki.poker.web

import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller()
@RequestMapping()
class IndexController {
    @GetMapping()
    fun index(model: Model, session: HttpSession): String {
        model.addAttribute("user", session.getUser()?.toRef())
        model.addAttribute("title", "Hello!")
        model.addAttribute("content", "fragments/index/home")
        return "fragments/main"
    }
}
