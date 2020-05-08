package org.parking.controller;

import org.parking.service.Parking;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.parking.service.Parking.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@RestController
public class ParkingController {

    private final Parking parking;

    public ParkingController(Parking parking) {
        this.parking = parking;
    }

    @PostMapping("/parking/ticket")
    public ResponseEntity<Ticket> createTicket(@RequestParam("engine") EngineType engineType) {
        return parking
                .enter(engineType)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(SERVICE_UNAVAILABLE).build());
    }

    @GetMapping("/parking/ticket")
    public List<Ticket> tickets() {
        return parking.getTickets();
    }

    @GetMapping("/parking/ticket/{id}")
    public ResponseEntity<Ticket> getTicket(@PathVariable("id") UUID id) {
        return parking.getTicket(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(NOT_FOUND).build());
    }

    @DeleteMapping("/parking/ticket/{id}")
    public LocalDateTime repayTicket(@PathVariable("id") UUID id, @RequestParam("payment") double payment) {
        try {
            return parking.leave(id, payment);
        } catch (ParkingException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/parking/ticket/{id}/owed")
    public Double getOwed(@PathVariable("id") UUID id) {
        return parking.checkOwed(id);
    }
}



