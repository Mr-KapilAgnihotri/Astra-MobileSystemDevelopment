package com.yourname.trackr.repository

import com.yourname.trackr.data.local.SessionDao
import com.yourname.trackr.data.local.SessionEntity
import kotlinx.coroutines.flow.Flow

class SessionRepository(private val sessionDao: SessionDao) {

    val allSessions: Flow<List<SessionEntity>> = sessionDao.getAllSessions()

    suspend fun insert(session: SessionEntity): Long = sessionDao.insert(session)

    suspend fun delete(session: SessionEntity) = sessionDao.delete(session)

    suspend fun getSessionById(sessionId: Long): SessionEntity? =
        sessionDao.getSessionById(sessionId)

    suspend fun getAllSessionsOnce(): List<SessionEntity> = sessionDao.getAllSessionsOnce()

    /** Read-modify-write: only the photo changes, everything else about the session stays as saved. */
    suspend fun updatePhoto(sessionId: Long, photoUri: String?) {
        val session = sessionDao.getSessionById(sessionId) ?: return
        sessionDao.update(session.copy(photoUri = photoUri))
    }
}
