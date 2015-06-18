package de.fu_berlin.agdb.nio_tools;

import de.fu_berlin.agdb.nio_tools.core.Connection;
import de.fu_berlin.agdb.nio_tools.core.IConnectionHandler;
import de.fu_berlin.agdb.nio_tools.core.ISendOperationCapable;

public abstract class AConnectionHandler implements IConnectionHandler {
	
	private Connection connection;
	private ISendOperationCapable sendOperationCapable;

	protected void handleReceivedData(byte[] data, Connection connection, ISendOperationCapable sendOperationCapable){
		this.connection = connection;
		this.sendOperationCapable = sendOperationCapable;
		handleReceivedData(data);
	}

	public void sendData(byte[] data){
		sendOperationCapable.addDataToSend(data, connection);
	}
}
