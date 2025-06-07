package com.makki.poker.web.dto

import com.makki.poker.assets.PokerSession
import com.makki.poker.assets.SessionConfig
import com.makki.poker.assets.SessionPoint
import com.makki.poker.assets.PokerSessionRound
import com.makki.poker.assets.SessionRoundTeamState
import java.math.RoundingMode
import java.text.DecimalFormat

data class PokerSessionShortDto(
    val id: String,
    var title: String?,
    var description: String?,
    val memberCount: Int,
    val created: Long
) {
    companion object {
        fun from(value: PokerSession): PokerSessionShortDto {
            return PokerSessionShortDto(
                id = value.id,
                title = value.title,
                description = value.description,
                memberCount = value.members.size,
                created = value.created
            )
        }
    }
}

data class PokerSessionDto(
    val id: String,
    val title: String?,
    val description: String?,
    val config: PokerSessionConfigDto,
    val rounds: Map<String, SessionRoundDto>,
    val members: Map<String, UserDto>,
    val observers: Map<String, UserDto>,
    val creator: UserDto,
    val created: Long
) {
    companion object {
        fun from(value: PokerSession): PokerSessionDto {
            return PokerSessionDto(
                id = value.id,
                title = value.title,
                description = value.description,
                config = PokerSessionConfigDto.from(value.config),
                rounds = value.rounds.mapValues {
                    SessionRoundDto.from(it.value, value.observers.keys)
                },
                members = value.members.mapValues { UserDto.from(it.value) },
                observers = value.observers.mapValues { UserDto.from(it.value) },
                creator = UserDto.from(value.creator),
                created = value.created
            )
        }
    }
}

data class PokerSessionConfigDto(
    val points: List<SessionPointDto>,
    val teams: List<String>
) {
    companion object {
        fun from(value: SessionConfig): PokerSessionConfigDto {
            return PokerSessionConfigDto(
                points = value.points.map { SessionPointDto.from(it) },
                teams = value.teams
            )
        }
    }
}

data class SessionPointDto(
    val id: String,
    val label: String
) {
    companion object {
        fun from(value: SessionPoint): SessionPointDto = SessionPointDto(id = value.id, label = value.label)
    }
}

data class SessionRoundDto(
    val id: String,
    val title: String?,
    val comment: String?,
    val created: Long,
    val teamStateMap: Map<String, SessionRoundTeamStateDto>,
    val open: Boolean,
    val forceOpen: Boolean,
    val average: Double?,
    val sum: Double?,
) {
    companion object {
        fun from(value: PokerSessionRound, activeMembers: Set<String>?): SessionRoundDto {
            val teamStateMap = value.teamStateMap.mapValues {
                SessionRoundTeamStateDto.from(it.value, value.forceOpen, activeMembers)
            }
            val scores = teamStateMap.mapNotNull { it.value.average }.takeIf { it.isNotEmpty() }
            return SessionRoundDto(
                id = value.id,
                title = value.title,
                comment = value.comment,
                created = value.created,
                teamStateMap = teamStateMap,
                open = teamStateMap.all { it.value.open },
                forceOpen = value.forceOpen,
                average = scores?.average()?.roundOffDecimal(),
                sum = scores?.sum()?.roundOffDecimal()
            )
        }
    }
}

data class SessionRoundTeamStateDto(
    val usersByScore: Map<String, SessionPointDto>,
    val average: Double?,
    val open: Boolean,
) {
    companion object {
        fun from(value: SessionRoundTeamState, forceOpen: Boolean, activeMembers: Set<String>?): SessionRoundTeamStateDto {
            return SessionRoundTeamStateDto(
                usersByScore = value.usersByScore.mapValues { SessionPointDto.from(it.value) },
                average = value.usersByScore.mapNotNull { it.value.fScore }
                    .takeIf { it.isNotEmpty() }?.average()?.roundOffDecimal(),
                open = forceOpen || activeMembers?.let { value.usersByScore.keys.containsAll(it) } ?: false
            )
        }
    }
}

private fun Double.roundOffDecimal(): Double? {
    val df = DecimalFormat("#.##")
    df.roundingMode = RoundingMode.CEILING
    return df.format(this).toDouble()
}
