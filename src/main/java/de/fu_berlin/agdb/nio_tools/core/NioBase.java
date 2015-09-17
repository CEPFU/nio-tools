package de.fu_berlin.agdb.nio_tools.core;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public abstract class NioBase implements Runnable, ISendOperationCapable, IPackageReceivedStrategy, IPackageSendStrategy{
	
	private static final Logger logger = LogManager.getLogger(NioBase.class);
	
	private Selector selector;
	
	public NioBase() throws IOException {
		selector = SelectorProvider.provider().openSelector();
	}
	
	protected abstract void acceptClient(SelectionKey selectionKey) throws IOException;
	
	protected abstract void removeClient(SelectionKey selectionKey);
	
	protected abstract void connectToServer(SelectionKey selectionKey) throws IOException;
	
	protected abstract DataPackage getWriteDataPackage(SelectionKey selectionKey);
	
	protected abstract DataPackage getReadDataPackage(SelectionKey selectionKey);

	
	protected Selector getSelector(){
		return selector;
	}
	
	protected void readData(SelectionKey selectionKey, IPackageReceivedStrategy packageReceivedStrategy) throws IOException {
		DataPackage dataPackage = getReadDataPackage(selectionKey);
		SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
		socketChannel.read(dataPackage.getBufferToReceiveData());
		
		if(dataPackage.doneReceiving()){
			packageReceivedStrategy.receivedPackage(dataPackage, selectionKey);
		}
	}

	protected void writeData(SelectionKey selectionKey, IPackageSendStrategy packageSendStrategy) {
		DataPackage dataPackage = getWriteDataPackage(selectionKey);
		SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
		try {
			socketChannel.write(dataPackage.getBufferToSend());
		} catch (IOException e) {
			logger.error("Dropping client because an exception happened:", e);
			removeClient(selectionKey);
		}
		
		if(dataPackage.doneSending()){
			packageSendStrategy.sendPackage(dataPackage, selectionKey);
		}
	}
	
	public void run() {
		try {
			while (true) {
				if(selector.select() > 0){
					Iterator<SelectionKey> selectedKeyIterator = selector.selectedKeys().iterator();
					while (selectedKeyIterator.hasNext()) {
						SelectionKey selectionKey = selectedKeyIterator.next();
						selectedKeyIterator.remove();
						
						if(selectionKey.isAcceptable()){
							acceptClient(selectionKey);
						} else if (selectionKey.isConnectable()){
							connectToServer(selectionKey);
						} else if (selectionKey.isReadable()){
							readData(selectionKey, this);
						} else if (selectionKey.isWritable()) {
							writeData(selectionKey, this);
						}
					}
				}
			}
		} catch (IOException e) {
			logger.error("Error while running NioBase", e);
		}
	}
}
