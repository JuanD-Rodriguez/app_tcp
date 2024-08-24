/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cliente;
import javax.swing.*;
import java.io.*;
import java.net.Socket;

/**
 *
 * @author juan.rodriguez
 */
public class cliente extends javax.swing.JFrame {


    private final int PORT = 12345;
    private final String SAVE_DIR = "C:\\ArchivosRecibidos\\"; // Ruta donde se guardarán los archivos
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String clientName; // Nombre del cliente
    private DefaultListModel<String> clientListModel;
    private JList<String> clientList;

    public cliente() {
        initComponents();
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        this.setTitle("Cliente ");
        bConectar = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        mensajesTxt = new javax.swing.JTextArea();
        mensajeTxt = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        btEnviar = new javax.swing.JButton();
        clientListModel = new DefaultListModel<>();
        clientList = new JList<>(clientListModel);
        JScrollPane clientListScrollPane = new JScrollPane(clientList);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        bConectar.setFont(new java.awt.Font("Segoe UI", 0, 14));
        bConectar.setText("CONECTAR CON SERVIDOR");
        bConectar.addActionListener(evt -> bConectarActionPerformed(evt));
        getContentPane().add(bConectar);
        bConectar.setBounds(260, 40, 210, 40);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 14));
        jLabel1.setForeground(new java.awt.Color(204, 0, 0));
        jLabel1.setText("CLIENTE");
        getContentPane().add(jLabel1);
        jLabel1.setBounds(110, 10, 250, 17);

        mensajesTxt.setColumns(20);
        mensajesTxt.setRows(5);
        mensajesTxt.setEnabled(false);
        jScrollPane1.setViewportView(mensajesTxt);

        getContentPane().add(jScrollPane1);
        jScrollPane1.setBounds(30, 210, 410, 110);

        mensajeTxt.setFont(new java.awt.Font("Verdana", 0, 14));
        getContentPane().add(mensajeTxt);
        mensajeTxt.setBounds(40, 120, 350, 30);

        jLabel2.setFont(new java.awt.Font("Verdana", 0, 14));
        jLabel2.setText("Mensaje:");
        getContentPane().add(jLabel2);
        jLabel2.setBounds(20, 90, 120, 30);

        btEnviar.setFont(new java.awt.Font("Verdana", 0, 14));
        btEnviar.setText("Enviar mensaje (elija el usuario)");
        btEnviar.addActionListener(evt -> btEnviarActionPerformed(evt));
        getContentPane().add(btEnviar);
        btEnviar.setBounds(327, 160, 120, 27);

        clientListScrollPane.setBounds(30, 330, 180, 100);
        getContentPane().add(clientListScrollPane);

        JButton sendToClientButton = new JButton("Enviar archivo (elija el usuario)");
        sendToClientButton.setBounds(220, 330, 220, 40);
        sendToClientButton.addActionListener(evt -> enviarArchivoACliente());
        getContentPane().add(sendToClientButton);

        setSize(new java.awt.Dimension(491, 475));
        setLocationRelativeTo(null);
    }

    private void bConectarActionPerformed(java.awt.event.ActionEvent evt) {
        conectar();
    }

private void btEnviarActionPerformed(java.awt.event.ActionEvent evt) {
    enviarMensaje();
}

private void enviarMensaje() {
    String message = mensajeTxt.getText().trim();
    String selectedClient = clientList.getSelectedValue(); // Obtener el cliente seleccionado

    if (message.isEmpty()) {
        JOptionPane.showMessageDialog(this, "El mensaje no puede estar vacío");
        return;
    }

    if (selectedClient == null) {
        JOptionPane.showMessageDialog(this, "Seleccione un cliente para enviar el mensaje.");
        return;
    }

    out.println("MSG_TO:" + selectedClient + ":" + message); // Enviar mensaje al servidor
    mensajeTxt.setText(""); // Limpiar la caja de texto después de enviar
}


    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new cliente().setVisible(true));
    }
