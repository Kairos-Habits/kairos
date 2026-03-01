package com.rghsoftware.kairos.checklist

import com.rghsoftware.kairos.domain.Mode
import com.rghsoftware.kairos.domain.SessionId
import com.rghsoftware.kairos.domain.TaskId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalUuidApi::class)
class ChecklistFlowStateMachineTest {
    private val baseTime: Instant = Clock.System.now()
    private val defaultConfig = ChecklistFlowConfig(
        cooldownDuration = 90.seconds,
        autoShowOnPresence = true,
    )

    // ===== Presence Detection Tests =====

    @Test
    fun `Idle state transitions to SelectingMode on presence`() {
        val initialState = ChecklistFlowState(sessionState = SessionState.Idle)

        val event = ChecklistFlowEvent.PresenceChanged(
            isPresent = true,
            timestamp = baseTime,
        )
        val transition = initialState.reduce(event, defaultConfig)

        assertTrue(transition.newState.isActive)
        assertEquals(SessionState.SelectingMode, transition.newState.sessionState)
        assertNull(transition.newState.cooldownUntil)
    }

    @Test
    fun `SelectingMode stays unchanged on presence`() {
        val initialState = ChecklistFlowState(sessionState = SessionState.SelectingMode)

        val event = ChecklistFlowEvent.PresenceChanged(
            isPresent = true,
            timestamp = baseTime,
        )
        val transition = initialState.reduce(event, defaultConfig)

        assertEquals(SessionState.SelectingMode, transition.newState.sessionState)
    }

    @Test
    fun `InSession stays unchanged on presence`() {
        val sessionId = SessionId("test-session")
        val initialState = ChecklistFlowState(
            sessionState = SessionState.InSession(
                sessionId = sessionId,
                mode = Mode.LEAVING,
                completedTaskIds = emptySet(),
                sessionStartedAt = baseTime,
            ),
        )

        val event = ChecklistFlowEvent.PresenceChanged(
            isPresent = true,
            timestamp = baseTime,
        )
        val transition = initialState.reduce(event, defaultConfig)

        assertEquals(SessionState.InSession::class, transition.newState.sessionState::class)
        val inSession = transition.newState.sessionState as SessionState.InSession
        assertEquals(sessionId, inSession.sessionId)
    }

    @Test
    fun `Presence absent returns to Idle from SelectingMode`() {
        val initialState = ChecklistFlowState(sessionState = SessionState.SelectingMode)

        val event = ChecklistFlowEvent.PresenceChanged(
            isPresent = false,
            timestamp = baseTime,
        )
        val transition = initialState.reduce(event, defaultConfig)

        assertEquals(SessionState.Idle, transition.newState.sessionState)
        assertFalse(transition.newState.isActive)
    }

    @Test
    fun `Cooldown stays in Cooldown on presence absent`() {
        val sessionId = SessionId("test-session")
        val cooldownUntil = baseTime + 90.seconds
        val initialState = ChecklistFlowState(
            sessionState = SessionState.Cooldown(
                previousSessionId = sessionId,
                mode = Mode.LEAVING,
            ),
            cooldownUntil = cooldownUntil,
        )

        val event = ChecklistFlowEvent.PresenceChanged(
            isPresent = false,
            timestamp = baseTime + 30.seconds,
        )
        val transition = initialState.reduce(event, defaultConfig)

        assertEquals(SessionState.Cooldown::class, transition.newState.sessionState::class)
        assertEquals(cooldownUntil, transition.newState.cooldownUntil)
    }

    // ===== Mode Selection Tests =====

    @Test
    fun `ModeSelected from SelectingMode creates InSession`() {
        val initialState = ChecklistFlowState(sessionState = SessionState.SelectingMode)

        val event = ChecklistFlowEvent.ModeSelected(
            mode = Mode.LEAVING,
            timestamp = baseTime,
        )
        val transition = initialState.reduce(event, defaultConfig)

        assertTrue(transition.newState.isActive)
        assertEquals(SessionState.InSession::class, transition.newState.sessionState::class)
        val inSession = transition.newState.sessionState as SessionState.InSession
        assertEquals(Mode.LEAVING, inSession.mode)
        assertTrue(inSession.completedTaskIds.isEmpty())

        // Effect should be SessionStarted
        assertTrue(transition.effect is TransitionEffect.SessionStarted)
    }

