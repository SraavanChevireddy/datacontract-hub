package com.sraavan.dchub.compat;

import java.util.List;

/**
 * Outcome of comparing a candidate schema against the current one under a given mode.
 *
 * @param mode          the mode the check was run under
 * @param compatible    whether the change is allowed under {@code mode}
 * @param changes       every structural difference detected (including non-breaking ones)
 * @param violations    the subset of {@code changes} that actually violate {@code mode}
 */
public record CompatibilityResult(
        CompatibilityMode mode,
        boolean compatible,
        List<Change> changes,
        List<Change> violations) {

    public CompatibilityResult {
        changes = List.copyOf(changes);
        violations = List.copyOf(violations);
    }
}
