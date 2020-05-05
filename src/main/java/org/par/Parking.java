package org.par;

import java.time.LocalDateTime;
import java.util.*;

import static java.util.stream.Collectors.toMap;
import static org.par.Parking.EngineType.*;

public class Parking {

    private final Map<EngineType, List<String>> slots;

    public Parking(Map<EngineType, List<String>> slots) {
        this.slots = slots.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> new ArrayList<>(e.getValue())));
    }

    public Optional<Ticket> enter(EngineType engineType) {
        switch (engineType) {
            case HI_ELECTRIC:
            case ELECTRIC:
                return assign(engineType)
                        .map(s -> new Ticket(engineType, s));
            case GAS:
            default:
                return assign(GAS)
                        .or(() -> assign(ELECTRIC))
                        .or(() -> assign(HI_ELECTRIC))
                        .map(s -> new Ticket(GAS, s));
        }
    }

    private Optional<String> assign(EngineType t) {
        var matchingSlots = slots.putIfAbsent(t, new ArrayList<>());
        if (matchingSlots != null && !matchingSlots.isEmpty())
            return Optional.of(matchingSlots.remove(0));
        else
            return Optional.empty();
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
    }

    //receipt
}
