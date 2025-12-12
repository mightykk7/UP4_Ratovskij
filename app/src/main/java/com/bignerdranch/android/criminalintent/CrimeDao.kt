package com.bignerdranch.android.criminalintent

import androidx.room.*
import java.util.UUID

@Dao
interface CrimeDao {
    @Query("SELECT * FROM crime WHERE id = :id")
    suspend fun getCrime(id: UUID): Crime?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrime(crime: Crime)

    @Update
    suspend fun updateCrime(crime: Crime)
}