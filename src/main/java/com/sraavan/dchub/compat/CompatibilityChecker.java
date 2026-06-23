package com.sraavan.dchub.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The compatibility engine. Pure, dependency-free, fully unit-testable.
 *
 * <p>Given a {@code current} schema and a {@code candidate} schema, it walks both
 * trees, records every structural {@link Change}, and decides whether the candidate
 * is allowed under a {@link CompatibilityMode}.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li><b>Add field</b> — breaks forward compatibility only if the new field is
 *       required (not nullable and has no default), because old writers won't supply it.
 *       Never breaks backward compatibility.</li>
 *   <li><b>Remove field</b> — breaks backward compatibility if the removed field was
 *       required, because new readers expect it. Never breaks forward compatibility.</li>
 *   <li><b>Type change</b> — safe in the direction it widens. {@code old.widensTo(new)}
 *       keeps backward compatibility; {@code new.widensTo(old)} keeps forward.</li>
 *   <li><b>Nullability</b> — making a field nullable breaks forward (old readers may not
 *       expect null); making it required breaks backward (old data may have null).</li>
 * </ul>
 */
public final class CompatibilityChecker {

    public CompatibilityResult check(Schema current, Schema candidate, CompatibilityMode mode) {
        List<Change> changes = new ArrayList<>();
        diff("", current.fields(), candidate.fields(), changes);

        List<Change> violations = new ArrayList<>();
        for (Change c : changes) {
            if (violates(c, mode)) {
                violations.add(c);
            }
        }
        boolean compatible = violations.isEmpty();
        return new CompatibilityResult(mode, compatible, changes, violations);
    }

    private boolean violates(Change c, CompatibilityMode mode) {
        return switch (mode) {
            case NONE -> false;
            case BACKWARD -> c.breaksBackward();
            case FORWARD -> c.breaksForward();
            case FULL -> c.breaksBackward() || c.breaksForward();
        };
    }

    private void diff(String prefix, List<SchemaField> oldFields, List<SchemaField> newFields,
                      List<Change> out) {
        Map<String, SchemaField> oldByName = index(oldFields);
        Map<String, SchemaField> newByName = index(newFields);

        // Removed fields (present in old, gone in new).
        for (SchemaField f : oldFields) {
            if (!newByName.containsKey(f.name())) {
                boolean wasRequired = !f.nullable() && !f.hasDefault();
                out.add(new Change(
                        path(prefix, f.name()),
                        Change.Kind.FIELD_REMOVED,
                        wasRequired,   // new readers expecting a required field => backward break
                        false,
                        "Field '" + f.name() + "' was removed"
                                + (wasRequired ? " (was required)" : "")));
            }
        }

        // Added fields (present in new, absent in old).
        for (SchemaField f : newFields) {
            if (!oldByName.containsKey(f.name())) {
                boolean nowRequired = !f.nullable() && !f.hasDefault();
                out.add(new Change(
                        path(prefix, f.name()),
                        Change.Kind.FIELD_ADDED,
                        false,
                        nowRequired,   // old writers won't supply it => forward break
                        "Field '" + f.name() + "' was added"
                                + (nowRequired ? " (required, no default)" : "")));
            }
        }

        // Fields present in both: compare type / nullability / nested structure.
        for (SchemaField oldF : oldFields) {
            SchemaField newF = newByName.get(oldF.name());
            if (newF == null) {
                continue;
            }
            String p = path(prefix, oldF.name());
            compareTypes(p, oldF, newF, out);
            compareNullability(p, oldF, newF, out);

            if (oldF.type() == FieldType.STRUCT && newF.type() == FieldType.STRUCT) {
                diff(p, oldF.children(), newF.children(), out);
            }
        }
    }

    private void compareTypes(String path, SchemaField oldF, SchemaField newF, List<Change> out) {
        if (oldF.type() == newF.type()) {
            return;
        }
        boolean structToScalar = oldF.type() == FieldType.STRUCT || newF.type() == FieldType.STRUCT;
        if (structToScalar) {
            out.add(new Change(path, Change.Kind.STRUCT_TO_SCALAR, true, true,
                    "Field '" + path + "' changed between struct and scalar ("
                            + oldF.type() + " -> " + newF.type() + ")"));
            return;
        }
        boolean keepsBackward = oldF.type().widensTo(newF.type()); // old data fits new type
        boolean keepsForward = newF.type().widensTo(oldF.type());  // new data fits old type
        out.add(new Change(path, Change.Kind.TYPE_CHANGED,
                !keepsBackward, !keepsForward,
                "Field '" + path + "' type changed " + oldF.type() + " -> " + newF.type()));
    }

    private void compareNullability(String path, SchemaField oldF, SchemaField newF, List<Change> out) {
        if (oldF.nullable() == newF.nullable()) {
            return;
        }
        if (newF.nullable()) {
            // old -> new: became nullable. Old readers may not expect null => forward break.
            out.add(new Change(path, Change.Kind.NULLABILITY_CHANGED, false, true,
                    "Field '" + path + "' became nullable"));
        } else {
            // became required. Old data may contain null => backward break.
            out.add(new Change(path, Change.Kind.NULLABILITY_CHANGED, true, false,
                    "Field '" + path + "' became required (was nullable)"));
        }
    }

    private static Map<String, SchemaField> index(List<SchemaField> fields) {
        Map<String, SchemaField> m = new java.util.LinkedHashMap<>();
        for (SchemaField f : fields) {
            m.put(f.name(), f);
        }
        return m;
    }

    private static String path(String prefix, String name) {
        return prefix.isEmpty() ? name : prefix + "." + name;
    }
}
