package de.fu_berlin.agdb.nio_tools.core;

import java.nio.channels.SelectionKey;

public interface IPackageSendStrategy {

	public void sendPackage(DataPackage dataPackage, SelectionKey selectionKey);
	
}
