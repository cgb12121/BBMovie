package com.bbmovie.camundaengine.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UniversityObjectTest {

    @Test
    void jsonAliasWebPageMapsToWebPagesField() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = """
                {
                  "name": "Demo",
                  "web_page": ["https://demo.edu"]
                }
                """;

        UniversityObject object = mapper.readValue(json, UniversityObject.class);

        assertEquals("Demo", object.getName());
        assertEquals(1, object.getWeb_pages().size());
        assertEquals("https://demo.edu", object.getWeb_pages().get(0));
    }
}
