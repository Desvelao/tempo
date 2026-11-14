package app.trainer.games

import app.trainer.core.Game
import app.trainer.core.Skill
import app.trainer.data.GameResultDao
import app.trainer.data.GameResultEntity
import app.trainer.data.ResultRepository
import app.trainer.data.SessionDao
import app.trainer.data.SessionEntity
import app.trainer.data.SkillRatingDao
import app.trainer.data.SkillLastPlayed
import app.trainer.data.SkillRatingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

private class FakeDao : GameResultDao {
    val saved = mutableListOf<GameResultEntity>()
    override suspend fun insert(result: GameResultEntity): Long { saved += result; return saved.size.toLong() }
    override suspend fun lastScore(gameId: String): Int? = saved.lastOrNull { it.gameId == gameId }?.score
    override suspend fun forSession(sessionId: Long) = saved.filter { it.sessionId == sessionId }
    override fun observeAll(): Flow<List<GameResultEntity>> = MutableStateFlow(saved.toList())
    override suspend fun clear() { saved.clear() }
    override suspend fun all(): List<GameResultEntity> = saved.toList()
    override suspend fun insertAll(results: List<GameResultEntity>) { saved += results }
    override suspend fun lastPlayedBySkill() = saved.groupBy { it.skill }.map { (skill, rs) -> SkillLastPlayed(skill, rs.maxOf { it.playedAt }) }
}

private class FakeRatingDao : SkillRatingDao {
    val ratings = mutableMapOf<Skill, SkillRatingEntity>()
    override fun observeAll(): Flow<List<SkillRatingEntity>> = MutableStateFlow(ratings.values.toList())
    override suspend fun upsert(rating: SkillRatingEntity) { ratings[rating.skill] = rating }
    override suspend fun clear() { ratings.clear() }
    override suspend fun all(): List<SkillRatingEntity> = ratings.values.toList()
    override suspend fun insertAll(ratings: List<SkillRatingEntity>) { ratings.forEach { this.ratings[it.skill] = it } }
}

private class FakeSessionDao : SessionDao {
    override suspend fun insert(session: SessionEntity) = 1L
    override suspend fun finish(id: Long, finishedAt: Long) {}
    override fun observeFinishedAt(): Flow<List<Long>> = MutableStateFlow(emptyList())
    override suspend fun clear() {}
    override suspend fun all(): List<SessionEntity> = emptyList()
    override suspend fun insertAll(sessions: List<SessionEntity>) {}
}

/** Round = a number; correct answer is the same number. Round 0.. times out when [timed]. */
private class EchoGame(val timed: Boolean) : Game<Int, Int> {
    override val id = "echo"
    override val skill = Skill.SPEED
    override fun generateRound(difficulty: Int, random: Random) = random.nextInt(100)
    override fun evaluate(round: Int, answer: Int) = round == answer
    override fun timeLimitMs(difficulty: Int): Long? = if (timed) 1000L else null
}

@OptIn(ExperimentalCoroutinesApi::class)
class RoundGameViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope2.play(vm: RoundGameViewModel<Int, Int>, answerCorrectly: Boolean) {
        var guard = 0
        while (vm.state.value.phase != RoundPhase.Finished && guard++ < 10_000) {
            testScheduler.advanceTimeBy(50); testScheduler.runCurrent()
            val s = vm.state.value
            if (s.phase == RoundPhase.Playing) vm.onAnswer(if (answerCorrectly) s.round!! else -1)
        }
    }

    @Test fun savesOneResultWithSummary() = runTest {
        val dao = FakeDao(); val ratings = FakeRatingDao()
        val vm = RoundGameViewModel(EchoGame(false), ResultRepository(dao, ratings, FakeSessionDao()), rounds = 5, random = Random(1))
        play(vm, answerCorrectly = true)
        assertEquals(1, dao.saved.size)
        val s = vm.state.value.summary!!
        assertEquals(5, s.correct); assertEquals(1.0, s.accuracy, 0.0)
        assertEquals("echo", dao.saved[0].gameId)
        assertEquals(s.finalLevel, ratings.ratings[Skill.SPEED]!!.level)
        assertTrue(dao.saved[0].score > 0)
    }

    @Test fun timeoutCountsAsWrong() = runTest {
        val dao = FakeDao(); val ratings = FakeRatingDao()
        val vm = RoundGameViewModel(EchoGame(true), ResultRepository(dao, ratings, FakeSessionDao()), rounds = 3, random = Random(1))
        var guard = 0
        while (vm.state.value.phase != RoundPhase.Finished && guard++ < 10_000) {
            testScheduler.advanceTimeBy(100); testScheduler.runCurrent()
        }
        assertEquals(0, vm.state.value.summary!!.correct)
        assertEquals(1, dao.saved.size)
    }

    @Test fun lastPlayedBySkillReportsTheNewestResultPerSkill() = runTest {
        val dao = FakeDao()
        val repo = ResultRepository(dao, FakeRatingDao(), FakeSessionDao())
        fun result(skill: Skill, at: Long) = GameResultEntity(
            gameId = "g", skill = skill, score = 1, accuracy = 1.0, avgReactionMs = 0, difficulty = 1, playedAt = at,
        )
        repo.save(result(Skill.MEMORY, 10)); repo.save(result(Skill.MEMORY, 30)); repo.save(result(Skill.SPEED, 20))
        assertEquals(mapOf(Skill.MEMORY to 30L, Skill.SPEED to 20L), repo.lastPlayedBySkill())
    }
}

private typealias TestScope2 = kotlinx.coroutines.test.TestScope
