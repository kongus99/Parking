package org.parking.controller;

import org.junit.jupiter.api.Test;
import org.parking.service.Parking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.parking.service.Parking.EngineType.*;
import static org.parking.service.Parking.Slot;
import static org.parking.service.Parking.Ticket;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParkingController.class)
public class ParkingControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockBean
    private Parking parking;

    @Test
    public void parkingStartsEmpty() throws Exception {
        when(parking.getTickets()).thenReturn(new ArrayList<>());
        mvc.perform(get("/parking/ticket"))
                .andExpect(status().isOk())
                .andExpect(content().string("[]"));
    }

    @Test
    public void youCanEnterParking() throws Exception {
        when(parking.enter(GAS)).thenReturn(Optional.of(new Ticket(GAS, new Slot("G", GAS))));
        mvc.perform(post("/parking/ticket?engine=GAS"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"engineType\":\"GAS\"")));
    }

    @Test
    public void youCannotEnterParkingWhenTooManySlotsReserved() throws Exception {
        when(parking.enter(ELECTRIC))
                .thenReturn(Optional.of(new Ticket(ELECTRIC, new Slot("E", ELECTRIC))))
                .thenReturn(Optional.empty());
        mvc.perform(post("/parking/ticket?engine=ELECTRIC"))
                .andExpect(status().isOk());
        mvc.perform(post("/parking/ticket?engine=ELECTRIC"))
                .andExpect(status().is(HttpStatus.SERVICE_UNAVAILABLE.value()));
    }

    @Test
    public void youCanRetrieveYourTicket() throws Exception {
        var uuid = UUID.randomUUID();
        when(parking.getTicket(uuid))
                .thenReturn(Optional.of(new Ticket(HI_ELECTRIC, new Slot("H", HI_ELECTRIC))))
                .thenReturn(Optional.empty());
        mvc.perform(get("/parking/ticket/" + uuid))
                .andExpect(status().isOk());
        mvc.perform(get("/parking/ticket/" + uuid))
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    public void youCanLeaveParkingIfYouPayEnough() throws Exception {
        var uuid = UUID.randomUUID();
        when(parking.leave(uuid, 0))
                .thenThrow(Parking.ParkingException.class);
        when(parking.leave(uuid, 10))
                .thenReturn(LocalDateTime.now());
        mvc.perform(delete("/parking/ticket/" + uuid + "?payment=10"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("T")));
        mvc.perform(delete("/parking/ticket/" + uuid + "?payment=0"))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    public void youCanCheckHowMuchYouOweForYourTicket() throws Exception {
        var uuid = UUID.randomUUID();
        when(parking.checkOwed(uuid))
                .thenReturn(111.0);
        mvc.perform(get("/parking/ticket/" + uuid + "/owed"))
                .andExpect(status().isOk())
                .andExpect(content().string("111.0"));
    }
}


