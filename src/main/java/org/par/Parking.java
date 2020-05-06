package org.par;

import javax.lang.model.util.Types;
import java.time.LocalDateTime;
import java.util.*;

import static java.util.stream.Collectors.toMap;
import static org.par.Parking.EngineType.*;

public class Parking {

    private final Map<EngineType, List<String>> slots;
    private final Set<Ticket> tickets = new HashSet<>();

    public Parking(Map<EngineType, List<String>> slots) {
        this.slots = slots.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> new ArrayList<>(e.getValue())));
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

    private Ticket createTicket(EngineType engineType, String slot) {
        var ticket = new Ticket(engineType, slot);
        tickets.add(ticket);
        return ticket;
    }

    private Optional<String> assign(EngineType t) {
        var matchingSlots = slots.putIfAbsent(t, new ArrayList<>());
        if (matchingSlots != null && !matchingSlots.isEmpty())
            return Optional.of(matchingSlots.remove(0));
        else
            return Optional.empty();
    }

    public boolean leave(Ticket ticket) {
        return tickets.remove(ticket);
    }

    public enum EngineType {
        GAS, ELECTRIC, HI_ELECTRIC
    }

    public static final class Ticket {
        public final UUID id;
        public final EngineType engineType;
        public final LocalDateTime issueTime;
        public final String slotId;

        public Ticket(EngineType engineType, String slotId) {
            this.slotId = slotId;
            this.id = UUID.randomUUID();
            this.engineType = engineType;
            this.issueTime = LocalDateTime.now();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Ticket ticket = (Ticket) o;
            return id.equals(ticket.id) &&
                    engineType == ticket.engineType &&
                    issueTime.equals(ticket.issueTime) &&
                    slotId.equals(ticket.slotId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, engineType, issueTime, slotId);
        }
    }

    //receipt
}
