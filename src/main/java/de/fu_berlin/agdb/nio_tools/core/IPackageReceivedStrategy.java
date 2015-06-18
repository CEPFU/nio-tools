package de.fu_berlin.agdb.nio_tools.core;

import java.nio.channels.SelectionKey;

public interface IPackageReceivedStrategy {

	public void receivedPackage(DataPackage dataPackage, SelectionKey selectionKey);

}
