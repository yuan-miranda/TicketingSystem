package ticketing;

import java.io.*;
import java.util.ArrayList;

public class FileHandler {

    private static final String BOOKINGS_FILE = "bookings.txt";
    private static final String TRIPS_FILE = "trips.txt";

    // -------------------------------------------------------------------------
    // Bookings
    // -------------------------------------------------------------------------

    public static void saveBookings(ArrayList<Passenger> passengers) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(BOOKINGS_FILE))) {
            for (Passenger p : passengers) {
                // Column order: code|name|date|from|to|seat|class|type|fare|status
                writer.write(String.join("|",
                        p.getBookingCode(),
                        p.getPassengerName(),
                        p.getDate(),
                        p.getFrom(),
                        p.getTo(),
                        p.getSeatNumber(),
                        p.getSeatClass(),
                        p.getTicketType(),
                        String.format("%.2f", p.getPrice()),
                        p.getStatus()));
                writer.newLine();
            }
        }
    }

    public static ArrayList<Passenger> loadBookings() throws IOException {
        ArrayList<Passenger> list = new ArrayList<>();
        File file = new File(BOOKINGS_FILE);
        if (!file.exists())
            return list;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;

                String[] parts = line.split("\\|");
                Passenger p = parsePassengerLine(parts);
                if (p != null)
                    list.add(p);
            }
        }
        return list;
    }

    /*
     * Supports three historical line formats so that old bookings.txt files
     * continue to load correctly after format changes:
     *
     * 10 fields, starts with "BK-" → current format
     * code|name|date|from|to|seat|class|type|price|status
     *
     * 10 fields, no "BK-" prefix → legacy format
     * name|from|to|date|seat|class|type|price|code|status
     *
     * 11 fields → oldest format (had an extra ID column)
     * name|id|from|to|date|seat|class|type|price|code|status
     */
    private static Passenger parsePassengerLine(String[] parts) {
        try {
            Passenger p;
            if (parts.length == 10 && parts[0].startsWith("BK-")) {
                p = new Passenger(parts[1], parts[3], parts[4], parts[2],
                        parts[5], parts[6], parts[7],
                        Double.parseDouble(parts[8]), parts[0]);
                p.setStatus(parts[9]);
            } else if (parts.length == 10) {
                p = new Passenger(parts[0], parts[1], parts[2], parts[3],
                        parts[4], parts[5], parts[6],
                        Double.parseDouble(parts[7]), parts[8]);
                p.setStatus(parts[9]);
            } else if (parts.length == 11) {
                p = new Passenger(parts[0], parts[2], parts[3], parts[4],
                        parts[5], parts[6], parts[7],
                        Double.parseDouble(parts[8]), parts[9]);
                p.setStatus(parts[10]);
            } else {
                return null;
            }
            return p;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Trips
    // -------------------------------------------------------------------------

    public static void saveTrips(Trip[] trips) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TRIPS_FILE))) {
            for (Trip t : trips) {
                writer.write(String.join("|",
                        t.getTripCode(),
                        t.getOrigin(),
                        t.getDestination(),
                        t.getDepartureTime(),
                        t.getArrivalTime(),
                        String.valueOf(t.getTotalSeats()),
                        String.valueOf(t.getAvailableSeats()),
                        String.valueOf(t.getEconomyPrice()),
                        String.valueOf(t.getBusinessPrice())));
                writer.newLine();
            }
        }
    }

    public static ArrayList<Trip> loadTrips() throws IOException {
        ArrayList<Trip> list = new ArrayList<>();
        File file = new File(TRIPS_FILE);
        if (!file.exists())
            return list;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;

                String[] parts = line.split("\\|");
                if (parts.length != 9)
                    continue;

                try {
                    Trip trip = new Trip(
                            parts[0], parts[1], parts[2], parts[3], parts[4],
                            Integer.parseInt(parts[5]),
                            Double.parseDouble(parts[7]),
                            Double.parseDouble(parts[8]));
                    trip.setAvailableSeats(Integer.parseInt(parts[6]));
                    list.add(trip);
                } catch (NumberFormatException ignored) {
                    // Skip malformed rows.
                }
            }
        }
        return list;
    }

    // -------------------------------------------------------------------------
    // Receipts
    // -------------------------------------------------------------------------

    public static void saveReceipt(Passenger p) throws IOException {
        String filename = "Receipt_" + p.getBookingCode() + ".txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("==============================================");
            writer.newLine();
            writer.write("       PHILIPPINE EXPRESS BUS LINES          ");
            writer.newLine();
            writer.write("            BOOKING RECEIPT                  ");
            writer.newLine();
            writer.write("==============================================");
            writer.newLine();
            writer.write("Booking Code  : " + p.getBookingCode());
            writer.newLine();
            writer.write("Passenger     : " + p.getPassengerName());
            writer.newLine();
            writer.write("----------------------------------------------");
            writer.newLine();
            writer.write("From          : " + p.getFrom());
            writer.newLine();
            writer.write("To            : " + p.getTo());
            writer.newLine();
            writer.write("Date          : " + p.getDate());
            writer.newLine();
            writer.write("Seat          : " + p.getSeatNumber());
            writer.newLine();
            writer.write("Class         : " + p.getSeatClass());
            writer.newLine();
            writer.write("Ticket Type   : " + p.getTicketType());
            writer.newLine();
            writer.write("----------------------------------------------");
            writer.newLine();
            writer.write(String.format("TOTAL FARE     : ₱%.2f", p.getPrice()));
            writer.newLine();
            writer.write("Status        : " + p.getStatus());
            writer.newLine();
            writer.write("==============================================");
            writer.newLine();
            writer.write("  Thank you for choosing Philippine Express!  ");
            writer.newLine();
            writer.write("==============================================");
            writer.newLine();
        }
    }
}