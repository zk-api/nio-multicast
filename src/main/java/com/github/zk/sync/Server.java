package com.github.zk.sync;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.*;

/**
 * @author zhaokai
 * @date 2019/11/24 下午12:59
 */
public class Server {
    private static ThreadPoolExecutor threadPool = new ThreadPoolExecutor(4, 8, 5,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(10), new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "zk");
        }
    });

    public static void main(String[] args) {
        String localHost = "192.168.1.107";
        int port = 10000;
        String networkInterf = "192.168.1.107";
        String multicastHost = "232.0.0.1";
        receive(localHost, port, networkInterf, multicastHost);
    }

    private static void receive(String localHost, int port, String networkInterf, String multicastHost) {
        try {
            // 设置组播组
            InetAddress group = InetAddress.getByName(multicastHost);
            // 设置本地接口
            NetworkInterface interf = NetworkInterface.getByInetAddress(InetAddress.getByName(networkInterf));
            // 设置源
            InetAddress source = InetAddress.getByName(localHost);
            /**
             *  创建通道
             */
            DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET)
                    .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                    .bind(new InetSocketAddress(port));
            channel.configureBlocking(true);
//            channel.join(group, interf, source);
            channel.join(group, interf);
            System.out.println("服务端准备接收......");
            while (true) {
                ByteBuffer buffer = ByteBuffer.allocate(8192);
                channel.receive(buffer);
                buffer.flip();
                threadPool.execute(new Runnable() {
                    private ByteBuffer buffer;
                    public Runnable setBuffer(ByteBuffer buffer) {
                        this.buffer = buffer;
                        return this;
                    }
                    @Override
                    public void run() {
                        System.out.println("【" + Thread.currentThread() + "】" + "线程执行:" + new String(buffer.array(),0,
                                buffer.limit()));
                        buffer.clear();
                    }
                }.setBuffer(buffer));
            }
        } catch (UnknownHostException | SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
