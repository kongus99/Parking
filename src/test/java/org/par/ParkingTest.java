package org.par;

import javafx.util.Pair;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.par.Parking.EngineType;
import static org.par.Parking.EngineType.*;
import static org.par.Parking.Ticket;

public class ParkingTest {

    private void assertRightSlotAssigned(Parking parking, Ticket ticket, EngineType engineType) {
        var issued = parking.enter(engineType);
        var expected = Optional.ofNullable(ticket);
        assertEquals(expected.map(t -> t.engineType), issued.map(t -> t.engineType));
        assertEquals(expected.map(t -> t.slotId), issued.map(t -> t.slotId));
        issued.ifPresent(i -> {
            assertTrue(i.issueTime.isBefore(LocalDateTime.now().plus(1, ChronoUnit.SECONDS)),
                    "Non valid timestamp was issued");
            assertNotEquals(expected.map(t -> t.id), issued.map(t -> t.id));
        });
    }

    @Test
    void whenEnteringParkingYouGetTicketForRightCarType() {
        var parking = new Parking(Map.of(GAS, List.of("A"), ELECTRIC, List.of("B"), HI_ELECTRIC, List.of("C")));
        assertRightSlotAssigned(parking, new Ticket(GAS, "A"), GAS);
        assertRightSlotAssigned(parking, new Ticket(ELECTRIC, "B"), ELECTRIC);
        assertRightSlotAssigned(parking, new Ticket(HI_ELECTRIC, "C"), HI_ELECTRIC);
        assertRightSlotAssigned(parking, null, ELECTRIC);
        assertRightSlotAssigned(parking, null, HI_ELECTRIC);
        assertRightSlotAssigned(parking, null, GAS);
    }

    private String generateName(EngineType engineType, int i) {
        switch (engineType) {
            case ELECTRIC:
                return "E" + i;
            case HI_ELECTRIC:
                return "H" + i;
            case GAS:
            default:
                return "G" + i;
        }
    }

    private void assertParkingCapacity(Parking parking, EngineType engineType, int carsNumber) {
        IntStream.range(0, carsNumber).forEach(i ->
                assertRightSlotAssigned(parking, new Ticket(engineType, generateName(engineType, i)), engineType)
        );
        assertRightSlotAssigned(parking, null, engineType);
    }

    @TestFactory
    Stream<DynamicTest> parkingCanHaveNumberOfSlotsConfigured() {
        var seed = System.currentTimeMillis();
        var generator = new Random(seed).ints(1, 100).iterator();
        var tests = IntStream.range(0, 20)
                .mapToObj(i -> Map.of(GAS, generator.next(), ELECTRIC, generator.next(), HI_ELECTRIC, generator.next()))
                .iterator();
        return DynamicTest.stream(tests, (input) -> "seed:" + seed + " input:" + input, this::assertParkingRespectsItsSize).parallel();
    }

    private void assertParkingRespectsItsSize(Map<EngineType, Integer> indexedSizes) {
        BiFunction<EngineType, Integer, List<String>> namesGenerator =
                (eng, size) -> IntStream.range(0, size).mapToObj(i -> generateName(eng, i)).collect(toList());
        var slots = indexedSizes.entrySet()
                .stream()
                .map(e -> new Pair<>(e.getKey(), namesGenerator.apply(e.getKey(), e.getValue())))
                .collect(toMap(Pair::getKey, Pair::getValue));
        var parking = new Parking(slots);
        indexedSizes.keySet().stream().sorted(Comparator.comparingInt(EngineType::ordinal).reversed())
                .forEach(t -> assertParkingCapacity(parking, t, indexedSizes.get(t)));
    }


}
