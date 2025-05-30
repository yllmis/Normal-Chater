package src;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * 客户端处理线程类
 * 负责处理单个客户端的消息收发
 */
public class ClientHandler implements Runnable {
    private Socket clientSocket;         // 客户端socket连接
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

    public String getUsername() {
        return username;
    }
    
    public String getCurrentRoomId() {
        return currentRoomId;
    }
    
    public void setCurrentRoomId(String roomId) {
        this.currentRoomId = roomId;
    }

    public void sendMessage(String message) {
        out.println(message);
    }
    
    public void disconnect() {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.out.println("关闭客户端连接时出错: " + e.getMessage());
        }
    }
    
    @Override
    public void run() {
        try {
            String message;
            
            while ((message = in.readLine()) != null) {
                System.out.println("接收到消息: " + message);
                if (message.startsWith("LOGIN|")) {                    
                    username = message.substring(6);
                    sendMessage("SYSTEM|欢迎加入聊天室，" + username + "！请选择房间开始聊天");
                    sendMessage(server.getRoomList());
                } else if (message.startsWith("JOINROOM|")) {
                    String roomId = message.substring(9);
                    handleJoinRoom(roomId);
                } else if (message.startsWith("CHAT|")) {
                    if (!username.isEmpty() && !currentRoomId.isEmpty()) {
                        handleChatMessage(message);
                    }
                } else if (message.startsWith("LEAVEROOM|")) {
                    handleLeaveRoom();
                } else if (message.startsWith("CREATEROOM|")) {
                    String roomName = message.substring(11);
                    String roomId = server.createRoom(roomName);
                    if (roomId != null) {
                        sendMessage("SYSTEM|房间创建成功，房间ID: " + roomId);
                        sendMessage(server.getRoomList());
                    } else {
                        sendMessage("SYSTEM|房间创建失败，可能已达到最大房间数");
                    }
                } else if (message.startsWith("LOGOUT|")) {
                    System.out.println("用户主动登出: " + username);
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("客户端连接异常: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            if (!username.isEmpty()) {
                if (!currentRoomId.isEmpty()) {
                    handleLeaveRoom();
                }
                server.removeClient(this);
                System.out.println("用户 " + username + " 断开连接");
            }
        }
    }
    
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
        
        if (!currentRoomId.isEmpty()) {
            handleLeaveRoom();
        }
        
        if (room.addClient(this)) {
            currentRoomId = roomId;
            sendMessage("SYSTEM|成功加入房间: " + room.getRoomName());
            room.broadcast("SYSTEM|" + username + " 加入了房间", this);
            room.broadcastRoomUserList();
            server.broadcastRoomList();
        } else {
            sendMessage("SYSTEM|加入房间失败");
        }
    }
    
    private void handleLeaveRoom() {
        if (!currentRoomId.isEmpty()) {
            Room room = server.getRoom(currentRoomId);
            if (room != null) {
                room.removeClient(this);
                room.broadcast("SYSTEM|" + username + " 离开了房间", null);
                room.broadcastRoomUserList();
                server.broadcastRoomList();
            }
            currentRoomId = "";
            sendMessage("SYSTEM|已离开房间");
        }
    }
    
    private void handleChatMessage(String message) {
        Room room = server.getRoom(currentRoomId);
        if (room != null) {
            room.broadcastToAll(message);
            System.out.println("在房间 " + currentRoomId + " 广播消息: " + message);
        }
    }
}
