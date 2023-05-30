import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.*;

public class ChatClientGUI extends JFrame {
    private static final String SERVER_HOST = "localhost";//服务器地址
    private static final int SERVER_PORT = 12345;//服务器端口

    private Socket socket;//客户端套接字
    private PrintWriter writer;//输出流
    private Scanner scanner;

    private JTextArea chatBox;//聊天框
    private JTextField messageField;//消息输入框

    public ChatClientGUI() {
        super("聊天室客户端");

        chatBox = new JTextArea();
        chatBox.setEditable(false);//设置为不可编辑

        JScrollPane jsp = new JScrollPane(chatBox);
        jsp.setPreferredSize(new Dimension(400, 300));
        chatBox.setFont(new Font("宋体", Font.PLAIN, 12));

        messageField = new JTextField();
        messageField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage(messageField.getText());
            }
        });

        JButton send = new JButton("发送信息");
        send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage(messageField.getText());
            }
        });

        JPanel input = new JPanel();
        input.setLayout(new BorderLayout());
        input.add(messageField, BorderLayout.CENTER);
        input.add(send, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(jsp, BorderLayout.CENTER);
        add(input, BorderLayout.SOUTH);

        setDefaultCloseOperation((JFrame.DISPOSE_ON_CLOSE));
        pack();
        setVisible(true);

        connectToServer();

    }

    private void connectToServer() {
        try {
            //通过socket连接服务器端口
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            scanner = new Scanner(new InputStreamReader(socket.getInputStream(), "UTF-8"));

            //初始欢迎
            String welcomeMessage = scanner.nextLine();
            appendMessage(welcomeMessage);

            Thread messageListener = new Thread(new MessageListener());//创建一个新的线程来监听消息
            messageListener.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String message) {//发送消息并清空输入框
        writer.println(message);
        ;
        messageField.setText("");
    }

    private void appendMessage(String message) {//在消息框中进行消息追加
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                chatBox.append(message + "\n");
            }
        });
    }


    private class MessageListener implements Runnable {
        @Override
        //使用一个无限循环来持续监听消息。
        // 通过scanner.nextLine()从输入流中读取下一行消息，并将该消息传递给appendMessage方法，以将其追加到聊天区域。
        public void run() {
            try {
                while (true) {
                    String message = scanner.nextLine();
                    appendMessage(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                    ;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ChatClientGUI();
            }
        });
    }
}
