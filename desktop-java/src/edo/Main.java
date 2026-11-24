package edo;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

// Aquí arranca todo el programa
public class Main {

    public static void main(String[] args) {
        // Atrapa errores que no se cacharon en otro lado para que no truene en silencio
        Thread.setDefaultUncaughtExceptionHandler((hilo, ex) -> {
            ex.printStackTrace();
            try {
                JOptionPane.showMessageDialog(null,
                        "Ocurrió un error inesperado:\n" + ex
                        + "\n\nEl programa seguirá abierto; revisa los datos e inténtalo de nuevo.",
                        "Error inesperado", JOptionPane.ERROR_MESSAGE);
            } catch (RuntimeException ignored) {
            }
        });

        // Ponemos el tema Nimbus si está disponible
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
        }

        // Abrimos la ventana en el hilo de interfaz de Swing
        SwingUtilities.invokeLater(() -> new VentanaPrincipal().setVisible(true));
    }
}