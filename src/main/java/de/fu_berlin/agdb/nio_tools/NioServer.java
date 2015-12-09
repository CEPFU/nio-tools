package de.fu_berlin.agdb.nio_tools;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.fu_berlin.agdb.nio_tools.core.Connection;
import de.fu_berlin.agdb.nio_tools.core.DataPackage;
import de.fu_berlin.agdb.nio_tools.core.NioBase;

public class NioServer extends NioBase {
	
	private HashMap<SelectionKey, DataPackage> readDataMap;
	private HashMap<SelectionKey, List<DataPackage>> writeDataMap;
	
	private HashMap<SelectionKey, Connection> selectionKeyConnectionIdMap;
	private HashMap<Connection, SelectionKey> connectionIdSelectionKeyMap;

	private AConnectionHandler connectionHandler;
	
	public NioServer(int port, AConnectionHandler connectionHandler) throws IOException {
		this.connectionHandler = connectionHandler;
		
		readDataMap = new HashMap<SelectionKey, DataPackage>();
		writeDataMap = new HashMap<SelectionKey, List<DataPackage>>();
		
		selectionKeyConnectionIdMap = new HashMap<SelectionKey, Connection>();
		connectionIdSelectionKeyMap = new HashMap<Connection, SelectionKey>();
		
		ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.configureBlocking(false);
		
		InetSocketAddress inetSocketAddress = new InetSocketAddress("0.0.0.0", port);
		serverSocketChannel.bind(inetSocketAddress);
		
		serverSocketChannel.register(getSelector(), SelectionKey.OP_ACCEPT);
	}

	@Override
	protected void connectToServer(SelectionKey selectionKey) throws IOException {
		throw new UnsupportedOperationException("This is a server that can't connect to servers.");
	}

	@Override
	protected synchronized void acceptClient(SelectionKey selectionKey) throws IOException {
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
		
		SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);
		SelectionKey newSelectionKey = socketChannel.register(getSelector(), SelectionKey.OP_CONNECT);
		
		Connection connection = new Connection();
		selectionKeyConnectionIdMap.put(newSelectionKey, connection);
		connectionIdSelectionKeyMap.put(connection, newSelectionKey);
	}
	
	@Override
	public synchronized void removeClient(SelectionKey selectionKey){
		connectionIdSelectionKeyMap.remove(selectionKeyConnectionIdMap.get(selectionKey));
		selectionKeyConnectionIdMap.remove(selectionKey);
	}

	@Override
	protected synchronized DataPackage getReadDataPackage(SelectionKey selectionKey){
		DataPackage dataPackage = readDataMap.get(selectionKey);
		if(dataPackage == null){
			dataPackage = new DataPackage();
			readDataMap.put(selectionKey, dataPackage);
		}
		return dataPackage;
	}

	public synchronized void receivedPackage(DataPackage dataPackage, SelectionKey selectionKey) {
		readDataMap.remove(selectionKey);
		connectionHandler.handleReceivedData(dataPackage.getPayload(), selectionKeyConnectionIdMap.get(selectionKey), this);
	}

	@Override
	protected synchronized DataPackage getWriteDataPackage(SelectionKey selectionKey) {
		List<DataPackage> writeDataQueue = writeDataMap.get(selectionKey);
		DataPackage dataPackage = writeDataQueue.get(0);
		return dataPackage;
	}
	
	public synchronized void sendPackage(DataPackage dataPackage, SelectionKey selectionKey) {
		List<DataPackage> writeDataQueue = writeDataMap.get(selectionKey);
		writeDataQueue.remove(0);
		if(writeDataQueue.isEmpty()){
			writeDataMap.remove(selectionKey);
			selectionKey.interestOps(SelectionKey.OP_READ);
		}
	}

	public synchronized void addDataToSend(byte[] data, Connection connection) {
		List<DataPackage> writeDataQueue = writeDataMap.get(connectionIdSelectionKeyMap.get(connection));
		if(writeDataQueue == null){
			writeDataQueue = new ArrayList<DataPackage>();
			writeDataMap.put(connectionIdSelectionKeyMap.get(connection), writeDataQueue);
		}
		DataPackage dataPackage = new DataPackage(data);
		writeDataQueue.add(dataPackage);
		SelectionKey selectionKey = connectionIdSelectionKeyMap.get(connection);
		selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		selectionKey.selector().wakeup();
	}
	
	public void bordcastData(byte[] data){
		for (Connection connection : selectionKeyConnectionIdMap.values()) {
			addDataToSend(data, connection);
		}
	}
}
