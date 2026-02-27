package com.rghsoftware.kairos.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.Instant

@OptIn(ExperimentalUuidApi::class)
class SyncEngineTest {
    private val baseTime = Instant.fromEpochMilliseconds(1_000_000_000)
    private val deviceId = "device-1"

    @Test
    fun `merge returns empty list when both inputs are empty`() {
        val result = SyncEngine.merge(emptyList(), emptyList())
        assertEquals(0, result.size)
    }

    @Test
    fun `merge returns local events when remote is empty`() {
        val local = listOf(
            createTaskCompleted("task-1", baseTime),
        )

        val result = SyncEngine.merge(local, emptyList())

        assertEquals(1, result.size)
        assertEquals("task-1", (result[0] as SyncEvent.TaskCompleted).taskId)
    }

    @Test
    fun `merge returns remote events when local is empty`() {
        val remote = listOf(
            createTaskCompleted("task-2", baseTime),
        )

        val result = SyncEngine.merge(emptyList(), remote)

        assertEquals(1, result.size)
        assertEquals("task-2", (result[0] as SyncEvent.TaskCompleted).taskId)
    }

    @Test
    fun `merge deduplicates by eventId`() {
        val eventId = Uuid.random()
        val event = SyncEvent.TaskCompleted(
            eventId = eventId,
            timestamp = baseTime,
            deviceId = deviceId,
            taskId = "task-1",
            completedAt = baseTime,
        )

        val result = SyncEngine.merge(listOf(event), listOf(event))

        assertEquals(1, result.size)
    }

    @Test
    fun `merge orders events chronologically by timestamp`() {
        val earlier = createTaskCompleted("task-1", baseTime)
        val later = createTaskCompleted("task-2", baseTime.plus(1000.milliseconds))

        val result = SyncEngine.merge(listOf(later), listOf(earlier))

        assertEquals(2, result.size)
        assertEquals("task-1", (result[0] as SyncEvent.TaskCompleted).taskId)
        assertEquals("task-2", (result[1] as SyncEvent.TaskCompleted).taskId)
    }

    @Test
    fun `merge breaks timestamp ties by eventId`() {
        val sameTime = baseTime
        val event1 = SyncEvent.TaskCompleted(
            eventId = Uuid.parse("00000000-0000-0000-0000-000000000001"),
            timestamp = sameTime,
            deviceId = deviceId,
            taskId = "task-1",
            completedAt = sameTime,
        )
        val event2 = SyncEvent.TaskCompleted(
            eventId = Uuid.parse("00000000-0000-0000-0000-000000000002"),
            timestamp = sameTime,
            deviceId = deviceId,
            taskId = "task-2",
            completedAt = sameTime,
        )

        val result = SyncEngine.merge(listOf(event2), listOf(event1))

        assertEquals(2, result.size)
        assertEquals("task-1", (result[0] as SyncEvent.TaskCompleted).taskId)
        assertEquals("task-2", (result[1] as SyncEvent.TaskCompleted).taskId)
    }

    @Test
    fun `detectConflicts returns empty when no conflicts`() {
        val local = listOf(createTaskCompleted("task-1", baseTime))
        val remote = listOf(createTaskCompleted("task-2", baseTime))

        val result = SyncEngine.detectConflicts(local, remote)

        assertEquals(0, result.size)
    }

    @Test
    fun `detectConflicts identifies conflict for same entity with different events`() {
        val local = listOf(createTaskCompleted("task-1", baseTime))
        val remote = listOf(
            createTaskCompleted("task-1", baseTime.plus(1.seconds)),
        )

        val result = SyncEngine.detectConflicts(local, remote)

        assertEquals(1, result.size)
        assertEquals("task-1", result[0].entityId)
    }

    @Test
    fun `detectConflicts returns empty when events match`() {
        val event = createTaskCompleted("task-1", baseTime)
        val local = listOf(event)
        val remote = listOf(event)

        val result = SyncEngine.detectConflicts(local, remote)

        assertEquals(0, result.size)
    }

    @Test
    fun `resolveConflict uses last-write-wins strategy`() {
        val earlier = createTaskCompleted("task-1", baseTime)
        val later = createTaskCompleted("task-1", baseTime.plus(1.seconds))

        val conflict = SyncConflict(
            entityId = "task-1",
            localEvents = listOf(earlier),
            remoteEvents = listOf(later),
        )

        val result = SyncEngine.resolveConflict(conflict)

        assertEquals(later.eventId, result?.eventId)
    }

    @Test
    fun `merge handles mixed event types`() {
        val taskEvent = createTaskCompleted("task-1", baseTime)
        val sessionEvent = SyncEvent.ChecklistSessionStarted(
            eventId = Uuid.random(),
            timestamp = baseTime.plus(1.seconds),
            deviceId = deviceId,
            sessionId = "session-1",
            triggeredBy = "presence",
        )

        val result = SyncEngine.merge(listOf(taskEvent), listOf(sessionEvent))

        assertEquals(2, result.size)
        assertTrue(result[0] is SyncEvent.TaskCompleted)
        assertTrue(result[1] is SyncEvent.ChecklistSessionStarted)
    }

    @Test
    fun `merge preserves event order across multiple events`() {
        val events = (0..5).map { i ->
            createTaskCompleted("task-$i", baseTime.plus((i * 1000).milliseconds))
        }.shuffled()

        val local = events.take(3)
        val remote = events.drop(3)

        val result = SyncEngine.merge(local, remote)

        assertEquals(6, result.size)
        for (i in 0..5) {
            assertEquals("task-$i", (result[i] as SyncEvent.TaskCompleted).taskId)
        }
    }

    private fun createTaskCompleted(taskId: String, timestamp: Instant): SyncEvent.TaskCompleted {
        return SyncEvent.TaskCompleted(
            eventId = Uuid.random(),
            timestamp = timestamp,
            deviceId = deviceId,
            taskId = taskId,
            completedAt = timestamp,
        )
    }
}
