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

class StaffTimetableDataTest {
    @Test void extractedSeedHasUniqueProfilesAndValidPeriods() throws Exception {
        ObjectMapper mapper=new ObjectMapper();
        InputStream source=getClass().getResourceAsStream("/seed/staff-timetable.json");
        assertNotNull(source,"seed file should be packaged");
        List<StaffTimetableSeeder.SeedFaculty> staff=mapper.readValue(source,new TypeReference<List<StaffTimetableSeeder.SeedFaculty>>(){});
        assertEquals(31,staff.size(),"source timetable contains 31 faculty records");
        Set<String> ids=new HashSet<>();
        for(StaffTimetableSeeder.SeedFaculty f:staff){
            assertTrue(ids.add(f.id),"faculty source keys must be unique");
            assertNotNull(f.name);assertFalse(f.name.trim().isEmpty());
            for(StaffTimetableSeeder.SeedDay day:f.schedule)for(StaffTimetableSeeder.SeedSlot slot:day.slots){
                assertTrue(slot.period>=1&&slot.period<=7);
                assertTrue(LocalTime.parse(slot.start).isBefore(LocalTime.parse(slot.end)),"class start must precede end");
            }
        }
    }
}
