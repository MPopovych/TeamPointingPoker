package com.makki.poker.service

import com.makki.poker.assets.User
import com.makki.poker.assets.UserRef
import com.makki.poker.tools.LruCache
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Collections
import java.util.UUID

@Service
class UserService {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }

    private val memStorage = Collections.synchronizedMap(LruCache<String, User>(100))

    fun createAndStoreNewUser(name: String): User {
        val new = User(id = UUID.randomUUID().toString(), name = name, created = System.currentTimeMillis())
        log.info("Creating new user: $new")
        return new
    }

    fun getUserById(userId: String): User? {
        return memStorage[userId]
    }

    fun allUserRefs(): List<UserRef> {
        return memStorage.values.toList().map { it.toRef() }
    }
}