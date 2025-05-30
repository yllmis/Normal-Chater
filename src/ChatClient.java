package src;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;

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
    private PrintWriter out;    // 界面组件
    private JTextPane chatArea;          // 聊天记录显示区域
    private JTextField messageField;      // 消息输入框
    private JButton sendButton;          // 发送按钮
    private JList<String> userList;      // 用户列表
    private DefaultListModel<String> userListModel;  // 用户列表模型
    private JLabel currentUserLabel;     // 当前用户标签
    private JList<String> roomList;      // 房间列表
    private DefaultListModel<String> roomListModel;  // 房间列表模型
    private JLabel currentRoomLabel;     // 当前房间标签
    private JButton joinRoomButton;      // 加入房间按钮
    private JButton leaveRoomButton;     // 离开房间按钮    // 用户名和当前房间
    private String username;
    private String currentRoomId = "";
    private Map<String, String> roomNameToIdMap = new HashMap<>(); // 房间名称到ID的映射
    
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
        // 创建菜单栏
        createMenuBar();
        
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
        inputPanel.add(buttonPanel, BorderLayout.EAST);        // 用户列表区域
        userListModel = new DefaultListModel<>();

        
        // 房间列表区域
        roomListModel = new DefaultListModel<>();
        roomList = new JList<>(roomListModel);
        roomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane roomScrollPane = new JScrollPane(roomList);
        roomScrollPane.setBorder(BorderFactory.createTitledBorder("房间列表"));
        roomScrollPane.setPreferredSize(new Dimension(150, 120));


        userList = new JList<>(userListModel);
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setBorder(BorderFactory.createTitledBorder("在线用户"));
        userScrollPane.setPreferredSize(new Dimension(150, 120));

        // 使用CardLayout切换房间列表和用户列表
        JPanel cardPanel = new JPanel(new CardLayout());
        cardPanel.add(roomScrollPane, "roomList");
        cardPanel.add(userScrollPane, "userList");

        // 房间操作按钮
        joinRoomButton = new JButton("加入房间");
        leaveRoomButton = new JButton("离开房间");
        joinRoomButton.addActionListener(e -> joinSelectedRoom());
        leaveRoomButton.addActionListener(e -> leaveCurrentRoom());
        leaveRoomButton.setEnabled(false);

        roomButtonPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        roomButtonPanel.add(joinRoomButton);
        roomButtonPanel.add(leaveRoomButton);

        // 添加创建房间按钮
        createRoomButton = new JButton("创建房间");
        createRoomButton.addActionListener(e -> createNewRoom());
        roomButtonPanel.add(createRoomButton);

        // 添加房间号输入框
        roomIdField = new JTextField(); // 保存到成员变量
        roomIdField.setToolTipText("输入房间号直接加入");
        joinByIdButton = new JButton("输入房间号加入"); // 保存按钮引用
        joinByIdButton.addActionListener(e -> joinRoomById(roomIdField.getText().trim()));

        JPanel joinByIdPanel = new JPanel(new BorderLayout(5, 0));
        joinByIdPanel.add(roomIdField, BorderLayout.CENTER);
        joinByIdPanel.add(joinByIdButton, BorderLayout.EAST);
        roomButtonPanel.add(joinByIdPanel);

        // 添加列表切换按钮
        JButton toggleListButton = new JButton("显示用户列表");
        toggleListButton.addActionListener(e -> {
            CardLayout cl = (CardLayout)(cardPanel.getLayout());
            if (toggleListButton.getText().equals("显示用户列表")) {
                cl.show(cardPanel, "userList");
                toggleListButton.setText("显示房间列表");
            } else {
                cl.show(cardPanel, "roomList");
                toggleListButton.setText("显示用户列表");
            }
        });
        roomButtonPanel.add(toggleListButton);

        // 当前用户显示区域
        currentUserLabel = new JLabel("当前用户: " + username);
        currentUserLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        currentUserLabel.setForeground(new Color(0, 100, 0));
        currentUserLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        currentUserLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // 当前房间显示区域
        currentRoomLabel = new JLabel("当前房间: 未加入");
        currentRoomLabel.setFont(new Font("微软雅黑", Font.BOLD, 12));
        currentRoomLabel.setForeground(new Color(100, 0, 100));
        currentRoomLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        currentRoomLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // 创建右侧面板
        JPanel rightPanel = new JPanel(new BorderLayout());

        // 顶部信息面板
        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        infoPanel.add(currentUserLabel);
        infoPanel.add(currentRoomLabel);

        // 主内容面板
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(cardPanel, BorderLayout.CENTER);
        contentPanel.add(roomButtonPanel, BorderLayout.SOUTH);

        rightPanel.add(infoPanel, BorderLayout.NORTH);
        rightPanel.add(contentPanel, BorderLayout.CENTER);
        rightPanel.setPreferredSize(new Dimension(160, 0));

        // 保存CardLayout引用
        cardLayout = (CardLayout) cardPanel.getLayout();
        
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
        
        // 设置初始焦点和状态
        messageField.requestFocus();
        messageField.setEnabled(false); // 初始时禁用消息输入
        sendButton.setEnabled(false);   // 初始时禁用发送按钮
    }
      /**
     * 发送登录消息
     */
    private void sendLoginMessage() {
        out.println("LOGIN|" + username);
    }
    
    /**
     * 创建菜单栏
     */
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // 创建帮助菜单
        JMenu helpMenu = new JMenu("帮助");
        helpMenu.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        
        // 创建关于菜单项
        JMenuItem aboutMenuItem = new JMenuItem("关于");
        aboutMenuItem.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        aboutMenuItem.addActionListener(e -> showAboutDialog());
        
        // 将菜单项添加到帮助菜单
        helpMenu.add(aboutMenuItem);
        
        // 将帮助菜单添加到菜单栏
        menuBar.add(helpMenu);
        
        // 设置菜单栏
        setJMenuBar(menuBar);
    }
    
    /**
     * 显示关于对话框
     */
    private void showAboutDialog() {
        String aboutMessage = 
            "多用户网络聊天室\n\n" +
            "版本: 1.0\n" +
            "开发语言: Java\n" +
            "功能特性:\n" +
            "• 多用户在线聊天\n" +
            "• 房间创建与管理\n" +
            "• 用户列表显示\n" +
            "• 实时消息同步\n\n" +
            "开发时间: 2025年\n" +
            "技术支持: Java Socket + Swing GUI\n"  ;
        
        JOptionPane.showMessageDialog(
            this,
            aboutMessage,
            "关于 - 多用户网络聊天室",
            JOptionPane.INFORMATION_MESSAGE
        );
    }/**
     * 发送聊天消息
     */
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            // 检查是否已加入房间
            if (currentRoomId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请先加入一个房间再发送消息", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
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
    private CardLayout cardLayout; // 用于切换房间/用户列表

    private void handleSystemMessage(String content) {
        String time = getCurrentTime();
        String displayMessage = "[" + time + "] [系统] " + content + "\n";
        appendToChat(displayMessage, SYSTEM_MESSAGE_COLOR);
        System.out.println("显示系统消息: " + displayMessage);
        
        // 处理房间相关的系统消息
        if (content.startsWith("成功加入房间:")) {
            // 提取房间名称
            String roomName = content.substring(7).trim();
            currentRoomLabel.setText("当前房间: " + roomName);
            joinRoomButton.setEnabled(false);
            leaveRoomButton.setEnabled(true);
            messageField.setEnabled(true);
            sendButton.setEnabled(true);
            roomIdField.setEnabled(false); // 禁用房间号输入框
            joinByIdButton.setEnabled(false); // 禁用"输入房间号加入"按钮
            createRoomButton.setEnabled(false); // 禁用创建房间按钮
            cardLayout.show(cardPanel, "userList"); // 切换到用户列表
        } else if (content.startsWith("房间创建成功，房间ID: ")) {
            // 自动加入新创建的房间
            String roomId = content.substring("房间创建成功，房间ID: ".length()).trim();
            joinRoomById(roomId);
        } else if (content.equals("已离开房间")) {
            currentRoomId = "";
            currentRoomLabel.setText("当前房间: 未加入");
            joinRoomButton.setEnabled(true);
            leaveRoomButton.setEnabled(false);
            messageField.setEnabled(false);
            sendButton.setEnabled(false);
            userListModel.clear(); // 清空房间用户列表
            roomIdField.setEnabled(true); // 启用房间号输入框
            joinByIdButton.setEnabled(true); // 启用"输入房间号加入"按钮
            createRoomButton.setEnabled(true); // 启用创建房间按钮
            // 强制切换回房间列表并重置切换按钮状态
            cardLayout.show(cardPanel, "roomList");
            // 遍历roomButtonPanel的子组件找到toggleListButton
            for (Component comp : roomButtonPanel.getComponents()) {
                if (comp instanceof JButton) {
                    JButton button = (JButton)comp;
                    if (button.getText().equals("显示房间列表")) {
                        button.setText("显示用户列表");
                        break;
                    }
                }
            }
        }
    }/**
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
            Document doc = chatArea.getDocument();
            
            // 保存当前的文档样式
            StyleContext sc =  StyleContext.getDefaultStyleContext();
            AttributeSet aset = sc.addAttribute( SimpleAttributeSet.EMPTY, 
                 StyleConstants.Foreground, color);
            aset = sc.addAttribute(aset,  StyleConstants.FontFamily, CHAT_FONT.getFamily());
            aset = sc.addAttribute(aset,  StyleConstants.FontSize, CHAT_FONT.getSize());
            
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
     * 加入选中的房间
     */
    private void joinSelectedRoom() {
        String selectedRoom = roomList.getSelectedValue();
        if (selectedRoom != null) {
            // 解析房间信息 (格式: 房间名称 (ID: roomId, 人数: count))
            String roomId = extractRoomId(selectedRoom);
            if (roomId != null) {
                joinRoomById(roomId);
            }
        } else {
            JOptionPane.showMessageDialog(this, "请先选择一个房间", "提示", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    /**
     * 通过房间号加入房间
     * @param roomId 房间ID
     */
    private JPanel cardPanel; // CardLayout容器面板
    private JTextField roomIdField; // 保存房间号输入框引用
    private JButton joinByIdButton; // 保存"输入房间号加入"按钮引用
    private JButton createRoomButton; // 创建房间按钮
    private JPanel roomButtonPanel; // 房间操作按钮面板

    private void joinRoomById(String roomId) {
        if (!roomId.isEmpty()) {
            currentRoomId = roomId;
            out.println("JOINROOM|" + roomId);
            roomIdField.setText(""); // 直接使用保存的引用清空输入框
        } else {
            appendToChat("系统: 请输入有效的房间号", SYSTEM_MESSAGE_COLOR);
        }
    }
    
    /**
     * 创建新房间
     */
    private void createNewRoom() {
        String roomName = JOptionPane.showInputDialog(this, "请输入新房间名称:", "创建房间", JOptionPane.QUESTION_MESSAGE);
        if (roomName != null && !roomName.trim().isEmpty()) {
            out.println("CREATEROOM|" + roomName);
            // 服务器会返回SYSTEM消息，在handleSystemMessage中处理自动加入
        }
    }
    
    /**
     * 离开当前房间
     */
private void leaveCurrentRoom() {
    if (!currentRoomId.isEmpty()) {
        out.println("LEAVEROOM|" + currentRoomId);
        currentRoomId = "";
        currentRoomLabel.setText("当前房间: 未加入");
        joinRoomButton.setEnabled(true);
        leaveRoomButton.setEnabled(false);
        messageField.setEnabled(false);
        sendButton.setEnabled(false);
        userListModel.clear();
        roomIdField.setEnabled(true);
        joinByIdButton.setEnabled(true);
        createRoomButton.setEnabled(true);
        // 强制切换回房间列表
        cardLayout.show(cardPanel, "roomList");
        // 重置切换按钮状态
        for (Component comp : roomButtonPanel.getComponents()) {
            if (comp instanceof JButton && ((JButton)comp).getText().equals("显示房间列表")) {
                ((JButton)comp).setText("显示用户列表");
                break;
            }
        }
    }
}
  /**
     * 从房间显示文本中提取房间ID
     */
    private String extractRoomId(String roomText) {
        // 新的房间显示格式: "房间名称 (人数: count/10)"
        // 提取房间名称，然后从映射中获取房间ID
        int parenthesesIndex = roomText.indexOf(" (人数:");
        if (parenthesesIndex != -1) {
            String roomName = roomText.substring(0, parenthesesIndex);
            return roomNameToIdMap.get(roomName);
        }
        return null;
    }/**
     * 更新房间列表
     */
    private void updateRoomList(String roomListStr) {
        // 清空当前列表和映射
        roomListModel.clear();
        roomNameToIdMap.clear();
        
        // 解析房间列表字符串 (格式: ROOMLIST|roomId:roomName:userCount,...)
        String[] rooms = roomListStr.substring(9).split(",");
        for (String room : rooms) {
            if (!room.isEmpty()) {
                String[] parts = room.split(":");
                if (parts.length == 3) {
                    String roomId = parts[0];
                    String roomName = parts[1];
                    String userCount = parts[2];
                    String displayText = roomName + " (人数: " + userCount + "/10)";
                    roomListModel.addElement(displayText);
                    // 维护房间名称到ID的映射
                    roomNameToIdMap.put(roomName, roomId);
                }
            }
        }
    }
    
    /**
     * 更新房间用户列表
     */
    private void updateRoomUserList(String userListStr) {
        // 清空当前列表
        userListModel.clear();
        
        // 解析用户列表字符串 (格式: ROOMUSERLIST|roomId|user1,user2,...)
        String[] parts = userListStr.split("\\|", 3);
        if (parts.length == 3) {
            String roomId = parts[1];
            String usersPart = parts[2];
            
            // 只有当前房间的用户列表才更新
            if (roomId.equals(currentRoomId)) {
                String[] users = usersPart.split(",");
                for (String user : users) {
                    if (!user.isEmpty()) {
                        userListModel.addElement(user);
                    }
                }
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
                        } else if (msg.startsWith("ROOMLIST|")) {
                            updateRoomList(msg);
                        } else if (msg.startsWith("ROOMUSERLIST|")) {
                            updateRoomUserList(msg);
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
