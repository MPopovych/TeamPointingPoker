package com.makki.poker.service

import com.makki.poker.assets.PokerSession
import com.makki.poker.assets.PokerSessionRound
import com.makki.poker.assets.SessionPoint
import com.makki.poker.assets.UserRef
import com.makki.poker.web.cmd.CreateSessionCmd
import com.makki.poker.web.cmd.NewSessionRoundCmd
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Primary
@Profile("mock")
class SessionMockService: SessionServiceImpl() {
    override fun createDefaultSession(owner: UserRef, cmd: CreateSessionCmd): PokerSession {
        return super.createDefaultSession(owner, cmd).also {
            it.description = "Test description http://www.google.com/"
            it.members["test1"] = UserRef("test1", "TestMember1")
            it.members["test2"] = UserRef("test2", "TestMember2")
            it.members["test3"] = UserRef("test2", "TestMember3")
        }
    }

    override fun createDefaultRound(session: PokerSession, cmd: NewSessionRoundCmd): PokerSessionRound {
        return super.createDefaultRound(session, cmd).also {
            it.teamStateMap["QA"]?.also {  teamState ->
                teamState.usersByScore["test1"] = SessionPoint("1", 1f, "1")
                teamState.usersByScore["test2"] = SessionPoint("2", 2f, "2")
                teamState.usersByScore["test3"] = SessionPoint("2", 2f, "2")
            }
        }
    }
}
