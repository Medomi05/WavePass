package com.example.wavepass.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Query("SELECT * FROM playlists ORDER BY createdAtTimestamp DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    // Fetches a playlist with its songs, already ordered by position.
    // @Transaction ensures the parent + relation queries run as one atomic read.
    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun getPlaylistWithSongs(playlistId: Long): Flow<PlaylistWithSongs?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_song_cross_ref WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: Long): Int

    // Used when the user manually reorders songs within a playlist.
    @Query("UPDATE playlist_song_cross_ref SET position = :newPosition WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun updateSongPosition(playlistId: Long, songId: Long, newPosition: Int)

    @Query("SELECT * FROM playlist_song_cross_ref WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getCrossRefsForPlaylist(playlistId: Long): List<PlaylistSongCrossRef>
}