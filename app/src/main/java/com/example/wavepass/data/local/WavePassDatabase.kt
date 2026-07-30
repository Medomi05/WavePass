package com.example.wavepass.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// Central Room database for the app.
// Bundles all entities and DAOs together, and ensures only one instance
// of the database exists at any time.
@Database(
    entities = [Song::class, WaveProfile::class, Encounter::class, Playlist::class, PlaylistSongCrossRef::class],
    version = 3,
    exportSchema = false
)

@TypeConverters(Converters::class)
abstract class WavePassDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao
    abstract fun waveProfileDao(): WaveProfileDao
    abstract fun encounterDao(): EncounterDao

    abstract fun playlistDao(): PlaylistDao
    companion object {
        @Volatile
        private var INSTANCE: WavePassDatabase? = null

        fun getInstance(context: Context): WavePassDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WavePassDatabase::class.java,
                    "wavepass_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}