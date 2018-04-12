package main;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static java.nio.channels.SelectionKey.*;


class Client {
    private SocketChannel channel = null;
    private Selector selector = null;
    private ReceiveThread clientReceiver = null;
    private BlockingQueue<String> messageSynchronize = new ArrayBlockingQueue<>(2);
    String clientName = null;
    private List<ChatController> controllers = new ArrayList<>();
    int port = 0;


    Client(String clientName, ChatController controller, int port)
    {
        this.clientName = clientName;
        controllers.add(controller);
        this.port = port;
    }

    void makeConnection() throws IOException {

        channel = SocketChannel.open();
        channel.configureBlocking(false);
        selector = Selector.open();
        channel.register(selector, OP_CONNECT);
        channel.connect(new InetSocketAddress(ChatConstants.getLocalHost(), port));

        startClientReceiver();
    }

    void sendMessage(String message) {

        try {
            messageSynchronize.put(message);
            SelectionKey key = channel.keyFor(selector);
            key.interestOps(OP_WRITE);
            selector.wakeup();
        } catch (InterruptedException ignored) {

        }

    }

    private void startClientReceiver() {
        clientReceiver = new ReceiveThread(channel, controllers);
        clientReceiver.start();

    }


    private class ReceiveThread extends Thread {
        private SocketChannel channel = null;
        List<ChatController> controllerList = new ArrayList<>();

        ReceiveThread(SocketChannel client, List<ChatController> controllerList) {
            super("Receive thread");
            channel = client;
            this.controllerList = controllerList;
        }

        public void run() {
            try {
                while (channel.isOpen()) {
                    selector.select();

                    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                    SelectionKey key;
                    while (keys.hasNext()) {
                        key = keys.next();
                        keys.remove();
                        if (!key.isValid())
                            continue;
                        if (key.isConnectable()) {
                            channel.finishConnect();
                            key.interestOps(OP_WRITE);
                        }
                        if (key.isReadable()) {
                            readAndPrint();
                        }
                        if (key.isWritable()) {
                            String line = messageSynchronize.poll();
                            if (line != null) {
                                channel.write(ByteBuffer.wrap(line.getBytes()));
                                key.interestOps(OP_READ);
                            }
                        }
                    }
                }
            } catch (IOException ignored) {
            }

        }

        private void readAndPrint() throws IOException {
            ByteBuffer buf = ByteBuffer.allocate(2048);
            int nBytes = 0;
            nBytes = channel.read(buf);

            if (nBytes == 2048 || nBytes == 0)
                return;

            String message = new String(buf.array());

            for (ChatController c : controllerList) {
                c.messageReceiver(message);
            }
        }
    }


}
