import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ChatServer extends JFrame {
    private static final int SERVER_PORT = 12345;//服务器端口为12345

    private static JTextArea chatBox;

    public ChatServer() {
        super("聊天室服务器端");

        chatBox = new JTextArea();
        chatBox.setEditable(false);

        JScrollPane jsp = new JScrollPane(chatBox);
        jsp.setPreferredSize(new Dimension(400, 300));
        chatBox.setFont(new Font("宋体", Font.PLAIN, 12));

        setLayout(new BorderLayout());
        add(jsp, BorderLayout.CENTER);

        setDefaultCloseOperation((JFrame.DISPOSE_ON_CLOSE));
        pack();
        setVisible(true);
    }

    private static class ClientInfo {
        private Socket socket;//客户端端口
        private String nickname;//客户端昵称
        private String groupName;//客户端组名

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

    private static List<ClientInfo> clientSockets = new ArrayList<>();

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private PrintWriter writer;

        public String Name;
        public String GroupName;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        public void appendMessage(String message) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    chatBox.append(message + "\n");
                }
            });
        }

        @Override
        public void run() {
            try {
                Scanner scanner = new Scanner(clientSocket.getInputStream(), "UTF-8");//获取客户端的请求
                writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true);//向客户端发送信息

                writer.println("欢迎来到JIANG的聊天室！");

                writer.println("请输入您的昵称：");
                Name = scanner.nextLine();
                writer.println("欢迎" + Name + "进入聊天室");

                System.out.println("client " + clientSocket + ":" + Name);
                appendMessage("client " + clientSocket + ":" + Name);

                writer.println("请输入您要加入的群组：");
                GroupName = scanner.nextLine();
                writer.println("欢迎加入群组‘" + GroupName + "’");

                System.out.println("Client " + clientSocket + " joined group:" + GroupName);
                appendMessage("Client " + clientSocket + " joined group:" + GroupName);

                ClientInfo clientInfo = new ClientInfo(clientSocket, Name, GroupName);
                clientSockets.add(clientInfo);

                //进行消息发送方式的分离
                while (true) {
                    String message = scanner.nextLine();
                    if (message.startsWith("@p")) {
                        handlePersonToPersonMessage(message);
                    } else if (message.startsWith("@g")) {
                        handleGroupMessage(message, GroupName);
                    } else if (message.startsWith("@b")) {
                        handleBroadcastMessage(message);
                    } else {
                        System.out.println("Received message from " + Name + ":" + message);
                        sendToAllClients("用户昵称" + Name + ":" + message);
                        appendMessage("用户昵称" + Name + ":" + message);
                    }
                }
            } catch (IOException e) {//捕获输入输出操作过程中可能发生的异常情况，例如文件读写错误、网络连接问题
                e.printStackTrace();
            } finally {
                clientSockets.remove(clientSocket);
                System.out.println("Client " + clientSocket + " disconnected");
                sendToAllClients("Client " + clientSocket + " disconnected");
            }
        }

        private void handlePersonToPersonMessage(String message) {
            String[] parts = message.split(" ", 3);//将消息切成三片，分别是报头、接收者及消息内容
            if (parts.length == 3) {
                String receiver = parts[1];
                String p2pMessage = parts[2];

                System.out.println("Received Person-to-Person message from " + Name + " to " + receiver + ":" + p2pMessage);
                sendPersonToPerson(receiver, "用户昵称 " + Name + "（私聊消息）：" + p2pMessage);
                sendPersonToPerson(Name, "用户昵称 " + Name + "（私聊消息）：" + p2pMessage);
                appendMessage("用户昵称 " + Name + "（私聊:" + receiver + "）：" + p2pMessage);
            }
        }

        private void handleGroupMessage(String message, String groupName) {
            String[] parts = message.split(" ", 2);
            if (parts.length == 2) {
                String groupMessage = parts[1];

                System.out.println("Received group message from " + Name + "(group:" + groupName + "):" + groupMessage);
                sendToGroup(groupName, "用户昵称" + Name + "（群组消息）：" + groupMessage);
                appendMessage("用户昵称" + Name + "（群组消息）：" + groupMessage);
            }
        }

        private void handleBroadcastMessage(String message) {
            String[] parts = message.split(" ", 2);
            if (parts.length == 2) {
                String broadcastMessage = parts[1];

                System.out.println("Received broadcast message from " + Name + ":" + broadcastMessage);
                sendToAllClients("用户昵称 " + Name + "（广播消息）:" + broadcastMessage);
                appendMessage("用户昵称 " + Name + "（广播消息）:" + broadcastMessage);
            }
        }

        private void sendToAllClients(String message) {
            for (ClientInfo clientInfo : clientSockets) {
                try {//向所有客户端输出消息
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

        private void sendToGroup(String groupName, String message) {
            for (ClientInfo clientInfo : clientSockets) {
                if (clientInfo.getGroupName().equals(groupName)) {
                    try {
                        PrintWriter socketWriter = new PrintWriter(new OutputStreamWriter(clientInfo.getSocket().getOutputStream(), "UTF-8"), true);
                        socketWriter.println(message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private void sendPersonToPerson(String targetName, String message) {
            for (ClientInfo clientInfo : clientSockets) {
                if (clientInfo.getNickname().equals(targetName)) {
                    try {
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ChatServer();
            }
        });

        try {
            //创建一个ServerSocket对象，将其绑定到指定的服务器端口和本地地址
            //ServerSocket是java实现服务器端的套接字，监听指定的端口，接收客户端连接请求，并与客户端进行通信
            //SERVER_PORT是服务器绑定端口，为12345
            //backlog设为0表示使用默认值，即系统根据具体实现来选择一个合适的默认队列长度
            //InetAddress.getByName("localhost")获取本机IP
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT, 0, InetAddress.getByName("localhost"));

            System.out.println("Server started on port " + SERVER_PORT);

            while (true) {
                //serverSocket.accept()方法是一个阻塞调用，意味着程序会在此处暂停，直到有客户端请求连接才会继续执行。
                Socket clientSocket = serverSocket.accept();

                System.out.println("New client connected in " + clientSocket);
                //创建了一个新的线程clientThread，并将其与客户端的连接clientSocket关联。
                // 通过start()方法启动线程后，线程将执行ClientHandler对象中定义的任务
                Thread clientThread = new Thread(new ClientHandler(clientSocket));
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
