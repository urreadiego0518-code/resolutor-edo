package edo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

// Ventana principal: lee los datos, calcula en segundo plano y muestra resultados
public class VentanaPrincipal extends JFrame {

    private static final Color VERDE = new Color(30, 120, 50);
    private static final Color ROJO = new Color(190, 30, 30);
    private static final Color NARANJA = new Color(180, 100, 0);
    private static final Color GRIS = new Color(90, 90, 90);

    // Campos de entrada
    private final JComboBox<Ejemplo> cmbEjemplos = new JComboBox<>(Ejemplo.catalogo());
    private final JTextField txtEcuacion = new JTextField(14);
    private final JTextField txtExacta = new JTextField(14);
    private final JTextField txtX0 = new JTextField("0");
    private final JTextField txtY0 = new JTextField("1");
    private final JTextField txtH = new JTextField("0.1");
    private final JTextField txtXf = new JTextField("2");
    private final JSlider sldH = new JSlider(0, 100, 67);
    private final JCheckBox chkEuler = new JCheckBox("Euler", true);
    private final JCheckBox chkRK4 = new JCheckBox("RK4", true);
    private final JCheckBox chkRef = new JCheckBox("Exacta", true);
    private final JButton btnResolver = new JButton("Resolver");
    private final JButton btnCsv = new JButton("Exportar CSV");
    private final JButton btnPng = new JButton("Guardar gráfica");

    // Componentes para mostrar resultados
    private final PanelGrafica grafica = new PanelGrafica();
    private final JTable tabla = new JTable();
    private final JTextArea txtPasos = crearArea();
    private final JTextArea txtResumen = crearArea();
    private final JSpinner spnPaso = new JSpinner(new SpinnerNumberModel(0, 0, 0, 1));
    private final JLabel lblEstado = new JLabel(" ");
    private final JTabbedPane tabs = new JTabbedPane();
    private final JTable tablaConv = new JTable();
    private final JTextArea txtConv = crearArea();
    private final JButton btnConvergencia = new JButton("Calcular convergencia");

    private boolean ajustando = false;
    private boolean desdeDeslizador = false;

    // Guardamos el último cálculo para consultar pasos o exportar
    private Expresion fActual;
    private double[] xs, yEu, yRk, yRef;
    private double hActual;
    private boolean refEsExacta;

    // Hilos en segundo plano para no trabar la ventana
    private SwingWorker<Resultado, Void> trabajador;
    private long versionPeticion;
    private SwingWorker<Conv, Void> trabajadorConv;
    private final Timer esperaDeslizador = new Timer(160, e -> {
        if (ajustando) {
            return;
        }
        desdeDeslizador = true;
        try {
            resolver(false);
        } finally {
            desdeDeslizador = false;
        }
    });

    public VentanaPrincipal() {
        super("Resolutor de EDO — Método de Euler y Runge-Kutta 4");
        esperaDeslizador.setRepeats(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        construirInterfaz();
        conectarEventos();
        setSize(1200, 760);
        setMinimumSize(new Dimension(1000, 660));
        setLocationRelativeTo(null);

        cargarEjemplo((Ejemplo) cmbEjemplos.getSelectedItem());
        resolver(false);
    }

    // Arma paneles, pestañas y botones de la ventana
    private void construirInterfaz() {
        JPanel params = new JPanel(new GridBagLayout());
        params.setBorder(BorderFactory.createTitledBorder("Parámetros"));
        int fila = 0;
        fila = agregarFila(params, fila, "Ejemplo:", cmbEjemplos);
        fila = agregarFila(params, fila, "dy/dx = f(x, y):", txtEcuacion);
        fila = agregarFila(params, fila, "y(x) exacta (opc.):", txtExacta);
        fila = agregarFila(params, fila, "x0 (o t0):", txtX0);
        fila = agregarFila(params, fila, "y0 = y(x0):", txtY0);
        fila = agregarFila(params, fila, "h (tamaño de paso):", txtH);
        sldH.setToolTipText("Mueve el deslizador para cambiar h y ver cómo cambia el error");
        fila = agregarFilaCompleta(params, fila, sldH);
        fila = agregarFila(params, fila, "x final:", txtXf);

        JPanel checks = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        checks.add(new JLabel("Mostrar:"));
        checks.add(chkEuler);
        checks.add(chkRK4);
        checks.add(chkRef);
        fila = agregarFilaCompleta(params, fila, checks);

        btnResolver.setFont(btnResolver.getFont().deriveFont(Font.BOLD, 14f));
        fila = agregarFilaCompleta(params, fila, btnResolver);

        JPanel exportar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        exportar.add(btnCsv);
        exportar.add(btnPng);
        fila = agregarFilaCompleta(params, fila, exportar);

        txtEcuacion.setToolTipText("Ejemplos:  x - y   |   -0.5*(y-20)   |   y*cos(x)   |   0.3*y*(1-y/100)");
        txtExacta.setToolTipText("Opcional. Escríbela en función de x, ej: x - 1 + 2*exp(-x). Si la dejas vacía se usa una referencia numérica.");
        btnCsv.setToolTipText("Guarda la tabla de iteraciones en un archivo CSV (se abre en Excel).");
        btnPng.setToolTipText("Guarda la gráfica actual como imagen PNG.");

        JPanel oeste = new JPanel(new BorderLayout(0, 8));
        oeste.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 4));
        oeste.add(params, BorderLayout.NORTH);
        JScrollPane spResumen = new JScrollPane(txtResumen);
        spResumen.setBorder(BorderFactory.createTitledBorder("Resumen de errores"));
        oeste.add(spResumen, BorderLayout.CENTER);
        oeste.setPreferredSize(new Dimension(350, 100));

