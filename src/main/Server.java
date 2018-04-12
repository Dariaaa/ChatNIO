package main;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;

import static java.nio.ByteBuffer.allocate;
import static java.nio.channels.SelectionKey.OP_ACCEPT;

class Server implements Runnable {
    private static final Logger log = Logger.getLogger(Server.class);

    private ServerSocketChannel serverChannel = null;
    private Selector selector;
    private ByteBuffer readBuffer = allocate(8192);
    private int port;

    public Server() throws IOException {
        port = ChatConstants.getDefaultPort();
        configureServer();
    }

    public Server(int port) throws IOException {
        this.port = port;
        configureServer();
    }

    /**
     * Configuring server
     *
     * @throws IOException
     */
    private void configureServer() throws IOException {
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        InetSocketAddress inetSocketAddress = new InetSocketAddress(ChatConstants.getLocalHost(), port);
        serverChannel.socket().bind(inetSocketAddress);
        selector = SelectorProvider.provider().openSelector();
        serverChannel.register(selector, OP_ACCEPT);
    }


    /**
     * Reading SelectionKey results and react on events
     */
    @Override
    public void run() {
        try {
            log.info("Server starting on port " + ChatConstants.getDefaultPort());
            Iterator<SelectionKey> keys;
            SelectionKey key;
            while (serverChannel.isOpen()) {
                selector.select();
                keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    key = keys.next();
                    keys.remove();

                    if (!key.isValid())
                        continue;

                    if (key.isAcceptable())
                        acceptConnection(key);

                    if (key.isReadable())
                        readMessage(key);

                    if (key.isWritable())
                        readMessage(key);

                }
            }
        } catch (Exception e) {
            try {
                serverChannel.close();
            } catch (IOException ignored) {

            }
        }
    }

    /**
     * Accept new connection (client)
     *
     * @param key
     * @throws IOException
     */
    private void acceptConnection(SelectionKey key) throws IOException {
        SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
        String address = (new StringBuilder(socketChannel.socket().getInetAddress().toString())).append(":").append(socketChannel.socket().getPort()).toString();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ, address);
        log.info("accepted connection from: " + address);
    }

    /**
     * Read message from client
     *
     * @param key
     * @throws IOException
     */
    private void readMessage(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        StringBuilder messageBuilder = new StringBuilder();

        readBuffer.clear();
        int read = 0;
        while ((read = socketChannel.read(readBuffer)) > 0) {
            readBuffer.flip();
            byte[] bytes = new byte[readBuffer.limit()];
            readBuffer.get(bytes);
            messageBuilder.append(new String(bytes));
            readBuffer.clear();
        }
        String message;
        if (read < 0) {
            message = key.attachment() + " left the chat.";
            socketChannel.close();
        } else {
            message = messageBuilder.toString();
        }

        log.info(message);
        broadcastUsers(message);
    }

    /**
     * Broadcast clients about events in other clients
     *
     * @param msg
     * @throws IOException
     */
    private void broadcastUsers(String msg) throws IOException {
        ByteBuffer messageBuffer = ByteBuffer.wrap(msg.getBytes());
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                SocketChannel socketChannel = (SocketChannel) key.channel();
                socketChannel.write(messageBuffer);
                messageBuffer.rewind();
            }
        }
    }


    @Override
    public String toString() {
        return "Server in port:" + String.valueOf(ChatConstants.getDefaultPort());
    }

    public static void main(String[] args) throws IOException {
        new Thread(new Server()).start();
    }


}
