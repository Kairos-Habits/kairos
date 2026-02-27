package com.rghsoftware.kairos.display

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class DisplayModeStateMachineTest {
    private val baseTime = Instant.fromEpochMilliseconds(1_000_000_000)
    private val defaultConfig = DisplayModeConfig(
        manualOverrideDuration = 5.minutes,
        autoSwitchCooldown = 90.seconds,
    )

    @Test
    fun `FR-1 HA to KIOSK on presence with no override or cooldown`() {
        val initialState = DisplayModeState(displayMode = DisplayMode.HA)

        val event = DisplayModeEvent.PresenceChanged(PresenceState.Present, baseTime)
        val transition = initialState.reduce(event, defaultConfig)

        assertEquals(DisplayMode.KIOSK, transition.targetDisplayMode)
        assertTrue(transition.displayChanged)
        assertEquals(PresenceState.Present, transition.newState.presenceState)
        assertNotNull(transition.newState.cooldownUntil)
        assertNull(transition.newState.manualOverrideUntil)
    }

    @Test
    fun `FR-1 presence while already in KIOSK does not change display`() {
        val initialState = DisplayModeState(displayMode = DisplayMode.KIOSK)

        val event = DisplayModeEvent.PresenceChanged(PresenceState.Present, baseTime)
        val transition = initialState.reduce(event, defaultConfig)

        assertEquals(DisplayMode.KIOSK, transition.targetDisplayMode)
        assertFalse(transition.displayChanged)
        assertEquals(PresenceState.Present, transition.newState.presenceState)
        assertNull(transition.newState.cooldownUntil)
    }

    @Test
    fun `FR-2 Swipe toggles from HA to KIOSK and sets override`() {
        val initialState = DisplayModeState(displayMode = DisplayMode.HA)

        val event = DisplayModeEvent.SwipeToggle(baseTime)
        val transition = initialState.reduce(event, defaultConfig)

        assertEquals(DisplayMode.KIOSK, transition.targetDisplayMode)
        assertTrue(transition.displayChanged)
        assertNotNull(transition.newState.manualOverrideUntil)
        assertEquals(baseTime + defaultConfig.manualOverrideDuration, transition.newState.manualOverrideUntil)
    }

    @Test
    fun `FR-2 Swipe toggles from KIOSK to HA and sets override`() {
        val initialState = DisplayModeState(displayMode = DisplayMode.KIOSK)

        val event = DisplayModeEvent.SwipeToggle(baseTime)
        val transition = initialState.reduce(event, defaultConfig)

        assertEquals(DisplayMode.HA, transition.targetDisplayMode)
        assertTrue(transition.displayChanged)
        assertNotNull(transition.newState.manualOverrideUntil)
        assertEquals(baseTime + defaultConfig.manualOverrideDuration, transition.newState.manualOverrideUntil)
    }

    @Test
    fun `FR-2 Swipe resets override duration if already active`() {
        val existingOverride = baseTime + 3.minutes
        val initialState = DisplayModeState(
            displayMode = DisplayMode.HA,
            manualOverrideUntil = existingOverride,
        )

        val event = DisplayModeEvent.SwipeToggle(baseTime)
        val transition = initialState.reduce(event, defaultConfig)

        assertEquals(baseTime + defaultConfig.manualOverrideDuration, transition.newState.manualOverrideUntil)
    }

    @Test
    fun `FR-3 Cooldown blocks presence-triggered auto-switch`() {
        val cooldownEnd = baseTime + 90.seconds
        val initialState = DisplayModeState(
            displayMode = DisplayMode.HA,
            cooldownUntil = cooldownEnd,
        )

        val event = DisplayModeEvent.PresenceChanged(PresenceState.Present, baseTime + 30.seconds)
        val transition = initialState.reduce(event, defaultConfig)

        assertEquals(DisplayMode.HA, transition.targetDisplayMode)
        assertFalse(transition.displayChanged)
        assertEquals(PresenceState.Present, transition.newState.presenceState)
        assertEquals(cooldownEnd, transition.newState.cooldownUntil)
    }

    @Test
    fun `FR-3 Swipe is allowed during cooldown`() {
        val cooldownEnd = baseTime + 90.seconds
        val initialState = DisplayModeState(
            displayMode = DisplayMode.HA,
            cooldownUntil = cooldownEnd,
        )

        val event = DisplayModeEvent.SwipeToggle(baseTime)
        val transition = initialState.reduce(event, defaultConfig)

        assertEquals(DisplayMode.KIOSK, transition.targetDisplayMode)
        assertTrue(transition.displayChanged)
        assertEquals(cooldownEnd, transition.newState.cooldownUntil)
        assertNotNull(transition.newState.manualOverrideUntil)
    }

    @Test
    fun `FR-4 No auto-return to HA on ABSENT`() {
        val initialState = DisplayModeState(displayMode = DisplayMode.KIOSK)

        val event = DisplayModeEvent.PresenceChanged(PresenceState.Absent, baseTime)
        val transition = initialState.reduce(event, defaultConfig)

        assertEquals(DisplayMode.KIOSK, transition.targetDisplayMode)
        assertFalse(transition.displayChanged)
        assertEquals(PresenceState.Absent, transition.newState.presenceState)
    }

    @Test
    fun `Manual override blocks auto-switch on presence`() {
        val overrideEnd = baseTime + 5.minutes
        val initialState = DisplayModeState(
            displayMode = DisplayMode.HA,
            manualOverrideUntil = overrideEnd,
        )

        val event = DisplayModeEvent.PresenceChanged(PresenceState.Present, baseTime)
        val transition = initialState.reduce(event, defaultConfig)

        assertEquals(DisplayMode.HA, transition.targetDisplayMode)
        assertFalse(transition.displayChanged)
        assertEquals(PresenceState.Present, transition.newState.presenceState)
        assertEquals(overrideEnd, transition.newState.manualOverrideUntil)
    }

    @Test
    fun `TimerTick clears expired manual override`() {
        val overrideEnd = baseTime + 5.minutes
        val initialState = DisplayModeState(
            displayMode = DisplayMode.KIOSK,
            manualOverrideUntil = overrideEnd,
        )

        val event = DisplayModeEvent.TimerTick(baseTime + 6.minutes)
        val transition = initialState.reduce(event, defaultConfig)

        assertEquals(DisplayMode.KIOSK, transition.targetDisplayMode)
        assertFalse(transition.displayChanged)
        assertNull(transition.newState.manualOverrideUntil)
    }

    @Test
    fun `TimerTick does not clear active manual override`() {
        val overrideEnd = baseTime + 5.minutes
        val initialState = DisplayModeState(
            displayMode = DisplayMode.KIOSK,
            manualOverrideUntil = overrideEnd,
        )

        val event = DisplayModeEvent.TimerTick(baseTime + 3.minutes)
        val transition = initialState.reduce(event, defaultConfig)

        assertEquals(overrideEnd, transition.newState.manualOverrideUntil)
    }

    @Test
    fun `TimerTick clears expired cooldown`() {
        val cooldownEnd = baseTime + 90.seconds
        val initialState = DisplayModeState(
            displayMode = DisplayMode.KIOSK,
            cooldownUntil = cooldownEnd,
        )

        val event = DisplayModeEvent.TimerTick(baseTime + 120.seconds)
        val transition = initialState.reduce(event, defaultConfig)

        assertEquals(DisplayMode.KIOSK, transition.targetDisplayMode)
        assertFalse(transition.displayChanged)
        assertNull(transition.newState.cooldownUntil)
    }

    @Test
    fun `TimerTick does not clear active cooldown`() {
        val cooldownEnd = baseTime + 90.seconds
        val initialState = DisplayModeState(
            displayMode = DisplayMode.KIOSK,
            cooldownUntil = cooldownEnd,
        )

        val event = DisplayModeEvent.TimerTick(baseTime + 30.seconds)
        val transition = initialState.reduce(event, defaultConfig)

        assertEquals(cooldownEnd, transition.newState.cooldownUntil)
    }

    @Test
    fun `TimerTick clears both expired override and cooldown`() {
        val initialState = DisplayModeState(
            displayMode = DisplayMode.KIOSK,
            manualOverrideUntil = baseTime + 5.minutes,
            cooldownUntil = baseTime + 90.seconds,
        )

        val event = DisplayModeEvent.TimerTick(baseTime + 6.minutes)
        val transition = initialState.reduce(event, defaultConfig)

        assertNull(transition.newState.manualOverrideUntil)
        assertNull(transition.newState.cooldownUntil)
    }

    @Test
    fun `Configurable override duration is respected`() {
        val customConfig = DisplayModeConfig(
            manualOverrideDuration = 2.minutes,
            autoSwitchCooldown = 90.seconds,
        )
        val initialState = DisplayModeState(displayMode = DisplayMode.HA)

        val event = DisplayModeEvent.SwipeToggle(baseTime)
        val transition = initialState.reduce(event, customConfig)

        assertEquals(baseTime + 2.minutes, transition.newState.manualOverrideUntil)
    }

    @Test
    fun `Configurable cooldown duration is respected`() {
        val customConfig = DisplayModeConfig(
            manualOverrideDuration = 5.minutes,
            autoSwitchCooldown = 60.seconds,
        )
        val initialState = DisplayModeState(displayMode = DisplayMode.HA)

        val event = DisplayModeEvent.PresenceChanged(PresenceState.Present, baseTime)
        val transition = initialState.reduce(event, customConfig)

        assertEquals(baseTime + 60.seconds, transition.newState.cooldownUntil)
    }

    @Test
    fun `Rapid presence events do not extend cooldown`() {
        val initialState = DisplayModeState(displayMode = DisplayMode.HA)

        val firstEvent = DisplayModeEvent.PresenceChanged(PresenceState.Present, baseTime)
        val firstTransition = initialState.reduce(firstEvent, defaultConfig)
        val firstCooldown = firstTransition.newState.cooldownUntil!!

        val secondEvent = DisplayModeEvent.PresenceChanged(PresenceState.Present, baseTime + 10.seconds)
        val secondTransition = firstTransition.newState.reduce(secondEvent, defaultConfig)

        assertEquals(firstCooldown, secondTransition.newState.cooldownUntil)
    }

    @Test
    fun `Presence ABSENT updates state without affecting display`() {
        val initialState = DisplayModeState(
            displayMode = DisplayMode.KIOSK,
            presenceState = PresenceState.Present,
        )

        val event = DisplayModeEvent.PresenceChanged(PresenceState.Absent, baseTime)
        val transition = initialState.reduce(event, defaultConfig)

        assertEquals(DisplayMode.KIOSK, transition.targetDisplayMode)
        assertFalse(transition.displayChanged)
        assertEquals(PresenceState.Absent, transition.newState.presenceState)
    }
}

private fun assertTrue(value: Boolean) {
    kotlin.test.assertTrue(value)
}

private fun assertFalse(value: Boolean) {
    kotlin.test.assertFalse(value)
}
