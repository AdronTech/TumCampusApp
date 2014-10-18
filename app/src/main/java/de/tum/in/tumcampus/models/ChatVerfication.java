package de.tum.in.tumcampus.models;

@SuppressWarnings("UnusedDeclaration")
public class ChatVerfication {

	private String signature;

	public ChatVerfication(String signature) {
		this.signature = signature;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}
}
