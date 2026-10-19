package com.findmyfaculty.api;
import javax.validation.constraints.NotNull;
public class DecisionInput {
    @NotNull public String decision;
    public String suggestedStart;
}
