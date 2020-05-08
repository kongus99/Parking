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

/**
 * This class represents a simple paid parking functionality. It allows for entering the parking by the means
 * of issuing parking tickets - {@link Ticket Ticket} - that possess unique ids for a provided engine type.
 * They also contain the basic assigned slot info -  {@link Slot Slot} - that also contains info about the slot location (id) and the original slot engine type.
 * <p>
 * Those tickets can be then retrieved at any time using their id, and their current price can be evaluated
 * using the parking pricing strategy. The parking has limited number of slots, configured in the constructor and will not issue a new ticket
 * if it has no slot matching the provided engine type.
 * </p>
 * <p>
 * When leaving we need to provide enough compensation to leave. If it is insufficient or provided for the wrong ticket,
 * exception will be thrown.
 * </p>
 * <p>
 * The implementation also allows for retrieval of list of all current outstanding tickets.
 * </p>
 *
 * @author lucas dudek
 */
@Service
public class Parking {

    private final Function<Ticket, Double> pricing;
    private final Map<EngineType, List<Slot>> slots;
    private final Map<UUID, Ticket> tickets = new HashMap<>();

    /**
     * Spring default constructor.
     * The current implementation takes the {@link ParkingConfiguration.Default Default} class as given implementation,
     * but it can be replaced by switching active/default Spring profile and providing different implementation
     *
     * @param config the injected config
     */
    @Autowired
    public Parking(ParkingConfiguration config) {
        this(config::pricing, config.slots());
    }

    /**
     * Secondary constructor.
     * It allows to creation of the class directly, without extra classes. It requires the pricing function as well as
     * the slots configuration. The slots configuration consists of map of engine types to slot names, for the purpose
     * of directing parking user to the right slot. The names should be unique within a single engine type as well as
     * overall, though it is not required and not enforced currently.
     *
     * @param pricing the pricing function that calculates the current ticket price.
     * @param slots   the slots configuration map.
     */
    public Parking(Function<Ticket, Double> pricing, Map<EngineType, List<String>> slots) {
        this.pricing = pricing;
        this.slots = slots.entrySet()
                .stream()
                .collect(toMap(Map.Entry::getKey, e -> e.getValue()
                        .stream()
                        .map(id -> new Slot(id, e.getKey()))
                        .collect(Collectors.toList())));
    }

    /**
     * This method registers entry to the parking.
     * Assigns parking slot based on available slots, according to given compatibility rules. It tries to assign slot in
     * the order defined in the specs - GAS, ELECTRIC and HI_ELECTRIC. For the first one ELECTRIC and HI_ELECTRIC are also
     * acceptable, the others are slot-specific. The ticket is stored then on the list for further identification/retrieval.
     * The assignment is synchronized to allow for multiple entry points in the parking.
     *
     * @param engineType the type of engine of the car that enters the parking
     * @return {@code Optional.empty()} if no compatible slots were found or <code>Optional.of({@link Parking.Ticket Ticket})</code> if some was found
     */
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

    /**
     * This method allows the parking user to attempt leaving.
     * It requires valid ticket id and sufficient payment. If id is incorrect or not present among outstanding tickets
     * the appropriate exception will be thrown. Also, appropriate exception will be thrown when insufficient payment is provided.
     *
     * @param id      valid ticket id
     * @param payment sufficient payment for the supplied ticket
     * @return the time of parking departure/ticket invalidation
     * @throws ParkingException when the ticket id is not present among outstanding tickets or payment is insufficient
     */
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

    /**
     * Allows parking user to check how much he owes for the given ticket.
     *
     * @param id ticket id
     * @return the outstanding sum or zero if ticket is not awaiting payment
     */
    public double checkOwed(UUID id) {
        return getTicket(id).map(pricing).orElse(0.0);
    }

    /**
     * Retrieves ticket data for given id
     *
     * @param id ticket id
     * @return {@code Optional.empty()} if no ticket was found or <code>Optional.of({@link Parking.Ticket Ticket})</code> if some was found
     */
    public Optional<Ticket> getTicket(UUID id) {
        return Optional.ofNullable(tickets.get(id));
    }

    /**
     * Lists currently outstanding tickets, sorted by issue time
     *
     * @return list of outstanding tickets
     */
    public List<Ticket> getTickets() {
        return tickets.values()
                .stream()
                .sorted(Comparator.comparing(ticket -> ticket.issueTime))
                .collect(Collectors.toList());
    }

    /**
     * This enum represents three categories of engines:
     * gasoline(GAS),
     * 20kw power supply electric(ELECTRIC),
     * 50kw power supply electric(HI_ELECTRIC)
     */
    public enum EngineType {
        GAS, ELECTRIC, HI_ELECTRIC
    }

    /**
     * This data class represents the parking slot information. It includes the parking-specific description for the
     * parking slot(id) and the type of engine it can serve by default(engineType)
     */
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


    /**
     * This represents ticket information. It includes unique id, assigned engine type and time it was issued
     * (have significance in pricing calculation), as well as slot information
     */
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

    /**
     * Exception used when parking user attempts to leave the parking
     */
    public static class ParkingException extends RuntimeException {
        public ParkingException(String cause) {
            super(cause);
        }
    }
}
