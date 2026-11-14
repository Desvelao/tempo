package app.trainer.data

import app.trainer.core.GameList
import app.trainer.games.GameCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameListRepository(private val dao: GameListDao) {
    val lists: Flow<List<GameList>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun all(): List<GameList> = dao.all().map { it.toDomain() }

    suspend fun get(id: Long): GameList? = dao.get(id)?.toDomain()

    /** Inserts when [id] is 0, otherwise replaces. Returns the row id. */
    suspend fun save(id: Long, name: String, gameIds: List<String>, shuffle: Boolean): Long {
        val existing = if (id != 0L) dao.get(id) else null
        return dao.upsert(
            GameListEntity(
                id = id,
                name = name.trim().ifEmpty { "My list" },
                gameIds = gameIds.distinct().joinToString(","),
                shuffle = shuffle,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            ),
        )
    }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun clear() = dao.clear()

    private fun GameListEntity.toDomain() = GameList(
        id, name,
        // Drop ids that no longer exist in the catalog.
        gameIds.split(',').filter { id -> GameCatalog.all.any { it.id == id } },
        shuffle,
    )
}
