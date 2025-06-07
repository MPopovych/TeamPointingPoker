package com.makki.poker.web

import com.makki.poker.assets.MissingSessionException
import com.makki.poker.assets.UnauthenticatedException
import com.makki.poker.assets.UserNotInSessionException
import com.makki.poker.web.cmd.CreateSessionCmd
import com.makki.poker.service.SessionService
import com.makki.poker.web.dto.PokerSessionDto
import com.makki.poker.web.dto.PokerSessionShortDto
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.view.RedirectView
import java.text.SimpleDateFormat
import java.util.Date

@Controller
@RequestMapping("/session")
class SessionController(
    private val sessionService: SessionService
) {
    companion object {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd")
    }

    @GetMapping("/new")
    fun newPokerSession(model: Model, httpSession: HttpSession): String {
        model.addAttribute("user", httpSession.getUser()?.toRef())
        model.addAttribute("title", "Create a new session")
        model.addAttribute("sessionTitle", getDefaultTitle())
        model.addAttribute("content", "fragments/session/create")
        return "fragments/main"
    }

    @PostMapping("/new")
    fun submitPokerSession(
        httpSession: HttpSession,
        @ModelAttribute cmd: CreateSessionCmd
    ): RedirectView {
        val user = httpSession.getUser()?.toRef() ?: throw UnauthenticatedException()
        cmd.validate()
        val session = sessionService.createSession(user, cmd)

        return RedirectView("/session/${session.id}")
    }

    @GetMapping("/{sessionId}")
    fun sessionView(model: Model, httpSession: HttpSession, @PathVariable sessionId: String): String {
        val user = httpSession.getUser()?.toRef() ?: throw UnauthenticatedException()

        val session = try {
            sessionService.getSessionForUser(user, sessionId)
                .let { PokerSessionDto.from(it) }
        } catch (_: UserNotInSessionException) {
            return "redirect:/session/${sessionId}/join"
        } catch (_: MissingSessionException) {
            return "redirect:/session/new"
        }

        // join session
        model.addAttribute("user", user)
        model.addAttribute("title", "Session ${session.title ?: session.id}")
        model.addAttribute("data", session)
        model.addAttribute("content", "fragments/session/view")
        return "fragments/main"
    }

    @GetMapping("/{sessionId}/join")
    fun joinSessionView(model: Model, httpSession: HttpSession, @PathVariable sessionId: String): String {
        val user = httpSession.getUser()?.toRef() ?: throw IllegalStateException("Unauthenticated user")

        val session = sessionService.getSession(sessionId)
        if (session.config.joinPassword == null) {
            sessionService.addUserToSession(sessionId, user, null)
            return "redirect:/session/${sessionId}"
        }

        model.addAttribute("user", user)
        model.addAttribute("content", "fragments/session/join")
        return "fragments/main"
    }

    @PostMapping("/{sessionId}/join")
    fun joinSession(
        httpSession: HttpSession, @PathVariable sessionId: String, @RequestParam password: String
    ): String {
        val user = httpSession.getUser()?.toRef() ?: throw IllegalStateException("Unauthenticated user")
        sessionService.addUserToSession(sessionId, user, password)
        return "redirect:/session/${sessionId}"
    }

    @GetMapping()
    fun sessionList(model: Model, httpSession: HttpSession): String {
        val user = httpSession.getUser()?.toRef()  ?: throw IllegalStateException("Unauthenticated user")

        val sessions = sessionService.getUserSessions(user).map {
            PokerSessionShortDto.from(it)
        }
        model.addAttribute("data", sessions)
        model.addAttribute("user", httpSession.getUser()?.toRef())
        model.addAttribute("title", "My sessions")
        model.addAttribute("content", "fragments/session/list/list")
        return "fragments/main"
    }

    private fun getDefaultTitle(): String {
        return "${dateFormat.format(Date())} Poker Session"
    }
}