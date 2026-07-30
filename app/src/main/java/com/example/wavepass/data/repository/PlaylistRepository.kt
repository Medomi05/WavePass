package com.example.wavepass.data.repository

import com.example.wavepass.data.local.Playlist
import com.example.wavepass.data.local.PlaylistDao
import com.example.wavepass.data.local.PlaylistSongCrossRef
import com.example.wavepass.data.local.PlaylistWithSongs
import com.example.wavepass.data.local.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlaylistRepository(private val playlistDao: PlaylistDao) {

    fun getAllPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    suspend fun createPlaylist(name: String): Long {
        return playlistDao.insertPlaylist(
            Playlist(name = name, createdAtTimestamp = System.currentTimeMillis())
        )
    }

    suspend fun deletePlaylist(playlist: Playlist) = playlistDao.deletePlaylist(playlist)

    // Returns the playlist with its songs correctly ordered by position,
    // since Room's @Relation doesn't guarantee ordering on its own.
    fun getPlaylistWithOrderedSongs(playlistId: Long): Flow<PlaylistWithSongs?> {
        return playlistDao.getPlaylistWithSongs(playlistId).map { playlistWithSongs ->
            if (playlistWithSongs == null) return@map null

            val crossRefs = playlistDao.getCrossRefsForPlaylist(playlistId)
            val positionBySongId = crossRefs.associate { it.songId to it.position }

            val orderedSongs = playlistWithSongs.songs.sortedBy { song ->
                positionBySongId[song.id] ?: Int.MAX_VALUE
            }

            playlistWithSongs.copy(songs = orderedSongs)
        }
    }

    // Adds a song to a playlist at the end of the current order.
    suspend fun addSongToPlaylist(playlistId: Long, song: Song) {
        val nextPosition = playlistDao.getMaxPosition(playlistId) + 1
        playlistDao.addSongToPlaylist(
            PlaylistSongCrossRef(playlistId = playlistId, songId = song.id, position = nextPosition)
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    // Called when the user drags a song to a new position within a playlist.
    // Reassigns positions for the whole list to keep them sequential (0, 1, 2, ...).
    suspend fun reorderSongs(playlistId: Long, orderedSongIds: List<Long>) {
        orderedSongIds.forEachIndexed { index, songId ->
            playlistDao.updateSongPosition(playlistId, songId, index)
        }
    }
}