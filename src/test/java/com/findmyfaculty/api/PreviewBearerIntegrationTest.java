package com.findmyfaculty.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("preview")
@TestPropertySource(properties={"app.admin.username=preview-token-admin","app.admin.password=PreviewTokenAdmin2026!","app.preview.faculty-username=preview-saraswathi","app.preview.faculty-password=PreviewFacultyPass2026!"})
class PreviewBearerIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test void previewLoginTokenAuthorizesWithoutBrowserCookieAndLogoutRevokesIt() throws Exception {
        String login="{\"username\":\"preview-token-admin\",\"password\":\"PreviewTokenAdmin2026!\",\"loginType\":\"ADMIN\"}";
        String response=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
            .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN")).andReturn().getResponse().getContentAsString();
        JsonNode json=mapper.readTree(response);String token=json.get("previewToken").asText();org.junit.jupiter.api.Assertions.assertTrue(token.length()>=40);
        mvc.perform(get("/api/admin/summary").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.faculty").value(31));
        String directory=mvc.perform(get("/api/admin/students").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode records=mapper.readTree(directory);int supplied=0;
        for(JsonNode row:records){
            String studentNumber=row.path("studentNumber").asText();
            if(studentNumber.startsWith("11249A")){
                supplied++;
                org.junit.jupiter.api.Assertions.assertEquals("ADND",row.path("department").asText());
                org.junit.jupiter.api.Assertions.assertEquals("III Year",row.path("yearLabel").asText());
                org.junit.jupiter.api.Assertions.assertTrue(row.path("section").asText().contains("S3"));
                org.junit.jupiter.api.Assertions.assertFalse(row.path("loginEnabled").asBoolean());
            }
        }
        org.junit.jupiter.api.Assertions.assertEquals(5,supplied,"preview student records should be directory-only entries in the approved S3 group");
        String facultyLogin="{\"username\":\"preview-saraswathi\",\"password\":\"PreviewFacultyPass2026!\",\"loginType\":\"FACULTY\"}";
        String facultyResponse=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(facultyLogin))
            .andExpect(status().isOk()).andExpect(jsonPath("$.facultyName").value("Dr. M.Saraswathi"))
            .andReturn().getResponse().getContentAsString();
        String facultyToken=mapper.readTree(facultyResponse).get("previewToken").asText();
        mvc.perform(get("/api/faculty/me/timetable?day=MONDAY").header("Authorization","Bearer "+facultyToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$[2].subject").value("S3 JAVA"));
        mvc.perform(post("/api/auth/logout").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/summary").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
    }
}
