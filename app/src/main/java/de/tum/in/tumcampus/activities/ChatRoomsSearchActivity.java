package de.tum.in.tumcampus.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.gson.Gson;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collections;
import java.util.List;

import de.tum.in.tumcampus.R;
import de.tum.in.tumcampus.activities.generic.ActivityForAccessingTumOnline;
import de.tum.in.tumcampus.adapters.LecturesListAdapter;
import de.tum.in.tumcampus.auxiliary.ChatClient;
import de.tum.in.tumcampus.auxiliary.Const;
import de.tum.in.tumcampus.auxiliary.RSASigner;
import de.tum.in.tumcampus.auxiliary.Utils;
import de.tum.in.tumcampus.models.ChatMember;
import de.tum.in.tumcampus.models.ChatPublicKey;
import de.tum.in.tumcampus.models.ChatRegistrationId;
import de.tum.in.tumcampus.models.ChatRoom;
import de.tum.in.tumcampus.models.LecturesSearchRow;
import de.tum.in.tumcampus.models.LecturesSearchRowSet;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * This activity presents the chat rooms of user's
 * lectures using the TUMOnline web service
 *
 * @author Jana Pejic
 */
public class ChatRoomsSearchActivity extends ActivityForAccessingTumOnline {

    /**
     * filtered list which will be shown
     */
    LecturesSearchRowSet lecturesList = null;

    /**
     * UI elements
     */
    private StickyListHeadersListView lvMyLecturesList;

    private ChatRoom currentChatRoom;
    private ChatMember currentChatMember;
    private PrivateKey currentPrivateKey;

    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    String SENDER_ID = "1028528438269";

    static final String TAG = "GCM";

    GoogleCloudMessaging gcm;
    String regId;

