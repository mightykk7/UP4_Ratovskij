package com.bignerdranch.android.criminalintent

import android.content.Context
import java.util.UUID

class CrimeRepository private constructor(context: Context) {
    private val database = CrimeDatabase.getDatabase(context)
    private val crimeDao = database.crimeDao()

    suspend fun getCrime(id: UUID): Crime? = crimeDao.getCrime(id)
    suspend fun insertCrime(crime: Crime) = crimeDao.insertCrime(crime)
    suspend fun updateCrime(crime: Crime) = crimeDao.updateCrime(crime)

    companion object {
        @Volatile
        private var INSTANCE: CrimeRepository? = null

        fun getInstance(context: Context): CrimeRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = CrimeRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}