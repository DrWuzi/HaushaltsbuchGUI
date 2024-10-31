// src/UIMenuHelper.java
import org.jdesktop.swingx.JXDatePicker;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.function.Consumer;

public class UIMenuHelper {
    public static void openCategoryWindow(JFrame parent, Connection conn, Runnable loadCategories, Runnable loadData) {
        JFrame categoryFrame = new JFrame("Kategorien");
        categoryFrame.setLayout(new BorderLayout());

        // Table for displaying categories
        DefaultTableModel tableModel = new DefaultTableModel(new String[]{"ID", "Bezeichnung", "Kurzbeschreibung", "Einzahlung/Auszahlung"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 0; // ID column is not editable
            }
        };
        JTable categoryTable = new JTable(tableModel);
        loadCategoriesIntoTable(conn, tableModel);

        // Set custom cell editor for "Einzahlung/Auszahlung" column
        TableColumn einAusColumn = categoryTable.getColumnModel().getColumn(3);
        einAusColumn.setCellEditor(new DefaultCellEditor(new JComboBox<>(TransactionType.values())));

        // Add a listener to save changes to the database
        categoryTable.getModel().addTableModelListener(e -> {
            int row = e.getFirstRow();
            int column = e.getColumn();
            if (column != -1) {
                int id = (int) tableModel.getValueAt(row, 0);
                String bezeichnung = (String) tableModel.getValueAt(row, 1);
                String kurzbeschreibung = (String) tableModel.getValueAt(row, 2);
                TransactionType einAus = (TransactionType) tableModel.getValueAt(row, 3);

                try {
                    String sql = "UPDATE Kategorie SET Bezeichnung = ?, Kurzbeschreibung = ?, Einzahlung_Auszahlung = ? WHERE ID = ?";
                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, bezeichnung);
                    pstmt.setString(2, kurzbeschreibung);
                    pstmt.setInt(3, einAus.getValue());
                    pstmt.setInt(4, id);
                    pstmt.executeUpdate();

                    // Update the dropdown in the main UI
                    loadCategories.run();
                    loadData.run();

                } catch (SQLException ex) {
                    ex.printStackTrace();
                    UIHelper.showErrorDialog(categoryFrame, "Fehler beim Aktualisieren der Kategorie!");
                }
            }
        });

        // Panel for adding new categories
        JPanel addCategoryPanel = new JPanel(new GridLayout(4, 2));
        JTextField bezeichnungField = new JTextField();
        JTextField kurzbeschreibungField = new JTextField();
        JComboBox<TransactionType> einAusDropdown = new JComboBox<>(TransactionType.values());

        JButton saveButton = new JButton("Speichern");
        saveButton.addActionListener(e -> {
            String bezeichnung = bezeichnungField.getText();
            String kurzbeschreibung = kurzbeschreibungField.getText();
            TransactionType einAus = (TransactionType) einAusDropdown.getSelectedItem();

            if (bezeichnung.isEmpty()) {
                UIHelper.showErrorDialog(categoryFrame, "Bezeichnung darf nicht leer sein!");
                return;
            }

            if (categoryExists(conn, bezeichnung, einAus)) {
                UIHelper.showErrorDialog(categoryFrame, "Kategorie mit diesem Namen und Typ existiert bereits!");
                return;
            }

            try {
                String sql = "INSERT INTO Kategorie (Bezeichnung, Kurzbeschreibung, Einzahlung_Auszahlung) VALUES (?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, bezeichnung);
                pstmt.setString(2, kurzbeschreibung);
                pstmt.setInt(3, einAus.getValue());
                pstmt.executeUpdate();

                UIHelper.showInfoDialog(categoryFrame, "Kategorie erfolgreich hinzugefügt!");
                loadCategoriesIntoTable(conn, tableModel);
                bezeichnungField.setText("");
                kurzbeschreibungField.setText("");
                einAusDropdown.setSelectedIndex(0);

                // Update the dropdown in the main UI
                loadCategories.run();
                loadData.run();

            } catch (SQLException ex) {
                ex.printStackTrace();
                UIHelper.showErrorDialog(categoryFrame, "Fehler beim Hinzufügen der Kategorie!");
            }
        });

        addCategoryPanel.add(new JLabel("Bezeichnung:"));
        addCategoryPanel.add(bezeichnungField);
        addCategoryPanel.add(new JLabel("Kurzbeschreibung:"));
        addCategoryPanel.add(kurzbeschreibungField);
        addCategoryPanel.add(new JLabel("Einzahlung/Auszahlung:"));
        addCategoryPanel.add(einAusDropdown);
        addCategoryPanel.add(new JLabel());
        addCategoryPanel.add(saveButton);

