package com.florastore.web_ban_hoa.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagedResponseJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializeCanonicalAndLegacyPaginationFields() {
        PagedResponse<String> response = PagedResponse.from(
                new PageImpl<>(List.of("rose", "lily"), PageRequest.of(1, 2), 5)
        );

        JsonNode json = objectMapper.valueToTree(response);

        assertEquals(1, json.get("currentPage").asInt());
        assertEquals(1, json.get("number").asInt());
        assertEquals(3, json.get("totalPages").asInt());
        assertFalse(json.get("first").asBoolean());
        assertFalse(json.get("last").asBoolean());
    }

    @Test
    void shouldMarkEmptyFirstPageAsFirstAndLast() {
        PagedResponse<String> response = PagedResponse.from(
                new PageImpl<>(List.of(), PageRequest.of(0, 10), 0)
        );

        JsonNode json = objectMapper.valueToTree(response);

        assertTrue(json.get("first").asBoolean());
        assertTrue(json.get("last").asBoolean());
        assertEquals(0, json.get("number").asInt());
        assertEquals(0, json.get("currentPage").asInt());
        assertEquals(0, json.get("totalPages").asInt());
    }
}
