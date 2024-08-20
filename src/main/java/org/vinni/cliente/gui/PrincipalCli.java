package org.vinni.cliente.gui;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * author: Vinni 2024
 */
public class PrincipalCli extends javax.swing.JFrame {

    private final int PORT = 12345;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String clientName; // Nombre del cliente
    private DefaultListModel clientListModel;
    private JList clientList;

    /**
     * Creates new form Principal1
     */
    public PrincipalCli() {
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
        clientListModel = new DefaultListModel();
        clientList = new JList(clientListModel);
        JScrollPane clientListScrollPane = new JScrollPane(clientList);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        bConectar.setFont(new java.awt.Font("Segoe UI", 0, 14));
        bConectar.setText("CONECTAR CON SERVIDOR");
        bConectar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bConectarActionPerformed(evt);
            }
        });
        getContentPane().add(bConectar);
        bConectar.setBounds(260, 40, 210, 40);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 14));
        jLabel1.setForeground(new java.awt.Color(204, 0, 0));
        jLabel1.setText("CLIENTE TCP : DFRACK");
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
        btEnviar.setText("Enviar");
        btEnviar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btEnviarActionPerformed(evt);
            }
        });
        getContentPane().add(btEnviar);
        btEnviar.setBounds(327, 160, 120, 27);

        clientListScrollPane.setBounds(30, 330, 180, 100);
        getContentPane().add(clientListScrollPane);

        JButton sendToClientButton = new JButton("Enviar a Cliente Seleccionado");
        sendToClientButton.setBounds(220, 330, 220, 40);
        sendToClientButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                String selectedClient = (String) clientList.getSelectedValue();
                if (selectedClient != null) {
                    String message = mensajeTxt.getText();
                    if (!message.isEmpty()) {
                        out.println("TO:" + selectedClient + ":" + message);
                        mensajeTxt.setText("");
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "No se ha seleccionado un cliente");
                }
            }
        });
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

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new PrincipalCli().setVisible(true);
            }
        });
    }

    private void conectar() {
        // Preguntar al usuario por un nombre
        clientName = JOptionPane.showInputDialog(this, "Ingrese su nombre:");

        if (clientName == null || clientName.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre no puede estar vacío");
            return;
        }

        JOptionPane.showMessageDialog(this, "Conectando con servidor");
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        if (socket == null || socket.isClosed()) {
                            socket = new Socket("localhost", PORT); // Asume que el servidor está en localhost y escucha en el puerto 12345
                            out = new PrintWriter(socket.getOutputStream(), true);
                            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            out.println(clientName); // Envía el nombre del cliente al servidor

                            mensajesTxt.append("Conectado al servidor como " + clientName + "\n");

                            // Iniciar un hilo para leer los mensajes del servidor
                            new Thread(new Runnable() {
                                public void run() {
                                    String fromServer;
                                    try {
                                        while ((fromServer = in.readLine()) != null) {
                                            if (fromServer.startsWith("CLIENT_LIST")) {
                                                updateClientList(fromServer);
                                            } else {
                                                mensajesTxt.append("Servidor: " + fromServer + "\n");
                                            }
                                        }
                                    } catch (IOException ex) {
                                        mensajesTxt.append("Desconectado del servidor. Intentando reconectar...\n");
                                    }
                                }
                            }).start();

                            break; // Salir del bucle si la conexión fue exitosa
                        }
                    } catch (IOException e) {
                        mensajesTxt.append("Error conectando al servidor. Intentando de nuevo en 5 segundos...\n");
                        try {
                            Thread.sleep(5000); // Esperar 5 segundos antes de intentar reconectar
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
        }).start();
    }

    private void enviarMensaje() {
        String message = mensajeTxt.getText();
        if (message != null && !message.trim().isEmpty()) {
            out.println(message);
            mensajeTxt.setText("");
        }
    }

    private void updateClientList(final String clientListString) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                clientListModel.clear();
                String[] clients = clientListString.split(",");
                for (int i = 1; i < clients.length; i++) {
                    clientListModel.addElement(clients[i]);
                }
            }
        });
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
