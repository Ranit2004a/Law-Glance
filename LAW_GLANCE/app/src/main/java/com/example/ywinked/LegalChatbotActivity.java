package com.example.ywinked;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

public class LegalChatbotActivity extends AppCompatActivity {

    private RecyclerView recyclerViewChat;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private ImageButton buttonMic;
    private ChatAdapter chatAdapter;
    private ArrayList<MessageModel> messagesList;

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private boolean isListening = false;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private static final MediaType JSON =
            MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
            
    private String userEmail = "anonymous@example.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legal_chatbot);

        // Load user email from SharedPreferences session
        SharedPreferences sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userEmail = sharedPref.getString("email", "anonymous@example.com");

        recyclerViewChat = findViewById(R.id.recyclerViewChat);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        buttonMic = findViewById(R.id.buttonMic);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        View btnClearChat = findViewById(R.id.btnClearChat);
        if (btnClearChat != null) {
            btnClearChat.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(LegalChatbotActivity.this)
                        .setTitle("Clear Chat History")
                        .setMessage("Are you sure you want to delete all messages? This action cannot be undone.")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            clearChatOnServer();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.getDefault());
            }
        });

        // Initialize SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        buttonMic.setOnClickListener(v -> {
            if (isListening) {
                stopSpeechRecognition();
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, 
                            new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
                } else {
                    startSpeechRecognition();
                }
            }
        });

        messagesList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messagesList, this::speak, (message, position) -> {
            if (message.getDatabaseId() != null) {
                new androidx.appcompat.app.AlertDialog.Builder(LegalChatbotActivity.this)
                        .setTitle("Delete Message")
                        .setMessage("Do you want to delete this message?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            deleteChatLogFromServer(message.getDatabaseId());
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);

        recyclerViewChat.setLayoutManager(layoutManager);
        recyclerViewChat.setAdapter(chatAdapter);

        buttonSend.setOnClickListener(v -> {
            String message = editTextMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                addMessage(message, true);
                editTextMessage.setText("");
                showThinking(); // Show thinking bubble
                sendMessageToBot(message);
            }
        });

        loadChatHistory();
    }

    private void loadChatHistory() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore.getInstance().collection("Users")
                .document(uid)
                .collection("ChatHistory")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    messagesList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String logId = doc.getId();
                        String userMsg = doc.getString("userMessage");
                        String botResp = doc.getString("botResponse");

                        if (userMsg != null) {
                            MessageModel userMsgModel = new MessageModel(userMsg, true);
                            userMsgModel.setDatabaseId(logId);
                            messagesList.add(userMsgModel);
                        }
                        if (botResp != null) {
                            MessageModel botMsgModel = new MessageModel(botResp, false);
                            botMsgModel.setDatabaseId(logId);
                            messagesList.add(botMsgModel);
                        }
                    }
                    chatAdapter.notifyDataSetChanged();
                    if (messagesList.size() > 0) {
                        recyclerViewChat.scrollToPosition(messagesList.size() - 1);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LegalChatbotActivity.this, "Failed to load chat history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private MessageModel addMessage(String message, boolean isUser) {
        MessageModel model = new MessageModel(message, isUser);
        messagesList.add(model);
        chatAdapter.notifyItemInserted(messagesList.size() - 1);
        recyclerViewChat.scrollToPosition(messagesList.size() - 1);
        return model;
    }

    private void showThinking() {
        messagesList.add(new MessageModel(true));
        chatAdapter.notifyItemInserted(messagesList.size() - 1);
        recyclerViewChat.scrollToPosition(messagesList.size() - 1);
    }

    private void hideThinking() {
        if (messagesList.size() > 0) {
            int lastIndex = messagesList.size() - 1;
            if (messagesList.get(lastIndex).isThinking()) {
                messagesList.remove(lastIndex);
                chatAdapter.notifyItemRemoved(lastIndex);
            }
        }
    }

    private void sendMessageToBot(String userMessage) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            hideThinking();
            addMessage("❌ Error: User not logged in", false);
            return;
        }

        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("query", userMessage); // FastAPI expects "query"

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        String url = NetworkConfig.BASE_URL + "/query"; // Call FastAPI via Cloudflare tunnel

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    hideThinking();
                    addMessage("❌ Network error: " + e.getMessage(), false);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    hideThinking();
                    if (!response.isSuccessful()) {
                        addMessage("❌ AI Service Error: " + response.message(), false);
                        return;
                    }

                    try {
                        JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                        String botReply = jsonResponse.get("answer").getAsString();

                        // Save chat conversation to Firestore
                        java.util.Map<String, Object> chatLog = new java.util.HashMap<>();
                        chatLog.put("userMessage", userMessage);
                        chatLog.put("botResponse", botReply);
                        chatLog.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

                        FirebaseFirestore.getInstance().collection("Users")
                                .document(uid)
                                .collection("ChatHistory")
                                .add(chatLog)
                                .addOnSuccessListener(documentReference -> {
                                    String logId = documentReference.getId();
                                    MessageModel botMsg = addMessage(botReply.trim(), false);
                                    botMsg.setDatabaseId(logId);
                                    if (messagesList.size() >= 2) {
                                        MessageModel userMsg = messagesList.get(messagesList.size() - 2);
                                        if (userMsg.isUser()) {
                                            userMsg.setDatabaseId(logId);
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    // Still show the message even if saving to history failed
                                    addMessage(botReply.trim(), false);
                                    Toast.makeText(LegalChatbotActivity.this, "Failed to save to history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });

                    } catch (Exception e) {
                        addMessage("❌ Error parsing AI response", false);
                    }
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSpeechRecognition();
            } else {
                Toast.makeText(this, "Permission denied to record audio", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                isListening = true;
                editTextMessage.setHint("Listening...");
                buttonMic.setColorFilter(ContextCompat.getColor(LegalChatbotActivity.this, android.R.color.holo_red_light));
            }
            
            @Override
            public void onBeginningOfSpeech() {}
            
            @Override
            public void onRmsChanged(float rmsdB) {}
            
            @Override
            public void onBufferReceived(byte[] buffer) {}
            
            @Override
            public void onEndOfSpeech() {
                stopSpeechRecognition();
            }
            
            @Override
            public void onError(int error) {
                stopSpeechRecognition();
                String message;
                switch (error) {
                    case SpeechRecognizer.ERROR_AUDIO:
                        message = "Audio recording error"; break;
                    case SpeechRecognizer.ERROR_CLIENT:
                        message = "Client side error"; break;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        message = "Insufficient permissions"; break;
                    case SpeechRecognizer.ERROR_NETWORK:
                        message = "Network error"; break;
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        message = "Network timeout"; break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        message = "No match found"; break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        message = "Recognition service busy"; break;
                    case SpeechRecognizer.ERROR_SERVER:
                        message = "Server error"; break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        message = "No speech input"; break;
                    default:
                        message = "Speech recognition error"; break;
                }
                Toast.makeText(LegalChatbotActivity.this, message, Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String text = matches.get(0);
                    String currentText = editTextMessage.getText().toString();
                    if (currentText.isEmpty()) {
                        editTextMessage.setText(text);
                    } else {
                        editTextMessage.setText(currentText + " " + text);
                    }
                    editTextMessage.setSelection(editTextMessage.getText().length());
                }
            }
            
            @Override
            public void onPartialResults(Bundle partialResults) {}
            
            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
        
        speechRecognizer.startListening(intent);
    }
    
    private void stopSpeechRecognition() {
        isListening = false;
        editTextMessage.setHint("Type your message...");
        buttonMic.clearColorFilter();
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }

    private void speak(String text) {
        if (textToSpeech != null) {
            if (textToSpeech.isSpeaking()) {
                textToSpeech.stop();
            } else {
                // Strip Markdown characters for cleaner speaking (e.g. asterisks, bullet marks)
                String cleanText = text.replaceAll("\\*+", "")
                                      .replaceAll("📄 Sources:", "")
                                      .replaceAll("•", "")
                                      .replaceAll("❌", "")
                                      .trim();
                textToSpeech.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "ChatTTS");
            }
        }
    }

    private void clearChatOnServer() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("Users")
                .document(uid)
                .collection("ChatHistory")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    com.google.firebase.firestore.WriteBatch batch = FirebaseFirestore.getInstance().batch();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit().addOnSuccessListener(aVoid -> {
                        messagesList.clear();
                        chatAdapter.notifyDataSetChanged();
                        Toast.makeText(LegalChatbotActivity.this, "Chat history cleared", Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(LegalChatbotActivity.this, "Failed to clear history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LegalChatbotActivity.this, "Failed to retrieve history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteChatLogFromServer(String id) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("Users")
                .document(uid)
                .collection("ChatHistory")
                .document(id)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    for (int i = messagesList.size() - 1; i >= 0; i--) {
                        MessageModel msg = messagesList.get(i);
                        if (msg.getDatabaseId() != null && msg.getDatabaseId().equals(id)) {
                            messagesList.remove(i);
                            chatAdapter.notifyItemRemoved(i);
                        }
                    }
                    Toast.makeText(LegalChatbotActivity.this, "Message deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LegalChatbotActivity.this, "Failed to delete message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }
}
