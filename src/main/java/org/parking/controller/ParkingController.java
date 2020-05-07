package org.parking.controller;

import org.parking.service.Parking;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.parking.service.Parking.EngineType;
import static org.parking.service.Parking.Ticket;
import static org.springframework.http.HttpStatus.*;

@RestController
public class ParkingController {

    private final Parking parking;

    public ParkingController(Parking parking) {
        this.parking = parking;
    }

    @GetMapping("/parking/ticket")
    public ResponseEntity<Ticket> ticket(@RequestParam("engine") EngineType engineType) {
        return parking
                .enter(engineType)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(SERVICE_UNAVAILABLE).build());
    }

}



