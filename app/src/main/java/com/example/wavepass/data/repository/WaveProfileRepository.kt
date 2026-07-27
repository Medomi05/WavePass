package com.example.wavepass.data.repository

import com.example.wavepass.data.local.WaveProfile
import com.example.wavepass.data.local.WaveProfileDao
import kotlinx.coroutines.flow.Flow

class WaveProfileRepository(private val waveProfileDao: WaveProfileDao) {

    fun getProfile(): Flow<WaveProfile?> = waveProfileDao.getProfile()

    suspend fun saveProfile(profile: WaveProfile) = waveProfileDao.insertOrUpdate(profile)
}