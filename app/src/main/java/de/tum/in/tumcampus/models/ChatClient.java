package de.tum.in.tumcampus.models;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.tum.in.tumcampus.auxiliary.Const;
import de.tum.in.tumcampus.auxiliary.NetUtils;
import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.android.AndroidLog;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

public class ChatClient {

    private static final String API_URL = Const.CHAT_URL;

    private static ChatClient instance = null;
    private ChatService service = null;

    private static Context c = null;


    private ChatClient() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(API_URL)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setLog(new AndroidLog("suqmadiq"))
                .setRequestInterceptor(requestInterceptor)
                .build();
        service = restAdapter.create(ChatService.class);
    }

    public static ChatClient getInstance(Context c) {
        ChatClient.c = c;
        if (instance == null) {
            instance = new ChatClient();
        }
        return instance;
    }

    RequestInterceptor requestInterceptor = new RequestInterceptor() {
        @Override
        public void intercept(RequestFacade request) {
            //Log.e("TCA Chat", "hooking ID: " + NetUtils.getDeviceID(ChatClient.c) + " " + Utils.getInternalSettingString(ChatClient.c, Const.GCM_REG_ID, ""));
            Log.e("TCA Chat",request.toString());
            request.addHeader("X-DEVICE-ID", NetUtils.getDeviceID(ChatClient.c));
        }
    };

    private interface ChatService {

        @POST("/rooms/")
        void createChatRoom(@Body ChatRoom chatRoom, Callback<ChatRoom> cb);

        @GET("/rooms/{roomName}/")
        List<ChatRoom> getChatRoomWithName(@Path("roomName") String roomName);

        @GET("/rooms/{groupId}")
        ChatRoom getChatRoom(@Path("groupId") String id);

        @POST("/rooms/{groupId}/join/")
        void joinChatRoom(@Path("groupId") String groupId, @Body ChatMember chatMember, Callback<ChatRoom> cb);

        @POST("/rooms/{groupId}/leave/")
        void leaveChatRoom(@Path("groupId") String groupId, @Body ChatMember chatMember, Callback<ChatRoom> cb);

        @POST("/rooms/{groupId}/messages/")
        CreateChatMessage sendMessage(@Path("groupId") String groupId, @Body CreateChatMessage chatMessage);

        @GET("/rooms/{groupId}/messages/{page}/")
        void getMessages(@Path("groupId") String groupId, @Path("page") int page, Callback<ArrayList<ListChatMessage>> cb);

        @GET("/rooms/{groupId}/messages/{page}/")
        ArrayList<ListChatMessage> getMessages(@Path("groupId") String groupId, @Path("page") long page);

        @GET("/rooms/{groupId}/messages/")
        ArrayList<ListChatMessage> getNewMessages(@Path("groupId") String groupId);

        @POST("/members/")
        ChatMember createMember(@Body ChatMember chatMember);

        @GET("/members/{lrz_id}/")
        List<ChatMember> getMember(@Path("lrz_id") String lrzId);

        @POST("/members/{memberId}/pubkeys/")
        void uploadPublicKey(@Path("memberId") String memberId, @Body ChatPublicKey publicKey, Callback<ChatPublicKey> cb);

        @POST("/members/{memberId}/rooms/")
        List<ChatRoom> getMemberRooms(@Path("memberId") String memberId, @Body ChatVerfication verfication);

        @GET("/members/{memberId}/pubkeys/")
        List<ChatPublicKey> getPublicKeysForMember(@Path("memberId") String memberId);

        @POST("/members/{memberId}/registration_ids/add_id")
        void uploadRegistrationId(@Path("memberId") String memberId, @Body ChatRegistrationId regId, Callback<ChatRegistrationId> cb);

    }

    public void createGroup(ChatRoom chatRoom, Callback<ChatRoom> cb) {
        service.createChatRoom(chatRoom, cb);
    }

    public List<ChatRoom> getChatRoomWithName(ChatRoom chatRoom) {
        return service.getChatRoomWithName(chatRoom.getName());
    }

    public ChatRoom getChatRoom(String id) {
        return service.getChatRoom(id);
    }

    public ChatMember createMember(ChatMember chatMember) {
        return service.createMember(chatMember);
    }

    public List<ChatMember> getMember(String lrzId) {
        return service.getMember(lrzId);
    }

    public void joinChatRoom(ChatRoom chatRoom, ChatMember chatMember, Callback<ChatRoom> cb) {
        service.joinChatRoom(chatRoom.getGroupId(), chatMember, cb);
    }

    public void leaveChatRoom(ChatRoom chatRoom, ChatMember chatMember, Callback<ChatRoom> cb) {
        service.leaveChatRoom(chatRoom.getGroupId(), chatMember, cb);
    }

    public CreateChatMessage sendMessage(String groupId, CreateChatMessage chatMessage) {
        return service.sendMessage(groupId, chatMessage);
    }

    public void getMessages(String groupId, int page, Callback<ArrayList<ListChatMessage>> cb) {
        service.getMessages(groupId, page, cb);
    }

    public ArrayList<ListChatMessage> getMessages(String groupId, long page) {
        return service.getMessages(groupId, page);
    }

    public ArrayList<ListChatMessage> getNewMessages(String groupId) {
        return service.getNewMessages(groupId);
    }

    public void uploadPublicKey(String memberId, ChatPublicKey publicKey, Callback<ChatPublicKey> cb) {
        service.uploadPublicKey(memberId, publicKey, cb);
    }

    public List<ChatRoom> getMemberRooms(String memberId, ChatVerfication verfication) {
        return service.getMemberRooms(memberId, verfication);
    }

    public List<ChatPublicKey> getPublicKeysForMember(String memberId) {
        return service.getPublicKeysForMember(memberId);
    }

    public void uploadRegistrationId(String memberId, ChatRegistrationId regId, Callback<ChatRegistrationId> cb) {
        service.uploadRegistrationId(memberId, regId, cb);
    }
}