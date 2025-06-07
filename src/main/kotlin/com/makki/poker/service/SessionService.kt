package com.makki.poker.service

import com.makki.poker.assets.MissingSessionException
import com.makki.poker.assets.PokerSession
import com.makki.poker.assets.SessionConfig
import com.makki.poker.assets.SessionPoint
import com.makki.poker.assets.PokerSessionRound
import com.makki.poker.assets.SessionRoundTeamState
import com.makki.poker.assets.UserRef
import com.makki.poker.web.cmd.CreateSessionCmd
import com.makki.poker.web.cmd.NewSessionRoundCmd
import com.makki.poker.web.cmd.SessionPointCmd
import com.makki.poker.tools.LruCache
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Collections
import java.util.UUID
import kotlin.collections.LinkedHashMap

interface SessionService {
    fun findSession(sessionId: String): PokerSession?
    fun getSession(sessionId: String): PokerSession
    fun getSessionForUser(user: UserRef, sessionId: String): PokerSession
    fun createSession(owner: UserRef, cmd: CreateSessionCmd): PokerSession
    fun editSession(sessionId: String, user: UserRef, title: String?, description: String?): PokerSession
    fun addUserToSession(sessionId: String, user: UserRef, password: String?)

    fun startNewRound(sessionId: String, user: UserRef, cmd: NewSessionRoundCmd): PokerSession
    fun placeVote(sessionId: String, user: UserRef, roundId: String, teamId: String, voteId: String): PokerSession
    fun toggleVoteReveal(sessionId: String, user: UserRef, roundId: String): PokerSession

    fun getUserObservedSessions(user: UserRef): List<PokerSession>
    fun getUserSessions(user: UserRef): List<PokerSession>
}

@Service
open class SessionServiceImpl: SessionService {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }

    private val memStorage = Collections.synchronizedMap(LruCache<String, PokerSession>(200))

    override fun findSession(sessionId: String): PokerSession? {
        return memStorage[sessionId]
    }

    override fun getSessionForUser(user: UserRef, sessionId: String): PokerSession {
        return getSession(sessionId).validateUser(user)
    }

    override fun getSession(sessionId: String): PokerSession {
        return findSession(sessionId) ?: throw MissingSessionException()
    }

    override fun createSession(owner: UserRef, cmd: CreateSessionCmd): PokerSession {
        return createDefaultSession(owner, cmd).also {
            memStorage[it.id] = it
            log.info("Created new PokerSession ${it.id} and saved to mem cache")

            startNewRound(it.id, owner, NewSessionRoundCmd(null, null))
        }
    }

    override fun addUserToSession(sessionId: String, user: UserRef, password: String?) {
        val session = getSession(sessionId)
        if (password?.takeIf { it.isNotBlank() } != session.config.joinPassword) {
            log.info("User ${user.name}(${user.id}) is trying to access session ${session.id} with a bad password")
            throw IllegalStateException("The password provided is invalid")
        }
        session.addUser(user)
    }

    override fun editSession(sessionId: String, user: UserRef, title: String?, description: String?): PokerSession {
        val session = getSession(sessionId).validateUser(user)
        session.setDescriptionChecked(description)
        session.setTitleChecked(title)
        return session
    }

    override fun startNewRound(sessionId: String, user: UserRef, cmd: NewSessionRoundCmd): PokerSession {
        val session = getSession(sessionId).validateUser(user)
        val round = createDefaultRound(session, cmd)
        session.addNewRound(round)
        log.info("Added a round to PokerSession ${session.id}")

        return session
    }

    override fun placeVote(sessionId: String, user: UserRef, roundId: String, teamId: String, voteId: String): PokerSession {
        val session = getSession(sessionId).validateUser(user)
        val round = session.rounds[roundId]
            ?: throw Exception("Round $roundId not found")
        val team = round.teamStateMap[teamId]
            ?: throw Exception("Team $teamId not found")
        val vote = session.config.points.find { it.id == voteId }
            ?: throw IllegalStateException("No vote with id $voteId")

        team.usersByScore[user.id] = vote
        return session
    }

    override fun toggleVoteReveal(sessionId: String, user: UserRef, roundId: String): PokerSession {
        val session = getSession(sessionId).validateUser(user)
        val round = session.rounds[roundId]
            ?: throw Exception("Round $roundId not found")

        round.forceOpen = !round.forceOpen
        return session
    }

    override fun getUserObservedSessions(user: UserRef): List<PokerSession> {
        return memStorage.values.filter { it.isObserving(user) }
    }

    override fun getUserSessions(user: UserRef): List<PokerSession> {
        return memStorage.values.filter { it.isMember(user) }
    }

    protected open fun createDefaultSession(owner: UserRef, cmd: CreateSessionCmd): PokerSession {
        return PokerSession(
            id = UUID.randomUUID().toString(),
            title = cmd.title?.takeIf { it.isNotBlank() },
            description = cmd.description?.takeIf { it.isNotBlank() },
            config = SessionConfig(
                points = cmd.config.points.toAsset(),
                teams = cmd.config.teams,
                joinPassword = cmd.config.joinPassword?.takeIf { it.isNotBlank() }
            ),
            rounds = LinkedHashMap(),
            members = LinkedHashMap(),
            observers = LinkedHashMap(),
            creator = owner,
            created = System.currentTimeMillis(),
            updated = System.currentTimeMillis()
        ).also {
            it.members[owner.id] = owner
        }
    }

    protected open fun createDefaultRound(session: PokerSession, cmd: NewSessionRoundCmd): PokerSessionRound {
        val teamStateMap = session.config.teams.associateWith {
            SessionRoundTeamState(
                usersByScore = LinkedHashMap()
            )
        }
        return PokerSessionRound(
            id = UUID.randomUUID().toString(),
            title = cmd.title?.takeIf { it.isNotBlank() },
            comment = cmd.comment?.takeIf { it.isNotBlank() },
            created = System.currentTimeMillis(),
            teamStateMap = teamStateMap,
            forceOpen = false
        )
    }

    fun PokerSession.validateUser(user: UserRef): PokerSession {
        this.checkUserOrThrow(user)
        return this
    }

    protected fun List<SessionPointCmd>.toAsset(): List<SessionPoint> {
        return this.mapIndexed { index, cmd ->
            SessionPoint(
                id = index.toString(),
                fScore = cmd.value,
                label = cmd.label
            )
        }
    }
}