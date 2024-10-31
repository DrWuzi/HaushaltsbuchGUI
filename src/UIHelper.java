// src/UIHelper.java
import javax.swing.*;
import java.awt.*;

public class UIHelper {
    public static void showErrorDialog(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Fehler", JOptionPane.ERROR_MESSAGE);
    }

    public static void showInfoDialog(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }
}
