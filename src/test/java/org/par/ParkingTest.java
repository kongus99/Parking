package org.par;

import javafx.util.Pair;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.par.Parking.*;
import static org.par.Parking.EngineType.*;
import static org.par.ParkingTest.ParkingAssertions.assertEnterAndLeave;
import static org.par.ParkingTest.ParkingAssertions.assertRightSlotAssigned;

public class ParkingTest {
    private static String generateName(EngineType engineType, int i) {
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

    @Test
    void whenEnteringParkingYouGetTicketForRightCarType() {
        var parking = new Parking(Map.of(GAS, List.of("A"), ELECTRIC, List.of("B"), HI_ELECTRIC, List.of("C")));
        assertRightSlotAssigned(parking, new Ticket(GAS, new Slot("A", GAS)), GAS);
        assertRightSlotAssigned(parking, new Ticket(ELECTRIC, new Slot("B", ELECTRIC)), ELECTRIC);
        assertRightSlotAssigned(parking, new Ticket(HI_ELECTRIC, new Slot("C", HI_ELECTRIC)), HI_ELECTRIC);
        assertRightSlotAssigned(parking, null, ELECTRIC);
        assertRightSlotAssigned(parking, null, HI_ELECTRIC);
        assertRightSlotAssigned(parking, null, GAS);
    }

    @Test
    void differentSlotTypesCanBeAssignedForGasEngines() {
        var parking = new Parking(Map.of(GAS, List.of("A"), ELECTRIC, List.of("B"), HI_ELECTRIC, List.of("C")));
        assertRightSlotAssigned(parking, new Ticket(GAS, new Slot("A", GAS)), GAS);
        assertRightSlotAssigned(parking, new Ticket(GAS, new Slot("B", ELECTRIC)), GAS);
        assertRightSlotAssigned(parking, new Ticket(GAS, new Slot("C", HI_ELECTRIC)), GAS);
        assertRightSlotAssigned(parking, null, GAS);
    }

    @TestFactory
    Stream<DynamicTest> parkingCanHaveNumberOfSlotsConfigured() {
        var seed = System.currentTimeMillis();
        var generator = new Random(seed).ints(1, 100).iterator();
        var tests = IntStream.range(0, 20)
                .mapToObj(i -> Map.of(GAS, generator.next(), ELECTRIC, generator.next(), HI_ELECTRIC, generator.next()))
                .iterator();
        return DynamicTest.stream(tests, (input) -> "seed:" + seed + " input:" + input, ParkingAssertions::assertParkingRespectsItsSize).parallel();
    }

    @ParameterizedTest
    @CsvSource({"ELECTRIC", "GAS", "ELECTRIC", "GAS", "HI_ELECTRIC"})
    void parkingSlotsCanBeReused(EngineType engineType) {
        var parking = new Parking(Map.of(engineType, List.of("A")));
        assertFalse(parking.leave(new Ticket(engineType, new Slot("A", engineType))), "Spoofed ticket should not work.");
        assertEnterAndLeave(parking, engineType);
        assertEnterAndLeave(parking, engineType);
    }

    public static class ParkingAssertions {

        static void assertEnterAndLeave(Parking parking, EngineType engineType) {
            parking.enter(engineType).ifPresentOrElse(t -> {
                assertTrue(parking.leave(t), "Issued ticket should work the first time.");
                assertFalse(parking.leave(t), "Issued ticket should not work the second time.");
            }, () -> {
                throw new AssertionError("Ticket was not generated");
            });
        }

        static void assertRightSlotAssigned(Parking parking, Ticket ticket, EngineType engineType) {
            var issued = parking.enter(engineType);
            var expected = Optional.ofNullable(ticket);
            assertEquals(expected.map(t -> t.engineType), issued.map(t -> t.engineType));
            assertEquals(expected.map(t -> t.slot), issued.map(t -> t.slot));
            issued.ifPresent(i -> {
                assertTrue(i.issueTime.isBefore(LocalDateTime.now().plus(1, ChronoUnit.SECONDS)),
                        "Non valid timestamp was issued");
                assertNotEquals(expected.map(t -> t.id), issued.map(t -> t.id));
                assertNotEquals(expected, issued);
            });
        }

        static void assertParkingCapacity(Parking parking, EngineType engineType, int carsNumber) {
            IntStream.range(0, carsNumber).forEach(i ->
                    assertRightSlotAssigned(parking, new Ticket(engineType, new Slot(generateName(engineType, i), engineType)), engineType)
            );
            assertRightSlotAssigned(parking, null, engineType);
        }

        static void assertParkingRespectsItsSize(Map<EngineType, Integer> indexedSizes) {
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


}
