package com.findmyfaculty.api;
import com.findmyfaculty.domain.ClassSection;
import java.util.List;
public class ClassSectionView {
    public Long id;public String sourceKey;public String year;public String semester;public String section;public String displayName;public String classIncharge;public List<ClassCourseView> courses;
    public ClassSectionView(ClassSection s,List<ClassCourseView> c){id=s.id;sourceKey=s.sourceKey;year=s.year;semester=s.semester;section=s.section;displayName=s.displayName();classIncharge=s.classIncharge;courses=c;}
}
