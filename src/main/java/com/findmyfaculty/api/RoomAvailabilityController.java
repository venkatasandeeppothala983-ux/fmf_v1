package com.findmyfaculty.api;

import com.findmyfaculty.service.RoomAvailabilityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomAvailabilityController {
    private final RoomAvailabilityService availability;

    public RoomAvailabilityController(RoomAvailabilityService availability) {
        this.availability = availability;
    }

    @GetMapping("/availability")
    public List<RoomAvailabilityView> availability() {
        return availability.currentSnapshot();
    }
}
