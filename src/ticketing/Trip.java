package ticketing;

public class Trip {

    private String tripCode;
    private String origin;
    private String destination;
    private String departureTime;
    private String arrivalTime;
    private int totalSeats;
    private int availableSeats;
    private double economyPrice;
    private double businessPrice;

    public Trip(String tripCode, String origin, String destination,
            String departureTime, String arrivalTime,
            int totalSeats, double economyPrice, double businessPrice) {
        this.tripCode = tripCode;
        this.origin = origin;
        this.destination = destination;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.totalSeats = totalSeats;
        this.availableSeats = totalSeats;
        this.economyPrice = economyPrice;
        this.businessPrice = businessPrice;
    }

    public String getTripCode() {
        return tripCode;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public double getEconomyPrice() {
        return economyPrice;
    }

    public double getBusinessPrice() {
        return businessPrice;
    }

    public void setAvailableSeats(int seats) {
        this.availableSeats = seats;
    }

    public void setEconomyPrice(double economyPrice) {
        this.economyPrice = economyPrice;
    }

    public void setBusinessPrice(double businessPrice) {
        this.businessPrice = businessPrice;
    }

    public boolean hasAvailableSeat() {
        return availableSeats > 0;
    }

    public double getPriceForClass(String seatClass) {
        return "Business".equals(seatClass) ? businessPrice : economyPrice;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s → %s | Dep: %s | Arr: %s | Seats: %d | Eco: ₱%.2f | Bus: ₱%.2f",
                tripCode, origin, destination, departureTime, arrivalTime,
                availableSeats, economyPrice, businessPrice);
    }
}