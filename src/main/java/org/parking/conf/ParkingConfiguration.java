package org.parking.conf;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static org.parking.service.Parking.EngineType;
import static org.parking.service.Parking.EngineType.*;
import static org.parking.service.Parking.Ticket;

public interface ParkingConfiguration {
    default Map<EngineType, List<String>> slots() {
        return Map.of(GAS, List.of("A"), ELECTRIC, List.of("B"), HI_ELECTRIC, List.of("C"));
    }

    default Double pricing(Ticket t) {
        switch (t.engineType) {
            case GAS:
                return 20.0;
            case ELECTRIC:
                return 10.0;
            case HI_ELECTRIC:
            default:
                return 5.0;
        }
    }
    @Profile("dev")
    @Component
    class Default implements ParkingConfiguration {
    }
}
