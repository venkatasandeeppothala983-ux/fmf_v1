package com.findmyfaculty.service;

import com.findmyfaculty.api.FacultyView;
import com.findmyfaculty.domain.*;
import com.findmyfaculty.repo.StatusRepository;
import com.findmyfaculty.repo.TimetableRepository;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class AvailabilityService {
    private final StatusRepository statusRepo;
    private final TimetableRepository timetableRepo;
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    public AvailabilityService(StatusRepository s, TimetableRepository t) { statusRepo=s; timetableRepo=t; }

    public FacultyView view(Faculty faculty) {
        FacultyView out=new FacultyView(faculty);
        LocalDateTime now=LocalDateTime.now(ZONE);
        List<TimetableEntry> today=timetableRepo.findByFaculty_IdAndDayIgnoreCaseOrderByStartTimeAsc(faculty.id,now.getDayOfWeek().name());
        TimetableEntry active=today.stream().filter(e->!now.toLocalTime().isBefore(e.startTime)&&now.toLocalTime().isBefore(e.endTime)).findFirst().orElse(null);
        if(active!=null){out.currentClass=active.subject;out.currentRoom=active.room;}
        TimetableEntry next=today.stream().filter(e->e.startTime.isAfter(now.toLocalTime())).min(Comparator.comparing(e->e.startTime)).orElse(null);
        if(next!=null)out.nextClass=next.subject+" at "+next.startTime+(next.room==null?"":" · "+next.room);
        Optional<FacultyStatusUpdate> latest=statusRepo.findFirstByFaculty_IdOrderByUpdatedAtDesc(faculty.id);
        if(latest.isPresent()&&latest.get().expectedReturn!=null&&latest.get().expectedReturn.isAfter(now)){
            FacultyStatusUpdate update=latest.get();out.status=update.status;out.statusSource="SELF_REPORTED";out.location=update.location;out.note=update.note;out.statusUpdatedAt=update.updatedAt;out.expectedReturn=update.expectedReturn;
        }else if(active!=null){out.status=StatusType.TEACHING;out.statusSource="TIMETABLE";}
        else{out.status=StatusType.UNKNOWN;out.statusSource="NO_CURRENT_STATUS";}
        return out;
    }
    public ZoneId zone(){return ZONE;}
}
