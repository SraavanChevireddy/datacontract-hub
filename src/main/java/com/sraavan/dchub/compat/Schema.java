package com.sraavan.dchub.compat;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * An ordered set of top-level fields describing one version of a data contract.
 */
public record Schema(List<SchemaField> fields) {

    public Schema {
        Objects.requireNonNull(fields, "fields");
        fields = List.copyOf(fields);
    }

    /** @return fields indexed by name for fast lookup during comparison. */
    public Map<String, SchemaField> byName() {
        return fields.stream().collect(Collectors.toMap(
                SchemaField::name, Function.identity(), (a, b) -> a, java.util.LinkedHashMap::new));
    }
}
