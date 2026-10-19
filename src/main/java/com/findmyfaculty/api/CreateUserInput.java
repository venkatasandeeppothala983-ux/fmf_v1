package com.findmyfaculty.api;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
public class CreateUserInput {
    @NotBlank @Size(max=80) public String username;
    @NotBlank @Size(min=12,max=72) public String password;
    @NotNull public String role;
    public Long facultyId;
    public Long sectionId;
}
