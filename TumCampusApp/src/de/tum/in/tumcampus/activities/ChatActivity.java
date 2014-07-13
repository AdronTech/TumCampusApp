package de.tum.in.tumcampus.activities;

import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.gson.Gson;

import de.tum.in.tumcampus.R;
import de.tum.in.tumcampus.adapters.ChatHistoryAdapter;
import de.tum.in.tumcampus.auxiliary.ChatClient;
import de.tum.in.tumcampus.auxiliary.Const;
import de.tum.in.tumcampus.auxiliary.RSASigner;
import de.tum.in.tumcampus.models.ChatMember;
import de.tum.in.tumcampus.models.ChatMessage;
import de.tum.in.tumcampus.models.ChatMessage2;
import de.tum.in.tumcampus.models.ChatRoom;

/**
 * 
 * 
 * linked files: res.layout.activity_chat
 * 
 * @author Jana Pejic
 */
public class ChatActivity extends SherlockActivity implements OnClickListener {
	
	/** UI elements */
	private ListView lvMessageHistory;
	private ChatHistoryAdapter chatHistoryAdapter;
	private ArrayList<ChatMessage2> chatHistory;
	
	// Objects for disabling or enabling the options menu items
	private MenuItem menuItemLeaveChatRoom;
	
	private EditText etMessage;
	private Button btnSend;
	
	private ChatRoom currentChatRoom;
	private ChatMember currentChatMember;
	
	private boolean messageSentSuccessfully = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);
		
		getIntentData();
		bindUIElements();
		loadChatHistory();
	}
	
	@Override
	public void onClick(View view) {
		// SEND MESSAGE
		if (view.getId() == btnSend.getId()) {
			PrivateKey privateKey = currentChatMember.getPrivateKey();
			if (privateKey == null) {
				Log.e("ChatActivity", "Private key does not exist!");
			}
			
			ChatMessage newMessage = new ChatMessage(etMessage.getText().toString(), currentChatMember.getUrl());
			
			// Generate signature
			RSASigner signer = new RSASigner(privateKey);
			String signature = signer.sign(newMessage.getText());
 			newMessage.setSignature(signature);
 			
 			while (!messageSentSuccessfully) {
				try {
					// Send the message to the server
					ChatMessage newlyCreatedMessage = ChatClient.getInstance().sendMessage(currentChatRoom.getGroupId(), newMessage);
					
					// TODO: uncomment when we no longer need two message classes
					//chatHistory.add(newlyCreatedMessage);
					//chatHistoryAdapter.notifyDataSetChanged();
						
					messageSentSuccessfully = true;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
 			}
			etMessage.setText("");	
		}
	}
	
	private void getIntentData() {
		Bundle extras = getIntent().getExtras();
		currentChatRoom = new Gson().fromJson(extras.getString(Const.CURRENT_CHAT_ROOM), ChatRoom.class);
		currentChatMember = new Gson().fromJson(extras.getString(Const.CURRENT_CHAT_MEMBER), ChatMember.class);
		getActionBar().setTitle(currentChatRoom.getName());
	}
	
	private void bindUIElements() {
		lvMessageHistory = (ListView) findViewById(R.id.lvMessageHistory);
		etMessage = (EditText) findViewById(R.id.etMessage);
		btnSend = (Button) findViewById(R.id.btnSend);
		btnSend.setOnClickListener(this);
	}
	
	// TODO: maybe make this sync
	private void loadChatHistory() {
		ChatClient.getInstance().getMessagesCb(currentChatRoom.getGroupId(), new Callback<List<ChatMessage2>>() {
			@Override
			public void success(List<ChatMessage2> downloadedChatHistory, Response arg1) {
				Log.d("Success loading chat history", arg1.toString());
				chatHistory = (ArrayList<ChatMessage2>) downloadedChatHistory;
				chatHistoryAdapter = new ChatHistoryAdapter(ChatActivity.this, chatHistory, currentChatMember.getUrl());
				lvMessageHistory.setAdapter(chatHistoryAdapter);
			}
			
			@Override
			public void failure(RetrofitError arg0) {
				Log.e("Failure loading chat history", arg0.toString());
			}
		});
	}
	
	// Action Bar
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		// Inflate the menu; this adds items to the action bar if it is present.
		this.getSupportMenuInflater().inflate(R.menu.menu_activity_chat, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		this.menuItemLeaveChatRoom = menu.findItem(R.id.action_leave_chat_room);
		
		//this.setMenuEnabled(true);
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_leave_chat_room:
			// Remove CHAT_TERMS_SHOWN for this room to enable rejoining the room
			PreferenceManager.getDefaultSharedPreferences(this).edit().remove(Const.CHAT_TERMS_SHOWN + "_" + currentChatRoom.getName()).commit();
			
			ChatClient.getInstance().leaveChatRoom(currentChatRoom.getGroupId(), currentChatMember.getUserId(), new Callback<String>() {
				
				@Override
				public void success(String arg0, Response arg1) {
					Log.d("Success leaving chat room", arg0.toString());
				}
				
				@Override
				public void failure(RetrofitError arg0) {
					Log.e("Failure leaving chat room", arg0.toString());
				}
			});
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
