package ticketing;

import java.util.ArrayList;
import java.util.Random;

public class BookingManager {

    // Fixed list of predefined routes; ArrayList holds dynamic bookings.
    private Trip[] availableTrips;
    private ArrayList<Passenger> passengerList;

    private final Random random;

    public BookingManager() {
        passengerList = new ArrayList<>();
        random = new Random();
        initializeTrips();
    }

    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------

    private void initializeTrips() {
        availableTrips = new Trip[] {
                new Trip("BUS-001", "Manila", "Baguio", "06:00 AM", "12:00 PM", 50, 450.00, 850.00),
                new Trip("BUS-002", "Manila", "Cebu", "07:00 AM", "06:00 PM", 50, 1200.00, 2100.00),
                new Trip("BUS-003", "Manila", "Davao", "08:00 AM", "09:00 PM", 45, 1500.00, 2600.00),
                new Trip("BUS-004", "Manila", "Iloilo", "09:00 AM", "07:00 PM", 45, 1100.00, 1950.00),
                new Trip("BUS-005", "Manila", "Legazpi", "10:00 AM", "04:00 PM", 40, 650.00, 1150.00),
                new Trip("BUS-006", "Baguio", "Manila", "12:00 PM", "06:00 PM", 50, 450.00, 850.00),
                new Trip("BUS-007", "Cebu", "Manila", "07:00 AM", "06:00 PM", 50, 1200.00, 2100.00),
                new Trip("BUS-008", "Davao", "Manila", "05:00 AM", "06:00 PM", 45, 1500.00, 2600.00),
                new Trip("BUS-009", "Manila", "Zamboanga", "08:30 AM", "10:00 PM", 40, 1700.00, 2900.00),
                new Trip("BUS-010", "Manila", "Tacloban", "07:30 AM", "05:30 PM", 40, 950.00, 1750.00),
        };
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public ArrayList<Trip> searchTrip(String from, String to) {
        ArrayList<Trip> results = new ArrayList<>();
        for (Trip trip : availableTrips) {
            boolean matchFrom = from.isEmpty() || trip.getOrigin().equalsIgnoreCase(from);
            boolean matchTo = to.isEmpty() || trip.getDestination().equalsIgnoreCase(to);
            if (matchFrom && matchTo) {
                results.add(trip);
            }
        }
        return results;
    }

    public String bookSeat(String name, String tripCode,
            String seatClass, String ticketType, String date) {
        Trip trip = findTripByCode(tripCode);
        if (trip == null || !trip.hasAvailableSeat())
            return null;

        String seatNumber = assignSeatNumber(trip, seatClass, date);
        if (seatNumber == null)
            return null;

        double basePrice = trip.getPriceForClass(seatClass);
        double finalPrice = "Round Trip".equals(ticketType) ? basePrice * 2 : basePrice;
        String bookingCode = generateBookingCode();

        Passenger passenger = new Passenger(
                name,
                trip.getOrigin(), trip.getDestination(),
                date, seatNumber, seatClass, ticketType,
                finalPrice, bookingCode);

        passengerList.add(passenger);
        recalculateAvailableSeats();
        return bookingCode;
    }

    /*
     * Validates and applies a status change to the given booking.
     * Returns false if the booking code is not found or the status value is not
     * one of: Confirmed, Completed, Cancelled.
     */
    public boolean updatePassengerStatus(String bookingCode, String newStatus) {
        Passenger passenger = findPassenger(bookingCode);
        if (passenger == null || newStatus == null)
            return false;

        String status = newStatus.trim();
        if (!status.equalsIgnoreCase("Confirmed")
                && !status.equalsIgnoreCase("Completed")
                && !status.equalsIgnoreCase("Cancelled")) {
            return false;
        }

        if (status.equalsIgnoreCase("Completed")) {
            passenger.setStatus("Completed");
        } else if (status.equalsIgnoreCase("Cancelled")) {
            passenger.setStatus("Cancelled");
        } else {
            passenger.setStatus("Confirmed");
        }

        recalculateAvailableSeats();
        return true;
    }

    public Passenger findPassenger(String bookingCode) {
        for (Passenger p : passengerList) {
            if (p.getBookingCode().equalsIgnoreCase(bookingCode)) {
                return p;
            }
        }
        return null;
    }

    public Trip findTripByCode(String code) {
        for (Trip t : availableTrips) {
            if (t.getTripCode().equalsIgnoreCase(code)) {
                return t;
            }
        }
        return null;
    }

    public ArrayList<Passenger> getAllPassengers() {
        return passengerList;
    }

    public Trip[] getAllTrips() {
        return availableTrips;
    }

    public void setPassengerList(ArrayList<Passenger> list) {
        this.passengerList = (list == null) ? new ArrayList<>() : list;
        recalculateAvailableSeats();
    }

    public void setTrips(Trip[] trips) {
        if (trips != null && trips.length > 0) {
            this.availableTrips = trips;
            recalculateAvailableSeats();
        }
    }

    // Exposed so the GUI can trigger a seat count refresh after inline edits.
    public void recomputeTripSeatAvailability() {
        recalculateAvailableSeats();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String generateBookingCode() {
        return "BK-" + (100000 + random.nextInt(900000));
    }

    /*
     * Finds the first unoccupied seat on a given trip for a specific date.
     * Seat numbers are prefixed with "B" (Business) or "E" (Economy).
     * Returns null when all seats on that trip/date combination are taken.
     */
    private String assignSeatNumber(Trip trip, String seatClass, String date) {
        boolean[] occupied = new boolean[trip.getTotalSeats() + 1];
        String prefix = "Business".equalsIgnoreCase(seatClass) ? "B" : "E";

        for (Passenger p : passengerList) {
            if (!"Confirmed".equalsIgnoreCase(p.getStatus()))
                continue;
            if (!trip.getOrigin().equalsIgnoreCase(p.getFrom())
                    || !trip.getDestination().equalsIgnoreCase(p.getTo())
                    || !date.equalsIgnoreCase(p.getDate()))
                continue;

            int seatNo = parseSeatNumber(p.getSeatNumber());
            if (seatNo >= 1 && seatNo <= trip.getTotalSeats()) {
                occupied[seatNo] = true;
            }
        }

        for (int i = 1; i <= trip.getTotalSeats(); i++) {
            if (!occupied[i]) {
                return prefix + String.format("%02d", i);
            }
        }
        return null;
    }

    private void recalculateAvailableSeats() {
        for (Trip trip : availableTrips) {
            int confirmedCount = 0;
            for (Passenger p : passengerList) {
                if ("Confirmed".equalsIgnoreCase(p.getStatus())
                        && trip.getOrigin().equalsIgnoreCase(p.getFrom())
                        && trip.getDestination().equalsIgnoreCase(p.getTo())) {
                    confirmedCount++;
                }
            }
            trip.setAvailableSeats(Math.max(0, trip.getTotalSeats() - confirmedCount));
        }
    }

    private int parseSeatNumber(String seatNumber) {
        if (seatNumber == null)
            return -1;
        String digits = seatNumber.replaceAll("\\D+", "");
        if (digits.isEmpty())
            return -1;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }
}