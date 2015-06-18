package de.fu_berlin.agdb.nio_tools.core;

public interface IConnectionHandler {
	public void handleReceivedData(byte[] data);
	public void sendData(byte[] data);
}
