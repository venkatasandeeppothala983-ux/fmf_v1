package com.findmyfaculty.service;

import com.findmyfaculty.domain.Faculty;
import com.findmyfaculty.domain.TimetableEntry;
import com.findmyfaculty.repo.FacultyRepository;
import com.findmyfaculty.repo.TimetableRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.io.Reader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

@Service
public class TimetableImportService {
    private final FacultyRepository faculties;private final TimetableRepository timetable;
    private static final Set<String> HEADERS=new HashSet<>(Arrays.asList("facultyKey","day","period","startTime","endTime","subject","room"));
    public TimetableImportService(FacultyRepository f,TimetableRepository t){faculties=f;timetable=t;}
    @Transactional
    public Map<String,Object> importCsv(InputStream input)throws Exception{
        List<String> errors=new ArrayList<>();List<TimetableEntry> staged=new ArrayList<>();Set<Long> touched=new HashSet<>();
        try(Reader reader=new InputStreamReader(input, StandardCharsets.UTF_8);CSVParser parser=CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim().parse(reader)){
            Set<String> headers=parser.getHeaderMap().keySet();if(!headers.containsAll(HEADERS))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"CSV must include headers: facultyKey,day,period,startTime,endTime,subject,room");
            for(CSVRecord row:parser){int rowNum=(int)row.getRecordNumber()+1;try{
                String key=row.get("facultyKey");Faculty faculty=faculties.findBySourceKey(key).orElseThrow(()->new IllegalArgumentException("unknown facultyKey "+key));
                String day=row.get("day").toUpperCase(Locale.ROOT);DayOfWeek parsed=DayOfWeek.valueOf(day);if(parsed==DayOfWeek.SUNDAY)throw new IllegalArgumentException("Sunday is not a teaching day in this timetable");
                int period=Integer.parseInt(row.get("period"));if(period<1||period>7)throw new IllegalArgumentException("period must be 1–7");
                LocalTime start=LocalTime.parse(row.get("startTime")),end=LocalTime.parse(row.get("endTime"));if(!start.isBefore(end))throw new IllegalArgumentException("startTime must precede endTime");
                String subject=row.get("subject").trim();if(subject.isEmpty())throw new IllegalArgumentException("subject is required");String room=row.get("room").trim();
                TimetableEntry e=new TimetableEntry();e.faculty=faculty;e.day=day;e.period=period;e.startTime=start;e.endTime=end;e.subject=subject;e.room=room.isEmpty()?null:room;e.sourceFile="Admin CSV import";
                for(TimetableEntry prior:staged){if(!prior.day.equals(day)||!overlap(start,end,prior.startTime,prior.endTime))continue;if(prior.faculty.id.equals(faculty.id))throw new IllegalArgumentException("faculty timetable overlap with another imported row");if(e.room!=null&&prior.room!=null&&e.room.equalsIgnoreCase(prior.room))throw new IllegalArgumentException("room "+e.room+" is assigned to two classes at the same time");}
                staged.add(e);touched.add(faculty.id);
            }catch(Exception ex){errors.add("Row "+rowNum+": "+ex.getMessage());}}
        }
        if(!errors.isEmpty())throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,String.join(" | ",errors));
        if(staged.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"CSV has no timetable rows");
        timetable.deleteForFacultyIds(touched);timetable.saveAll(staged);
        Map<String,Object> result=new LinkedHashMap<>();result.put("importedRows",staged.size());result.put("facultyUpdated",touched.size());result.put("validation","passed; timetable rows replaced atomically for included faculty");return result;
    }
    private boolean overlap(LocalTime a,LocalTime b,LocalTime c,LocalTime d){return a.isBefore(d)&&b.isAfter(c);}
}
