package com.findmyfaculty.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** Read-only convenience for synthetic accounts in the ephemeral H2 preview profile. */
@RestController
@Profile("preview")
@RequestMapping("/api/preview")
public class PreviewDemoCredentialsController {
    @Value("${app.admin.username:}") private String adminUsername;
    @Value("${app.admin.password:}") private String adminPassword;
    @Value("${app.preview.student-username:}") private String studentUsername;
    @Value("${app.preview.student-password:}") private String studentPassword;
    @Value("${app.preview.faculty-username:}") private String facultyUsername;
    @Value("${app.preview.faculty-password:}") private String facultyPassword;

    @GetMapping("/demo-logins") public Map<String,Map<String,String>> credentials(){
        Map<String,Map<String,String>> result=new LinkedHashMap<>();
        add(result,"ADMIN",adminUsername,adminPassword);add(result,"STUDENT",studentUsername,studentPassword);add(result,"FACULTY",facultyUsername,facultyPassword);return result;
    }
    private void add(Map<String,Map<String,String>> result,String role,String username,String password){
        if(username==null||username.trim().isEmpty()||password==null||password.isEmpty())return;
        Map<String,String> c=new LinkedHashMap<>();c.put("username",username);c.put("password",password);result.put(role,c);
    }
}
