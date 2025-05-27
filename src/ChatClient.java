package src;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 聊天客户端类
 * 负责提供图形界面和处理与服务器的通信
 */
public class ChatClient extends JFrame {
    // 添加字体和颜色常量
    private static final Font CHAT_FONT = new Font("微软雅黑", Font.PLAIN, 14);
    private static final Color SYSTEM_MESSAGE_COLOR = new Color(128, 128, 128);
    private static final Color MY_MESSAGE_COLOR = new Color(0, 0, 255);
    private static final Color OTHER_MESSAGE_COLOR = new Color(0, 0, 0);
    
    // 服务器地址和端口
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8888;
      // 网络组件
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
      // 界面组件
    private JTextPane chatArea;          // 聊天记录显示区域
    private JTextField messageField;      // 消息输入框
    private JButton sendButton;          // 发送按钮
    private JList<String> userList;      // 用户列表
    private DefaultListModel<String> userListModel;  // 用户列表模型
    private JLabel currentUserLabel;     // 当前用户标签
    
    // 用户名
    private String username;
    
    /**
     * 构造函数，初始化聊天客户端
     */
    public ChatClient() {
        // 设置窗口标题
        super("多用户网络聊天室");
        
        // 获取用户名
        username = JOptionPane.showInputDialog(this, "请输入您的昵称:", "登录", JOptionPane.QUESTION_MESSAGE);
        if (username == null || username.trim().isEmpty()) {
            username = "游客" + (int)(Math.random() * 1000);
        }
        
        // 初始化网络连接
        initNetworking();
        
        // 初始化图形界面
        initGUI();
        
        // 创建消息接收线程
        new MessageReceiver().start();
        
        // 发送登录消息
        sendLoginMessage();
    }
    
    /**
     * 初始化网络连接
     */
    private void initNetworking() {
        try {
            // 连接到服务器
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            
            // 初始化输入输出流，指定UTF-8编码
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            
            System.out.println("已连接到服务器");
        } catch (IOException e) {
            System.out.println("连接服务器失败: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "无法连接到服务器: " + e.getMessage(), "连接错误", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
    
    /**
     * 初始化图形界面
     */
    private void initGUI() {
        // 设置窗口属性
        setSize(700, 500);
        setMinimumSize(new Dimension(600, 400));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);  // 居中显示
        
        // 创建面板和组件
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
          // 聊天记录区域
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setFont(CHAT_FONT); // 设置字体
        chatArea.setBackground(new Color(250, 250, 250));
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
    // 消息输入区域
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        messageField = new JTextField();
        messageField.addActionListener(e -> sendMessage());
        
        // 创建包含发送和退出按钮的面板
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        sendButton = new JButton("发送");
        sendButton.addActionListener(e -> sendMessage());
        
        // 创建退出按钮
        JButton exitButton = new JButton("退出");
        exitButton.addActionListener(e -> exitApplication());
        
        // 添加按钮到按钮面板
        buttonPanel.add(sendButton);
        buttonPanel.add(exitButton);
        
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);
          // 用户列表区域
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setBorder(BorderFactory.createTitledBorder("在线用户"));
        userScrollPane.setPreferredSize(new Dimension(150, 0));
        
        // 当前用户显示区域
        currentUserLabel = new JLabel("当前用户: " + username);
        currentUserLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        currentUserLabel.setForeground(new Color(0, 100, 0));
        currentUserLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        currentUserLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // 创建右侧面板，包含当前用户标签和用户列表
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(currentUserLabel, BorderLayout.NORTH);
        rightPanel.add(userScrollPane, BorderLayout.CENTER);
        rightPanel.setPreferredSize(new Dimension(150, 0));
        
        // 将组件添加到主面板
        mainPanel.add(chatScrollPane, BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);
        mainPanel.add(rightPanel, BorderLayout.EAST);
        
        // 将主面板添加到窗口
        add(mainPanel);
          // 窗口关闭事件处理
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });
        
        // 显示窗口
        setVisible(true);
        