    public ChatRoomsSearchActivity() {
        super(Const.LECTURES_PERSONAL, R.layout.activity_lectures);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // bind UI elements
        lvMyLecturesList = (StickyListHeadersListView) findViewById(R.id.lvMyLecturesList);
        lvMyLecturesList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                LecturesSearchRow item = (LecturesSearchRow) lvMyLecturesList.getItemAtPosition(position);

                checkPlayServicesAndRegister();

                // set bundle for LectureDetails and show it
                Bundle bundle = new Bundle();
                final Intent intent = new Intent(ChatRoomsSearchActivity.this, ChatActivity.class);
                intent.putExtras(bundle);

                String chatRoomUid = item.getSemester_id() + ":" + item.getTitel();

                currentChatRoom = new ChatRoom(chatRoomUid);
                ChatClient.getInstance().createGroup(currentChatRoom, new Callback<ChatRoom>() {
                    @Override
                    public void success(ChatRoom newlyCreatedChatRoom, Response arg1) {
                        // The POST request is successful because the chat room did not exist
                        // The newly created chat room is returned
                        Log.d("Success creating chat room", newlyCreatedChatRoom.toString());
                        currentChatRoom = newlyCreatedChatRoom;

                        showTermsIfNeeded(intent);
                    }

                    @Override
                    public void failure(RetrofitError arg0) {
                        // The POST request in unsuccessful because the chat room already exists,
                        // so we are trying to retrieve it with an additional GET request
                        Log.d("Failure creating chat room - trying to GET it from the server", arg0.toString());
                        List<ChatRoom> chatRooms = ChatClient.getInstance().getChatRoomWithName(currentChatRoom);
                        if(chatRooms!=null)
                            currentChatRoom = chatRooms.get(0);

                        showTermsIfNeeded(intent);
                    }
                });
            }
        });

        requestFetch();

        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        populateCurrentChatMember(sharedPrefs);
    }

    private void populateCurrentChatMember(final SharedPreferences sharedPrefs) {
        try {
            if (currentChatMember == null) {
                String lrzId = sharedPrefs.getString(Const.LRZ_ID, "");
                if (sharedPrefs.contains(Const.CHAT_ROOM_DISPLAY_NAME)) {
                    // If this is not the first time this user is opening the chat,
                    // we GET their data from the server using their lrzId
                    List<ChatMember> members = ChatClient.getInstance().getMember(lrzId);
                    currentChatMember = members.get(0);

                    checkPlayServicesAndRegister();
                } else {
                    // If the user is opening the chat for the first time, we need to display
                    // a dialog where they can enter their desired display name
                    currentChatMember = new ChatMember(lrzId);

                    LinearLayout layout = new LinearLayout(this);
                    layout.setOrientation(LinearLayout.VERTICAL);

                    final EditText etDisplayName = new EditText(this);
                    etDisplayName.setHint(R.string.display_name);
                    layout.addView(etDisplayName);

                    AlertDialog.Builder builder = new AlertDialog.Builder(ChatRoomsSearchActivity.this);
                    builder.setTitle(R.string.chat_display_name_title)
                            .setView(layout)
                            .setPositiveButton(getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    currentChatMember.setDisplayName(etDisplayName.getText().toString()); // TODO: Disallow empty display name

                                    // Save display name in shared preferences
                                    Editor editor = sharedPrefs.edit();
                                    editor.putString(Const.CHAT_ROOM_DISPLAY_NAME, currentChatMember.getDisplayName());
                                    editor.apply();

                                    // After the user has entered their display name,
                                    // send a request to the server to create the new member
                                    currentChatMember = ChatClient.getInstance().createMember(currentChatMember);

                                    checkPlayServicesAndRegister();
                                }
                            });

                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }
            }
        } catch (RetrofitError retrofitError) {
            retrofitError.printStackTrace();
            Utils.showLongCenteredToast(this, "Server is currently unavailable");
            this.finish();
        }
    }


    @Override
    public void onFetch(String rawResponse) {
        // deserialize the XML
        Serializer serializer = new Persister();
        try {
            lecturesList = serializer.read(LecturesSearchRowSet.class, rawResponse);
        } catch (Exception e) {
            Log.d("SIMPLEXML", "wont work: " + e.getMessage());
            progressLayout.setVisibility(View.GONE);
            failedTokenLayout.setVisibility(View.VISIBLE);
            e.printStackTrace();
            return;
        }

        if (lecturesList == null) {
            // no results found
            //TODO view no results
            lvMyLecturesList.setAdapter(null);
            return;
        }

        // Sort lectures by semester id
        List<LecturesSearchRow> lectures = lecturesList.getLehrveranstaltungen();
        Collections.sort(lectures);

        // set ListView to data via the LecturesListAdapter
        lvMyLecturesList.setAdapter(new LecturesListAdapter(ChatRoomsSearchActivity.this, lectures));
        progressLayout.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check device for Play Services APK.
        populateCurrentChatMember(PreferenceManager.getDefaultSharedPreferences(this));
    }

    private void showTermsIfNeeded(final Intent intent) {
        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ChatRoomsSearchActivity.this);
        //sharedPrefs.edit().remove(Const.CHAT_TERMS_SHOWN + "_" + currentChatRoom.getName()).commit();

        // If the terms have not been shown for this chat room, show them
        if (!sharedPrefs.getBoolean(Const.CHAT_TERMS_SHOWN + "_" + currentChatRoom.getName(), false)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ChatRoomsSearchActivity.this);
            builder.setTitle(R.string.chat_terms_title)
                    .setMessage(getResources().getString(R.string.chat_terms_body))
                    .setPositiveButton(getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (currentChatMember.getLrzId() != null) {
                                // Generate signature
                                RSASigner signer = new RSASigner(currentPrivateKey);
                                String signature = signer.sign(currentChatMember.getLrzId());
                                currentChatMember.setSignature(signature);

                                ChatClient.getInstance().joinChatRoom(currentChatRoom, currentChatMember, new Callback<ChatRoom>() {
                                    @Override
                                    public void success(ChatRoom arg0, Response arg1) {
                                        Log.d("Success joining chat room", arg0.toString());
                                        // Remember in sharedPrefs that the terms dialog was shown
                                        Editor editor = sharedPrefs.edit();
                                        editor.putBoolean(Const.CHAT_TERMS_SHOWN + "_" + currentChatRoom.getName(), true);
                                        editor.apply();

                                        moveToChatActivity(intent);
                                    }

                                    @Override
                                    public void failure(RetrofitError arg0) {
                                        Log.e("Failure joining chat room", arg0.toString());
                                        Utils.showLongCenteredToast(ChatRoomsSearchActivity.this, "Please activate your public key first");
                                    }
                                });
                            }
                        }
                    });

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        } else { // If the terms were already shown, just enter the chat room
            moveToChatActivity(intent);
        }
    }


    private void moveToChatActivity(final Intent intent) {
        // We need to move to the next activity now and provide the necessary data for it
        // We are sure that both currentChatRoom and currentChatMember exist
        intent.putExtra(Const.CURRENT_CHAT_ROOM, new Gson().toJson(currentChatRoom));
        intent.putExtra(Const.CURRENT_CHAT_MEMBER, new Gson().toJson(currentChatMember));
        startActivity(intent);
    }

    private PrivateKey retrieveOrGeneratePrivateKey() {
        // Generate/Retrieve private key
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (sharedPrefs.contains(Const.PRIVATE_KEY)) {
            // If the key is already generated, retrieve it from shared preferences
            String privateKeyString = sharedPrefs.getString(Const.PRIVATE_KEY, "");
            byte[] privateKeyBytes = Base64.decode(privateKeyString, Base64.DEFAULT);
            KeyFactory keyFactory;
            try {
                keyFactory = KeyFactory.getInstance("RSA");
                PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
                return keyFactory.generatePrivate(privateKeySpec);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            }
        } else {
            // If the key is not in shared preferences, generate key-pair
            KeyPairGenerator keyGen = null;
            try {
                keyGen = KeyPairGenerator.getInstance("RSA");
                keyGen.initialize(1024);
                KeyPair keyPair = keyGen.generateKeyPair();

                String publicKeyString = Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.DEFAULT);
                String privateKeyString = Base64.encodeToString(keyPair.getPrivate().getEncoded(), Base64.DEFAULT);

                // Save private key in shared preferences
                Editor editor = sharedPrefs.edit();
                editor.putString(Const.PRIVATE_KEY, privateKeyString);
                editor.apply();

                // Upload public key to the server
                ChatClient.getInstance().uploadPublicKey(currentChatMember.getUserId(), new ChatPublicKey(publicKeyString), new Callback<ChatPublicKey>() {
                    @Override
                    public void success(ChatPublicKey arg0, Response arg1) {
                        Log.d("Success uploading public key", arg0.toString());
                        Utils.showLongCenteredToast(ChatRoomsSearchActivity.this, "Public key activation mail sent to " + currentChatMember.getLrzId() + "@mytum.de");
                    }

                    @Override
                    public void failure(RetrofitError arg0) {
                        Log.e("Failure uploading public key", arg0.toString());
                    }
                });

                return keyPair.getPrivate();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            return null;
        }
        return null;
    }


    // GCM methods

    private void checkPlayServicesAndRegister() {
        currentPrivateKey = retrieveOrGeneratePrivateKey();

        // Check device for Play Services APK. If check succeeds,
        // proceed with GCM registration.
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            //getGCMPreferences(getApplicationContext()).edit().remove(Const.GCM_REG_ID).commit();
            regId = getRegistrationId(getApplicationContext());

            if (regId.isEmpty()) {
                registerInBackground();
            } else {
                // If the regId is not empty, we still need to check whether
                // it was successfully sent to the TCA server, because this
                // can fail due to user not confirming their private key
                boolean sentToTCAServer = isRegistrationIdSentToTCAServer(getApplicationContext());
                if (!sentToTCAServer) {
                    sendRegistrationIdToBackend();
                }
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Gets the current registration ID for application on GCM service.
     * <p/>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     * registration ID.
     */
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(Const.GCM_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    private boolean isRegistrationIdSentToTCAServer(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        return prefs.getBoolean(Const.GCM_REG_ID_SENT_TO_SERVER, false);
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGCMPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(ChatRoomsSearchActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p/>
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg;
                try {
                    Context context = getApplicationContext();
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regId = gcm.register(SENDER_ID);
                    msg = "GCM registration successful";

                    // You should send the registration ID to your server over HTTP,
                    // so it can use GCM/HTTP or CCS to send messages to your app.
                    sendRegistrationIdToBackend();

                    // For this demo: we don't need to send it because the device will send
                    // upstream messages to a server that echo back the message using the
                    // 'from' address in the message.

                    // Persist the regID - no need to register again.
                    storeRegistrationId(context, regId);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                Log.e(TAG, msg);
            }
        }.execute(null, null, null);
    }

    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP
     * or CCS to send messages to your app. Not needed for this demo since the
     * device sends upstream messages to a server that echoes back the message
     * using the 'from' address in the message.
     */
    private void sendRegistrationIdToBackend() {
        // Generate signature
        RSASigner signer = new RSASigner(currentPrivateKey);
        String signature = signer.sign(currentChatMember.getLrzId());

        ChatClient.getInstance().uploadRegistrationId(currentChatMember.getUserId(), new ChatRegistrationId(regId, signature), new Callback<ChatRegistrationId>() {
            @Override
            public void success(ChatRegistrationId arg0, Response arg1) {
                Log.d("Success uploading GCM registration id", arg0.toString());
                // Store in shared preferences the information that the
                // GCM registration id was sent to the TCA server successfully
                SharedPreferences sharedPrefs = getGCMPreferences(ChatRoomsSearchActivity.this);
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putBoolean(Const.GCM_REG_ID_SENT_TO_SERVER, true);
                editor.apply();
            }

            @Override
            public void failure(RetrofitError arg0) {
                Log.e("Failure uploading GCM registration id", arg0.toString());
            }
        });
    }

    /**
     * Stores the registration ID and app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId   registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Const.GCM_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.apply();
    }
}
