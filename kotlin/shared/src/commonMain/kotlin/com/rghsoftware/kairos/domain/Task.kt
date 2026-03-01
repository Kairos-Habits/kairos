package com.rghsoftware.kairos.domain

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Value class for Task identifiers.
 * Prevents ID mix-ups between different entity types.
 */
@JvmInline
value class TaskId(val value: String)

/**
 * Represents a habit task to be completed during a checklist session.
 * Tasks are context-based (LEAVING or ARRIVING) rather than time-based.
 *
 * @property id Unique identifier for this task
 * @property name Human-readable task description
 * @property mode The context (LEAVING or ARRIVING) this task belongs to
 * @property sortOrder Order in which tasks appear in the checklist
 * @property isActive Whether this task is currently active (soft delete support)
 */
@OptIn(ExperimentalUuidApi::class)
data class Task(
    val id: TaskId = TaskId(Uuid.random().toString()),
    val name: String,
    val mode: Mode,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
)
