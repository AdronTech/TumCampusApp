package de.tum.in.tumcampus.models;

import java.util.ArrayList;

@SuppressWarnings("UnusedDeclaration")
public class ChatRoom {
	
	private String messages;
	private String url;
	private String name;
	private int id;
	
	private ArrayList<String> members = new ArrayList<String>();
	
	public ChatRoom(String name) {
		super();
		this.name = name;
	}
	
	public String getMessages() {
		return messages;
	}
	public void setMessages(String messages) {
		this.messages = messages;
	}
	String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public ArrayList<String> getMembers() {
		return members;
	}
	public void setMembers(ArrayList<String> members) {
		this.members = members;
	}

    @Override
    public String toString() {
        return id+": "+name;
    }
}
