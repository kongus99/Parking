package org.parking.conf;

import org.parking.service.Parking;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static org.parking.service.Parking.EngineType;
import static org.parking.service.Parking.EngineType.*;
import static org.parking.service.Parking.Ticket;

/**
 * This is a convenience interface for creating {@link Parking Parking} instance. By default,
 * when running the Spring app that accompanies the {@link Parking Parking} class, the {@link Default Default}
 * class is used. However, by changing the Spring active/default profile it should be easy to replace this configuration with another.
 *
 * @author lucas dudek
 */
public interface ParkingConfiguration {
    /**
     * Holds map of slots used in {@link Parking Parking} constructor
     *
     * @return map of slots
     * @see Parking#Parking(ParkingConfiguration) Parking constructors for more details
     */
    default Map<EngineType, List<String>> slots() {
        return Map.of(GAS, List.of("A"), ELECTRIC, List.of("B"), HI_ELECTRIC, List.of("C"));
    }

    /**
     * Holds pricing strategy used in {@link Parking Parking} constructor
     *
     * @param t ticket used for establishing the price for
     * @return the current payment for that ticket
     * @see Parking#Parking(ParkingConfiguration) Parking constructors for more details
     */
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

    /**
     * Default configuration used as an injection bean for Spring
     */
    @Profile("dev")
    @Component
    class Default implements ParkingConfiguration {
    }
}
