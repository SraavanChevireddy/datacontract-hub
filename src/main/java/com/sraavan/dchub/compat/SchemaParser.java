package com.sraavan.dchub.compat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the project's compact JSON schema representation into a {@link Schema}.
 *
 * <p>Format (a JSON object whose keys are field names):
 * <pre>
 * {
 *   "id":      { "type": "long" },
 *   "email":   { "type": "string", "nullable": true },
 *   "coupon":  { "type": "string", "default": "" },
 *   "address": { "type": "struct", "fields": {
 *                  "zip": { "type": "string" }
 *               }}
 * }
 * </pre>
 * A field is required unless it is {@code nullable} or declares a {@code default}.
 */
public final class SchemaParser {

    private final ObjectMapper mapper;

    public SchemaParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public Schema parse(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            return new Schema(parseFields(root));
        } catch (SchemaParseException e) {
            throw e;
        } catch (Exception e) {
            throw new SchemaParseException("Invalid schema JSON: " + e.getMessage());
        }
    }

    private List<SchemaField> parseFields(JsonNode node) {
        if (node == null || !node.isObject()) {
            throw new SchemaParseException("Expected a JSON object of fields");
        }
        List<SchemaField> fields = new ArrayList<>();
        node.fieldNames().forEachRemaining(name -> fields.add(parseField(name, node.get(name))));
        return fields;
    }

    private SchemaField parseField(String name, JsonNode def) {
        if (def == null || !def.isObject() || !def.has("type")) {
            throw new SchemaParseException("Field '" + name + "' must declare a type");
        }
        String typeStr = def.get("type").asText().toUpperCase();
        FieldType type;
        try {
            type = FieldType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            throw new SchemaParseException("Field '" + name + "' has unknown type '" + typeStr + "'");
        }
        boolean nullable = def.path("nullable").asBoolean(false);
        boolean hasDefault = def.has("default");

        if (type == FieldType.STRUCT) {
            List<SchemaField> children = parseFields(def.get("fields"));
            return new SchemaField(name, type, nullable, hasDefault, children);
        }
        return new SchemaField(name, type, nullable, hasDefault, List.of());
    }
}
