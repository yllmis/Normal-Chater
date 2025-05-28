package src;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * 客户端处理线程类
 * 负责处理单个客户端的消息收发
 */
public class ClientHandler implements Runnable {    private Socket clientSocket;         // 客户端socket连接
    private ChatServer server;           // 服务器引用
    private BufferedReader in;           // 输入流
    private PrintWriter out;             // 输出流
    private String username = "";        // 用户名
    private String currentRoomId = "";   // 当前所在房间ID
    
    /**
     * 构造函数，初始化客户端处理线程
     * @param socket 客户端socket连接
     * @param server 服务器引用
     */
    public ClientHandler(Socket socket, ChatServer server) {
        this.clientSocket = socket;
        this.server = server;
        
        try {
            // 初始化输入输出流，指定UTF-8编码
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);
        } catch (IOException e) {
            System.out.println("客户端处理线程初始化异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
      /**
     * 获取用户名
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * 获取当前房间ID
     * @return 当前房间ID
     */
    public String getCurrentRoomId() {
        return currentRoomId;
    }
    
    /**
     * 设置当前房间ID
     * @param roomId 房间ID
     */
    public void setCurrentRoomId(String roomId) {
        this.currentRoomId = roomId;
    }
      /**
     * 发送消息给客户端
     * @param message 要发送的消息内容
     */
    public void sendMessage(String message) {
        out.println(message);
    }
    
    /**
     * 断开客户端连接
     */
    public void disconnect() {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.out.println("关闭客户端连接时出错: " + e.getMessage());
        }
    }
    
    /**
     * 线程运行方法，处理客户端消息
     */
    @Override
    public void run() {
        try {
            String message;
            
            // 持续读取客户端发送的消息
            while ((message = in.readLine()) != null) {
                System.out.println("接收到消息: " + message);                // 处理消息，按协议格式解析
                if (message.startsWith("LOGIN|")) {                    
                    // 处理登录消息: LOGIN|username
                    username = message.substring(6);
                    // 向新用户发送欢迎消息
                    sendMessage("SYSTEM|欢迎加入聊天室，" + username + "！请选择房间开始聊天");
                    // 发送房间列表给新用户
                    sendMessage(server.getRoomList());
                } else if (message.startsWith("JOINROOM|")) {
                    // 处理加入房间消息: JOINROOM|roomId
                    String roomId = message.substring(9);
                    handleJoinRoom(roomId);
                } else if (message.startsWith("CHAT|")) {
                    // 处理聊天消息: CHAT|username:content
                    if (!username.isEmpty() && !currentRoomId.isEmpty()) {
                        handleChatMessage(message);
                    }
                } else if (message.startsWith("LEAVEROOM|")) {
                    // 处理离开房间消息: LEAVEROOM|roomId
                    handleLeaveRoom();
                } else if (message.startsWith("LOGOUT|")) {
                    // 处理用户主动登出: LOGOUT|username
                    System.out.println("用户主动登出: " + username);
                    break; // 退出循环，触发finally块中的清理工作
                }
            }
        } catch (IOException e) {
            System.out.println("客户端连接异常: " + e.getMessage());        } finally {
            // 客户端断开连接后的清理工作
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
              // 从服务器的客户端列表中移除
            if (!username.isEmpty()) {
                // 如果在房间中，先离开房间
                if (!currentRoomId.isEmpty()) {
                    handleLeaveRoom();
                }
                server.removeClient(this);
                System.out.println("用户 " + username + " 断开连接");
            }
        }
    }
    
    /**
     * 处理加入房间请求
     * @param roomId 房间ID
     */
    private void handleJoinRoom(String roomId) {
        Room room = server.getRoom(roomId);
        if (room == null) {
            sendMessage("SYSTEM|房间不存在");
            return;
        }
        
        if (room.isFull()) {
            sendMessage("SYSTEM|房间已满，无法加入");
            return;
        }
        
        // 如果已在其他房间，先离开
        if (!currentRoomId.isEmpty()) {
            handleLeaveRoom();
        }
        
        // 加入新房间
        if (room.addClient(this)) {
            currentRoomId = roomId;
            sendMessage("SYSTEM|成功加入房间: " + room.getRoomName());
            room.broadcast("SYSTEM|" + username + " 加入了房间", this);
            
            // 广播房间用户列表更新
            room.broadcastRoomUserList();
            // 广播全局房间列表更新
            server.broadcastRoomList();
        } else {
            sendMessage("SYSTEM|加入房间失败");
        }
    }
    
    /**
     * 处理离开房间请求
     */
    private void handleLeaveRoom() {
        if (!currentRoomId.isEmpty()) {
            Room room = server.getRoom(currentRoomId);
            if (room != null) {
                room.removeClient(this);
                room.broadcast("SYSTEM|" + username + " 离开了房间", null);
                
                // 广播房间用户列表更新
                room.broadcastRoomUserList();
                // 广播全局房间列表更新
                server.broadcastRoomList();
            }
            currentRoomId = "";
            sendMessage("SYSTEM|已离开房间");
        }
    }
    
    /**
     * 处理聊天消息
     * @param message 消息内容
     */
    private void handleChatMessage(String message) {
        Room room = server.getRoom(currentRoomId);
        if (room != null) {
            // 在房间内广播消息
            room.broadcastToAll(message);
            System.out.println("在房间 " + currentRoomId + " 广播消息: " + message);
        }
    }
}
