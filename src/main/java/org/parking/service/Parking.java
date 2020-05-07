package org.parking.service;

import org.parking.conf.ParkingConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static org.parking.service.Parking.EngineType.*;

@Service
public class Parking {

    private final Function<Ticket, Double> pricing;
    private final Map<EngineType, List<Slot>> slots;
    private final Map<UUID, Ticket> tickets = new HashMap<>();

    @Autowired
    public Parking(ParkingConfiguration config) {
        this(config::pricing, config.slots());
    }

    public Parking(Function<Ticket, Double> pricing, Map<EngineType, List<String>> slots) {
        this.pricing = pricing;
        this.slots = slots.entrySet()
                .stream()
                .collect(toMap(Map.Entry::getKey, e -> e.getValue()
                        .stream()
                        .map(id -> new Slot(id, e.getKey()))
                        .collect(Collectors.toList())));
    }

    public Optional<Ticket> enter(EngineType engineType) {
        switch (engineType) {
            case HI_ELECTRIC:
            case ELECTRIC:
                return assign(engineType)
                        .map(s -> createTicket(engineType, s));
            case GAS:
            default:
                return assign(GAS)
                        .or(() -> assign(ELECTRIC))
                        .or(() -> assign(HI_ELECTRIC))
                        .map(s -> createTicket(GAS, s));
        }
    }

    private Ticket createTicket(EngineType engineType, Slot slot) {
        var ticket = new Ticket(engineType, slot);
        tickets.put(ticket.id, ticket);
        return ticket;
    }

    private synchronized Optional<Slot> assign(EngineType t) {
        var matchingSlots = slots.putIfAbsent(t, new ArrayList<>());
        if (matchingSlots != null && !matchingSlots.isEmpty())
            return Optional.of(matchingSlots.remove(0));
        else
            return Optional.empty();

    }

    public synchronized LocalDateTime leave(UUID id, double payment) {
        var ticket = tickets.get(id);
        if (ticket != null) {
            var owed = checkOwed(id);
            if (payment < owed)
                throw new ParkingException("Insufficient payment:" + payment + " ,required:" + owed);
            slots.merge(ticket.slot.engineType, List.of(ticket.slot), (l1, l2) ->
                    Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList()));
            tickets.remove(id);
            return LocalDateTime.now();
        } else throw new ParkingException("Unknown ticket " + id);
    }

    public double checkOwed(UUID id) {
        return getTicket(id).map(pricing).orElse(0.0);
    }

    public Optional<Ticket> getTicket(UUID id) {
        return Optional.ofNullable(tickets.get(id));
    }

    public List<Ticket> getTickets() {
        return tickets.values()
                .stream()
                .sorted(Comparator.comparing(ticket -> ticket.issueTime))
                .collect(Collectors.toList());
    }

    public enum EngineType {
        GAS, ELECTRIC, HI_ELECTRIC
    }

    public static final class Slot {
        public final String id;
        public final EngineType engineType;

        public Slot(String id, EngineType engineType) {
            this.id = id;
            this.engineType = engineType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Slot slot = (Slot) o;
            return id.equals(slot.id) &&
                    engineType == slot.engineType;
        }
        @Override
        public int hashCode() {
            return Objects.hash(id, engineType);
        }
    }


    public static final class Ticket {
        public final UUID id;
        public final EngineType engineType;
        public final LocalDateTime issueTime;
        public final Slot slot;

        public Ticket(EngineType engineType, Slot slot) {
            this.slot = slot;
            this.id = UUID.randomUUID();
            this.engineType = engineType;
            this.issueTime = LocalDateTime.now();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Ticket ticket = (Ticket) o;
            return id.equals(ticket.id) && engineType == ticket.engineType && issueTime.equals(ticket.issueTime) && slot.equals(ticket.slot);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, engineType, issueTime, slot);
        }
    }

    public static class ParkingException extends RuntimeException {
        public ParkingException(String cause) {
            super(cause);
        }
    }
}
