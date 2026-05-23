import enums.PaymentMethod;
import enums.vechicle;
import models.*;
import models.FeeCalculator.HourlyFeeStrategy;
import models.PaymentProcessor.CashPaymentProcessor;
import models.SpotAssignmentStrategy.NearestSpotStrategy;
import models.parkingSpot.*;

public class Main {
    public static void main(String[] args) {
        // 1. Create Parking Lot (Singleton)
        Address address = new Address("MG Road", "Near Mall", "Bangalore", "560001");
        ParkingLot lot = ParkingLot.getInstance("LOT-1", "City Mall Parking", address);

        // 2. Add floors with spots
        ParkingFloor floor1 = new ParkingFloor(1);
        floor1.addSpot(new CompactSpot());
        floor1.addSpot(new CompactSpot());
        floor1.addSpot(new LargeSpot());
        floor1.addSpot(new EvSpot());
        floor1.addSpot(new HandiCappedSpot());

        ParkingFloor floor2 = new ParkingFloor(2);
        floor2.addSpot(new LargeSpot());
        floor2.addSpot(new LargeSpot());
        floor2.addSpot(new CompactSpot());

        lot.addFloor(floor1);
        lot.addFloor(floor2);

        // 3. Setup gates
        EntryGate entryGate = new EntryGate("ENTRY-1", new NearestSpotStrategy());
        ExitGate exitGate = new ExitGate("EXIT-1", new HourlyFeeStrategy());
        lot.addEntryGate(entryGate);
        lot.addExitGate(exitGate);

        // 4. Display availability
        System.out.println("\n--- Before parking ---");
        DisplayBoard board = new DisplayBoard("DB-1", floor1);
        board.showAvailability();

        // 5. Vehicle enters
        Vehical car = new Vehical(1234, vechicle.CAR);
        Ticket ticket = entryGate.processEntry(car, lot.getFloors());
        System.out.println("Ticket issued: " + ticket.getTicketId()
                + " | Spot: " + ticket.getSpot().getSpotId());

        // 6. Display availability after parking
        System.out.println("\n--- After parking ---");
        board.showAvailability();

        // 7. Vehicle exits — simulate some time passing
        System.out.println("\n--- Vehicle exiting ---");
        Payment payment = exitGate.processExit(ticket, PaymentMethod.CASH, new CashPaymentProcessor());
        System.out.println("Payment: " + payment.getPaymentId()
                + " | Amount: " + payment.getAmount()
                + " | Status: " + payment.getStatus());

        // 8. Display availability after exit
        System.out.println("\n--- After exit ---");
        board.showAvailability();
    }
}
