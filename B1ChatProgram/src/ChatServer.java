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

    //聊天室服务器端的构造函数
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

    //定义一个内部类ClientInfo，用于保存客户端的信息
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

    //定义一个静态的List<ClientInfo>对象clientSockets，用于保存所有客户端的信息
    private static List<ClientInfo> clientSockets = new ArrayList<>();

    //定义一个内部类ClientHandler，用于处理客户端的请求
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
                //使用Scanner和PrintWriter对象来获取客户端的输入输出流
                Scanner scanner = new Scanner(clientSocket.getInputStream(), "UTF-8");//获取客户端的请求
                writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true);//向客户端发送信息

                //向客户端输出欢迎信息
                writer.println("欢迎来到JIANG的聊天室！");

                //进行客户端的初始化
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

                //将客户端的信息保存到clientSockets中
                ClientInfo clientInfo = new ClientInfo(clientSocket, Name, GroupName);
                clientSockets.add(clientInfo);

                //进行消息发送方式的分离，根据消息的报头来判断消息的类型
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
                //当客户端断开连接时，将其从clientSockets中移除
                clientSockets.remove(clientSocket);
                System.out.println("Client " + clientSocket + " disconnected");
                sendToAllClients("Client " + clientSocket + " disconnected");
                appendMessage("Client " + clientSocket + " disconnected");
            }
        }

        //处理客户端的私聊消息，格式为@p 接收者 消息内容
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

        //处理客户端的群聊消息，格式为@g 消息内容
        private void handleGroupMessage(String message, String groupName) {
            String[] parts = message.split(" ", 2);
            if (parts.length == 2) {
                String groupMessage = parts[1];

                System.out.println("Received group message from " + Name + "(group:" + groupName + "):" + groupMessage);
                sendToGroup(groupName, "用户昵称" + Name + "（群组消息）：" + groupMessage);
                appendMessage("用户昵称" + Name + "（群组消息）：" + groupMessage);
            }
        }

        //处理客户端的广播消息，格式为@b 消息内容
        private void handleBroadcastMessage(String message) {
            String[] parts = message.split(" ", 2);
            if (parts.length == 2) {
                String broadcastMessage = parts[1];

                System.out.println("Received broadcast message from " + Name + ":" + broadcastMessage);
                sendToAllClients("用户昵称 " + Name + "（广播消息）:" + broadcastMessage);
                appendMessage("用户昵称 " + Name + "（广播消息）:" + broadcastMessage);
            }
        }

        //向所有客户端发送消息
        private void sendToAllClients(String message) {
            //遍历clientSockets中的所有客户端
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

        //向指定群组的所有客户端发送消息
        private void sendToGroup(String groupName, String message) {
            //遍历clientSockets中的所有客户端
            for (ClientInfo clientInfo : clientSockets) {
                //如果客户端的组名与指定的组名相同，则向该客户端发送消息
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

        //向指定客户端发送消息
        private void sendPersonToPerson(String targetName, String message) {
            //遍历clientSockets中的所有客户端
            for (ClientInfo clientInfo : clientSockets) {
                //如果客户端的昵称与指定的昵称相同，则向该客户端发送消息
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

            //服务器端的主线程，用于监听客户端的连接请求
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