private void conectar() {
    clientName = JOptionPane.showInputDialog(this, "Ingrese su nombre:");

    if (clientName == null || clientName.trim().isEmpty()) {
        JOptionPane.showMessageDialog(this, "El nombre no puede estar vacío");
        return;
    }

    new Thread(() -> {
        while (true) {
            try {
                if (socket == null || socket.isClosed()) {
                    socket = new Socket("localhost", PORT);  // Intentar conectarse al servidor
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out.println(clientName);  // Enviar el nombre del cliente al servidor

                    mensajesTxt.append("Conectado al servidor como " + clientName + "\n");

                    // Iniciar un hilo para escuchar mensajes del servidor
                    new Thread(() -> {
                        String fromServer;
                        try {
                            while ((fromServer = in.readLine()) != null) {
                                if (fromServer.startsWith("CLIENT_LIST")) {
                                    updateClientList(fromServer);
                                } else if (fromServer.startsWith("RECEIVE_FILE:")) {
                                    recibirArchivo(fromServer);
                                } else if (fromServer.startsWith("MSG_FROM:")) {
                                    String[] parts = fromServer.split(":", 3);
                                    String sender = parts[1];
                                    String message = parts[2];
                                    mensajesTxt.append(sender + ": " + message + "\n");
                                } else {
                                    mensajesTxt.append("Servidor: " + fromServer + "\n");
                                }
                            }
                        } catch (IOException ex) {
                            mensajesTxt.append("Desconectado del servidor. Intentando reconectar...\n");
                            try {
                                socket.close();  // Asegurarse de cerrar el socket
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();

                    break;  // Salir del bucle si se establece la conexión
                }
            } catch (IOException e) {
                mensajesTxt.append("Error conectando al servidor. Intentando de nuevo en 5 segundos...\n");
                try {
                    Thread.sleep(5000);  // Esperar 5 segundos antes de intentar reconectar
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }).start();
}

private void updateClientList(final String clientListString) {
    SwingUtilities.invokeLater(() -> {
        clientListModel.clear();
        String[] clients = clientListString.split(",");
        for (int i = 1; i < clients.length; i++) {
            if (!clientListModel.contains(clients[i])) {
                clientListModel.addElement(clients[i]);
            }
        }
    });
}



    private void enviarArchivoACliente() {
        String selectedClient = clientList.getSelectedValue();
        if (selectedClient == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un cliente de la lista.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            new Thread(() -> {
                try {
                    out.println("SEND_FILE:" + selectedClient + ":" + file.getName());
                    out.println(file.length());

                    FileInputStream fis = new FileInputStream(file);
                    OutputStream os = socket.getOutputStream();
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                    os.flush();
                    fis.close();

                    mensajesTxt.append("Archivo " + file.getName() + " enviado a " + selectedClient + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                    mensajesTxt.append("Error enviando archivo: " + e.getMessage() + "\n");
                }
            }).start();
        }
    }

    private void recibirArchivo(String message) throws IOException {
        String[] parts = message.split(":");
        String sender = parts[1];
        String fileName = parts[2];
        long fileSize = Long.parseLong(parts[3]);

        // Asegúrate de que la carpeta SAVE_DIR existe
        File dir = new File(SAVE_DIR);
        if (!dir.exists()) {
            dir.mkdirs(); // Crear el directorio si no existe
        }

        // Guardar el archivo en la carpeta SAVE_DIR
        File file = new File(SAVE_DIR + fileName);
        FileOutputStream fos = new FileOutputStream(file);

        byte[] buffer = new byte[4096];
        int bytesRead;
        InputStream is = socket.getInputStream();
        while (fileSize > 0 && (bytesRead = is.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
            fos.write(buffer, 0, bytesRead);
            fileSize -= bytesRead;
        }

        fos.close();
        mensajesTxt.append("Archivo recibido de " + sender + ": " + fileName + "\n");
    }


    // Variables declaration - do not modify
    private javax.swing.JButton bConectar;
    private javax.swing.JButton btEnviar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea mensajesTxt;
    private JTextField mensajeTxt;
    // End of variables declaration
}
