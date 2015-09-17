package de.fu_berlin.agdb.nio_tools;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import de.fu_berlin.agdb.nio_tools.core.Connection;
import de.fu_berlin.agdb.nio_tools.core.DataPackage;
import de.fu_berlin.agdb.nio_tools.core.NioBase;

public class NioClient extends NioBase {

	private List<DataPackage> writeDataQueue;
	private DataPackage readDataPackage;
	private AConnectionHandler connectionHandler;

	public NioClient(String host, int port, AConnectionHandler connectionHandler) throws IOException {
		writeDataQueue = new ArrayList<DataPackage>();
		this.connectionHandler = connectionHandler;
		
		SocketChannel socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);
		socketChannel.connect(new InetSocketAddress(host, port));
		socketChannel.register(getSelector(), SelectionKey.OP_CONNECT);
	}
	
	@Override
	protected void acceptClient(SelectionKey selectionKey) throws IOException {
		throw new UnsupportedOperationException("This is a client which can't accept clients.");
	}
	
	@Override
	protected void removeClient(SelectionKey selectionKey) {
		throw new UnsupportedOperationException("This is a client which can't accept clients.");
	}

	@Override
	protected void connectToServer(SelectionKey selectionKey) throws IOException {
		SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
		socketChannel.finishConnect();
		selectionKey.interestOps(SelectionKey.OP_READ);
	}
	
	@Override
	protected DataPackage getReadDataPackage(SelectionKey selectionKey){
		if(readDataPackage == null){
			readDataPackage = new DataPackage();
		}
		return readDataPackage;
	}

	public void receivedPackage(DataPackage dataPackage, SelectionKey selectionKey) {
		readDataPackage = null;
		connectionHandler.handleReceivedData(dataPackage.getPayload());
	}
	
	@Override
	protected DataPackage getWriteDataPackage(SelectionKey selectionKey){
		return writeDataQueue.get(0);
	}
	
	public void sendPackage(DataPackage dataPackage, SelectionKey selectionKey) {
		writeDataQueue.remove(0);
		if(writeDataQueue.size() == 0){
			selectionKey.interestOps(SelectionKey.OP_READ);
		}
	}

	@Override
	public void addDataToSend(byte[] data, Connection connection) {
		addDataToSend(data);
	}
	
	public void addDataToSend(byte[] data){
		writeDataQueue.add(readDataPackage);
		getSelector().wakeup();
	}
}
