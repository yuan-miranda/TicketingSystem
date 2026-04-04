package ticketing;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class TicketingSystemGUI extends JFrame {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    private static final Color BG_MAIN = new Color(244, 247, 249);
    private static final Color PANEL_BG = new Color(255, 255, 255);
    private static final Color PRIMARY_BLUE = new Color(30, 136, 229);
    private static final Color TEXT_DARK = new Color(33, 37, 41);
    private static final Color TEXT_MUTED = new Color(108, 117, 125);
    private static final Color SUCCESS = new Color(40, 167, 69);
    private static final Color TABLE_ALT = new Color(248, 249, 250);
    private static final Color BORDER_COLOR = new Color(200, 200, 200);

    private static final String BUS_LOGO_PATH = "assets/phxlogo.png";

    // Parallel arrays that map combo-box indices to trip codes.
    private static final String[] TRIP_CODES = {
            "BUS-001", "BUS-002", "BUS-003", "BUS-004", "BUS-005",
            "BUS-006", "BUS-007", "BUS-008", "BUS-009", "BUS-010"
    };
    private static final String[] TRIP_LABELS = {
            "BUS-001 (Manila - Baguio)",
            "BUS-002 (Manila - Cebu)",
            "BUS-003 (Manila - Davao)",
            "BUS-004 (Manila - Iloilo)",
            "BUS-005 (Manila - Legazpi)",
            "BUS-006 (Baguio - Manila)",
            "BUS-007 (Cebu - Manila)",
            "BUS-008 (Davao - Manila)",
            "BUS-009 (Manila - Zamboanga)",
            "BUS-010 (Manila - Tacloban)"
    };

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final BookingManager manager;
    private JTabbedPane tabbedPane;

    // Search Trips tab
    private JComboBox<String> fromCombo, toCombo;
    private JTable tripTable;
    private DefaultTableModel tripTableModel;
    private boolean isSyncingTripTable;

    // Book Ticket tab
    private JTextField nameField;
    private JComboBox<String> tripCodeCombo, classCombo, ticketTypeCombo;
    private JLabel priceLabel;

    // Passengers tab
    private JTable passengerTable;
    private DefaultTableModel passengerTableModel;
    private boolean isSyncingPassengerTable;
    private ArrayList<Passenger> filteredPassengerList;

    // Auto-refresh
    private long lastBookingsFileModified = -1L;
    private long lastTripsFileModified = -1L;
    private Timer autoRefreshTimer;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public TicketingSystemGUI() {
        manager = new BookingManager();
        loadDataOnStartup();

        setTitle("Philippine Express Bus Ticketing System");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1100, 750);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(BG_MAIN);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveAndExit();
            }
        });

        buildUI();
        syncFileTimestamps();
        startAutoFileRefreshWatcher();
        setVisible(true);
    }

    // -------------------------------------------------------------------------
    // UI construction
    // -------------------------------------------------------------------------

    private void buildUI() {
        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(BG_MAIN);
        tabbedPane.setForeground(TEXT_DARK);
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabbedPane.setFocusable(false);

        tabbedPane.addTab("Search Trips", buildSearchPanel());
        tabbedPane.addTab("Book Ticket", buildBookPanel());
        tabbedPane.addTab("Passengers", buildPassengersPanel());

        add(tabbedPane, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PANEL_BG);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        left.setBackground(PANEL_BG);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setBackground(PANEL_BG);
        titlePanel.add(makeLabel("PHILIPPINE EXPRESS", Font.BOLD, 22, PRIMARY_BLUE));
        titlePanel.add(makeLabel("Bus Ticketing System", Font.PLAIN, 13, TEXT_MUTED));

        left.add(createBusLogoLabel());
        left.add(titlePanel);

        JLabel timeLabel = makeLabel("", Font.BOLD, 14, TEXT_MUTED);
        timeLabel.setPreferredSize(new Dimension(300, 50));
        timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        timeLabel.setBorder(new javax.swing.border.EmptyBorder(0, 0, 0, 20));

        Timer clock = new Timer(1000,
                e -> timeLabel.setText(new SimpleDateFormat("EEE, MMM dd, yyyy | hh:mm:ss a").format(new Date())));
        timeLabel.setText(new SimpleDateFormat("EEE, MMM dd, yyyy | hh:mm:ss a").format(new Date()));
        clock.start();

        header.add(left, BorderLayout.WEST);
        header.add(timeLabel, BorderLayout.EAST);
        return header;
    }

    private JLabel createBusLogoLabel() {
        java.net.URL logoUrl = getClass().getClassLoader().getResource(BUS_LOGO_PATH);
        if (logoUrl != null) {
            return new JLabel(scaleIconToFit(new ImageIcon(logoUrl), 180, 72));
        }
        // Fallback text label when the logo asset is unavailable.
        JLabel fallback = new JLabel("BUS");
        fallback.setFont(new Font("Segoe UI", Font.BOLD, 32));
        fallback.setForeground(PRIMARY_BLUE);
        return fallback;
    }

    private ImageIcon scaleIconToFit(ImageIcon original, int maxWidth, int maxHeight) {
        int w = original.getIconWidth();
        int h = original.getIconHeight();
        if (w <= 0 || h <= 0)
            return original;

        double scale = Math.min(Math.min((double) maxWidth / w, (double) maxHeight / h), 1.0);
        int targetW = Math.max(1, (int) Math.round(w * scale));
        int targetH = Math.max(1, (int) Math.round(h * scale));
        return new ImageIcon(original.getImage().getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH));
    }

    // -------------------------------------------------------------------------
    // Search Trips tab
    // -------------------------------------------------------------------------

    private JPanel buildSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(BG_MAIN);

        JPanel filterBar = new JPanel(new BorderLayout());
        filterBar.setBackground(PANEL_BG);

        String[] locations = { "(All)", "Manila", "Baguio", "Cebu", "Davao",
                "Iloilo", "Legazpi", "Zamboanga", "Tacloban" };
        fromCombo = styledCombo(locations);
        toCombo = styledCombo(locations);

        JPanel leftFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 15));
        leftFilters.setBackground(PANEL_BG);
        leftFilters.add(makeLabel("Origin:", Font.BOLD, 14, TEXT_DARK));
        leftFilters.add(fromCombo);
        leftFilters.add(makeLabel("Destination:", Font.BOLD, 14, TEXT_DARK));
        leftFilters.add(toCombo);

        JButton searchBtn = styledButton("Search", PRIMARY_BLUE, Color.WHITE);
        JButton showAllBtn = styledButton("Clear Filters", PANEL_BG, TEXT_DARK);
        JButton refreshBtn = styledButton("Refresh", PANEL_BG, TEXT_DARK);

        searchBtn.addActionListener(e -> searchTrips());
        showAllBtn.addActionListener(e -> {
            fromCombo.setSelectedIndex(0);
            toCombo.setSelectedIndex(0);
            searchTrips();
        });
        refreshBtn.addActionListener(e -> refreshFromFiles(true));

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 15));
        rightActions.setBackground(PANEL_BG);
        rightActions.add(refreshBtn);
        rightActions.add(showAllBtn);
        rightActions.add(searchBtn);

        filterBar.add(leftFilters, BorderLayout.WEST);
        filterBar.add(rightActions, BorderLayout.EAST);

        String[] cols = { "Trip Code", "From", "To", "Departure", "Arrival",
                "Avail. Seats", "Economy (P)", "Business (P)" };
        tripTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                // Only available seats (5) and price columns (6, 7) are editable.
                return c == 5 || c == 6 || c == 7;
            }
        };

        tripTableModel.addTableModelListener(e -> {
            if (isSyncingTripTable || e.getType() != TableModelEvent.UPDATE)
                return;
            int row = e.getFirstRow();
            int col = e.getColumn();
            if (row < 0 || (col != 5 && col != 6 && col != 7))
                return;

            if (col == 5) {
                updateTripSeatsFromTable(row);
            } else {
                updateTripPriceFromTable(row, col);
            }
        });

        tripTable = buildStyledTable(tripTableModel);

        panel.add(filterBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(tripTable), BorderLayout.CENTER);

        searchTrips();
        return panel;
    }

    private void searchTrips() {
        String from = comboValue(fromCombo);
        String to = comboValue(toCombo);

        tripTableModel.setRowCount(0);
        for (Trip t : manager.searchTrip(from, to)) {
            tripTableModel.addRow(new Object[] {
                    t.getTripCode(), t.getOrigin(), t.getDestination(),
                    t.getDepartureTime(), t.getArrivalTime(),
                    String.valueOf(t.getAvailableSeats()),
                    formatPrice(t.getEconomyPrice()), formatPrice(t.getBusinessPrice())
            });
        }
    }

    private void updateTripPriceFromTable(int row, int col) {
        String tripCode = String.valueOf(tripTableModel.getValueAt(row, 0));
        String text = rawTableText(row, col).replace("P", "").replace(",", "");

        double price;
        try {
            price = Double.parseDouble(text);
            if (price <= 0)
                throw new NumberFormatException("Price must be positive");
        } catch (NumberFormatException ex) {
            Trip trip = manager.findTripByCode(tripCode);
            if (trip != null) {
                double original = (col == 6) ? trip.getEconomyPrice() : trip.getBusinessPrice();
                setTableValue(tripTableModel, isSyncingTripTable, row, col, formatPrice(original));
                isSyncingTripTable = false;
                JOptionPane.showMessageDialog(this,
                        "Invalid price. Please enter a positive number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }

        Trip trip = manager.findTripByCode(tripCode);
        if (trip == null)
            return;

        if (col == 6)
            trip.setEconomyPrice(price);
        else
            trip.setBusinessPrice(price);

        setTableValue(tripTableModel, isSyncingTripTable, row, col, formatPrice(price));
        isSyncingTripTable = false;
        updatePricePreview();
        persistTrips();
    }

    private void updateTripSeatsFromTable(int row) {
        String tripCode = String.valueOf(tripTableModel.getValueAt(row, 0));
        Trip trip = manager.findTripByCode(tripCode);
        if (trip == null)
            return;

        String text = rawTableText(row, 5);
        int seats;
        try {
            seats = Integer.parseInt(text);
            if (seats < 0 || seats > trip.getTotalSeats())
                throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            setTableValue(tripTableModel, isSyncingTripTable, row, 5, String.valueOf(trip.getAvailableSeats()));
            isSyncingTripTable = false;
            JOptionPane.showMessageDialog(this,
                    "Invalid seat value. Enter a number from 0 to " + trip.getTotalSeats() + ".",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        trip.setAvailableSeats(seats);
        setTableValue(tripTableModel, isSyncingTripTable, row, 5, String.valueOf(seats));
        isSyncingTripTable = false;
        persistTrips();
    }

    // -------------------------------------------------------------------------
    // Book Ticket tab
    // -------------------------------------------------------------------------

    private JPanel buildBookPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG_MAIN);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(PANEL_BG);
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 10, 8, 10);
        gc.fill = GridBagConstraints.HORIZONTAL;

        nameField = styledTextField("");
        tripCodeCombo = styledCombo(TRIP_LABELS);
        classCombo = styledCombo(new String[] { "Economy", "Business" });
        ticketTypeCombo = styledCombo(new String[] { "One-Way", "Round Trip" });
        priceLabel = makeLabel("P0.00", Font.BOLD, 22, PRIMARY_BLUE);

        ActionListener priceUpdater = e -> updatePricePreview();
        tripCodeCombo.addActionListener(priceUpdater);
        classCombo.addActionListener(priceUpdater);
        ticketTypeCombo.addActionListener(priceUpdater);
        updatePricePreview();

        addFormRow(form, gc, 0, "Passenger Name:", nameField);
        addFormRow(form, gc, 1, "Trip Code:", tripCodeCombo);
        addFormRow(form, gc, 2, "Seat Class:", classCombo);
        addFormRow(form, gc, 3, "Ticket Type:", ticketTypeCombo);

        gc.gridx = 0;
        gc.gridy = 4;
        form.add(makeLabel("Total Fare:", Font.BOLD, 14, TEXT_DARK), gc);
        gc.gridx = 1;
        form.add(priceLabel, gc);

        JButton clearBtn = styledButton("Clear", PANEL_BG, TEXT_DARK);
        JButton bookBtn = styledButton("Book Now", SUCCESS, Color.WHITE);

        clearBtn.addActionListener(e -> clearBookForm());
        bookBtn.addActionListener(e -> confirmBooking());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.setBackground(PANEL_BG);
        btns.add(clearBtn);
        btns.add(bookBtn);

        gc.gridx = 0;
        gc.gridy = 5;
        gc.gridwidth = 2;
        form.add(btns, gc);

        panel.add(form);
        return panel;
    }

    private void addFormRow(JPanel form, GridBagConstraints gc, int row, String label, JComponent field) {
        gc.gridy = row;
        gc.gridx = 0;
        gc.gridwidth = 1;
        form.add(makeLabel(label, Font.PLAIN, 14, TEXT_DARK), gc);
        gc.gridx = 1;
        field.setPreferredSize(new Dimension(300, 35));
        form.add(field, gc);
    }

    private void updatePricePreview() {
        String code = getSelectedTripCode();
        String cls = classCombo.getSelectedItem().toString();
        String type = ticketTypeCombo.getSelectedItem().toString();

        for (Trip t : manager.getAllTrips()) {
            if (t.getTripCode().equals(code)) {
                double base = t.getPriceForClass(cls);
                double total = "Round Trip".equals(type) ? base * 2 : base;
                priceLabel.setText(String.format("P%.2f", total));
                return;
            }
        }
    }

    private void confirmBooking() {
        String name = nameField.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fill all fields.");
            return;
        }
        if (name.split("\\s+").length < 2) {
            JOptionPane.showMessageDialog(this, "Name must have at least 2 words (e.g., John Smith).");
            return;
        }

        try {
            String tripCode = getSelectedTripCode();
            String seatClass = classCombo.getSelectedItem().toString();
            String ticketType = ticketTypeCombo.getSelectedItem().toString();
            String date = new SimpleDateFormat("MM/dd/yyyy").format(new Date());

            String bookingCode = manager.bookSeat(name, tripCode, seatClass, ticketType, date);
            if (bookingCode == null)
                throw new RuntimeException("No available seats for that trip.");

            FileHandler.saveBookings(manager.getAllPassengers());
            FileHandler.saveReceipt(manager.findPassenger(bookingCode));
            refreshPassengerTable();
            searchTrips();
            clearBookForm();
            JOptionPane.showMessageDialog(this, "Booking confirmed! Code: " + bookingCode);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }

    private void clearBookForm() {
        nameField.setText("");
        tripCodeCombo.setSelectedIndex(0);
        classCombo.setSelectedIndex(0);
        ticketTypeCombo.setSelectedIndex(0);
        updatePricePreview();
    }

    private String getSelectedTripCode() {
        int idx = tripCodeCombo.getSelectedIndex();
        if (idx >= 0 && idx < TRIP_CODES.length)
            return TRIP_CODES[idx];
        return tripCodeCombo.getSelectedItem() == null ? "" : tripCodeCombo.getSelectedItem().toString();
    }

    // -------------------------------------------------------------------------
    // Passengers tab
    // -------------------------------------------------------------------------

    private JPanel buildPassengersPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(BG_MAIN);

        JTextField searchField = styledTextField("");
        searchField.setPreferredSize(new Dimension(180, 29));

        JButton refreshBtn = styledButton("Refresh", PANEL_BG, TEXT_DARK);
        refreshBtn.addActionListener(e -> refreshFromFiles(true));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(PANEL_BG);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 15));
        rightPanel.setBackground(PANEL_BG);
        rightPanel.add(makeLabel("Search:", Font.PLAIN, 13, TEXT_DARK));
        rightPanel.add(searchField);
        rightPanel.add(refreshBtn);
        top.add(rightPanel, BorderLayout.EAST);

        String[] cols = { "Code", "Name", "Date", "From", "To", "Seat", "Class", "Type", "Fare", "Status" };
        passengerTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c != 0; // Booking code column is read-only.
            }
        };

        passengerTableModel.addTableModelListener(e -> {
            if (isSyncingPassengerTable || e.getType() != TableModelEvent.UPDATE)
                return;
            int row = e.getFirstRow();
            int col = e.getColumn();
            if (row < 0 || col < 1 || col > 9)
                return;
            updatePassengerFromTable(row, col);
        });

        passengerTable = buildStyledTable(passengerTableModel);
        passengerTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        JComboBox<String> statusCombo = new JComboBox<>(new String[] { "Confirmed", "Completed", "Cancelled" });
        statusCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusCombo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                statusCombo.hidePopup();
            }
        });
        DefaultCellEditor statusEditor = new DefaultCellEditor(statusCombo);
        statusEditor.setClickCountToStart(2);
        passengerTable.getColumnModel().getColumn(9).setCellEditor(statusEditor);

        refreshPassengerTable();

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                filterPassengers(searchField.getText());
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                filterPassengers(searchField.getText());
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                filterPassengers(searchField.getText());
            }
        });

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(passengerTable), BorderLayout.CENTER);
        return panel;
    }

    private void refreshPassengerTable() {
        filteredPassengerList = new ArrayList<>(manager.getAllPassengers());
        passengerTableModel.setRowCount(0);
        for (Passenger p : filteredPassengerList) {
            passengerTableModel.addRow(buildPassengerRow(p));
        }
    }

    private void filterPassengers(String searchText) {
        String query = searchText.toLowerCase().trim();
        passengerTableModel.setRowCount(0);

        for (Passenger p : filteredPassengerList) {
            boolean matches = query.isEmpty()
                    || p.getBookingCode().toLowerCase().contains(query)
                    || p.getPassengerName().toLowerCase().contains(query)
                    || p.getFrom().toLowerCase().contains(query)
                    || p.getTo().toLowerCase().contains(query)
                    || p.getDate().toLowerCase().contains(query)
                    || p.getSeatNumber().toLowerCase().contains(query)
                    || p.getSeatClass().toLowerCase().contains(query)
                    || p.getTicketType().toLowerCase().contains(query)
                    || p.getStatus().toLowerCase().contains(query);

            if (matches)
                passengerTableModel.addRow(buildPassengerRow(p));
        }
    }

    private Object[] buildPassengerRow(Passenger p) {
        return new Object[] {
                p.getBookingCode(), p.getPassengerName(), p.getDate(), p.getFrom(), p.getTo(),
                p.getSeatNumber(), p.getSeatClass(), p.getTicketType(), formatPrice(p.getPrice()), p.getStatus()
        };
    }

    private void updatePassengerFromTable(int row, int col) {
        String bookingCode = String.valueOf(passengerTableModel.getValueAt(row, 0));
        Passenger p = manager.findPassenger(bookingCode);
        if (p == null)
            return;

        String value = String.valueOf(passengerTableModel.getValueAt(row, col)).trim();
        try {
            switch (col) {
                case 1:
                    if (value.isEmpty())
                        throw new IllegalArgumentException("Name cannot be empty.");
                    p.setPassengerName(value);
                    break;
                case 2:
                    if (value.isEmpty())
                        throw new IllegalArgumentException("Date cannot be empty.");
                    p.setDate(value);
                    break;
                case 3:
                    if (value.isEmpty())
                        throw new IllegalArgumentException("From cannot be empty.");
                    p.setFrom(value);
                    break;
                case 4:
                    if (value.isEmpty())
                        throw new IllegalArgumentException("To cannot be empty.");
                    p.setTo(value);
                    break;
                case 5:
                    if (value.isEmpty())
                        throw new IllegalArgumentException("Seat cannot be empty.");
                    p.setSeatNumber(value);
                    break;
                case 6:
                    if (!"Economy".equalsIgnoreCase(value) && !"Business".equalsIgnoreCase(value))
                        throw new IllegalArgumentException("Class must be Economy or Business.");
                    p.setSeatClass("Business".equalsIgnoreCase(value) ? "Business" : "Economy");
                    break;
                case 7:
                    if (!"One-Way".equalsIgnoreCase(value) && !"Round Trip".equalsIgnoreCase(value))
                        throw new IllegalArgumentException("Type must be One-Way or Round Trip.");
                    p.setTicketType("Round Trip".equalsIgnoreCase(value) ? "Round Trip" : "One-Way");
                    break;
                case 8:
                    double fare = Double.parseDouble(value.replace("P", "").replace(",", ""));
                    if (fare < 0)
                        throw new IllegalArgumentException("Fare cannot be negative.");
                    p.setPrice(fare);
                    break;
                case 9:
                    if (!manager.updatePassengerStatus(bookingCode, value))
                        throw new IllegalArgumentException("Status must be Confirmed, Completed, or Cancelled.");
                    break;
                default:
                    return;
            }

            manager.recomputeTripSeatAvailability();
            FileHandler.saveBookings(manager.getAllPassengers());
            FileHandler.saveTrips(manager.getAllTrips());

            isSyncingPassengerTable = true;
            refreshPassengerTable();
            isSyncingPassengerTable = false;
            searchTrips();

        } catch (Exception ex) {
            isSyncingPassengerTable = true;
            refreshPassengerTable();
            isSyncingPassengerTable = false;
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------------------------------------------------------
    // Status bar
    // -------------------------------------------------------------------------

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        bar.setBackground(TABLE_ALT);
        bar.add(makeLabel("Ready", Font.PLAIN, 12, TEXT_MUTED));
        return bar;
    }

    // -------------------------------------------------------------------------
    // Data loading / saving / auto-refresh
    // -------------------------------------------------------------------------

    private void loadDataOnStartup() {
        refreshFromFiles(false);
    }

    private void loadDataFromFiles() throws Exception {
        ArrayList<Passenger> passengers = FileHandler.loadBookings();
        manager.setPassengerList(passengers);

        ArrayList<Trip> trips = FileHandler.loadTrips();
        if (!trips.isEmpty()) {
            manager.setTrips(trips.toArray(new Trip[0]));
        }
    }

    private void refreshFromFiles(boolean showFeedback) {
        try {
            loadDataFromFiles();
            syncFileTimestamps();
            refreshVisibleData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Unable to refresh data from files.", "Refresh Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshVisibleData() {
        if (tripTableModel != null)
            searchTrips();
        if (passengerTableModel != null) {
            isSyncingPassengerTable = true;
            refreshPassengerTable();
            isSyncingPassengerTable = false;
        }
        if (priceLabel != null)
            updatePricePreview();
    }

    private void startAutoFileRefreshWatcher() {
        autoRefreshTimer = new Timer(2000, e -> {
            long currentBookings = fileLastModified("bookings.txt");
            long currentTrips = fileLastModified("trips.txt");
            if ((currentBookings > 0 && currentBookings != lastBookingsFileModified)
                    || (currentTrips > 0 && currentTrips != lastTripsFileModified)) {
                refreshFromFiles(false);
            }
        });
        autoRefreshTimer.start();
    }

    private void syncFileTimestamps() {
        lastBookingsFileModified = fileLastModified("bookings.txt");
        lastTripsFileModified = fileLastModified("trips.txt");
    }

    private long fileLastModified(String fileName) {
        java.io.File file = new java.io.File(fileName);
        return file.exists() ? file.lastModified() : -1L;
    }

    private void saveAndExit() {
        try {
            FileHandler.saveBookings(manager.getAllPassengers());
            FileHandler.saveTrips(manager.getAllTrips());
        } catch (Exception ignored) {
        }
        System.exit(0);
    }

    private void persistTrips() {
        try {
            FileHandler.saveTrips(manager.getAllTrips());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Unable to save trip changes.", "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------------------------------------------------------
    // Shared UI helpers
    // -------------------------------------------------------------------------

    private JLabel makeLabel(String text, int style, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", style, size));
        l.setForeground(color);
        return l;
    }

    private JButton styledButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        return b;
    }

    private JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setBackground(PANEL_BG);
        c.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        return c;
    }

    private JTextField styledTextField(String text) {
        JTextField f = new JTextField(text);
        f.setBackground(PANEL_BG);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        return f;
    }

    private JTable buildStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.setShowGrid(true);
        table.setGridColor(BORDER_COLOR);
        return table;
    }

    private String formatPrice(double price) {
        return String.format("%.2f", price);
    }

    private String comboValue(JComboBox<String> combo) {
        String val = combo.getSelectedItem().toString();
        return "(All)".equals(val) ? "" : val;
    }

    private String rawTableText(int row, int col) {
        Object raw = tripTableModel.getValueAt(row, col);
        return raw == null ? "" : raw.toString().trim();
    }

    /*
     * Wraps the isSyncing guard and setValueAt call into one place to avoid
     * repeating the flag management at every inline-edit correction site.
     */
    private void setTableValue(DefaultTableModel model, boolean syncFlag, int row, int col, Object value) {
        syncFlag = true;
        model.setValueAt(value, row, col);
    }

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TicketingSystemGUI::new);
    }
}