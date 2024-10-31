import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DetailsTable extends JFrame {
    public DetailsTable(JFrame parent, Connection conn, String bezeichnung) {
        setTitle("Details for " + bezeichnung);
        setSize(600, 150);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        DefaultTableModel newTableModel = new DefaultTableModel(new String[]{"Bezeichnung", "Beschreibung", "Transaction Type"}, 0);
        JTable newTable = new JTable(newTableModel);

        // Center the content of the table
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        newTable.setDefaultRenderer(Object.class, centerRenderer);

        // Load data into the new table
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT Bezeichnung, Kurzbeschreibung, Einzahlung_Auszahlung FROM Kategorie WHERE Bezeichnung = ?")) {
            pstmt.setString(1, bezeichnung);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String bez = rs.getString("Bezeichnung");
                String beschreibung = rs.getString("Kurzbeschreibung");
                TransactionType transactionType = TransactionType.fromValue(rs.getInt("Einzahlung_Auszahlung"));
                newTableModel.addRow(new Object[]{bez, beschreibung, transactionType.name()});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        add(new JScrollPane(newTable));

        // Center the frame relative to the parent
        setLocationRelativeTo(parent);

        setVisible(true);
    }
}
