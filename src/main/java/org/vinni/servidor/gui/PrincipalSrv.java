package org.vinni.servidor.gui;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Author: Vinni
 */
public class PrincipalSrv extends javax.swing.JFrame {
    private final int PORT = 12345;
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new ArrayList<ClientHandler>();
    private DefaultListModel clientListModel;
    private JList clientList;

    /**
     * Creates new form Principal1
     */
    public PrincipalSrv() {
        initComponents();
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        this.setTitle("Servidor ...");

        bIniciar = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        mensajesTxt = new JTextArea();
        jScrollPane1 = new javax.swing.JScrollPane();
        clientListModel = new DefaultListModel(); // Eliminado el operador diamante
        clientList = new JList(clientListModel);  // Eliminado el operador diamante
        JScrollPane clientScrollPane = new JScrollPane(clientList);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        bIniciar.setFont(new java.awt.Font("Segoe UI", 0, 18));
        bIniciar.setText("INICIAR SERVIDOR");
        bIniciar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bIniciarActionPerformed(evt);
            }
        });
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
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new PrincipalSrv().setVisible(true);
            }
        });
    }

    private void bIniciarActionPerformed(java.awt.event.ActionEvent evt) {
        iniciarServidor();
    }

    private void iniciarServidor() {
        JOptionPane.showMessageDialog(this, "Iniciando servidor");
        new Thread(new Runnable() {
            public void run() {
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
            }
        }).start();
    }

    private void sendMessageToClient(String message, String targetClientName) {
        for (ClientHandler client : clients) {
            if (client.getClientName().equals(targetClientName)) {
                client.out.println(message);
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
                this.clientName = in.readLine(); // Asume que el primer mensaje del cliente es su nombre
                
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        clientListModel.addElement(clientName);
                        broadcastClientList();
                    }
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
                    if (message.startsWith("TO:")) {
                        // Extraer el destinatario y el mensaje
                        final String[] parts = message.split(":", 3);
                        final String targetClient = parts[1];
                        final String actualMessage = parts[2];
                        
                        // Mostrar el mensaje en el servidor
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                mensajesTxt.append(clientName + " -> " + targetClient + ": " + actualMessage + "\n");
                            }
                        });
                        
                        // Enviar el mensaje al cliente destinatario
                        sendMessageToClient(clientName + ": " + actualMessage, targetClient);
                    } else {
                        // Mostrar cualquier otro mensaje directamente en el servidor
                        final String finalMessage = message; // Necesario para usar dentro del Runnable
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                mensajesTxt.append(clientName + ": " + finalMessage + "\n");
                            }
                        });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            clientListModel.removeElement(clientName);
                            broadcastClientList();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
