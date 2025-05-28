package src;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * 聊天服务器主类
 * 负责监听8888端口，接受客户端连接，并为每个客户端创建处理线程
 */
public class ChatServer {
    // 服务器端口号
    private static final int PORT = 8888;
    // 最大房间数量
    private static final int MAX_ROOMS = 10;
    // 存储所有连接的客户端处理线程
    private Vector<ClientHandler> clients = new Vector<>();
    // 存储所有房间
    private Map<String, Room> rooms = new HashMap<>();
    // 服务器socket
    private ServerSocket serverSocket;    // 服务器运行标志
    private boolean isRunning = false;
    
    /**
     * 构造函数，初始化默认房间
     */
    public ChatServer() {
        initializeDefaultRooms();
    }
    
    /**
     * 初始化默认房间
     */
    private void initializeDefaultRooms() {
        rooms.put("room1", new Room("room1", "大厅"));
        rooms.put("room2", new Room("room2", "游戏讨论"));
        rooms.put("room3", new Room("room3", "技术交流"));
    }
    
    /**
     * 启动服务器
     */
    public void start() {
        try {
            // 创建服务器Socket并绑定端口
            serverSocket = new ServerSocket(PORT);
            isRunning = true;
            System.out.println("聊天服务器已启动，正在监听端口: " + PORT);
            System.out.println("输入 'quit' 或按 Ctrl+C 来关闭服务器");
            
            while (isRunning) {
                try {
                    // 接受客户端连接
                    Socket clientSocket = serverSocket.accept();
                    
                    if (!isRunning) {
                        clientSocket.close();
                        break;
                    }
                    
                    System.out.println("新客户端连接: " + clientSocket.getInetAddress().getHostAddress());
                    
                    // 为每个客户端创建一个处理线程
                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    clients.add(handler);
                    
                    // 启动客户端处理线程
                    new Thread(handler).start();
                } catch (SocketException e) {
                    if (isRunning) {
                        System.out.println("服务器socket异常: " + e.getMessage());
                    }
                    // 如果是因为关闭服务器导致的异常，则正常退出循环
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("服务器异常: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }
    
    /**
     * 停止服务器
     */
    public void stop() {
        System.out.println("正在关闭服务器...");
        isRunning = false;
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.out.println("关闭服务器socket时出错: " + e.getMessage());
        }
    }
    
    /**
     * 清理资源
     */
    private void cleanup() {
        System.out.println("正在清理资源...");
        
        // 通知所有客户端服务器即将关闭
        broadcastToAll("SERVER|服务器即将关闭，连接将断开");
        
        // 关闭所有客户端连接
        for (ClientHandler client : new Vector<>(clients)) {
            client.disconnect();
        }
        
        clients.clear();
        System.out.println("服务器已关闭");
    }
    
    /**
     * 检查服务器是否正在运行
     * @return true如果服务器正在运行
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * 向所有客户端广播消息
     * @param message 要广播的消息内容
     * @param sender 发送者（不发送给自己）
     */
    public void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            // 向除发送者以外的所有客户端发送消息
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }
    
    /**
     * 向所有客户端广播消息（包括发送者）
     * @param message 要广播的消息内容
     */
    public void broadcastToAll(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }
    
    /**
     * 向所有客户端广播在线用户列表更新
     */
    public void broadcastUserList() {
        ArrayList<String> users = getOnlineUsers();
        StringBuilder userList = new StringBuilder("USERLIST|");
        
        for (String user : users) {
            userList.append(user).append(",");
        }
        
        // 移除最后一个逗号
        if (users.size() > 0) {
            userList.deleteCharAt(userList.length() - 1);
        }
        
        String userListMessage = userList.toString();
        for (ClientHandler client : clients) {
            client.sendMessage(userListMessage);
        }
    }
    
    /**
     * 移除离线的客户端处理线程
     * @param client 要移除的客户端处理线程
     */
    public void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("客户端离线，当前在线人数: " + clients.size());
    }
      /**
     * 获取在线用户列表
     * @return 在线用户名称列表
     */
    public ArrayList<String> getOnlineUsers() {
        ArrayList<String> usernames = new ArrayList<>();
        for (ClientHandler client : clients) {
            usernames.add(client.getUsername());
        }
        return usernames;
    }
    
    /**
     * 获取房间列表
     * @return 房间信息字符串
     */
    public String getRoomList() {
        StringBuilder roomList = new StringBuilder("ROOMLIST|");
        for (Room room : rooms.values()) {
            roomList.append(room.getRoomId()).append(":")
                   .append(room.getRoomName()).append(":")
                   .append(room.getUserCount()).append(",");
        }
        
        // 移除最后一个逗号
        if (!rooms.isEmpty()) {
            roomList.deleteCharAt(roomList.length() - 1);
        }
        
        return roomList.toString();
    }
    
    /**
     * 获取指定房间
     * @param roomId 房间ID
     * @return 房间对象，如果不存在则返回null
     */
    public Room getRoom(String roomId) {
        return rooms.get(roomId);
    }
    
    /**
     * 创建新房间
     * @param roomName 房间名称
     * @return 房间ID，如果创建失败返回null
     */
    public String createRoom(String roomName) {
        if (rooms.size() >= MAX_ROOMS) {
            return null; // 房间数量已达上限
        }
        
        String roomId = "room" + (rooms.size() + 1);
        Room room = new Room(roomId, roomName);
        rooms.put(roomId, room);
        
        // 广播房间列表更新
        broadcastRoomList();
        
        return roomId;
    }
    
    /**
     * 向所有客户端广播房间列表
     */
    public void broadcastRoomList() {
        String roomListMessage = getRoomList();
        for (ClientHandler client : clients) {
            client.sendMessage(roomListMessage);
        }
    }
    
    /**
     * 主方法，启动服务器
     */
    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        
        // 添加关闭钩子，处理Ctrl+C等信号
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n收到关闭信号，正在关闭服务器...");
            server.stop();
        }));
        
        // 在单独的线程中启动服务器
        Thread serverThread = new Thread(() -> {
            server.start();
        });
        serverThread.start();
        
        // 等待服务器启动
        try {
            Thread.sleep(1000); // 给服务器一点时间启动
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 控制台输入监听
        Scanner scanner = new Scanner(System.in);
        System.out.println("服务器控制台已就绪，输入 'help' 查看可用命令");
        
        while (true) {
            try {
                System.out.print("ChatServer> ");
                String input = scanner.nextLine().trim().toLowerCase();
                
                if ("quit".equals(input) || "exit".equals(input)) {
                    System.out.println("收到退出命令，正在关闭服务器...");
                    server.stop();
                    break;
                } else if ("status".equals(input)) {
                    if (server.isRunning()) {
                        System.out.println("服务器状态: 运行中");
                        System.out.println("当前在线用户数: " + server.clients.size());
                        System.out.println("在线用户: " + server.getOnlineUsers());
                    } else {
                        System.out.println("服务器状态: 已停止");
                    }
                } else if ("help".equals(input)) {
                    System.out.println("可用命令:");
                    System.out.println("  quit/exit - 关闭服务器");
                    System.out.println("  status - 查看服务器状态");
                    System.out.println("  help - 显示帮助信息");
                } else if (!input.isEmpty()) {
                    System.out.println("未知命令: " + input + "，输入 'help' 查看可用命令");
                }
                
                // 如果服务器已停止，退出命令循环
                if (!server.isRunning()) {
                    break;
                }
            } catch (Exception e) {
                System.out.println("输入处理异常: " + e.getMessage());
                break;
            }
        }
        
        scanner.close();
        
        // 等待服务器线程结束
        try {
            serverThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("程序退出");
    }
}
