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

/**
 * Spring REST controller for {@link Parking Parking}. It holds a singleton bean representing a single parking instance.
 * It should be easily extensible to include multiple parking instances, if the return type is changed to include the assigned
 * parking information. Then it can be used either locally from the entrance or via web interface.
 *
 * @author lucas dudek
 * @see Parking See also Parking class for more info
 */
@RestController
public class ParkingController {

    private final Parking parking;

    /**
     * Default Spring injection constructor.
     *
     * @param parking injected {@link Parking Parking} instance
     */
    public ParkingController(Parking parking) {
        this.parking = parking;
    }

    /**
     * POST method to create a parking ticket. Requires engine type as parameter.
     *
     * @param engineType requested engine type for parking ticket
     * @return parking ticket if slot was available, {@link HttpStatus#SERVICE_UNAVAILABLE 503(SERVICE_UNAVAILABLE)} if currently not available
     */
    @PostMapping("/parking/ticket")
    public ResponseEntity<Ticket> createTicket(@RequestParam("engine") EngineType engineType) {
        return parking
                .enter(engineType)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(SERVICE_UNAVAILABLE).build());
    }

    /**
     * GET method to retrieve all outstanding parking tickets.
     *
     * @return list of all parking tickets
     */
    @GetMapping("/parking/ticket")
    public List<Ticket> tickets() {
        return parking.getTickets();
    }


    /**
     * GET method to retrieve outstanding ticket with given id.
     *
     * @param id id of requested ticket
     * @return parking ticket it exists, {@link HttpStatus#NOT_FOUND 404(NOT_FOUND)} if not
     */
    @GetMapping("/parking/ticket/{id}")
    public ResponseEntity<Ticket> getTicket(@PathVariable("id") UUID id) {
        return parking.getTicket(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(NOT_FOUND).build());
    }

    /**
     * DELETE method to repay and remove outstanding ticket from parking instance.
     *
     * @param id      id of the ticket to repay
     * @param payment sufficient payment for the ticket
     * @return date of ticket repayment, {@link HttpStatus#BAD_REQUEST 400(BAD_REQUEST)} if not all the data given was valid
     */
    @DeleteMapping("/parking/ticket/{id}")
    public LocalDateTime repayTicket(@PathVariable("id") UUID id, @RequestParam("payment") double payment) {
        try {
            return parking.leave(id, payment);
        } catch (ParkingException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }


    /**
     * GET method to retrieve outstanding ticket's current payment amount.
     *
     * @param id id of requested ticket
     * @return amount owed for the ticket
     */
    @GetMapping("/parking/ticket/{id}/owed")
    public Double getOwed(@PathVariable("id") UUID id) {
        return parking.checkOwed(id);
    }
}