    @Test
    fun `ModeSelected from Idle is ignored`() {
        val initialState = ChecklistFlowState(sessionState = SessionState.Idle)

        val event = ChecklistFlowEvent.ModeSelected(
            mode = Mode.LEAVING,
            timestamp = baseTime,
        )
        val transition = initialState.reduce(event, defaultConfig)

        assertEquals(SessionState.Idle, transition.newState.sessionState)
        assertNull(transition.effect)
    }

    // ===== Task Toggle Tests =====

    @Test
    fun `TaskToggled adds task to completed set`() {
        val sessionId = SessionId("test-session")
        val taskId = TaskId("task-1")
        val initialState = ChecklistFlowState(
            sessionState = SessionState.InSession(
                sessionId = sessionId,
                mode = Mode.LEAVING,
                completedTaskIds = emptySet(),
                sessionStartedAt = baseTime,
            ),
        )

        val event = ChecklistFlowEvent.TaskToggled(
            taskId = taskId,
            timestamp = baseTime,
        )
        val transition = initialState.reduce(event, defaultConfig)

        val inSession = transition.newState.sessionState as SessionState.InSession
        assertTrue(taskId in inSession.completedTaskIds)
        assertEquals(1, inSession.completedCount)

        // Effect should be TaskCompletionToggled
        assertTrue(transition.effect is TransitionEffect.TaskCompletionToggled)
        val effect = transition.effect as TransitionEffect.TaskCompletionToggled
        assertTrue(effect.isCompleted)
    }

    @Test
    fun `TaskToggled removes task from completed set when already complete`() {
        val sessionId = SessionId("test-session")
        val taskId = TaskId("task-1")
        val initialState = ChecklistFlowState(
            sessionState = SessionState.InSession(
                sessionId = sessionId,
                mode = Mode.LEAVING,
                completedTaskIds = setOf(taskId),
                sessionStartedAt = baseTime,
            ),
        )

        val event = ChecklistFlowEvent.TaskToggled(
            taskId = taskId,
            timestamp = baseTime,
        )
        val transition = initialState.reduce(event, defaultConfig)

        val inSession = transition.newState.sessionState as SessionState.InSession
        assertFalse(taskId in inSession.completedTaskIds)
        assertEquals(0, inSession.completedCount)

        val effect = transition.effect as TransitionEffect.TaskCompletionToggled
        assertFalse(effect.isCompleted)
    }

    @Test
    fun `TaskToggled from non-InSession state is ignored`() {
        val initialState = ChecklistFlowState(sessionState = SessionState.Idle)
        val taskId = TaskId("task-1")

        val event = ChecklistFlowEvent.TaskToggled(
            taskId = taskId,
            timestamp = baseTime,
        )
        val transition = initialState.reduce(event, defaultConfig)

        assertEquals(SessionState.Idle, transition.newState.sessionState)
        assertNull(transition.effect)
    }

    // ===== Session Completion Tests =====

    @Test
    fun `SessionCompleted transitions to Cooldown`() {
        val sessionId = SessionId("test-session")
        val completedTasks = setOf(TaskId("task-1"), TaskId("task-2"))
        val initialState = ChecklistFlowState(
            sessionState = SessionState.InSession(
                sessionId = sessionId,
                mode = Mode.ARRIVING,
                completedTaskIds = completedTasks,
                sessionStartedAt = baseTime,
            ),
        )

        val event = ChecklistFlowEvent.SessionCompleted(timestamp = baseTime)
        val transition = initialState.reduce(event, defaultConfig)

        assertEquals(SessionState.Cooldown::class, transition.newState.sessionState::class)
        assertNotNull(transition.newState.cooldownUntil)
        assertEquals(baseTime + defaultConfig.cooldownDuration, transition.newState.cooldownUntil)

        // Effect should be SessionFinished
        assertTrue(transition.effect is TransitionEffect.SessionFinished)
        val effect = transition.effect as TransitionEffect.SessionFinished
        assertEquals(sessionId, effect.sessionId)
        assertEquals(Mode.ARRIVING, effect.mode)
        assertEquals(completedTasks, effect.completedTaskIds)
    }

    @Test
    fun `SessionCompleted from non-InSession is ignored`() {
        val initialState = ChecklistFlowState(sessionState = SessionState.SelectingMode)

        val event = ChecklistFlowEvent.SessionCompleted(timestamp = baseTime)
        val transition = initialState.reduce(event, defaultConfig)

        assertEquals(SessionState.SelectingMode, transition.newState.sessionState)
        assertNull(transition.effect)
    }

    // ===== Cooldown Tests =====

