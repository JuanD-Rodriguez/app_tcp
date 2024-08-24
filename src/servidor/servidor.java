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
        jLabel1.setText("SERVIDOR TCP");
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

    private void iniciarServidor() {
        JOptionPane.showMessageDialog(this, "Iniciando servidor");
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                mensajesTxt.append("Servidor TCP en ejecución en el puerto " + PORT + "\n");
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler clientHandler = new ClientHandler(clientSocket);
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

                synchronized (clients) {
                    // Verifica si el cliente ya está en la lista para evitar duplicados
                    boolean clientExists = clients.stream().anyMatch(c -> c.getClientName().equals(this.clientName));
                    if (!clientExists) {
                        clients.add(this);
                        broadcastClientList();
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    if (!clientListModel.contains(clientName)) {
                        clientListModel.addElement(clientName);
                        mensajesTxt.append("Nuevo cliente conectado: " + clientName + "\n");
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
                if (message.startsWith("SEND_FILE:")) {
                    // Manejo de archivos
                    String[] parts = message.split(":", 3);
                    String targetClient = parts[1];
                    String fileName = parts[2];
                    long fileSize = Long.parseLong(in.readLine());

                    SwingUtilities.invokeLater(() -> 
                        mensajesTxt.append(clientName + " envió un archivo a " + targetClient + "\n")
                    );

                    sendFileToClient(clientName, targetClient, fileName, fileSize, new DataInputStream(clientSocket.getInputStream()));
                } else if (message.startsWith("MSG_TO:")) {
                    // Manejo de mensajes
                    String[] parts = message.split(":", 3);
                    String targetClient = parts[1];
                    String msgContent = parts[2];

                    sendMessageToClient(clientName, targetClient, msgContent);
                } else {
                    final String finalMessage = message;
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
                synchronized (clients) {
                    clients.remove(this);
                    broadcastClientList();
                }
                SwingUtilities.invokeLater(() -> {
                    clientListModel.removeElement(clientName);
                    mensajesTxt.append("Cliente desconectado: " + clientName + "\n");
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendMessageToClient(String sender, String targetClientName, String message) {
        for (ClientHandler client : clients) {
            if (client.getClientName().equals(targetClientName)) {
                client.out.println("MSG_FROM:" + sender + ":" + message);
                break;
            }
        }
    }

        private void broadcastClientList() {
            StringBuilder clientListString = new StringBuilder("CLIENT_LIST");
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    clientListString.append(",").append(client.getClientName());
                }
            }
            
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    client.out.println(clientListString.toString());
                }
            }
        }

        private void sendFile(String sender, String fileName, long fileSize, DataInputStream dis) {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }
    }

    // Variables declaration - do not modify
    private javax.swing.JButton bIniciar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JTextArea mensajesTxt;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration
}
