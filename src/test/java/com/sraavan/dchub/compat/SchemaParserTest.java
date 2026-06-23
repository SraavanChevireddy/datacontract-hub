package com.sraavan.dchub.compat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaParserTest {

    private final SchemaParser parser = new SchemaParser(new ObjectMapper());

    @Test
    void parsesScalarFieldsWithFlags() {
        Schema s = parser.parse("""
                {
                  "id":    { "type": "long" },
                  "email": { "type": "string", "nullable": true },
                  "coupon":{ "type": "string", "default": "" }
                }
                """);
        assertEquals(3, s.fields().size());
        SchemaField id = s.byName().get("id");
        assertEquals(FieldType.LONG, id.type());
        assertFalse(id.nullable());
        assertTrue(s.byName().get("email").nullable());
        assertTrue(s.byName().get("coupon").hasDefault());
    }

    @Test
    void parsesNestedStruct() {
        Schema s = parser.parse("""
                {
                  "address": { "type": "struct", "fields": {
                      "zip": { "type": "string" },
                      "geo": { "type": "struct", "fields": {
                          "lat": { "type": "double" }
                      }}
                  }}
                }
                """);
        SchemaField address = s.byName().get("address");
        assertEquals(FieldType.STRUCT, address.type());
        assertEquals(2, address.children().size());
    }

    @Test
    void rejectsUnknownType() {
        assertThrows(SchemaParseException.class,
                () -> parser.parse("{ \"x\": { \"type\": \"uuid\" } }"));
    }

    @Test
    void rejectsFieldWithoutType() {
        assertThrows(SchemaParseException.class,
                () -> parser.parse("{ \"x\": { \"nullable\": true } }"));
    }

    @Test
    void rejectsNonObjectRoot() {
        assertThrows(SchemaParseException.class, () -> parser.parse("[1, 2, 3]"));
    }
}
