package dev.taikin.mcplugins.cifviewer.mp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonParserTest {

    @Test
    @SuppressWarnings("unchecked")
    void parsesNestedMaterialsProjectResponse() {
        String json = """
                {
                  "data": [
                    {
                      "material_id": "mp-149",
                      "structure": {
                        "lattice": {"matrix": [[5.4, 0, 0], [0, 5.4, 0], [0, 0, 5.4]]},
                        "sites": [
                          {"species": [{"element": "Si", "occu": 1.0}], "abc": [0.0, 0.0, 0.0]}
                        ]
                      }
                    }
                  ]
                }
                """;

        Map<String, Object> root = (Map<String, Object>) JsonParser.parse(json);
        List<Object> data = (List<Object>) root.get("data");
        Map<String, Object> document = (Map<String, Object>) data.getFirst();
        Map<String, Object> structure = (Map<String, Object>) document.get("structure");
        Map<String, Object> lattice = (Map<String, Object>) structure.get("lattice");
        List<Object> matrix = (List<Object>) lattice.get("matrix");

        assertEquals("mp-149", document.get("material_id"));
        assertEquals(3, matrix.size());
        assertEquals(5.4, ((Number) ((List<Object>) matrix.getFirst()).getFirst()).doubleValue(), 1.0e-9);
    }

    @Test
    void parsesEscapedStringsAndBooleans() {
        Object parsed = JsonParser.parse("{\"a\":\"x\\ny\",\"b\":true,\"c\":null}");
        Map<?, ?> map = (Map<?, ?>) parsed;

        assertEquals("x\ny", map.get("a"));
        assertEquals(Boolean.TRUE, map.get("b"));
        assertNull(map.get("c"));
    }
}
