/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package servidor;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 *
 * @author juan.rodriguez
 */
public class servidor extends javax.swing.JFrame {
    private final int PORT = 12345;
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new ArrayList<>();
    private DefaultListModel<String> clientListModel;
    private JList<String> clientList;

    public servidor() {
        initComponents();
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        this.setTitle("Servidor ...");

        bIniciar = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        mensajesTxt = new JTextArea();
        jScrollPane1 = new javax.swing.JScrollPane();
        clientListModel = new DefaultListModel<>();
        clientList = new JList<>(clientListModel);
        JScrollPane clientScrollPane = new JScrollPane(clientList);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        bIniciar.setFont(new java.awt.Font("Segoe UI", 0, 18));
        bIniciar.setText("INICIAR SERVIDOR");
        bIniciar.addActionListener(evt -> iniciarServidor());
        getContentPane().add(bIniciar);
        bIniciar.setBounds(100, 90, 250, 40);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 14));
        jLabel1.setForeground(new java.awt.Color(204, 0, 0));
        jLabel1.setText("SERVIDOR TCP : HOEL");
        getContentPane().add(jLabel1);
        jLabel1.setBounds(150, 10, 160, 17);

        mensajesTxt.setColumns(25);
        mensajesTxt.setRows(5);
        jScrollPane1.setViewportView(mensajesTxt);
        getContentPane().add(jScrollPane1);
        jScrollPane1.setBounds(20, 160, 410, 70);

        clientScrollPane.setBounds(20, 240, 200, 100);
        getContentPane().add(clientScrollPane);

        setSize(new java.awt.Dimension(491, 390));
        setLocationRelativeTo(null);
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new servidor().setVisible(true));
    }

    private void bIniciarActionPerformed(java.awt.event.ActionEvent evt) {
        iniciarServidor();
    }

    private void iniciarServidor() {
        JOptionPane.showMessageDialog(this, "Iniciando servidor");
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                mensajesTxt.append("Servidor TCP en ejecución en el puerto " + PORT + "\n");
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    clients.add(clientHandler);
                    new Thread(clientHandler).start();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                mensajesTxt.append("Error en el servidor: " + ex.getMessage() + "\n");
            }
        }).start();
    }

    private void sendFileToClient(String sender, String targetClientName, String fileName, long fileSize, DataInputStream dis) {
        for (ClientHandler client : clients) {
            if (client.getClientName().equals(targetClientName)) {
                client.sendFile(sender, fileName, fileSize, dis);
                break;
            }
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;
        private String clientName;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            try {
                this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                this.out = new PrintWriter(clientSocket.getOutputStream(), true);
                this.clientName = in.readLine();
                SwingUtilities.invokeLater(() -> {
                    clientListModel.addElement(clientName);
                    broadcastClientList();
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public String getClientName() {
            return clientName;
        }
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("SEND_FILE:")) {
                        String[] parts = message.split(":", 3);
                        final String targetClient = parts[1]; // Marcar como final o asegurar que no se modifique
                        final String fileName = parts[2]; // Marcar como final o asegurar que no se modifique
                        final long fileSize = Long.parseLong(in.readLine()); // Marcar como final o asegurar que no se modifique

                        // Mantener la lambda sin modificar
                        SwingUtilities.invokeLater(() -> 
                            mensajesTxt.append(clientName + " envió un archivo a " + targetClient + "\n")
                        );

                        // Llamar a la función que maneja el envío del archivo
                        sendFileToClient(clientName, targetClient, fileName, fileSize, new DataInputStream(clientSocket.getInputStream()));
                    } else {
                        final String finalMessage = message; // Variable efectivamente final
                        SwingUtilities.invokeLater(() -> 
                            mensajesTxt.append(clientName + ": " + finalMessage + "\n")
                        );
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                    SwingUtilities.invokeLater(() -> {
                        clientListModel.removeElement(clientName);
                        broadcastClientList();
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void sendFile(String sender, String fileName, long fileSize, DataInputStream dis) {
            try {
                out.println("RECEIVE_FILE:" + sender + ":" + fileName + ":" + fileSize);
                OutputStream os = clientSocket.getOutputStream(); // Usar OutputStream para escribir bytes
                byte[] buffer = new byte[4096];
                int bytesRead;
                while (fileSize > 0 && (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                    os.write(buffer, 0, bytesRead); // Escribir los bytes en el OutputStream
                    fileSize -= bytesRead;
                }
                os.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void broadcastClientList() {
            StringBuilder clientListString = new StringBuilder("CLIENT_LIST");
            for (int i = 0; i < clientListModel.size(); i++) {
                clientListString.append(",").append(clientListModel.getElementAt(i));
            }
            for (ClientHandler client : clients) {
                client.out.println(clientListString.toString());
            }
        }
    }

    // Variables declaration - do not modify
    private javax.swing.JButton bIniciar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JTextArea mensajesTxt;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration
}
