#ifndef PRESENCE_DEBOUNCE_H
#define PRESENCE_DEBOUNCE_H

#include <stdbool.h>
#include <stdint.h>

/**
 * Debounce presence detection to prevent flapping.
 * Presence must be stable for PRESENCE_DEBOUNCE_MS before emitting PRESENT.
 * Absence must be stable for ABSENCE_DEBOUNCE_MS before emitting ABSENT.
 */

/* Default debounce intervals (in milliseconds) */
#define PRESENCE_DEBOUNCE_MS 1000
#define ABSENCE_DEBOUNCE_MS 2000

/**
 * Debouncer state.
 */
typedef struct {
    bool current_state;         /* Last emitted state: true=present, false=absent */
    bool raw_state;             /* Current raw sensor reading */
    int64_t state_changed_at;   /* Timestamp when raw_state last changed */
    int presence_debounce_ms;   /* Debounce interval for present->absent transition */
    int absence_debounce_ms;    /* Debounce interval for absent->present transition */
} presence_debouncer_t;

/**
 * Initialize the debouncer.
 * @param debouncer pointer to debouncer state
 * @param presence_debounce_ms debounce time for presence (ms)
 * @param absence_debounce_ms debounce time for absence (ms)
 */
void presence_debouncer_init(presence_debouncer_t *debouncer,
                              int presence_debounce_ms,
                              int absence_debounce_ms);

/**
 * Update the debouncer with a new raw sensor reading.
 * @param debouncer pointer to debouncer state
 * @param raw_present current raw sensor state
 * @param now_ms current timestamp in milliseconds
 * @return true if the debounced state changed and should be emitted
 */
bool presence_debouncer_update(presence_debouncer_t *debouncer,
                                bool raw_present,
                                int64_t now_ms);

/**
 * Get the current debounced state.
 * @param debouncer pointer to debouncer state
 * @return true if present, false if absent
 */
bool presence_debouncer_get_state(const presence_debouncer_t *debouncer);

#endif /* PRESENCE_DEBOUNCE_H */
