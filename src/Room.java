package src;

import java.util.*;

/**
 * 聊天房间类
 * 管理房间内的用户和消息广播
 */
public class Room {
    private String roomId;                               // 房间ID
    private String roomName;                            // 房间名称
    private Vector<ClientHandler> clients;              // 房间内的客户端
    private static final int MAX_USERS = 10;           // 房间最大用户数
    
    /**
     * 构造函数
     * @param roomId 房间ID
     * @param roomName 房间名称
     */
    public Room(String roomId, String roomName) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.clients = new Vector<>();
    }
    
    /**
     * 获取房间ID
     * @return 房间ID
     */
    public String getRoomId() {
        return roomId;
    }
    
    /**
     * 获取房间名称
     * @return 房间名称
     */
    public String getRoomName() {
        return roomName;
    }
    
    /**
     * 添加用户到房间
     * @param client 客户端处理器
     * @return 是否成功添加
     */
    public boolean addClient(ClientHandler client) {
        if (clients.size() >= MAX_USERS) {
            return false; // 房间已满
        }
        if (!clients.contains(client)) {
            clients.add(client);
            return true;
        }
        return false;
    }
    
    /**
     * 从房间移除用户
     * @param client 客户端处理器
     */
    public void removeClient(ClientHandler client) {
        clients.remove(client);
    }
    
    /**
     * 向房间内所有用户广播消息
     * @param message 消息内容
     * @param sender 发送者（可以为null）
     */
    public void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }
    
    /**
     * 向房间内所有用户广播消息（包括发送者）
     * @param message 消息内容
     */
    public void broadcastToAll(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }
    
    /**
     * 获取房间内用户列表
     * @return 用户名列表
     */
    public ArrayList<String> getUsers() {
        ArrayList<String> usernames = new ArrayList<>();
        for (ClientHandler client : clients) {
            usernames.add(client.getUsername());
        }
        return usernames;
    }
    
    /**
     * 获取房间内用户数量
     * @return 用户数量
     */
    public int getUserCount() {
        return clients.size();
    }
    
    /**
     * 检查房间是否为空
     * @return 是否为空
     */
    public boolean isEmpty() {
        return clients.isEmpty();
    }
    
    /**
     * 检查房间是否已满
     * @return 是否已满
     */
    public boolean isFull() {
        return clients.size() >= MAX_USERS;
    }
    
    /**
     * 向房间内所有用户广播房间用户列表更新
     */
    public void broadcastRoomUserList() {
        ArrayList<String> users = getUsers();
        StringBuilder userList = new StringBuilder("ROOMUSERLIST|" + roomId + "|");
        
        for (String user : users) {
            userList.append(user).append(",");
        }
        
        // 移除最后一个逗号
        if (users.size() > 0) {
            userList.deleteCharAt(userList.length() - 1);
        }
        
        String userListMessage = userList.toString();
        broadcastToAll(userListMessage);
    }
}
