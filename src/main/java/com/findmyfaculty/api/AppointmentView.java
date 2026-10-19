package com.findmyfaculty.api;
import com.findmyfaculty.domain.AppointmentRequest;
import java.time.LocalDateTime;
public class AppointmentView {
    public Long id; public Long facultyId; public String facultyName; public String studentUsername;
    public LocalDateTime requestedStart; public LocalDateTime suggestedStart; public String reason; public String state; public String conflictWarning; public LocalDateTime createdAt;
    public AppointmentView(AppointmentRequest a){id=a.id;facultyId=a.faculty.id;facultyName=a.faculty.name;studentUsername=a.student.username;requestedStart=a.requestedStart;suggestedStart=a.suggestedStart;reason=a.reason;state=a.state.name();conflictWarning=a.conflictWarning;createdAt=a.createdAt;}
}
