package com.findmyfaculty.api;

import javax.validation.constraints.*;
import java.util.List;

public final class AdminInputs {
    private AdminInputs() {}

    public static class DepartmentInput {
        @NotBlank @Size(max=24) public String code;
        @NotBlank @Size(max=140) public String name;
        @Size(max=500) public String description;
        public Boolean active;
    }

    public static class StudentInput {
        @Size(max=80) public String username;
        @Size(min=12, max=72) public String password;
        @Size(max=160) public String fullName;
        @NotBlank @Size(max=40) public String studentNumber;
        public Long departmentId;
        @Size(max=40) public String yearLabel;
        public Long sectionId;
        @Size(max=30) public String phone;
        public Boolean active;
    }

    public static class FacultyInput {
        @NotBlank @Size(max=160) public String name;
        @Size(max=100) public String cabin;
        @Email @Size(max=180) public String email;
        public Long departmentId;
        public List<Long> subjectIds;
        public Boolean active;
    }

    public static class SubjectInput {
        @Size(max=40) public String code;
        @NotBlank @Size(max=240) public String name;
        public Long departmentId;
        public Boolean active;
    }

    public static class ClassroomInput {
        @NotBlank @Size(max=80) public String code;
        @Size(max=120) public String building;
        @Min(1) @Max(1000) public Integer capacity;
        public Boolean active;
    }

    public static class TimetableInput {
        @NotNull public Long facultyId;
        @NotBlank @Size(max=12) public String day;
        @NotNull @Min(1) @Max(20) public Integer period;
        @NotBlank public String startTime;
        @NotBlank public String endTime;
        public Long subjectId;
        @Size(max=240) public String subject;
        public Long classroomId;
        @Size(max=100) public String room;
    }

    public static class ClassTimetableInput {
        @NotNull public Long sectionId;
        @NotBlank @Size(max=12) public String day;
        @NotNull @Min(1) @Max(20) public Integer period;
        @NotBlank public String startTime;
        @NotBlank public String endTime;
        public Long subjectId;
        @Size(max=240) public String subject;
        public Long classroomId;
        @Size(max=180) public String room;
    }

    public static class AnnouncementInput {
        @NotBlank @Size(max=160) public String title;
        @NotBlank @Size(max=4000) public String body;
        @Pattern(regexp="ALL|STUDENT|FACULTY") public String audience = "ALL";
        public Boolean published;
        public String startsAt;
        public String endsAt;
    }

    public static class AccountStateInput { @NotNull public Boolean enabled; }
    public static class PasswordResetInput { @NotBlank @Size(min=12,max=72) public String password; }
    public static class ProfileInput { @Size(max=30) public String phone; }
}
