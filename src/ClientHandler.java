package src;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

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
                System.out.println("接收到消息: " + message);
                  // 处理消息，按协议格式解析
                if (message.startsWith("LOGIN|")) {                    
                    // 处理登录消息: LOGIN|username
                    username = message.substring(6);
                    // 广播用户登录消息
                    server.broadcast("SYSTEM|" + username + "加入了聊天室", this);                    // 向新用户发送欢迎消息
                    sendMessage("SYSTEM|欢迎加入聊天室，" + username + "！");
                    // 广播更新在线用户列表
                    server.broadcastUserList();
                } else if (message.startsWith("CHAT|")) {
                    // 处理聊天消息: CHAT|username:content
                    if (!username.isEmpty()) {
                        // 广播聊天消息给所有人，包括发送者
                        server.broadcastToAll(message);
                        System.out.println("广播消息给所有人: " + message);
                    }
                } else if (message.startsWith("LOGOUT|")) {
                    // 处理用户主动登出: LOGOUT|username
                    System.out.println("用户主动登出: " + username);
                    break; // 退出循环，触发finally块中的清理工作
                }
            }
        } catch (IOException e) {
            System.out.println("客户端连接异常: " + e.getMessage());
        } finally {
            // 客户端断开连接后的清理工作
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
              // 从服务器的客户端列表中移除
            if (!username.isEmpty()) {
                server.removeClient(this);
                server.broadcast("SYSTEM|" + username + "离开了聊天室", null);
                // 广播更新在线用户列表
                server.broadcastUserList();
            }
        }
    }
}
