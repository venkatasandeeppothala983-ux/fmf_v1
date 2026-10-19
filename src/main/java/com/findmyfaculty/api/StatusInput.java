package com.findmyfaculty.api;
import javax.validation.constraints.NotNull;
public class StatusInput {
    @NotNull public String status;
    public String location;
    public String note;
    public String expectedReturn;
}