        // Add components to the frame
        categoryFrame.add(new JScrollPane(categoryTable), BorderLayout.CENTER);
        categoryFrame.add(addCategoryPanel, BorderLayout.EAST);

        // Add right-click context menu for deleting items
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Löschen");
        deleteItem.addActionListener(e -> {
            int selectedRow = categoryTable.getSelectedRow();
            if (selectedRow != -1) {
                int id = (int) tableModel.getValueAt(selectedRow, 0);

                // Check for related entries in the buchungen table
                try (PreparedStatement checkStmt = conn.prepareStatement("SELECT COUNT(*) FROM Buchungen WHERE KategorieID = ?")) {
                    checkStmt.setInt(1, id);
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next() && rs.getInt(1) > 0) {
                        int confirm = JOptionPane.showConfirmDialog(parent, "Kategorie wird in Buchungen verwendet. Möchten Sie alle Einträge löschen?", "Bestätigung", JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                            // Delete related entries in Buchungen
                            try (PreparedStatement deleteBuchungenStmt = conn.prepareStatement("DELETE FROM Buchungen WHERE KategorieID = ?")) {
                                deleteBuchungenStmt.setInt(1, id);
                                deleteBuchungenStmt.executeUpdate();
                                // Reload the main UI table
                                loadData.run();
                            }
                        } else {
                            return;
                        }
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    UIHelper.showErrorDialog(categoryFrame, "Fehler beim Überprüfen der Kategorie!");
                    return;
                }

                int confirm = JOptionPane.showConfirmDialog(parent, "Sind Sie sicher, dass Sie diese Kategorie löschen möchten?", "Bestätigung", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        String sql = "DELETE FROM Kategorie WHERE ID = ?";
                        PreparedStatement pstmt = conn.prepareStatement(sql);
                        pstmt.setInt(1, id);
                        pstmt.executeUpdate();
                        loadCategoriesIntoTable(conn, tableModel);

                        // Update the dropdown in the main UI
                        loadCategories.run();
                        loadData.run();

                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        UIHelper.showErrorDialog(categoryFrame, "Fehler beim Löschen der Kategorie!");
                    }
                }
            }
        });
        popupMenu.add(deleteItem);

        categoryTable.setComponentPopupMenu(popupMenu);
        categoryTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = categoryTable.rowAtPoint(e.getPoint());
                    categoryTable.setRowSelectionInterval(row, row);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = categoryTable.rowAtPoint(e.getPoint());
                    categoryTable.setRowSelectionInterval(row, row);
                }
            }
        });

        categoryFrame.setSize(800, 400);
        categoryFrame.setLocationRelativeTo(parent); // Center the frame relative to the parent
        categoryFrame.setVisible(true);
        categoryFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private static boolean categoryExists(Connection conn, String bezeichnung, TransactionType einAus) {
        try {
            String sql = "SELECT COUNT(*) FROM Kategorie WHERE Bezeichnung = ? AND Einzahlung_Auszahlung = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, bezeichnung);
            pstmt.setInt(2, einAus.getValue());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return true;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private static void loadCategoriesIntoTable(Connection conn, DefaultTableModel tableModel) {
        tableModel.setRowCount(0);
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT ID, Bezeichnung, Kurzbeschreibung, Einzahlung_Auszahlung FROM Kategorie");
            while (rs.next()) {
                int id = rs.getInt("ID");
                String bezeichnung = rs.getString("Bezeichnung");
                String kurzbeschreibung = rs.getString("Kurzbeschreibung");
                TransactionType einAus = TransactionType.fromValue(rs.getInt("Einzahlung_Auszahlung"));
                tableModel.addRow(new Object[]{id, bezeichnung, kurzbeschreibung, einAus});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void openSetDateWindow(JFrame parent, Consumer<LocalDate> setDateConsumer) {
        JDialog dialog = new JDialog(parent, "Datum manuell setzen", true);
        dialog.setLayout(new BorderLayout());

        JXDatePicker datePicker = new JXDatePicker();
        JButton setDateButton = new JButton("Setzen");
        JButton resetButton = new JButton("Zurücksetzen");

        resetButton.addActionListener(e -> datePicker.setDate(java.util.Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant())));

        setDateButton.addActionListener(e -> {
            LocalDate selectedDate = datePicker.getDate() != null
                    ? datePicker.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    : LocalDate.now();
            setDateConsumer.accept(selectedDate);
            dialog.dispose();
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(resetButton);
        buttonPanel.add(setDateButton);

        dialog.add(datePicker, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
}
