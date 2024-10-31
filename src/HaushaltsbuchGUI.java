// src/HaushaltsbuchGUI.java
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.function.Consumer;
import org.jdesktop.swingx.JXDatePicker;

public class HaushaltsbuchGUI extends JFrame {
    private Connection conn;
    private DefaultTableModel tableModel;
    private JComboBox<String> categoryDropdown;
    private JTextField infoField, betragField, searchField;
    private JTable buchungenTable;
    private TableRowSorter<DefaultTableModel> sorter;
    private LocalDate customDate;
    private JXDatePicker startDatePicker, endDatePicker;
    private JButton resetButton;
    private JPanel panel1;

    public HaushaltsbuchGUI() {
        connectToDatabase();
        initializeUIComponents();
        addRightClickMenu();
        addDoubleClickListener();
        loadData();
    }

    private void connectToDatabase() {
        try {
            conn = DatabaseConnection.getConnection();
        } catch (SQLException e) {
            UIHelper.showErrorDialog(this, "Fehler bei der Datenbankverbindung!");
            e.printStackTrace();
            System.exit(1);
        }
    }

    // src/HaushaltsbuchGUI.java
    private void initializeUIComponents() {
        setTitle("Haushaltsbuch");
        setLayout(new BorderLayout());

        JMenuBar menuBar = new JMenuBar();
        JMenu settingsMenu = new JMenu("Settings");

        JMenuItem neueKategorieItem = new JMenuItem("Kategorien");
        neueKategorieItem.addActionListener(e ->
                UIMenuHelper.openCategoryWindow(this, conn, this::loadCategories, this::loadData)
        );
        settingsMenu.add(neueKategorieItem);

        JMenuItem setDateItem = new JMenuItem("Datum manuell setzen");

        Consumer<LocalDate> setDateConsumer = this::setCustomDate;
        setDateItem.addActionListener(e ->
                UIMenuHelper.openSetDateWindow(this, setDateConsumer)
        );
        settingsMenu.add(setDateItem);

        menuBar.add(settingsMenu);
        setJMenuBar(menuBar);

        String[] columns = {
                BuchungFeld.ID.name(),
                BuchungFeld.INFO.name(),
                TransactionType.EINZAHLUNG.name(),
                TransactionType.AUSZAHLUNG.name(),
                BuchungFeld.DATUM.name(),
                KategorieFeld.BEZEICHNUNG.name()
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                if (column == 1 || column == 2 || column == 3) {
                    Object dateObj = getValueAt(row, 4);
                    if (dateObj instanceof LocalDate entryDate) {
                        return isEditableDate(entryDate);
                    }
                }
                return false;
            }
        };

        tableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int column = e.getColumn();
                String newValue = tableModel.getValueAt(row, column).toString();
                updateDatabase(row, column, newValue);
            }
        });

        buchungenTable = new JTable(tableModel);
        buchungenTable.getColumnModel().getColumn(0).setMinWidth(0);
        buchungenTable.getColumnModel().getColumn(0).setMaxWidth(0);
        buchungenTable.getColumnModel().getColumn(0).setWidth(0);
        buchungenTable.setAutoCreateRowSorter(true);
        sorter = new TableRowSorter<>(tableModel);
        buchungenTable.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(buchungenTable);

        searchField = new JTextField();
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                searchBooking();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                searchBooking();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                searchBooking();
            }
        });

        startDatePicker = new JXDatePicker();
        endDatePicker = new JXDatePicker();
        startDatePicker.addActionListener(e -> searchBooking());
        endDatePicker.addActionListener(e -> searchBooking());

        resetButton = new JButton("Reset All");
        resetButton.addActionListener(e -> resetFilters());

        JButton copyMarkdownButton = new JButton("Copy to Markdown");
        copyMarkdownButton.addActionListener(e -> {
            String markdown = convertTableToMarkdown();
            StringSelection selection = new StringSelection(markdown);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
            UIHelper.showInfoDialog(this, "Table copied to clipboard in Markdown format!");
        });

        JButton customSQLButton = new JButton("Custom SQL");
        customSQLButton.addActionListener(e -> openSQLDialog());

        // Add the button to the appropriate panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(copyMarkdownButton);
        buttonPanel.add(customSQLButton);

        // Add buttonPanel to the main UI layout
        add(buttonPanel, BorderLayout.SOUTH);

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(new JLabel("Suche Info: "), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);

        JPanel datePanel = new JPanel(new FlowLayout());
        datePanel.add(new JLabel("Von:"));
        datePanel.add(startDatePicker);
        datePanel.add(new JLabel("Bis:"));
        datePanel.add(endDatePicker);
        datePanel.add(resetButton);

        JPanel filterPanel = new JPanel(new BorderLayout());
        filterPanel.add(searchPanel, BorderLayout.NORTH);
        filterPanel.add(datePanel, BorderLayout.SOUTH);

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        tablePanel.add(filterPanel, BorderLayout.SOUTH);

        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        infoField = new JTextField();
        betragField = new JTextField();
        categoryDropdown = new JComboBox<>();

        loadCategories();

        JButton addButton = new JButton("Hinzufügen");
        addButton.addActionListener(e -> addBooking());

        inputPanel.add(new JLabel("Info:"));
        inputPanel.add(infoField);
        inputPanel.add(new JLabel("Betrag:"));
        inputPanel.add(betragField);
        inputPanel.add(new JLabel("Kategorie:"));
        inputPanel.add(categoryDropdown);
        inputPanel.add(new JLabel());
        inputPanel.add(addButton);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tablePanel, inputPanel);
        splitPane.setDividerLocation(500);

        add(splitPane, BorderLayout.CENTER);

        setSize(900, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void loadCategories() {
        categoryDropdown.removeAllItems();
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT Bezeichnung, Einzahlung_Auszahlung FROM Kategorie");
            while (rs.next()) {
                String bezeichnung = rs.getString("Bezeichnung");
                int einAus = rs.getInt("Einzahlung_Auszahlung");
                String suffix = (einAus == TransactionType.EINZAHLUNG.getValue()) ? " (E)" : " (A)";
                categoryDropdown.addItem(bezeichnung + suffix);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadData() {
        try {
            String sql = "SELECT b.ID, b.Info, k.Einzahlung_Auszahlung, b.Betrag, b.Datum, k.Bezeichnung " +
                    "FROM Buchungen b JOIN Kategorie k ON b.KategorieID = k.ID";
            ResultSet rs = executeQuery(sql);
            loadDataFromResultSet(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void addBooking() {
        String info = infoField.getText();
        String betragStr = betragField.getText();
        String category = (String) categoryDropdown.getSelectedItem();

        if (info.isEmpty() || betragStr.isEmpty()) {
            UIHelper.showErrorDialog(this, "Alle Felder müssen ausgefüllt werden!");
            return;
        }

        try {
            double betrag = Double.parseDouble(betragStr);
            int categoryId = getCategoryID(category);
            LocalDate date = (customDate != null) ? customDate : LocalDate.now();

            String sql = "INSERT INTO Buchungen (Datum, Info, Betrag, KategorieID) VALUES (?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setDate(1, Date.valueOf(date));
            pstmt.setString(2, info);
            pstmt.setDouble(3, betrag);
            pstmt.setInt(4, categoryId);
            pstmt.executeUpdate();

            tableModel.setRowCount(0);
            loadData();

            UIHelper.showInfoDialog(this, "Buchung erfolgreich hinzugefügt!");

            infoField.setText("");
            betragField.setText("");
            categoryDropdown.setSelectedIndex(-1);

        } catch (NumberFormatException ex) {
            UIHelper.showErrorDialog(this, "Ungültiger Betrag!");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private int getCategoryID(String category) throws SQLException {
        // Remove the suffix "(A)" or "(E)" from the category name
        if (category.endsWith(" (A)") || category.endsWith(" (E)")) {
            category = category.substring(0, category.length() - 4);
        }

        String sql = "SELECT ID FROM Kategorie WHERE Bezeichnung = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, category);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            return rs.getInt("ID");
        } else {
            throw new SQLException("Kategorie nicht gefunden.");
        }
    }

    private void searchBooking() {
        String searchText = searchField.getText();
        LocalDate startDate = startDatePicker.getDate() != null ? startDatePicker.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null;
        LocalDate endDate = endDatePicker.getDate() != null ? endDatePicker.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null;

        try {
            String sql = "SELECT b.ID, b.Info, k.Einzahlung_Auszahlung, b.Betrag, b.Datum, k.Bezeichnung " +
                    "FROM Buchungen b JOIN Kategorie k ON b.KategorieID = k.ID WHERE 1=1";
            if (!searchText.isEmpty()) {
                sql += " AND b.Info LIKE '%" + searchText + "%'";
            }
            if (startDate != null) {
                sql += " AND b.Datum >= '" + Date.valueOf(startDate) + "'";
            }
            if (endDate != null) {
                sql += " AND b.Datum <= '" + Date.valueOf(endDate) + "'";
            }
            ResultSet rs = executeQuery(sql);
            loadDataFromResultSet(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void updateDatabase(int row, int column, String newValue) {
        try {
            int id = (int) tableModel.getValueAt(row, 0);
            String columnNameStr = tableModel.getColumnName(column);

            if (columnNameStr.equals(TransactionType.EINZAHLUNG.name()) || columnNameStr.equals(TransactionType.AUSZAHLUNG.name())) {
                columnNameStr = BuchungFeld.BETRAG.name();
            }

            BuchungFeld columnName;
            try {
                columnName = BuchungFeld.valueOf(columnNameStr);
            } catch (IllegalArgumentException e) {
                throw new SQLException("Unknown column: " + columnNameStr);
            }

            String sql;
            PreparedStatement pstmt;

            switch (columnName) {
                case DATUM:
                    LocalDate newDate = LocalDate.parse(newValue);
                    sql = "UPDATE Buchungen SET Datum = ? WHERE ID = ?";
                    pstmt = conn.prepareStatement(sql);
                    pstmt.setDate(1, Date.valueOf(newDate));
                    pstmt.setInt(2, id);
                    break;
                case INFO:
                    sql = "UPDATE Buchungen SET Info = ? WHERE ID = ?";
                    pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, newValue);
                    pstmt.setInt(2, id);
                    break;
                case BETRAG:
                    sql = "UPDATE Buchungen SET Betrag = ? WHERE ID = ?";
                    pstmt = conn.prepareStatement(sql);
                    pstmt.setDouble(1, Double.parseDouble(newValue));
                    pstmt.setInt(2, id);
                    break;
                default:
                    throw new SQLException("Unknown column: " + columnName);
            }

            pstmt.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    void loadDataFromResultSet(ResultSet rs) throws SQLException {
        tableModel.setRowCount(0);
        while (rs.next()) {
            int id = rs.getInt("ID");
            String info = rs.getString("Info");
            double betrag = rs.getDouble("Betrag");
            TransactionType einzahlungAuszahlung = TransactionType.fromValue(rs.getInt("Einzahlung_Auszahlung"));
            LocalDate datum = rs.getDate("Datum").toLocalDate();
            String kategorie = rs.getString("Bezeichnung");

            addRowToTableModel(id, info, betrag, einzahlungAuszahlung, datum, kategorie);
        }
    }

    void addRowToTableModel(int id, String info, double betrag, TransactionType einzahlungAuszahlung, LocalDate datum, String kategorie) {
        Object[] row = new Object[6];
        row[0] = id;
        row[1] = info;
        if (einzahlungAuszahlung == TransactionType.EINZAHLUNG) {
            row[2] = betrag;
            row[3] = null;
        } else {
            row[2] = null;
            row[3] = betrag;
        }
        row[4] = datum;
        row[5] = kategorie;
        tableModel.addRow(row);
    }

    private void addRightClickMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Löschen");

        deleteItem.addActionListener(e -> {
            int selectedRow = buchungenTable.getSelectedRow();
            if (selectedRow != -1) {
                int modelRow = buchungenTable.convertRowIndexToModel(selectedRow);
                int id = (int) tableModel.getValueAt(modelRow, 0);

                int confirm = JOptionPane.showConfirmDialog(this, "Sind Sie sicher, dass Sie diese Buchung löschen möchten?", "Bestätigung", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    deleteBooking(id);
                    tableModel.removeRow(modelRow);
                }
            }
        });

        popupMenu.add(deleteItem);

        buchungenTable.setComponentPopupMenu(popupMenu);
        buchungenTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = buchungenTable.rowAtPoint(e.getPoint());
                    buchungenTable.setRowSelectionInterval(row, row);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = buchungenTable.rowAtPoint(e.getPoint());
                    buchungenTable.setRowSelectionInterval(row, row);
                }
            }
        });
    }

    private void setCustomDate(LocalDate date) {
        customDate = date;
    }

    void deleteBooking(int id) {
        try {
            String sql = "DELETE FROM Buchungen WHERE ID = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // Add this method to HaushaltsbuchGUI class
    private void addDoubleClickListener() {
        buchungenTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = buchungenTable.rowAtPoint(e.getPoint());
                    int column = buchungenTable.columnAtPoint(e.getPoint());
                    if (column == buchungenTable.getColumnModel().getColumnIndex(KategorieFeld.BEZEICHNUNG.name())) {
                        String bezeichnung = (String) tableModel.getValueAt(row, column);
                        openNewTable(bezeichnung);
                    }
                }
            }
        });
    }

    private void openNewTable(String bezeichnung) {
        new DetailsTable(this, conn, bezeichnung);
    }

    private void resetFilters() {
        searchField.setText("");
        startDatePicker.setDate(null);
        endDatePicker.setDate(null);
        loadData();
    }

    private String convertTableToMarkdown() {
        StringBuilder markdown = new StringBuilder();
        int columnCount = tableModel.getColumnCount();
        int rowCount = tableModel.getRowCount();

        // Add header
        for (int col = 0; col < columnCount; col++) {
            markdown.append("| ").append(tableModel.getColumnName(col)).append(" ");
        }
        markdown.append("|\n");

        // Add separator
        for (int col = 0; col < columnCount; col++) {
            markdown.append("|---");
        }
        markdown.append("|\n");

        // Add rows
        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < columnCount; col++) {
                Object value = tableModel.getValueAt(row, col);
                markdown.append("| ").append(value != null ? value.toString() : "").append(" ");
            }
            markdown.append("|\n");
        }

        return markdown.toString();
    }

    private void openSQLDialog() {
        JDialog sqlDialog = new JDialog(this, "Custom SQL Query", true);
        sqlDialog.setLayout(new BorderLayout());

        JTextArea sqlTextArea = new JTextArea(10, 50);
        JButton executeButton = new JButton("Execute");

        executeButton.addActionListener(e -> {
            String userSql = sqlTextArea.getText().trim();

            if (userSql.isEmpty()) {
                sqlDialog.dispose();
                return;
            }

            if (!userSql.toLowerCase().startsWith("select")) {
                userSql = "SELECT * FROM Buchungen WHERE " + userSql;
            }

            if (isValidSQL(userSql)) {
                try {
                    userSql = translateSQL(userSql);
                    ResultSet rs = executeQuery(userSql);
                    displayQueryResults(rs);
                    sqlDialog.dispose();
                } catch (SQLException ex) {
                    UIHelper.showErrorDialog(this, "Error executing SQL query!");
                    ex.printStackTrace();
                }
            } else {
                UIHelper.showErrorDialog(this, "Invalid SQL query! Only SELECT statements are allowed.");
            }
        });

        sqlDialog.add(new JScrollPane(sqlTextArea), BorderLayout.CENTER);
        sqlDialog.add(executeButton, BorderLayout.SOUTH);
        sqlDialog.pack();
        sqlDialog.setLocationRelativeTo(this);
        sqlDialog.setVisible(true);
    }

    private boolean isValidSQL(String sql) {
        String lowerSql = sql.toLowerCase();
        return lowerSql.startsWith("select") && !lowerSql.contains("delete") && !lowerSql.contains("update") && !lowerSql.contains("insert");
    }

    private String translateSQL(String sql) {
        return sql.replaceAll("(?i)einzahlen|auszahlen", "Betrag");
    }

    private void displayQueryResults(ResultSet rs) throws SQLException {
        DefaultTableModel model = new DefaultTableModel();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            model.addColumn(metaData.getColumnLabel(i));
        }

        while (rs.next()) {
            Object[] row = new Object[columnCount];
            for (int i = 0; i < columnCount; i++) {
                row[i] = rs.getObject(i + 1);
            }
            model.addRow(row);
        }

        JTable resultTable = new JTable(model);
        JDialog resultDialog = new JDialog(this, "Query Results", true);
        resultDialog.setLayout(new BorderLayout());
        resultDialog.add(new JScrollPane(resultTable), BorderLayout.CENTER);
        resultDialog.pack();
        resultDialog.setLocationRelativeTo(this);
        resultDialog.setVisible(true);
    }

    private ResultSet executeQuery(String sql) throws SQLException {
        Statement stmt = conn.createStatement();
        return stmt.executeQuery(sql);
    }

    private boolean isEditableDate(LocalDate date) {
        LocalDate referenceDate = (customDate != null) ? customDate : LocalDate.now();
        return !date.isBefore(referenceDate);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(HaushaltsbuchGUI::new);
    }
}