    @Test
    fun `Cooldown blocks presence-triggered mode selection`() {
        val cooldownUntil = baseTime + 90.seconds
        val initialState = ChecklistFlowState(
            sessionState = SessionState.Idle,
            cooldownUntil = cooldownUntil,
        )

        val event = ChecklistFlowEvent.PresenceChanged(
            isPresent = true,
            timestamp = baseTime + 30.seconds,
        )
        val transition = initialState.reduce(event, defaultConfig)

        assertEquals(SessionState.Idle, transition.newState.sessionState)
        assertFalse(transition.newState.isActive)
    }

    @Test
    fun `TimerTick clears expired cooldown`() {
        val sessionId = SessionId("test-session")
        val cooldownUntil = baseTime + 90.seconds
        val initialState = ChecklistFlowState(
            sessionState = SessionState.Cooldown(
                previousSessionId = sessionId,
                mode = Mode.LEAVING,
            ),
            cooldownUntil = cooldownUntil,
        )

        val event = ChecklistFlowEvent.TimerTick(timestamp = baseTime + 100.seconds)
        val transition = initialState.reduce(event, defaultConfig)

        assertEquals(SessionState.Idle, transition.newState.sessionState)
        assertNull(transition.newState.cooldownUntil)
    }

    @Test
    fun `TimerTick does not clear active cooldown`() {
        val sessionId = SessionId("test-session")
        val cooldownUntil = baseTime + 90.seconds
        val initialState = ChecklistFlowState(
            sessionState = SessionState.Cooldown(
                previousSessionId = sessionId,
                mode = Mode.LEAVING,
            ),
            cooldownUntil = cooldownUntil,
        )

        val event = ChecklistFlowEvent.TimerTick(timestamp = baseTime + 30.seconds)
        val transition = initialState.reduce(event, defaultConfig)

        assertEquals(SessionState.Cooldown::class, transition.newState.sessionState::class)
        assertEquals(cooldownUntil, transition.newState.cooldownUntil)
    }

    // ===== Cancel Tests =====

    @Test
    fun `Cancelled returns to Idle with no cooldown`() {
        val sessionId = SessionId("test-session")
        val initialState = ChecklistFlowState(
            sessionState = SessionState.InSession(
                sessionId = sessionId,
                mode = Mode.LEAVING,
                completedTaskIds = setOf(TaskId("task-1")),
                sessionStartedAt = baseTime,
            ),
            cooldownUntil = baseTime + 90.seconds,
        )

        val event = ChecklistFlowEvent.Cancelled(timestamp = baseTime)
        val transition = initialState.reduce(event, defaultConfig)

        assertEquals(SessionState.Idle, transition.newState.sessionState)
        assertNull(transition.newState.cooldownUntil)
        assertFalse(transition.newState.isActive)
    }

    @Test
    fun `Cancelled from SelectingMode returns to Idle`() {
        val initialState = ChecklistFlowState(sessionState = SessionState.SelectingMode)

        val event = ChecklistFlowEvent.Cancelled(timestamp = baseTime)
        val transition = initialState.reduce(event, defaultConfig)

        assertEquals(SessionState.Idle, transition.newState.sessionState)
        assertFalse(transition.newState.isActive)
    }

    // ===== Configuration Tests =====

    @Test
    fun `Configurable cooldown duration is respected`() {
        val customConfig = ChecklistFlowConfig(
            cooldownDuration = 60.seconds,
            autoShowOnPresence = true,
        )
        val sessionId = SessionId("test-session")
        val initialState = ChecklistFlowState(
            sessionState = SessionState.InSession(
                sessionId = sessionId,
                mode = Mode.LEAVING,
                completedTaskIds = emptySet(),
                sessionStartedAt = baseTime,
            ),
        )

        val event = ChecklistFlowEvent.SessionCompleted(timestamp = baseTime)
        val transition = initialState.reduce(event, customConfig)

        assertEquals(baseTime + 60.seconds, transition.newState.cooldownUntil)
    }

    @Test
    fun `Auto-show disabled prevents automatic mode selection`() {
        val noAutoShowConfig = ChecklistFlowConfig(
            cooldownDuration = 90.seconds,
            autoShowOnPresence = false,
        )
        val initialState = ChecklistFlowState(sessionState = SessionState.Idle)

        val event = ChecklistFlowEvent.PresenceChanged(
            isPresent = true,
            timestamp = baseTime,
        )
        val transition = initialState.reduce(event, noAutoShowConfig)

        assertEquals(SessionState.Idle, transition.newState.sessionState)
        assertFalse(transition.newState.isActive)
    }
}

private fun assertNotNull(value: Any?) {
    kotlin.test.assertNotNull(value)
}
