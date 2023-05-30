import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ChatServer {
    private static final int SERVER_PORT = 12345;

    //clientSockets是一个包含ClientInfo对象的集合。每个ClientInfo对象包含有关客户端连接的信息，如客户端的昵称和对应的Socket对象。
    private static List<ClientInfo> clientSockets = new ArrayList<>();


    public static void main(String[] args) {
        try {
            //创建了一个ServerSocket对象，并将其绑定到指定的服务器端口和本地地址。
            //ServerSocket是Java中用于实现服务器端的套接字。它监听指定的端口，接受客户端的连接请求，并与客户端进行通信。
            //SERVER_PORT是一个整数变量，表示服务器要绑定的端口号。
            //InetAddress.getByName("localhost")用于获取本地主机的IP地址。
            //设为0表示使用默认值，即系统根据具体实现来选择一个合适的默认队列长度。
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT, 0, InetAddress.getByName("localhost"));

            System.out.println("Chat Server started on port " + SERVER_PORT);

            while (true) {
                //ServerSocket类的一个方法，用于等待并接受客户端的连接请求。当有客户端请求连接时，该方法会返回一个新的Socket对象，
                //accept()方法是一个阻塞调用，意味着程序会在此处暂停，直到有客户端请求连接才会继续执行。
                Socket clientSocket = serverSocket.accept();

                System.out.println("New client connected: " + clientSocket);
                //创建了一个新的线程clientThread，并将其与客户端的连接clientSocket关联。
                // 通过start()方法启动线程后，线程将执行ClientHandler对象中定义的任务
                Thread clientThread = new Thread(new ClientHandler(clientSocket));
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientInfo {
        private Socket socket;
        private String nickname;
        private String groupName;

        public ClientInfo(Socket socket, String nickname, String groupName) {
            this.socket = socket;
            this.nickname = nickname;
            this.groupName = groupName;
        }

        public Socket getSocket() {
            return socket;
        }

        public String getNickname() {
            return nickname;
        }

        public String getGroupName() {
            return groupName;
        }

        public Socket getSocketByNickname(String nickname) {
            for (ClientInfo clientInfo : clientSockets) {
                if (clientInfo.getNickname().equals(nickname)) {
                    return clientInfo.getSocket();
                }
            }
            return null;
        }
    }
    //实现Runnable接口，可以将ClientHandler类的对象作为参数传递给线程，并在线程中执行run()方法中定义的任务。
    // 这样可以使得每个客户端连接都拥有独立的ClientHandler实例，处理与客户端的通信逻辑。
    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private PrintWriter writer;

        public String Name;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                Scanner scanner = new Scanner(clientSocket.getInputStream(), "UTF-8");
                writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true);

                //writer = new PrintWriter(clientSocket.getOutputStream(), true);
                writer.println("欢迎来到聊天室!");

                writer.println("请输入您的昵称:");
                Name = scanner.nextLine();
                writer.println("欢迎 " + Name + "!");
                System.out.println("Client " + clientSocket + " : " + Name);

                writer.println("请输入您要加入的群组:");
                String groupName = scanner.nextLine();
                writer.println("欢迎加入群组 '" + groupName + "'!");

                System.out.println("Client " + clientSocket + " joined group: " + groupName);

                ClientInfo clientInfo = new ClientInfo(clientSocket, Name, groupName);
                clientSockets.add(clientInfo);


                while (true) {
                    String message = scanner.nextLine();
                    if (message.startsWith("@p")) {
                        handleP2PMessage(message);
                    } else if (message.startsWith("@g")) {
                        handleGroupMessage(message, groupName);
                    } else if (message.startsWith("@b")) {
                        handleBroadcastMessage(message);
                    } else {
                        System.out.println("Received message from " + Name + ": " + message);
                        sendToAllClients("用户昵称 " + Name + ": " + message);
                    }
                }
                //它表示输入/输出操作过程中可能发生的异常情况，例如文件读写错误、网络连接问题等。
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                clientSockets.remove(clientSocket);
                System.out.println("Client disconnected: " + clientSocket);
                sendToAllClients("Client " + clientSocket + " disconnected.");
            }
        }


        private void handleP2PMessage(String message) {
            String[] parts = message.split(" ", 3);
            if (parts.length == 3) {
                String recipientName = parts[1];
                String p2pMessage = parts[2];

                System.out.println("Received P2P message from " + Name + " to " + recipientName + ": " + p2pMessage);
                sendP2PMessage(recipientName, "用户昵称 " + Name + " (私聊消息): " + p2pMessage);
            }
        }

        private void handleGroupMessage(String message, String senderGroup) {
            String[] parts = message.split(" ", 2);
            if (parts.length == 2) {
                String groupMessage = parts[1];

                System.out.println("Received group message from " + Name + " (group " + senderGroup + "): " + groupMessage);
                sendToGroup(senderGroup, "用户昵称 " + Name + " (群聊消息): " + groupMessage);

            }
        }

        private void handleBroadcastMessage(String message) {
            String[] parts = message.split(" ", 2);
            if (parts.length == 2) {
                String broadcastMessage = parts[1];

                System.out.println("Received broadcast message from " + Name + ": " + broadcastMessage);
                sendToAllClients("用户昵称 " + Name + " (广播消息): " + broadcastMessage);
            }
        }

        private void sendToAllClients(String message) {
            for (ClientInfo clientInfo : clientSockets) {
                try {
                    // PrintWriter socketWriter = new PrintWriter(clientInfo.getSocket().getOutputStream(), true);
                    PrintWriter socketWriter = new PrintWriter(new OutputStreamWriter(clientInfo.getSocket().getOutputStream(), "UTF-8"), true);

                    socketWriter.println(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        private void sendToGroup(String groupName, String message) {
            for (ClientInfo clientInfo : clientSockets) {
                if (clientInfo.getGroupName().equals(groupName)) {
                    try {
                        //PrintWriter socketWriter = new PrintWriter(clientInfo.getSocket().getOutputStream(), true);

                        //通过clientInfo.getSocket().getOutputStream()获取客户端的输出流，
                        //使用OutputStreamWriter和指定的字符编码（UTF-8）创建PrintWriter对象socketWriter，用于向客户端发送消息。
                        //使用socketWriter.println(message)将message发送给客户端
                        PrintWriter socketWriter = new PrintWriter(new OutputStreamWriter(clientInfo.getSocket().getOutputStream(), "UTF-8"), true);

                        socketWriter.println(message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private void sendP2PMessage(String recipientName, String message) {
            for (ClientInfo clientInfo : clientSockets) {
                //for循环遍历clientSockets列表，找到与接收者昵称匹配的客户端。
                if (clientInfo.getNickname().equals(recipientName)) {
                    try {
                        // PrintWriter socketWriter = new PrintWriter(clientInfo.getSocket().getOutputStream(), true);


                        //通过clientInfo.getSocket().getOutputStream()获取客户端的输出流，
                        //使用OutputStreamWriter和指定的字符编码（UTF-8）创建PrintWriter对象socketWriter，用于向客户端发送消息。
                        //使用socketWriter.println(message)将message发送给客户端
                        PrintWriter socketWriter = new PrintWriter(new OutputStreamWriter(clientInfo.getSocket().getOutputStream(), "UTF-8"), true);

                        socketWriter.println(message);
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }
}

