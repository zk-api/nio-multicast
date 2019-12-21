package com.github.zk.sync;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Date;

/**
 * @author zhaokai
 * @date 2019/11/24 下午12:59
 */
public class Client {
    public static void main(String[] args) {
        String localHost = "192.168.1.107";
        int port = 10000;
        String networkInterf = "192.168.1.107";
        String multicastHost = "232.0.0.1";
        send(localHost, port, networkInterf, multicastHost);
    }

    private static void send(String localHost, int port, String networkInterf, String multicastHost) {
        try {
            // 设置组播地址
            InetAddress group = InetAddress.getByName(multicastHost);
            // 绑定本地网卡
            NetworkInterface interf = NetworkInterface.getByInetAddress(InetAddress.getByName(networkInterf));
            //客户端应该不用做此设置，先留下，后续验证
            InetAddress source = InetAddress.getByName(localHost);

            // 初始化通道
            DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET)
                    // 官方解释：对于面向数据报的套接字，套接字选项用于允许多个程序绑定到相同的地址。
                    // 当套接字用于互联网协议（IP）组播时，应启用此选项
                    .setOption(StandardSocketOptions.SO_REUSEADDR, true);
            // 设置非阻塞方式
            channel.configureBlocking(false);
            //客户端应该不用做此设置，先留下，后续验证
//            channel.join(group, interf, source);
            // 加入组播组
            channel.join(group, interf);
            while (true) {
                // wrap方法不用buffer调用filp()方法切换
                ByteBuffer buffer = ByteBuffer.wrap(new Date().toString().getBytes());
                channel.send(buffer,new InetSocketAddress(port));
                System.out.println("发送数据:" + new String(buffer.array()));
                Thread.sleep(1000);
            }
        } catch (UnknownHostException | SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
