## 源特定组播收发（NIO阻塞模式）
源特定组播需要路由器支持此模式，并且源设置是由路由器设置的，客户端并不做处理。
### 客户端
客户端绑定地址主要包含以下几点：
1. 组播地址（224.0.0.0~239.255.255.255）
2. 本地网卡地址
3. 要发送的端口

实现的主要步骤如下：
- 设置组播组地址 `InetAddress group = InetAddress.getByName("232.0.0.1");`
- 绑定本地网卡 `NetworkInterface.getByInetAddress(InetAddress.getByName("192.168.1.107"));`
- 建立通信通道
    - 绑定网卡类型
    - 设置通道参数
- 设置是否阻塞
- 加入组播组
- 发送数据

客户端代码样例：
```java
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
```
### 服务端
1. 组播地址（224.0.0.0~239.255.255.255）
2. 本地网卡地址
3. 监听的端口
4. 组播源地址（发送端地址）

实现步骤与客户端类似，只是在创建通道时，需要绑定监听端口。

服务端代码样例：
```java
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
            // 这里本地路由器没有设置组播源，因此先注释。
            // 如果指定源，必须使用此方式加入组播组
//            channel.join(group, interf, source);
            //加入组播组
            channel.join(group, interf);
            System.out.println("服务端准备接收......");
            while (true) {
                ByteBuffer buffer = ByteBuffer.allocate(8192);
                channel.receive(buffer);
                buffer.flip();
                // 开启线程，防止此条数据延迟导致后续消息阻塞
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
```