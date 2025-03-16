package practica16;
import com.fazecast.jSerialComm.SerialPort;
import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.util.Scanner;

public class Practica16 {
    private static SerialPort puerto;
    private static boolean conectado = false;
    private static Thread lecturaThread;

    public static void main(String[] args) {
        JFrame frame = crearVentanaPrincipal();
        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Conexión", crearPanelConexion());
        tabbedPane.addTab("Acerca de...", crearPanelAcercaDe());

        frame.add(tabbedPane);
        frame.setVisible(true);
    }

    private static JFrame crearVentanaPrincipal() {
        JFrame frame = new JFrame("Adquisición de Datos");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null); 
        return frame;
    }
    
    private static JPanel crearPanelConexion() {
        JPanel conexionPanel = new JPanel(new GridBagLayout()); 
        conexionPanel.setBackground(new Color(173, 216, 230));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JTextField tempField = crearCampoTexto(Color.RED);
        JTextField humField = crearCampoTexto(Color.RED);

        JButton conectarButton = new JButton("Conectar");
        conectarButton.addActionListener(e -> manejarConexion(conectarButton, tempField, humField));

        JButton cerrarButton = crearBotonCerrar();

        // Añadir componentes al panel
        gbc.gridx = 0;
        gbc.gridy = 0;
        conexionPanel.add(new JLabel("Temperatura °C:"), gbc);
        gbc.gridx = 1;
        conexionPanel.add(tempField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        conexionPanel.add(new JLabel("Humedad relativa %:"), gbc);
        gbc.gridx = 1;
        conexionPanel.add(humField, gbc);

        // Espacio entre los campos de texto y los botones
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        conexionPanel.add(Box.createVerticalStrut(20), gbc); // Espaciador vertical

        // Botón de conectar/desconectar
        gbc.gridy = 3;
        conexionPanel.add(conectarButton, gbc);

        // Botón de cerrar
        gbc.gridy = 4;
        conexionPanel.add(cerrarButton, gbc);

        return conexionPanel;
    }

    private static JPanel crearPanelAcercaDe() {
        JPanel acercaDePanel = new JPanel(new GridBagLayout());
        acercaDePanel.setBackground(new Color(173, 216, 230));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridy = 0;

        agregarEtiqueta(acercaDePanel, "Interfaz hombre-máquina", new Font("Arial", Font.BOLD, 14), gbc);
        agregarEtiqueta(acercaDePanel, "SISTEMAS PROGRAMABLES", new Font("Arial", Font.PLAIN, 13), gbc);
        agregarEtiqueta(acercaDePanel, "Edwin López Santiago", null, gbc);

        gbc.insets = new Insets(20, 10, 10, 10);
        JButton cerrarButton = crearBotonCerrar();
        gbc.gridy++;
        acercaDePanel.add(cerrarButton, gbc);

        return acercaDePanel;
    }

    private static void agregarEtiqueta(JPanel panel, String texto, Font fuente, GridBagConstraints gbc) {
        JLabel etiqueta = new JLabel(texto, JLabel.CENTER);
        if (fuente != null) etiqueta.setFont(fuente);
        gbc.gridy++;
        panel.add(etiqueta, gbc);
    }

    private static JTextField crearCampoTexto(Color color) {
        JTextField campo = new JTextField(10);
        campo.setEditable(false);
        campo.setHorizontalAlignment(JTextField.RIGHT);
        campo.setForeground(color);
        return campo;
    }

    private static JButton crearBotonCerrar() {
        JButton cerrarButton = new JButton("Cerrar");
        cerrarButton.addActionListener(e -> System.exit(0));
        return cerrarButton;
    }

    private static void manejarConexion(JButton conectarButton, JTextField tempField, JTextField humField) {
        if (!conectado) {
            configurarPuerto(tempField, humField);
            conectarButton.setText(conectado ? "Desconectar" : "Conectar");
        } else {
            desconectarPuerto(tempField, humField);
            conectarButton.setText("Conectar");
        }
    }

    private static void configurarPuerto(JTextField tempField, JTextField humField) {
        puerto = SerialPort.getCommPort("COM4");
        puerto.setComPortParameters(9600, 8, 1, 0);
        puerto.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);

        if (puerto.openPort()) {
            conectado = true;
            iniciarLectura(tempField, humField);
        } else {
            mostrarMensaje("Error conectando al puerto");
        }
    }

    private static void iniciarLectura(JTextField tempField, JTextField humField) {
        lecturaThread = new Thread(() -> {
            try (InputStream in = puerto.getInputStream(); Scanner scanner = new Scanner(in)) {
                while (conectado && scanner.hasNextLine()) {
                    procesarDato(scanner.nextLine(), tempField, humField);
                }
            } catch (Exception ex) {
                mostrarMensaje("Error leyendo datos");
            }
        });
        lecturaThread.start();
    }

    private static void procesarDato(String data, JTextField tempField, JTextField humField) {
        if (data.contains("Humedad")) {
            humField.setText(data.split(": ")[1]);
        } else if (data.contains("Temperatura")) {
            tempField.setText(data.split(": ")[1]);
        }
    }

    private static void desconectarPuerto(JTextField tempField, JTextField humField) {
        conectado = false;
        if (lecturaThread != null && lecturaThread.isAlive()) {
            try {
                lecturaThread.join();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        if (puerto != null && puerto.isOpen()) puerto.closePort();
        tempField.setText("");
        humField.setText("");
    }

    private static void mostrarMensaje(String mensaje) {
        JOptionPane.showMessageDialog(null, mensaje);
    }
}
