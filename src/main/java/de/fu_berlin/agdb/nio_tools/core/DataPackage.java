package de.fu_berlin.agdb.nio_tools.core;

import java.nio.ByteBuffer;

public class DataPackage {

	private ByteBuffer sizeBuffer;
	private ByteBuffer payloadBuffer;
	
	public DataPackage(byte[] payload) {
		sizeBuffer = ByteBuffer.allocate(4);
		sizeBuffer.putInt(payload.length);
		sizeBuffer.flip();
		
		payloadBuffer = ByteBuffer.allocate(payload.length);
		payloadBuffer.put(payload);
		payloadBuffer.flip();
	}
	
	public ByteBuffer getBufferToSend(){
		if(sizeBuffer.remaining() > 0){
			return sizeBuffer;
		}
		return payloadBuffer;
	}
	
	public boolean doneSending(){
		return false;
	}
	
	public DataPackage(){
		sizeBuffer = ByteBuffer.allocate(4);
	}
	
	public ByteBuffer getBufferToReceiveData(){
		if(payloadBuffer == null){
			if(sizeBuffer.remaining() == sizeBuffer.limit()){
				payloadBuffer = ByteBuffer.allocate(sizeBuffer.getInt());
			} else {
				return sizeBuffer;
			}
		}
		return payloadBuffer;
	}
	
	public boolean doneReceiving(){
		return payloadBuffer != null && payloadBuffer.remaining() == payloadBuffer.limit();
	}

	public byte[] getPayload() {
		return payloadBuffer.array();
	}
}