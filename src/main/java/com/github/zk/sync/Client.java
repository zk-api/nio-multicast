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
        String multicastHost = "224.0.0.1";
        send(localHost, port, networkInterf, multicastHost);
    }

    private static void send(String localHost, int port, String networkInterf, String multicastHost) {
        try {
            InetAddress group = InetAddress.getByName(multicastHost);
            NetworkInterface interf = NetworkInterface.getByInetAddress(InetAddress.getByName(networkInterf));
            InetAddress source = InetAddress.getByName(localHost);

            DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET)
                    .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                    .bind(new InetSocketAddress(port));
            channel.configureBlocking(false);
//            channel.join(group, interf, source);
            channel.join(group, interf);
            while (true) {
//                ByteBuffer buffer = ByteBuffer.wrap(new Date().toString().getBytes());
//                buffer.flip();
                ByteBuffer buffer = ByteBuffer.allocate(8192);
                buffer.put(new Date().toString().getBytes());
                channel.send(buffer,new InetSocketAddress(port));
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
