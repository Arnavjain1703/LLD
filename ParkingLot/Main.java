import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import enums.PaymentMethod;
import enums.vechicle;
import models.*;
import models.FeeCalculator.HourlyFeeStrategy;
import models.PaymentProcessor.CashPaymentProcessor;
import models.PaymentProcessor.CardPaymentProcessor;
import models.SpotAssignmentStrategy.NearestSpotStrategy;
import models.SpotAssignmentStrategy.FarthestSpotStrategy;
import models.parkingSpot.*;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // 1. Create Parking Lot (Singleton)
        Address address = new Address("MG Road", "Near Mall", "Bangalore", "560001");
        ParkingLot lot = ParkingLot.getInstance("LOT-1", "City Mall Parking", address);

        // 2. Add floors with spots
        ParkingFloor floor1 = new ParkingFloor(1);
        floor1.addSpot(new CompactSpot());
        floor1.addSpot(new CompactSpot());
        floor1.addSpot(new CompactSpot());
        floor1.addSpot(new LargeSpot());
        floor1.addSpot(new EvSpot());
        floor1.addSpot(new HandiCappedSpot());

        ParkingFloor floor2 = new ParkingFloor(2);
        floor2.addSpot(new CompactSpot());
        floor2.addSpot(new CompactSpot());
        floor2.addSpot(new LargeSpot());
        floor2.addSpot(new LargeSpot());

        lot.addFloor(floor1);
        lot.addFloor(floor2);

        // 3. Setup multiple entry and exit gates (simulating real parking lot)
        EntryGate entryGate1 = new EntryGate("ENTRY-1", new NearestSpotStrategy());
        EntryGate entryGate2 = new EntryGate("ENTRY-2", new FarthestSpotStrategy());
        ExitGate exitGate1 = new ExitGate("EXIT-1", new HourlyFeeStrategy());
        ExitGate exitGate2 = new ExitGate("EXIT-2", new HourlyFeeStrategy());
        lot.addEntryGate(entryGate1);
        lot.addEntryGate(entryGate2);
        lot.addExitGate(exitGate1);
        lot.addExitGate(exitGate2);

        System.out.println("\n========== MULTI-THREADED PARKING SIMULATION ==========\n");

        // 4. Simulate multiple vehicles entering concurrently from different gates
        int numVehicles = 6;
        CountDownLatch entryLatch = new CountDownLatch(numVehicles);
        Ticket[] tickets = new Ticket[numVehicles];
        ExecutorService executor = Executors.newFixedThreadPool(4);

        Vehical[] vehicles = {
            new Vehical(1001, vechicle.CAR),
            new Vehical(1002, vechicle.CAR),
            new Vehical(1003, vechicle.CAR),
            new Vehical(1004, vechicle.TRUCK),
            new Vehical(1005, vechicle.EVCAR),
            new Vehical(1006, vechicle.CAR),
        };

        System.out.println("--- Phase 1: " + numVehicles + " vehicles entering concurrently ---\n");

        for (int i = 0; i < numVehicles; i++) {
            final int idx = i;
            EntryGate gate = (idx % 2 == 0) ? entryGate1 : entryGate2;
            executor.submit(() -> {
                try {
                    tickets[idx] = gate.processEntry(vehicles[idx], lot.getFloors());
                    System.out.println("[Thread " + Thread.currentThread().getName() + "] Vehicle "
                            + vehicles[idx].getVehicalNumber() + " parked at spot "
                            + tickets[idx].getSpot().getSpotId());
                } catch (RuntimeException e) {
                    System.out.println("[Thread " + Thread.currentThread().getName() + "] Vehicle "
                            + vehicles[idx].getVehicalNumber() + " FAILED: " + e.getMessage());
                } finally {
                    entryLatch.countDown();
                }
            });
        }

        entryLatch.await();
        System.out.println("\n--- All entries processed ---\n");

        // 5. Display availability after all entries
        DisplayBoard board1 = new DisplayBoard("DB-1", floor1);
        DisplayBoard board2 = new DisplayBoard("DB-2", floor2);
        board1.showAvailability();
        board2.showAvailability();

        // 6. Simulate some vehicles exiting concurrently
        System.out.println("\n--- Phase 2: Vehicles exiting concurrently ---\n");

        int numExits = 3;
        CountDownLatch exitLatch = new CountDownLatch(numExits);

        for (int i = 0; i < numExits; i++) {
            final int idx = i;
            if (tickets[idx] == null) {
                exitLatch.countDown();
                continue;
            }
            ExitGate gate = (idx % 2 == 0) ? exitGate1 : exitGate2;
            executor.submit(() -> {
                try {
                    Payment payment = gate.processExit(tickets[idx], PaymentMethod.CASH,
                            new CashPaymentProcessor());
                    System.out.println("[Thread " + Thread.currentThread().getName() + "] Vehicle "
                            + tickets[idx].getVehicle().getVehicalNumber()
                            + " exited | Payment: " + payment.getStatus());
                } finally {
                    exitLatch.countDown();
                }
            });
        }

        exitLatch.await();
        System.out.println("\n--- All exits processed ---\n");

        // 7. Display availability after exits (spots should be freed)
        board1.showAvailability();
        board2.showAvailability();

        // 8. Demonstrate re-entry into freed spots
        System.out.println("\n--- Phase 3: New vehicle enters freed spot ---\n");
        Vehical newCar = new Vehical(2001, vechicle.CAR);
        Ticket newTicket = entryGate1.processEntry(newCar, lot.getFloors());
        System.out.println("New vehicle " + newCar.getVehicalNumber()
                + " parked at spot " + newTicket.getSpot().getSpotId());

        executor.shutdown();
        System.out.println("\n========== SIMULATION COMPLETE ==========");
    }
}
