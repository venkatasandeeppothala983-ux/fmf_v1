package com.findmyfaculty.api;
import com.findmyfaculty.domain.ClassCourse;
public class ClassCourseView {
    public Long id;public String code;public String name;public String facultyName;
    public ClassCourseView(ClassCourse c){id=c.id;code=c.code;name=c.name;facultyName=c.facultyName;}
}
