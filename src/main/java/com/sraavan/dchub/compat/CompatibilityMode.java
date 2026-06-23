package com.sraavan.dchub.compat;

/**
 * Compatibility tiers a contract can be governed under, mirroring the well-understood
 * semantics from schema registries:
 *
 * <ul>
 *   <li>{@code BACKWARD} — new readers can read data written by the previous version.
 *       (Safe to deploy consumers first.)</li>
 *   <li>{@code FORWARD} — old readers can read data written by the new version.
 *       (Safe to deploy producers first.)</li>
 *   <li>{@code FULL} — both directions hold.</li>
 *   <li>{@code NONE} — no compatibility enforced; any change is allowed.</li>
 * </ul>
 */
public enum CompatibilityMode {
    BACKWARD,
    FORWARD,
    FULL,
    NONE
}
