import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;


class ChatClientGUI extends JFrame {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;

    private Socket socket;
    private PrintWriter writer;
    private Scanner scanner;

    private JTextArea chatArea;
    private JTextField messageField;

    public ChatClientGUI() {
        super("聊天客户端");


        chatArea = new JTextArea();
        chatArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        chatArea.setFont(new Font("宋体", Font.PLAIN, 12));
        messageField = new JTextField();
        messageField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage(messageField.getText());
            }
        });

        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage(messageField.getText());
            }
        });

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        pack();
        setVisible(true);

        connectToServer();
    }


    private void connectToServer() {
        try {

            socket = new Socket(SERVER_HOST, SERVER_PORT);
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            scanner = new Scanner(new InputStreamReader(socket.getInputStream(), "UTF-8"));

            //0
            String welcomeMessage = scanner.nextLine();
            appendMessage(welcomeMessage);

            //
            Thread messageListenerThread = new Thread(new MessageListener());
            messageListenerThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //发送消息并清空输入框
    private void sendMessage(String message) {
        writer.println(message);
        messageField.setText("");
    }
    //将接收到的消息追加到聊天区域中
    private void appendMessage(String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                chatArea.append(message + "\n");
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

                new ChatClientGUI();
                new ChatClientGUI();
                new ChatClientGUI();
                // 可以创建多个聊天窗口，模拟多个用户
            }
        });

    }

}
