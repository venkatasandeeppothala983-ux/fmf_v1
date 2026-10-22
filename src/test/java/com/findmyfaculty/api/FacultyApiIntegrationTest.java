package com.findmyfaculty.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.findmyfaculty.domain.Role;
import com.findmyfaculty.domain.UserAccount;
import com.findmyfaculty.repo.UserRepository;
import com.findmyfaculty.repo.AnnouncementRepository;
import com.findmyfaculty.domain.Announcement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FacultyApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired AnnouncementRepository announcements;
    @Test void healthIsPublic() throws Exception {mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));}
    @Test void roomAvailabilityRequiresAnAuthenticatedUser() throws Exception {mvc.perform(get("/api/rooms/availability")).andExpect(status().isUnauthorized());}
    @Test void signedInStudentCanViewScheduleBasedRoomAvailability() throws Exception {
        UserAccount account=new UserAccount();account.username="room-view-student";account.passwordHash=passwords.encode("StudentRoomView2026!");account.role=Role.STUDENT;users.save(account);
        var login=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"room-view-student\",\"password\":\"StudentRoomView2026!\",\"loginType\":\"STUDENT\"}"))
            .andExpect(status().isOk()).andReturn();MockHttpSession session=(MockHttpSession)login.getRequest().getSession(false);
        mvc.perform(get("/api/rooms/availability").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }
    @Test void connectedPortalRouteIsPublic() throws Exception {mvc.perform(get("/")).andExpect(status().isOk());}
    @Test void alternativesAreMarkedUnconfirmed() throws Exception {mvc.perform(get("/api/faculty/1/alternatives")).andExpect(status().isOk()).andExpect(jsonPath("$[0].certainty").value("UNCONFIRMED"));}
    @Test void uploadedClassSectionsAreSearchable() throws Exception {mvc.perform(get("/api/sections")).andExpect(status().isOk()).andExpect(jsonPath("$",org.hamcrest.Matchers.hasSize(19)));}
    @Test void realFacultySeedIsSearchableAndHasTimetable() throws Exception {
        String payload=mvc.perform(get("/api/faculty").param("q","K.Balachandran")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode list=mapper.readTree(payload);org.junit.jupiter.api.Assertions.assertEquals(1,list.size());long id=list.get(0).get("id").asLong();
        mvc.perform(get("/api/faculty/"+id+"/timetable").param("day","MONDAY")).andExpect(status().isOk()).andExpect(jsonPath("$[0].subject").value("SPM"));
    }
    @Test void uploadedSectionHasAReadableTimetable() throws Exception {
        String payload=mvc.perform(get("/api/sections")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode first=mapper.readTree(payload).get(0);long id=first.get("id").asLong();
        mvc.perform(get("/api/sections/"+id+"/timetable")).andExpect(status().isOk()).andExpect(jsonPath("$[0].sectionId").value(id)).andExpect(jsonPath("$[0].subject").isNotEmpty());
    }
    @Test void jsonLoginEstablishesSessionForSubsequentApiCalls() throws Exception {
        UserAccount account=new UserAccount();account.username="session-login-student";account.passwordHash=passwords.encode("StudentSession2026!");account.role=Role.STUDENT;users.save(account);
        String body="{\"username\":\"session-login-student\",\"password\":\"StudentSession2026!\"}";
        var result=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("STUDENT")).andReturn();
        MockHttpSession session=(MockHttpSession)result.getRequest().getSession(false);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.username").value("session-login-student"));
    }
    @Test void statusWritesRequireAuthentication() throws Exception {
        mvc.perform(post("/api/faculty/1/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"AVAILABLE\"}"))
            .andExpect(status().isUnauthorized());
    }
    @Test void loginTypeMustMatchStoredRoleAndErrorIsGeneric() throws Exception {
        UserAccount account=new UserAccount();account.username="role-check-student";account.passwordHash=passwords.encode("StudentSession2026!");account.role=Role.STUDENT;users.save(account);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"role-check-student\",\"password\":\"StudentSession2026!\",\"loginType\":\"ADMIN\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Invalid username or password"));
    }
    @Test void studentSessionCannotAccessAdminEndpoints() throws Exception {
        UserAccount account=new UserAccount();account.username="blocked-admin-student";account.passwordHash=passwords.encode("StudentSession2026!");account.role=Role.STUDENT;users.save(account);
        var result=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"blocked-admin-student\",\"password\":\"StudentSession2026!\",\"loginType\":\"STUDENT\"}"))
            .andExpect(status().isOk()).andReturn();
        MockHttpSession session=(MockHttpSession)result.getRequest().getSession(false);
        mvc.perform(get("/api/admin/summary").session(session)).andExpect(status().isForbidden());
    }
    @Test void studentCannotWriteFacultyAvailability() throws Exception {
        UserAccount account=new UserAccount();account.username="status-write-student";account.passwordHash=passwords.encode("StudentSession2026!");account.role=Role.STUDENT;users.save(account);
        var result=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"status-write-student\",\"password\":\"StudentSession2026!\",\"loginType\":\"STUDENT\"}"))
            .andExpect(status().isOk()).andReturn();MockHttpSession session=(MockHttpSession)result.getRequest().getSession(false);
        mvc.perform(post("/api/faculty/1/status").session(session).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"AVAILABLE\"}"))
            .andExpect(status().isForbidden());
    }
    @Test void disabledAccountCannotKeepUsingExistingSession() throws Exception {
        UserAccount account=new UserAccount();account.username="session-revoked-student";account.passwordHash=passwords.encode("StudentSession2026!");account.role=Role.STUDENT;account=users.save(account);
        var result=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"session-revoked-student\",\"password\":\"StudentSession2026!\",\"loginType\":\"STUDENT\"}"))
            .andExpect(status().isOk()).andReturn();
        MockHttpSession session=(MockHttpSession)result.getRequest().getSession(false);account.enabled=false;users.save(account);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }
    @Test void adminCanPublishAndRetrieveAnnouncement() throws Exception {
        UserAccount admin=new UserAccount();admin.username="announcement-test-admin";admin.passwordHash=passwords.encode("AdminAnnounce2026!");admin.role=Role.ADMIN;users.save(admin);
        var login=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"announcement-test-admin\",\"password\":\"AdminAnnounce2026!\",\"loginType\":\"ADMIN\"}"))
            .andExpect(status().isOk()).andReturn();MockHttpSession session=(MockHttpSession)login.getRequest().getSession(false);
        var created=mvc.perform(post("/api/admin/announcements").session(session).contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Test notice\",\"body\":\"A test-only notice\",\"audience\":\"ALL\",\"published\":true}"))
            .andExpect(status().isCreated()).andReturn();
        long id=mapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        Announcement row=announcements.findById(id).orElseThrow();org.junit.jupiter.api.Assertions.assertNotNull(row.createdBy);
        mvc.perform(get("/api/admin/announcements").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$[0].createdBy").value("announcement-test-admin"));
        mvc.perform(get("/api/announcements").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$[0].title").value("Test notice"));
    }
    @Test void adminCanCreateStudentWithAHashedPasswordAndProfile() throws Exception {
        UserAccount admin=new UserAccount();admin.username="crud-test-admin";admin.passwordHash=passwords.encode("AdminTestPass2026!");admin.role=Role.ADMIN;users.save(admin);
        var login=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"crud-test-admin\",\"password\":\"AdminTestPass2026!\",\"loginType\":\"ADMIN\"}"))
            .andExpect(status().isOk()).andReturn();MockHttpSession session=(MockHttpSession)login.getRequest().getSession(false);
        mvc.perform(post("/api/admin/students").session(session).contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"crud-test-student\",\"password\":\"StudentCreatePass2026!\",\"studentNumber\":\"TEST-STU-001\",\"yearLabel\":\"Test Year\",\"active\":true}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.username").value("crud-test-student"));
        UserAccount created=users.findByUsername("crud-test-student").orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotEquals("StudentCreatePass2026!",created.passwordHash);
        org.junit.jupiter.api.Assertions.assertTrue(passwords.matches("StudentCreatePass2026!",created.passwordHash));
        var studentLogin=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"crud-test-student\",\"password\":\"StudentCreatePass2026!\",\"loginType\":\"STUDENT\"}"))
            .andExpect(status().isOk()).andReturn();MockHttpSession studentSession=(MockHttpSession)studentLogin.getRequest().getSession(false);
        mvc.perform(get("/api/students/me").session(studentSession)).andExpect(status().isOk()).andExpect(jsonPath("$.studentNumber").value("TEST-STU-001"));
    }
}
