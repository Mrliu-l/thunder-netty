package com.thunder.liu.Nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class NioChatServer {

    private Charset charset = Charset.forName("UTF-8");

    private Selector selector;

    private String MESSAGE_CONTENT_SPLIT = "@#", USER_EXISTS = "系统提示用户名已存在";

    private Set<String> userNameSet = new HashSet<>();

    public NioChatServer(int port) throws IOException {
        //open socket
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        //socket绑定ip + 端口
        serverSocketChannel.bind(new InetSocketAddress(port));
        //open selector
        selector = Selector.open();
        serverSocketChannel.configureBlocking(false);
        //将selector绑定到socket上，并且设置socket开始接收请求
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("服务端启动成功，端口：" + port);
    }

    private void listen() throws IOException {
        while (true){
            int selectKeyNum = selector.select();
            if(selectKeyNum == 0) continue;
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()){
                SelectionKey selectionKey = iterator.next();
                iterator.remove();
                process(selectionKey);
            }
        }
    }

    private void process(SelectionKey key) throws IOException {
        if(key.isAcceptable()){ //状态为可以开始接收数据
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
            SocketChannel socketChannel = serverSocketChannel.accept();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
            key.interestOps(SelectionKey.OP_ACCEPT);
            socketChannel.write(charset.encode("请输昵称"));
        }else if(key.isReadable()){ //状态变为可以处理来自客户端的数据时
            SocketChannel channel = (SocketChannel) key.channel();
            StringBuilder message = new StringBuilder();
            ByteBuffer byteBuffer = ByteBuffer.allocate(2);
            try {
                while (true){
                    byteBuffer.clear();
                    int read = channel.read(byteBuffer);
                    if(read > 0){
                        byteBuffer.flip();
                        message.append(charset.decode(byteBuffer));
                        key.interestOps(SelectionKey.OP_READ);
                    }else{
                        break;
                    }
                }
            }catch (IOException e){
                key.cancel();
                channel.close();
            }
            if(message.length() > 0){
                String[] split = message.toString().split(MESSAGE_CONTENT_SPLIT);
                if(split != null && split.length == 1){//用户登录
                    String userName = split[0];
                    if(userNameSet.contains(userName)){
                        channel.write(charset.encode(USER_EXISTS));
                    }else{
                        userNameSet.add(userName);
                        int onlineNUm = getOnlineNum();
                        String msg = "欢迎" + userName + "进入聊天室，当前在线人数：" + onlineNUm;
                        boradCast(null, msg);
                    }
                }else if(split != null && split.length > 1){//发消息了
                    String userName = split[0];
                    String msg = message.substring(userName.length() + MESSAGE_CONTENT_SPLIT.length());
                    msg = userName + "说：" + msg;
                    if(userNameSet.contains(userName)){
                        boradCast(channel, msg);
                    }
                }
            }
        }
    }

    private void boradCast(SocketChannel channel, String msg) throws IOException {
        for (SelectionKey key : selector.keys()) {
            if(key.channel() == channel){
                continue;
            }
            if(key.channel() instanceof SocketChannel){
                SocketChannel socketChannel = (SocketChannel) key.channel();
                socketChannel.write(charset.encode(msg));
            }
        }
    }

    private int getOnlineNum() {
        int num = 0;
        for (SelectionKey key : selector.keys()) {
            if(key.isValid() && key.channel() instanceof SocketChannel){
                num++;
            }
        }
        return num;
    }

    public static void main(String[] args) throws IOException {
        new NioChatServer(8080).listen();
    }
}
