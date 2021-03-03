package com.thunder.liu.Nio;

import java.io.IOException;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class NioChatClient {

    private Charset charset = Charset.forName("UTF-8");

    private String userName = "";

    private String MESSAGE_CONTENT_SPLIT = "@#", USER_EXISTS = "系统提示用户名已存在";

    private Selector selector;

    private SocketChannel socketChannel;

    public NioChatClient(int port) throws IOException {
        socketChannel = SocketChannel.open(new InetSocketAddress(port));
        selector = Selector.open();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("客户端启动成功，端口：" + port);
    }

    private void session(){
        new Reader().start();
        new Writer().start();
    }

    private class Reader extends Thread{
        @Override
        public void run() {
            try {
                Scanner sc = new Scanner(System.in);
                while (sc.hasNextLine()){
                    String message = sc.nextLine();
                    if("".equals(message)) continue;
                    if("".equals(userName)){
                        userName = message;
                        message = message + MESSAGE_CONTENT_SPLIT;
                    }else{
                        message = userName + MESSAGE_CONTENT_SPLIT + message;
                    }
                    socketChannel.write(charset.encode(message));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class Writer extends Thread{
        @Override
        public void run() {
            try {
                while (true){
                    int select = selector.select();
                    if(select == 0) continue;
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()){
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        process(key);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();;
            }
        }

        private void process(SelectionKey key) throws IOException {
            if(key.isReadable()){
                SocketChannel socketChannel = (SocketChannel) key.channel();
                StringBuilder message = new StringBuilder();
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                while (socketChannel.read(byteBuffer) > 0){
                    byteBuffer.flip();
                    message.append(charset.decode(byteBuffer));
                }
                if(USER_EXISTS.equals(message)){
                    userName = "";
                }
                System.out.println(message.toString());
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new NioChatClient(8080).session();
    }
}
