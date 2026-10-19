package com.findmyfaculty.api;
import com.findmyfaculty.domain.Faculty;
import com.findmyfaculty.domain.StatusType;
import java.time.LocalDateTime;
import java.util.List;
public class FacultyView {
    public Long id;
    public String sourceKey;
    public String name;
    public String department;
    public String cabin;
    public String email;
    public List<String> courses;
    public StatusType status;
    public String statusSource;
    public String location;
    public String note;
    public LocalDateTime statusUpdatedAt;
    public LocalDateTime expectedReturn;
    public String currentClass;
    public String currentRoom;
    public String nextClass;
    public Boolean active;
    public FacultyView() {}
    public FacultyView(Faculty f) {
        id=f.id; sourceKey=f.sourceKey; name=f.name;
        department=f.departmentRef==null?f.department:f.departmentRef.name;
        cabin=f.cabin; email=f.email; active=f.active==null?true:f.active;
        java.util.LinkedHashSet<String> names=new java.util.LinkedHashSet<>();
        if(f.courses!=null)names.addAll(f.courses);
        if(f.subjects!=null)f.subjects.forEach(s->names.add(s.name));
        courses=new java.util.ArrayList<>(names);
    }
}
