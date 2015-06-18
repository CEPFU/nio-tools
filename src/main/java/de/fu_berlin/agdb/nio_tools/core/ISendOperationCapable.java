package de.fu_berlin.agdb.nio_tools.core;

public interface ISendOperationCapable {
	public void addDataToSend(byte[] data, Connection connection);
}