        // 设置初始焦点
        messageField.requestFocus();
    }
    
    /**
     * 发送登录消息
     */
    private void sendLoginMessage() {
        out.println("LOGIN|" + username);
    }    /**
     * 发送聊天消息
     */
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            // 发送消息到服务器
            out.println("CHAT|" + username + ":" + message);
            
            // 在自己的聊天区域显示消息
            String time = getCurrentTime();
            String displayMessage = "[" + time + "] " + username + ": " + message + "\n";
            appendToChat(displayMessage, MY_MESSAGE_COLOR);
            System.out.println("在UI上显示自己的消息: " + displayMessage);
            
            // 清空输入框
            messageField.setText("");
            messageField.requestFocus();
        }
    }
      /**
     * 断开连接
     */
    private void disconnect() {
        try {
            // 如果可能，发送登出消息
            if (socket != null && !socket.isClosed() && out != null) {
                out.println("LOGOUT|" + username);
            }
            
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 退出应用程序
     */
    private void exitApplication() {
        // 显示确认对话框
        int option = JOptionPane.showConfirmDialog(
            this,
            "确定要退出聊天室吗？",
            "退出确认",
            JOptionPane.YES_NO_OPTION
        );
        
        // 如果用户确认，则断开连接并退出
        if (option == JOptionPane.YES_OPTION) {
            disconnect();
            dispose();
            System.exit(0);
        }
    }
      /**
     * 获取当前时间的格式化字符串
     */
    private String getCurrentTime() {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        return format.format(new Date());
    }    /**
     * 处理收到的系统消息
     */
    private void handleSystemMessage(String content) {
        String time = getCurrentTime();
        String displayMessage = "[" + time + "] [系统] " + content + "\n";
        appendToChat(displayMessage, SYSTEM_MESSAGE_COLOR);
        System.out.println("显示系统消息: " + displayMessage);
    }    /**
     * 处理收到的聊天消息
     */
    private void handleChatMessage(String message) {
        // 消息格式: CHAT|username:content
        int colonIndex = message.indexOf(":");
        if (colonIndex > 0) {
            String sender = message.substring(5, colonIndex);
            String content = message.substring(colonIndex + 1);
            
            // 如果是自己发送的消息，不再显示（已经在发送时显示了）
            if (!sender.equals(username)) {
                String time = getCurrentTime();
                String displayMessage = "[" + time + "] " + sender + ": " + content + "\n";
                appendToChat(displayMessage, OTHER_MESSAGE_COLOR);
                System.out.println("显示他人消息: " + displayMessage);
            } else {
                System.out.println("收到自己的消息，不重复显示: " + sender + ": " + content);
            }
        }
    }
      /**
     * 在聊天区域添加带颜色的文本
     */
    private void appendToChat(String message, Color color) {
        try {
            // 获取文档
            javax.swing.text.Document doc = chatArea.getDocument();
            
            // 保存当前的文档样式
            javax.swing.text.StyleContext sc = javax.swing.text.StyleContext.getDefaultStyleContext();
            javax.swing.text.AttributeSet aset = sc.addAttribute(javax.swing.text.SimpleAttributeSet.EMPTY, 
                javax.swing.text.StyleConstants.Foreground, color);
            aset = sc.addAttribute(aset, javax.swing.text.StyleConstants.FontFamily, CHAT_FONT.getFamily());
            aset = sc.addAttribute(aset, javax.swing.text.StyleConstants.FontSize, CHAT_FONT.getSize());
            
            // 在文档末尾插入文本
            int len = doc.getLength();
            chatArea.setCaretPosition(len);
            chatArea.setCharacterAttributes(aset, false);
            
            // 使用插入而不是替换
            doc.insertString(len, message, aset);
            
            // 自动滚动到底部
            chatArea.setCaretPosition(doc.getLength());
            
            // 调试信息
            System.out.println("添加消息到聊天区域: " + message.trim());
        } catch (Exception e) {
            System.err.println("添加文本到聊天区域异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 更新在线用户列表
     */
    private void updateOnlineUsers(String userListStr) {
        // 清空当前列表
        userListModel.clear();
        
        // 解析用户列表字符串 (格式: USERLIST|user1,user2,...)
        String[] users = userListStr.substring(9).split(",");
        for (String user : users) {
            if (!user.isEmpty()) {
                userListModel.addElement(user);
            }
        }
    }
    
    /**
     * 消息接收线程，负责接收并处理服务器发来的消息
     */
    private class MessageReceiver extends Thread {
        @Override
        public void run() {
            try {
                String message;
                  // 持续读取服务器发来的消息
                while ((message = in.readLine()) != null) {
                    System.out.println("收到消息: " + message);
                    
                    final String msg = message;
                    
                    // 使用SwingUtilities.invokeLater在EDT线程中更新UI
                    SwingUtilities.invokeLater(() -> {
                        if (msg.startsWith("SYSTEM|")) {
                            handleSystemMessage(msg.substring(7));
                        } else if (msg.startsWith("CHAT|")) {
                            System.out.println("处理聊天消息: " + msg);
                            handleChatMessage(msg);
                        } else if (msg.startsWith("USERLIST|")) {
                            updateOnlineUsers(msg);
                        } else {
                            System.out.println("未知消息格式: " + msg);
                        }
                    });
                }
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    System.out.println("从服务器接收消息时发生错误: " + e.getMessage());
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(ChatClient.this, 
                            "与服务器的连接已断开", "连接错误", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }
        }
    }
    
    /**
     * 主方法，启动客户端
     */
    public static void main(String[] args) {
        // 使用SwingUtilities.invokeLater在EDT线程中创建并显示GUI
        SwingUtilities.invokeLater(() -> new ChatClient());
    }
}
