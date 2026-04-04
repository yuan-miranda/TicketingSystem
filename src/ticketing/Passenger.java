package ticketing;

import java.io.Serializable;

public class Passenger implements Serializable {

    private static final long serialVersionUID = 1L;

    private String passengerName;
    private String from;
    private String to;
    private String date;
    private String seatNumber;
    private String seatClass; // "Economy" or "Business"
    private String ticketType; // "One-Way" or "Round Trip"
    private double price;
    private String bookingCode;
    private String status; // "Confirmed", "Completed", or "Cancelled"

    public Passenger(String passengerName, String from, String to,
            String date, String seatNumber, String seatClass,
            String ticketType, double price, String bookingCode) {
        this.passengerName = passengerName;
        this.from = from;
        this.to = to;
        this.date = date;
        this.seatNumber = seatNumber;
        this.seatClass = seatClass;
        this.ticketType = ticketType;
        this.price = price;
        this.bookingCode = bookingCode;
        this.status = "Confirmed";
    }

    public String getPassengerName() {
        return passengerName;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getDate() {
        return date;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public String getSeatClass() {
        return seatClass;
    }

    public String getTicketType() {
        return ticketType;
    }

    public double getPrice() {
        return price;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public String getStatus() {
        return status;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public void setSeatClass(String seatClass) {
        this.seatClass = seatClass;
    }

    public void setTicketType(String ticketType) {
        this.ticketType = ticketType;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format("%s|%s|%s|%s|%s|%s|%s|%.2f|%s|%s",
                passengerName, from, to, date,
                seatNumber, seatClass, ticketType, price, bookingCode, status);
    }
}