        tabla.setRowHeight(22);
        tabla.setFillsViewportHeight(true);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JPanel pasos = new JPanel(new BorderLayout());
        JPanel barraPaso = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        barraPaso.add(new JLabel("Ver el paso número i ="));
        spnPaso.setPreferredSize(new Dimension(90, 26));
        barraPaso.add(spnPaso);
        barraPaso.add(new JLabel("(i = 0 es el primer paso, igual que en tu cuaderno)"));
        pasos.add(barraPaso, BorderLayout.NORTH);
        pasos.add(new JScrollPane(txtPasos), BorderLayout.CENTER);

        JTextArea teoria = crearArea();
        teoria.setText(TEXTO_TEORIA);
        teoria.setCaretPosition(0);

        tabs.addTab("Gráfica", grafica);
        tabs.addTab("Tabla de iteraciones", new JScrollPane(tabla));
        tabs.addTab("Paso a paso", pasos);
        tabs.addTab("Convergencia", construirPanelConvergencia());
        tabs.addTab("Teoría y ayuda", new JScrollPane(teoria));

        lblEstado.setBorder(BorderFactory.createEmptyBorder(4, 10, 6, 10));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(oeste, BorderLayout.WEST);
        JPanel centro = new JPanel(new BorderLayout());
        centro.setBorder(BorderFactory.createEmptyBorder(8, 4, 0, 8));
        centro.add(tabs, BorderLayout.CENTER);
        getContentPane().add(centro, BorderLayout.CENTER);
        getContentPane().add(lblEstado, BorderLayout.SOUTH);
    }

    private JPanel construirPanelConvergencia() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel arriba = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        arriba.add(btnConvergencia);
        arriba.add(new JLabel("Divide h entre 2 varias veces y compara los errores en el último punto."));
        p.add(arriba, BorderLayout.NORTH);

        tablaConv.setRowHeight(22);
        tablaConv.setFillsViewportHeight(true);
        tablaConv.getTableHeader().setReorderingAllowed(false);
        p.add(new JScrollPane(tablaConv), BorderLayout.CENTER);

        txtConv.setRows(6);
        JScrollPane sp = new JScrollPane(txtConv);
        sp.setPreferredSize(new Dimension(100, 130));
        sp.setBorder(BorderFactory.createTitledBorder("Lectura de los resultados"));
        p.add(sp, BorderLayout.SOUTH);
        return p;
    }

    private int agregarFila(JPanel p, int fila, String etiqueta, JComponent comp) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = fila;
        c.insets = new Insets(3, 4, 3, 4);
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        p.add(new JLabel(etiqueta), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        p.add(comp, c);
        return fila + 1;
    }

    private int agregarFilaCompleta(JPanel p, int fila, JComponent comp) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = fila;
        c.gridx = 0;
        c.gridwidth = 2;
        c.weightx = 1;
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        p.add(comp, c);
        return fila + 1;
    }

    private static JTextArea crearArea() {
        JTextArea a = new JTextArea();
        a.setEditable(false);
        a.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        a.setMargin(new Insets(8, 10, 8, 10));
        return a;
    }

    // Conecta botones, cajas de texto, deslizador y pestañas
    private void conectarEventos() {
        cmbEjemplos.addActionListener(e -> {
            if (ajustando) {
                return;
            }
            Ejemplo ej = (Ejemplo) cmbEjemplos.getSelectedItem();
            if (ej != null && ej.f != null) {
                cargarEjemplo(ej);
                resolver(false);
            }
        });

        btnResolver.addActionListener(e -> resolver(true));
        for (JTextField t : new JTextField[]{txtEcuacion, txtExacta, txtX0, txtY0, txtH, txtXf}) {
            t.addActionListener(e -> resolver(true));
        }

        sldH.addChangeListener(e -> {
            if (ajustando) {
                return;
            }
            double h = Math.pow(10, -3 + 3.5 * sldH.getValue() / 100.0);
            h = Double.parseDouble(String.format(Locale.US, "%.2g", h));
            ajustando = true;
            try {
                txtH.setText(Metodos.num(h));
            } finally {
                ajustando = false;
            }
            // Esperamos un poquito para no recalcular a cada milímetro que se mueve el slider
            esperaDeslizador.restart();
        });

        Runnable visibilidad = () -> grafica.setVisibles(chkEuler.isSelected(),
                chkRK4.isSelected(), chkRef.isSelected());
        chkEuler.addActionListener(e -> visibilidad.run());
        chkRK4.addActionListener(e -> visibilidad.run());
        chkRef.addActionListener(e -> visibilidad.run());

        spnPaso.addChangeListener(e -> actualizarPasos());

        btnCsv.addActionListener(e -> exportarCsv());
        btnPng.addActionListener(e -> guardarGrafica());
        btnConvergencia.addActionListener(e -> calcularConvergencia());
    }

    // Pone los valores de un ejemplo dentro de las cajas de texto
    private void cargarEjemplo(Ejemplo ej) {
        if (ej == null || ej.f == null) {
            return;
        }
        ajustando = true;
        txtEcuacion.setText(ej.f);
        txtExacta.setText(ej.exacta);
        txtX0.setText(Metodos.num(ej.x0));
        txtY0.setText(Metodos.num(ej.y0));
        txtH.setText(Metodos.num(ej.h));
        txtXf.setText(Metodos.num(ej.xf));
        ajustarSlider(ej.h);
        ajustando = false;
    }

    private void ajustarSlider(double h) {
        double v = (Math.log10(h) + 3) / 3.5 * 100;
        boolean antes = ajustando;
        ajustando = true;
        sldH.setValue((int) Math.round(Math.max(0, Math.min(100, v))));
        ajustando = antes;
    }

    private static final class Datos {
        final Expresion f;
        final Expresion exacta;
        final double x0, y0, h, xf;
        final int n;

        Datos(Expresion f, Expresion exacta, double x0, double y0, double h, double xf, int n) {
            this.f = f;
            this.exacta = exacta;
            this.x0 = x0;
            this.y0 = y0;
            this.h = h;
            this.xf = xf;
            this.n = n;
        }
    }

    private static final class Resultado {
        Datos d;
        double[] x, ye, yr, ref;
        boolean hayExacta;
        int m;
        long ms;
    }

    // Lee y valida todo lo que escribió el usuario
    private Datos leerDatos() {
        Expresion f = new Expresion(txtEcuacion.getText());
        double x0 = leerNumero(txtX0, "x0");
        double y0 = leerNumero(txtY0, "y0");
        double h = leerNumero(txtH, "h");
        double xf = leerNumero(txtXf, "x final");
        int n = Metodos.pasosPara(x0, xf, h);

        Expresion exacta = null;
        String textoExacta = txtExacta.getText().trim();
        if (!textoExacta.isEmpty()) {
            try {
                exacta = new Expresion(textoExacta);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Solución exacta: " + ex.getMessage());
            }
            if (exacta.dependeDeY()) {
                throw new IllegalArgumentException(
                        "La solución exacta debe escribirse solo en función de x "
                        + "(no puede contener y).");
            }
        }
        return new Datos(f, exacta, x0, y0, h, xf, n);
    }

    // Inicia el cálculo en segundo plano
    private void resolver(boolean mostrarDialogo) {
        final Datos d;
        try {
            d = leerDatos();
        } catch (IllegalArgumentException ex) {
            mostrarEstado("Error: " + ex.getMessage(), ROJO);
            if (mostrarDialogo) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Revisa los datos",
                        JOptionPane.WARNING_MESSAGE);
            }
            return;
        }
        if (trabajador != null && !trabajador.isDone()) {
            trabajador.cancel(true);
        }
        final long version = ++versionPeticion;
        mostrarEstado("Calculando " + d.n + " pasos…", GRIS);
        btnResolver.setEnabled(false);
        trabajador = new SwingWorker<Resultado, Void>() {
            @Override
            protected Resultado doInBackground() {
                return calcular(d);
            }

            @Override
            protected void done() {
                btnResolver.setEnabled(true);
                if (version != versionPeticion || isCancelled()) {
                    return;
                }
                try {
                    aplicar(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Throwable causa = ex.getCause();
                    String msg = causa == null ? ex.toString() : causa.getMessage();
                    mostrarEstado("Error inesperado: " + msg, ROJO);
                    if (mostrarDialogo) {
                        JOptionPane.showMessageDialog(VentanaPrincipal.this,
                                "No se pudo completar el cálculo:\n" + msg,
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        };
        trabajador.execute();
    }

    // Calcula Euler, RK4 y la referencia sin tocar la interfaz
    private static Resultado calcular(Datos d) {
        long t0 = System.nanoTime();
        double[] x = new double[d.n + 1];
        for (int i = 0; i <= d.n; i++) {
            x[i] = d.x0 + i * d.h;
        }
        double[] ye = Metodos.euler(d.f, d.x0, d.y0, d.h, d.n);
        double[] yr = Metodos.rk4(d.f, d.x0, d.y0, d.h, d.n);

        double[] ref = new double[d.n + 1];
        int m = 1;
        boolean hayExacta = d.exacta != null;
        if (hayExacta) {
            for (int i = 0; i <= d.n; i++) {
                ref[i] = d.exacta.evaluar(x[i], Double.NaN);
            }
        } else {
            // Si no hay fórmula exacta, usamos RK4 con paso más fino como referencia
            m = Math.max(1, Math.min(20, 2_000_000 / Math.max(1, d.n)));
            double[] fino = Metodos.rk4(d.f, d.x0, d.y0, d.h / m, d.n * m);
            for (int i = 0; i <= d.n; i++) {
                ref[i] = fino[i * m];
            }
        }

        Resultado r = new Resultado();
        r.d = d;
        r.x = x;
        r.ye = ye;
        r.yr = yr;
        r.ref = ref;
        r.hayExacta = hayExacta;
        r.m = m;
        r.ms = (System.nanoTime() - t0) / 1_000_000;
        return r;
    }

    // Pinta los resultados en gráfica, tabla, resumen y paso a paso
    private void aplicar(Resultado r) {
        fActual = r.d.f;
        xs = r.x;
        yEu = r.ye;
        yRk = r.yr;
        yRef = r.ref;
        hActual = r.d.h;
        refEsExacta = r.hayExacta;

        String nombreRef = r.hayExacta ? "Exacta" : "Referencia (RK4 fino)";
        chkRef.setText(r.hayExacta ? "Exacta" : "Referencia");
        grafica.setDatos(r.x, r.ye, r.yr, r.ref, nombreRef);
        grafica.setVisibles(chkEuler.isSelected(), chkRK4.isSelected(), chkRef.isSelected());
        llenarTabla(r.hayExacta);
        llenarResumen(r);
        if (!desdeDeslizador) {
            ajustarSlider(r.d.h);
        }
        spnPaso.setModel(new SpinnerNumberModel(0, 0, Math.max(0, r.d.n - 1), 1));
        actualizarPasos();

        boolean diverge = Analisis.hayNoFinitos(r.ye, r.yr);
        if (diverge) {
            mostrarEstado("Aviso: la solución numérica produjo valores no finitos (diverge). "
                    + "Prueba con un h más pequeño.", NARANJA);
        } else {
            mostrarEstado("Listo: " + r.d.n + " pasos con h = " + Metodos.num(r.d.h)
                    + " en " + r.ms + " ms.", VERDE);
        }
    }

    // Lee un número o expresión numérica (como 2*pi) desde una caja de texto
    private double leerNumero(JTextField campo, String nombre) {
        String t = campo.getText().trim().replace(',', '.');
        if (t.isEmpty()) {
            throw new IllegalArgumentException("Falta el valor de " + nombre + ".");
        }
        try {
            Expresion e = new Expresion(t);
            if (e.dependeDeX() || e.dependeDeY()) {
                throw new IllegalArgumentException(
                        "El valor de " + nombre + " debe ser un número (no puede usar x ni y).");
            }
            double v = e.evaluar(0, 0);
            if (!Metodos.finito(v)) {
                throw new IllegalArgumentException("El valor de " + nombre + " no es válido.");
            }
            return v;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Valor inválido en " + nombre + ": " + ex.getMessage());
        }
    }

    private void mostrarEstado(String texto, Color color) {
        lblEstado.setForeground(color);
        lblEstado.setText(texto);
    }

    private void llenarTabla(boolean hayExacta) {
        String[] cols = {"i", "x", "y Euler", "y RK4",
            hayExacta ? "y exacta" : "y referencia", "Error Euler", "Error RK4"};
        DefaultTableModel modelo = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        for (int i = 0; i < xs.length; i++) {
            modelo.addRow(new Object[]{
                i,
                Metodos.fmt(xs[i]),
                Metodos.fmt(yEu[i]),
                Metodos.fmt(yRk[i]),
                Metodos.fmt(yRef[i]),
                Metodos.fmtError(Math.abs(yRef[i] - yEu[i])),
                Metodos.fmtError(Math.abs(yRef[i] - yRk[i]))
            });
        }
        tabla.setModel(modelo);
        DefaultTableCellRenderer der = new DefaultTableCellRenderer();
        der.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c = 0; c < tabla.getColumnCount(); c++) {
            tabla.getColumnModel().getColumn(c).setCellRenderer(der);
            tabla.getColumnModel().getColumn(c).setPreferredWidth(c == 0 ? 55 : 110);
        }
    }

    private void llenarResumen(Resultado r) {
        double finalE = Analisis.errorFinal(r.ref, r.ye);
        double finalR = Analisis.errorFinal(r.ref, r.yr);
        double maxE = Analisis.errorMaximo(r.ref, r.ye);
        double maxR = Analisis.errorMaximo(r.ref, r.yr);
        int n = r.d.n;

        StringBuilder sb = new StringBuilder();
        sb.append("Pasos: n = ").append(n).append("   h = ").append(Metodos.num(r.d.h)).append("\n");
        sb.append("Referencia: ").append(r.hayExacta
                ? "solución exacta" : "RK4 con h/" + r.m + " (aprox.)").append("\n");
        double xUlt = xs[n];
        if (Math.abs(xUlt - r.d.xf) > 1e-9 * Math.max(1, Math.abs(r.d.xf))) {
            sb.append("x final real: ").append(Metodos.num(xUlt)).append("\n");
        }
        sb.append("\n");
        sb.append(String.format(Locale.US, "%-18s %11s %11s%n", "", "Euler", "RK4"));
        sb.append(String.format(Locale.US, "%-18s %11s %11s%n", "Error en x final",
                Metodos.fmtError(finalE), Metodos.fmtError(finalR)));
        sb.append(String.format(Locale.US, "%-18s %11s %11s%n", "Error máximo",
                Metodos.fmtError(maxE), Metodos.fmtError(maxR)));
        sb.append(String.format(Locale.US, "%-18s %11s %11s%n", "Valor en x final",
                Metodos.num(r.ye[n]), Metodos.num(r.yr[n])));
        sb.append("\n");
        if (!Metodos.finito(finalE) || !Metodos.finito(finalR)) {
            sb.append("Algún método produjo valores no finitos:\n");
            sb.append("el error no se puede medir con este h.\n");
        } else {
            double factor = Analisis.factorMejora(finalE, finalR);
            if (Metodos.finito(factor) && factor > 1) {
                sb.append(String.format(Locale.US,
                        "RK4 es unas %.0f veces más%nprecisa que Euler en x final.%n", factor));
            } else if (finalE == 0 && finalR == 0) {
                sb.append("Ambos métodos coinciden con la referencia.\n");
            } else if (Double.isInfinite(factor)) {
                sb.append("RK4 coincide con la referencia;\nEuler no.\n");
            }
        }
        sb.append("\nPrueba: reduce h a la mitad.\n");
        sb.append("Euler: error ≈ la mitad.\n");
        sb.append("RK4:   error ≈ 1/16.\n");
        txtResumen.setText(sb.toString());
        txtResumen.setCaretPosition(0);
    }

    private void actualizarPasos() {
        if (fActual == null || xs == null) {
            return;
        }
        int i = (Integer) spnPaso.getValue();
        if (i < 0 || i >= xs.length - 1) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Ecuación:  dy/dx = ").append(txtEcuacion.getText().trim()).append("\n");
        sb.append("Tamaño de paso:  h = ").append(Metodos.num(hActual)).append("\n\n");
        sb.append(Metodos.detalleEuler(fActual, xs[i], yEu[i], hActual, i));
        sb.append("\n--------------------------------------------------\n\n");
        sb.append(Metodos.detalleRK4(fActual, xs[i], yRk[i], hActual, i));
        sb.append("\n--------------------------------------------------\n\n");
        sb.append("Valores de referencia en x(i+1) = ").append(Metodos.num(xs[i + 1])).append(":\n");
        sb.append("  y referencia = ").append(Metodos.fmt(yRef[i + 1])).append("\n");
        sb.append("  y Euler      = ").append(Metodos.fmt(yEu[i + 1])).append("\n");
        sb.append("  y RK4        = ").append(Metodos.fmt(yRk[i + 1])).append("\n");
        sb.append("\n(Cada método continúa desde su propio valor y(i); por eso las cuentas\n");
        sb.append("de Euler y RK4 parten de valores distintos cuando i > 0.)\n");
        txtPasos.setText(sb.toString());
        txtPasos.setCaretPosition(0);
    }

    // Guarda la tabla en un archivo CSV
    private void exportarCsv() {
        if (xs == null) {
            mostrarEstado("Primero resuelve un problema para poder exportar.", NARANJA);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar tabla de iteraciones");
        fc.setSelectedFile(new File("iteraciones_edo.csv"));
        fc.setFileFilter(new FileNameExtensionFilter("Archivos CSV (*.csv)", "csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File archivo = fc.getSelectedFile();
        if (!archivo.getName().toLowerCase(Locale.ROOT).endsWith(".csv")) {
            archivo = new File(archivo.getParentFile(), archivo.getName() + ".csv");
        }
        try (PrintWriter out = new PrintWriter(archivo, "UTF-8")) {
            out.println("i,x,y_euler,y_rk4," + (refEsExacta ? "y_exacta" : "y_referencia")
                    + ",error_euler,error_rk4");
            for (int i = 0; i < xs.length; i++) {
                out.printf(Locale.US, "%d,%.12g,%.12g,%.12g,%.12g,%.12g,%.12g%n",
                        i, xs[i], yEu[i], yRk[i], yRef[i],
                        Math.abs(yRef[i] - yEu[i]), Math.abs(yRef[i] - yRk[i]));
            }
            mostrarEstado("Tabla exportada a " + archivo.getAbsolutePath(), VERDE);
        } catch (IOException ex) {
            mostrarEstado("No se pudo guardar el CSV: " + ex.getMessage(), ROJO);
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error al guardar",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // Guarda la gráfica en PNG
    private void guardarGrafica() {
        if (!grafica.hayDatos()) {
            mostrarEstado("Primero resuelve un problema para poder guardar la gráfica.", NARANJA);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar gráfica");
        fc.setSelectedFile(new File("grafica_edo.png"));
        fc.setFileFilter(new FileNameExtensionFilter("Imágenes PNG (*.png)", "png"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File archivo = fc.getSelectedFile();
        if (!archivo.getName().toLowerCase(Locale.ROOT).endsWith(".png")) {
            archivo = new File(archivo.getParentFile(), archivo.getName() + ".png");
        }
        try {
            grafica.guardarPng(archivo);
            mostrarEstado("Gráfica guardada en " + archivo.getAbsolutePath(), VERDE);
        } catch (IOException ex) {
            mostrarEstado("No se pudo guardar la imagen: " + ex.getMessage(), ROJO);
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error al guardar",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static final class Conv {
        double[] hs;
        int[] ns;
        double[] errE, errR;
        double ordenE, ordenR;
        boolean refExacta;
        String aviso;
    }

    // Calcula cómo baja el error al ir partiendo h a la mitad
    private void calcularConvergencia() {
        final Datos d;
        try {
            d = leerDatos();
        } catch (IllegalArgumentException ex) {
            mostrarEstado("Error: " + ex.getMessage(), ROJO);
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Revisa los datos",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (trabajadorConv != null && !trabajadorConv.isDone()) {
            trabajadorConv.cancel(true);
        }
        btnConvergencia.setEnabled(false);
        txtConv.setText("Calculando…");
        trabajadorConv = new SwingWorker<Conv, Void>() {
            @Override
            protected Conv doInBackground() {
                return calcularConvergencia(d);
            }

            @Override
            protected void done() {
                btnConvergencia.setEnabled(true);
                try {
                    mostrarConvergencia(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Throwable causa = ex.getCause();
                    txtConv.setText("No se pudo calcular: "
                            + (causa == null ? ex.toString() : causa.getMessage()));
                }
            }
        };
        trabajadorConv.execute();
    }

    private static Conv calcularConvergencia(Datos d) {
        int niveles = 5;
        while (niveles > 2 && (long) d.n * (1L << niveles) > 400_000) {
            niveles--;
        }
        int refDiv = 1 << niveles;

        double[] refFina = null;
        if (d.exacta == null) {
            double hFino = d.h / refDiv;
            int nFino = Metodos.pasosPara(d.x0, d.xf, hFino);
            refFina = Metodos.rk4(d.f, d.x0, d.y0, hFino, nFino);
        }

        Conv c = new Conv();
        c.hs = new double[niveles];
        c.ns = new int[niveles];
        c.errE = new double[niveles];
        c.errR = new double[niveles];
        c.refExacta = d.exacta != null;
        for (int k = 0; k < niveles; k++) {
            double hk = d.h / (1 << k);
            int nk = Metodos.pasosPara(d.x0, d.xf, hk);
            c.hs[k] = hk;
            c.ns[k] = nk;
            double[] ye = Metodos.euler(d.f, d.x0, d.y0, hk, nk);
            double[] yr = Metodos.rk4(d.f, d.x0, d.y0, hk, nk);
            double[] ref = new double[nk + 1];
            if (d.exacta != null) {
                for (int i = 0; i <= nk; i++) {
                    ref[i] = d.exacta.evaluar(d.x0 + i * hk, Double.NaN);
                }
            } else {
                int salto = 1 << (niveles - k);
                for (int i = 0; i <= nk; i++) {
                    ref[i] = refFina[i * salto];
                }
            }
            c.errE[k] = Analisis.errorFinal(ref, ye);
            c.errR[k] = Analisis.errorFinal(ref, yr);
        }
        if (niveles >= 2) {
            c.ordenE = Analisis.ordenEmpirico(c.errE[0], c.errE[1], 2);
            c.ordenR = Analisis.ordenEmpirico(c.errR[0], c.errR[1], 2);
        }
        if (niveles < 5) {
            c.aviso = "Se usaron " + niveles + " niveles (en vez de 5) para no exceder el "
                    + "límite de cálculo con este número de pasos.";
        }
        return c;
    }

    private void mostrarConvergencia(Conv c) {
        String[] cols = {"h", "n", "Error Euler", "Error RK4", "Factor Euler", "Factor RK4"};
        DefaultTableModel modelo = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int col) {
                return false;
            }
        };
        for (int k = 0; k < c.hs.length; k++) {
            double fE = k == 0 ? Double.NaN : Analisis.factorMejora(c.errE[k - 1], c.errE[k]);
            double fR = k == 0 ? Double.NaN : Analisis.factorMejora(c.errR[k - 1], c.errR[k]);
            modelo.addRow(new Object[]{
                Metodos.num(c.hs[k]),
                c.ns[k],
                Metodos.fmtError(c.errE[k]),
                Metodos.fmtError(c.errR[k]),
                k == 0 ? "—" : Metodos.num(fE),
                k == 0 ? "—" : Metodos.num(fR)
            });
        }
        tablaConv.setModel(modelo);
        DefaultTableCellRenderer der = new DefaultTableCellRenderer();
        der.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int col = 0; col < tablaConv.getColumnCount(); col++) {
            tablaConv.getColumnModel().getColumn(col).setCellRenderer(der);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Referencia: ").append(c.refExacta ? "solución exacta." : "RK4 con paso más fino (aprox.).").append("\n");
        sb.append("Al dividir h entre 2, el error debería dividirse entre 2^p.\n\n");
        if (Metodos.finito(c.ordenE)) {
            sb.append(String.format(Locale.US, "Orden estimado de Euler: p ≈ %.2f  (teoría: 1)%n", c.ordenE));
        }
        if (Metodos.finito(c.ordenR)) {
            sb.append(String.format(Locale.US, "Orden estimado de RK4:   p ≈ %.2f  (teoría: 4)%n", c.ordenR));
        }
        if (c.aviso != null) {
            sb.append("\n").append(c.aviso).append("\n");
        }
        sb.append("\nSi un factor se aleja de la teoría, suele ser porque el error ya está\n");
        sb.append("al nivel del redondeo de la computadora o de la referencia usada.\n");
        txtConv.setText(sb.toString());
        txtConv.setCaretPosition(0);
        mostrarEstado("Estudio de convergencia listo.", VERDE);
    }

    private static final String TEXTO_TEORIA
            = "PROBLEMA\n"
            + "  Resolver numéricamente  dy/dx = f(x, y)  con  y(x0) = y0.\n"
            + "  No se busca una fórmula para y(x): se calculan valores aproximados\n"
            + "  y(0), y(1), y(2)... avanzando de h en h.\n\n"
            + "MÉTODO DE EULER (1er orden)\n"
            + "  y(i+1) = y(i) + h · f(x(i), y(i))\n"
            + "  Usa UNA pendiente por paso: la del inicio del intervalo.\n"
            + "  Error global proporcional a h.\n\n"
            + "MÉTODO DE RUNGE-KUTTA 4 (RK4)\n"
            + "  k1 = f(x, y)\n"
            + "  k2 = f(x + h/2, y + k1·h/2)\n"
            + "  k3 = f(x + h/2, y + k2·h/2)\n"
            + "  k4 = f(x + h,   y + k3·h)\n"
            + "  y(i+1) = y(i) + (h/6)·(k1 + 2·k2 + 2·k3 + k4)\n"
            + "  Promedia CUATRO pendientes por paso (la de los extremos pesa 1 y\n"
            + "  la de los puntos medios pesa 2).\n"
            + "  Error global proporcional a h^4.\n\n"
            + "ERROR\n"
            + "  Error = | y referencia - y numérica |.\n"
            + "  Si escribes la solución exacta se usa esa. Si la dejas vacía, la\n"
            + "  referencia es RK4 con un paso 20 veces más pequeño.\n"
            + "  El error en cada fila ya incluye todo lo acumulado desde el primer paso.\n\n"
            + "CONVERGENCIA\n"
            + "  La pestaña Convergencia resuelve el mismo problema con h, h/2, h/4…\n"
            + "  y muestra cuánto baja el error. Si el método es de orden p, el error\n"
            + "  baja unas 2^p veces: 2 para Euler y 16 para RK4.\n\n"
            + "CÓMO ESCRIBIR LA ECUACIÓN\n"
            + "  Variables:  x (o t)  y  y\n"
            + "  Operadores: +  -  *  /  ^  y paréntesis. Se puede escribir 2x o 3(x+1).\n"
            + "  Funciones:  sin cos tan asin acos atan sinh cosh tanh\n"
            + "              exp  ln (natural)  log (base 10)  sqrt  abs\n"
            + "  Constantes: pi  e\n"
            + "  Usa punto decimal: 0.5\n\n"
            + "EJEMPLO DEL CUADERNO\n"
            + "  y' + y = x,  y(0) = 1,  h = 0.1   ->   dy/dx = x - y\n"
            + "  Euler:  y(0.1) = 0.9,   y(0.2) = 0.82\n"
            + "  RK4:    y(0.1) = 0.909675\n"
            + "  Exacta: y(x) = x - 1 + 2·e^(-x)   ->   y(0.1) = 0.909675\n\n"
            + "USO DE LA GRÁFICA\n"
            + "  Pasa el ratón sobre la gráfica para ver los valores de cada método\n"
            + "  en ese paso. Mueve el deslizador de h para ver cómo cambia el error.\n"
            + "  El botón «Guardar gráfica» exporta la imagen en PNG y «Exportar CSV»\n"
            + "  guarda la tabla para abrirla en Excel.\n";
}