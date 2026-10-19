package com.findmyfaculty.service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
class ClassTimetableDataTest {
    @Test void classTimetableContainsTheNineteenSourceSections() throws Exception {
        InputStream source=getClass().getResourceAsStream("/seed/class-timetable.json");assertNotNull(source);
        List<ClassTimetableSeeder.SeedSection> sections=new ObjectMapper().readValue(source,new TypeReference<List<ClassTimetableSeeder.SeedSection>>(){});
        assertEquals(19,sections.size());Set<String> keys=new HashSet<>();
        for(ClassTimetableSeeder.SeedSection section:sections){assertTrue(keys.add(section.id));assertFalse(section.section.trim().isEmpty());for(ClassTimetableSeeder.SeedDay day:section.schedule)for(ClassTimetableSeeder.SeedSlot slot:day.slots)assertTrue(LocalTime.parse(slot.start).isBefore(LocalTime.parse(slot.end)));}
    }
}
