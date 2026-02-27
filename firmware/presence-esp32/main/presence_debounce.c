#include "presence_debounce.h"
#include <string.h>

void presence_debouncer_init(presence_debouncer_t *debouncer,
                              int presence_debounce_ms,
                              int absence_debounce_ms)
{
    memset(debouncer, 0, sizeof(presence_debouncer_t));
    debouncer->current_state = false;
    debouncer->raw_state = false;
    debouncer->state_changed_at = 0;
    debouncer->presence_debounce_ms = presence_debounce_ms;
    debouncer->absence_debounce_ms = absence_debounce_ms;
}

bool presence_debouncer_update(presence_debouncer_t *debouncer,
                                bool raw_present,
                                int64_t now_ms)
{
    /* Check if raw state changed */
    if (raw_present != debouncer->raw_state) {
        debouncer->raw_state = raw_present;
        debouncer->state_changed_at = now_ms;
        return false;  /* State just changed, need to wait for debounce */
    }

    /* Raw state is stable, check if we should emit a transition */
    int64_t stable_duration = now_ms - debouncer->state_changed_at;

    /* Transition from absent to present */
    if (debouncer->raw_state && !debouncer->current_state) {
        if (stable_duration >= debouncer->presence_debounce_ms) {
            debouncer->current_state = true;
            return true;
        }
    }

    /* Transition from present to absent */
    if (!debouncer->raw_state && debouncer->current_state) {
        if (stable_duration >= debouncer->absence_debounce_ms) {
            debouncer->current_state = false;
            return true;
        }
    }

    return false;
}

bool presence_debouncer_get_state(const presence_debouncer_t *debouncer)
{
    return debouncer->current_state;
}
