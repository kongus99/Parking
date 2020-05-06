package org.par;

import javafx.util.Pair;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.par.Parking.*;
import static org.par.Parking.EngineType.*;
import static org.par.ParkingTest.ParkingAssertions.*;

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
        var parking = new Parking(t -> 0f, Map.of(GAS, List.of("A"), ELECTRIC, List.of("B"), HI_ELECTRIC, List.of("C")));
        assertRightSlotAssigned(parking, new Ticket(GAS, new Slot("A", GAS)), GAS);
        assertRightSlotAssigned(parking, new Ticket(ELECTRIC, new Slot("B", ELECTRIC)), ELECTRIC);
        assertRightSlotAssigned(parking, new Ticket(HI_ELECTRIC, new Slot("C", HI_ELECTRIC)), HI_ELECTRIC);
        assertRightSlotAssigned(parking, null, ELECTRIC);
        assertRightSlotAssigned(parking, null, HI_ELECTRIC);
        assertRightSlotAssigned(parking, null, GAS);
    }

    @Test
    void differentSlotTypesCanBeAssignedForGasEngines() {
        var parking = new Parking(t -> 0f, Map.of(GAS, List.of("A"), ELECTRIC, List.of("B"), HI_ELECTRIC, List.of("C")));
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
        var parking = new Parking(t -> 0f, Map.of(engineType, List.of("A")));
        assertThrows(ParkingException.class, () -> parking.leave(new Ticket(engineType, new Slot("A", engineType)), 0),
                "Spoofed ticket should not work.");
        assertEnterAndLeave(parking, engineType);
        assertThrows(ParkingException.class, () -> parking.leave(new Ticket(engineType, new Slot("A", engineType)), 0),
                "Spoofed ticket should not work.");
        assertEnterAndLeave(parking, engineType);
        assertRightSlotAssigned(parking, new Ticket(engineType, new Slot("A", engineType)), engineType);
        assertRightSlotAssigned(parking, null, engineType);
    }

    @Test
    void youCanOnlyLeaveOnlyWhenYouPayEnough() {
        Function<Ticket, Float> pricing = t -> {
            switch (t.engineType) {
                case GAS:
                    return Duration.between(t.issueTime, LocalDateTime.now().plus(4, DAYS)).toHours() * 5f + 20f;
                case ELECTRIC:
                    return Duration.between(t.issueTime, LocalDateTime.now().plus(2, DAYS)).toHours() * 10f + 10f;
                case HI_ELECTRIC:
                default:
                    return Duration.between(t.issueTime, LocalDateTime.now().plus(1, DAYS)).toHours() * 15f + 5f;
            }
        };
        var parking = new Parking(pricing, Map.of(GAS, List.of("A"), ELECTRIC, List.of("B"), HI_ELECTRIC, List.of("C")));
        assertPays(parking, GAS, 500f, 499f, 501f);
        assertPays(parking, ELECTRIC, 490f, 480f, 490f);
        assertPays(parking, HI_ELECTRIC, 365f, 300f, 400f);
    }

    public static class ParkingAssertions {

        static void checkTicket(Parking parking, EngineType engineType, Consumer<Ticket> checks) {
            parking.enter(engineType).ifPresentOrElse(checks,
                    () -> {
                        throw new AssertionError("Ticket was not generated");
                    });
        }

        static void assertEnterAndLeave(Parking parking, EngineType engineType) {
            checkTicket(parking, engineType, t -> {
                assertEquals(0, parking.leave(t, 0));
                assertThrows(ParkingException.class, () -> parking.leave(t, 0), "Issued ticket should not work the second time.");
            });
        }

        static void assertPays(Parking parking, EngineType engineType, float exact, float insufficient, float sufficient) {
            checkTicket(parking, engineType, t -> {
                assertEquals(exact, parking.checkOwed(t));
                assertThrows(ParkingException.class, () -> parking.leave(t, insufficient), "Should not leave because of insufficient payment");
                assertEquals(sufficient - exact, parking.leave(t, sufficient));
                assertThrows(ParkingException.class, () -> parking.leave(t, sufficient), "Should be already paid");
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
            var parking = new Parking(t -> 0f, slots);
            indexedSizes.keySet().stream().sorted(Comparator.comparingInt(EngineType::ordinal).reversed())
                    .forEach(t -> assertParkingCapacity(parking, t, indexedSizes.get(t)));
        }
    }


}
