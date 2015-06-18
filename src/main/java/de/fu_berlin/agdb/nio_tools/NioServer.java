package de.fu_berlin.agdb.nio_tools;

import java.io.IOException;
import java.net.InetAddress;
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
		
		InetAddress localHost = InetAddress.getLocalHost();
		InetSocketAddress inetSocketAddress = new InetSocketAddress(localHost, port);
		serverSocketChannel.bind(inetSocketAddress);
		
		serverSocketChannel.register(getSelector(), SelectionKey.OP_ACCEPT);
	}

	@Override
	protected void connectToServer(SelectionKey selectionKey) throws IOException {
		throw new UnsupportedOperationException("This is a server that can't connect to servers.");
	}

	@Override
	protected void acceptClient(SelectionKey selectionKey) throws IOException {
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
		
		SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);
		socketChannel.register(getSelector(), SelectionKey.OP_READ);
		
		Connection connection = new Connection();
		selectionKeyConnectionIdMap.put(selectionKey, connection);
		connectionIdSelectionKeyMap.put(connection, selectionKey);
	}

	@Override
	protected DataPackage getReadDataPackage(SelectionKey selectionKey) throws IOException {
		DataPackage dataPackage = readDataMap.get(selectionKey);
		if(dataPackage == null){
			dataPackage = new DataPackage();
			readDataMap.put(selectionKey, dataPackage);
		}
		return dataPackage;
	}

	public void receivedPackage(DataPackage dataPackage, SelectionKey selectionKey) {
		readDataMap.remove(selectionKey);
		connectionHandler.handleReceivedData(dataPackage.getPayload(), selectionKeyConnectionIdMap.get(selectionKey), this);
	}

	@Override
	protected DataPackage getWriteDataPackage(SelectionKey selectionKey) throws IOException {
		List<DataPackage> writeDataQueue = writeDataMap.get(selectionKey);
		DataPackage dataPackage = writeDataQueue.get(0);
		return dataPackage;
	}
	
	public void sendPackage(DataPackage dataPackage, SelectionKey selectionKey) {
		List<DataPackage> writeDataQueue = writeDataMap.get(selectionKey);
		writeDataQueue.remove(0);
		if(writeDataQueue.size() == 0){
			writeDataMap.remove(selectionKey);
			selectionKey.interestOps(SelectionKey.OP_READ);
		}
	}

	public void addDataToSend(byte[] data, Connection connection) {
		List<DataPackage> writeDataQueue = writeDataMap.get(connectionIdSelectionKeyMap.get(connection));
		if(writeDataQueue == null){
			writeDataQueue = new ArrayList<DataPackage>();
			writeDataMap.put(connectionIdSelectionKeyMap.get(connection), writeDataQueue);
		}
		DataPackage dataPackage = new DataPackage(data);
		writeDataQueue.add(dataPackage);
		SelectionKey selectionKey = connectionIdSelectionKeyMap.get(connection);
		selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
	}
}