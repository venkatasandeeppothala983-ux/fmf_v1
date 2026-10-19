package com.findmyfaculty.api;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
public class AppointmentInput {
    @NotNull public Long facultyId;
    @NotBlank public String requestedStart;
    @NotBlank @Size(max=500) public String reason;
}
