package com.example.ywinked;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import io.noties.markwon.Markwon;
import java.util.Locale;

public class RightsSearchActivity extends AppCompatActivity {

    private EditText searchEditText;
    private Button searchButton;
    private View cardSuggestions;
    private RecyclerView recyclerSuggestions;
    private RecyclerView recyclerCategories;
    private RecyclerView recyclerRights;

    private SuggestionAdapter suggestionAdapter;
    private ArrayList<SuggestionModel> suggestions;

    private List<CategoryModel> categoryList;
    private List<RightModel> allRights;
    private RightsAdapter rightsAdapter;
    private String selectedCategory = "All";

    private Dialog aiSearchDialog;
    private TextToSpeech textToSpeech;
    private Handler typewriterHandler = new Handler(Looper.getMainLooper());
    private Runnable typewriterRunnable;
    private boolean isSpeaking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rights_search);

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.getDefault());
            }
        });

        // 🔹 Find views
        searchEditText = findViewById(R.id.editSearch);
        searchButton = findViewById(R.id.btnSearch);
        cardSuggestions = findViewById(R.id.cardSuggestions);
        recyclerSuggestions = findViewById(R.id.recyclerSuggestions);
        recyclerCategories = findViewById(R.id.recyclerCategories);
        recyclerRights = findViewById(R.id.recyclerRights);

        // 🔹 Back button logic
        View topBar = findViewById(R.id.topBar);
        if (topBar != null) {
            View backBtn = topBar.findViewById(topBar.getResources().getIdentifier("ic_back", "id", getPackageName()));
            if (backBtn == null && topBar instanceof android.view.ViewGroup) {
                android.view.ViewGroup vg = (android.view.ViewGroup) topBar;
                if (vg.getChildCount() > 0 && vg.getChildAt(0) instanceof android.widget.ImageView) {
                    vg.getChildAt(0).setOnClickListener(v -> finish());
                }
            } else if (backBtn != null) {
                backBtn.setOnClickListener(v -> finish());
            }
        }

        // 🔹 Suggestions setup
        recyclerSuggestions.setLayoutManager(new LinearLayoutManager(this));
        suggestions = new ArrayList<>();
        suggestions.add(new SuggestionModel("Helmet fine in India"));
        suggestions.add(new SuggestionModel("FIR filing procedure"));
        suggestions.add(new SuggestionModel("Women safety laws India"));
        suggestions.add(new SuggestionModel("Cyber crime complaint India"));
        suggestions.add(new SuggestionModel("Tenant rights India"));

        suggestionAdapter = new SuggestionAdapter(suggestions, query -> {
            searchEditText.setText(query);
            searchLocalRights(query);
        });
        recyclerSuggestions.setAdapter(suggestionAdapter);

        // 🔹 Show/hide suggestions on focus
        searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && searchEditText.getText().length() > 0) cardSuggestions.setVisibility(View.VISIBLE);
            else cardSuggestions.setVisibility(View.GONE);
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    cardSuggestions.setVisibility(View.GONE);
                    filterRights(selectedCategory);
                } else {
                    if (searchEditText.isFocused()) cardSuggestions.setVisibility(View.VISIBLE);
                    searchLocalRights(s.toString());
                }
            }
        });

        // 🔹 Search button click (local filter + AI modal search dialog)
        searchButton.setOnClickListener(v -> {
            String query = searchEditText.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(this, "Please enter or select a topic", Toast.LENGTH_SHORT).show();
                return;
            }
            searchLocalRights(query);
            showAISearchDialog(query);
        });

        // 🔹 Categories setup
        categoryList = new ArrayList<>();
        categoryList.add(new CategoryModel("All", true));
        categoryList.add(new CategoryModel("IPC", false));
        categoryList.add(new CategoryModel("Women", false));
        categoryList.add(new CategoryModel("Cyber", false));
        categoryList.add(new CategoryModel("Tenant", false));
        categoryList.add(new CategoryModel("Traffic", false));
        categoryList.add(new CategoryModel("Consumer", false));
        categoryList.add(new CategoryModel("Others", false));

        CategoryAdapter categoryAdapter = new CategoryAdapter(categoryList, categoryName -> {
            selectedCategory = categoryName;
            String query = searchEditText.getText().toString().trim();
            if (query.isEmpty()) {
                filterRights(categoryName);
            } else {
                searchLocalRights(query);
            }
        });
        recyclerCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerCategories.setAdapter(categoryAdapter);

        // 🔹 Initialize empty rights list (loaded dynamically)
        allRights = new ArrayList<>();
        rightsAdapter = new RightsAdapter(allRights);
        recyclerRights.setLayoutManager(new LinearLayoutManager(this));
        recyclerRights.setAdapter(rightsAdapter);

        // Fetch rights dynamically
        fetchRightsFromBackend();
    }

    // 🔹 Fetch rights from backend using JWT
    private void fetchRightsFromBackend() {
        try {
            java.io.InputStream is = getAssets().open("rights.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            Gson gson = new Gson();
            Type listType = new TypeToken<List<RightModel>>(){}.getType();
            List<RightModel> fetched = gson.fromJson(json, listType);

            allRights.clear();
            if (fetched != null) {
                allRights.addAll(fetched);
            }
            filterRights(selectedCategory);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load rights: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 🔹 Filter rights by category
    private void filterRights(String categoryName) {
        List<RightModel> filtered = new ArrayList<>();
        if (categoryName.equals("All")) {
            filtered.addAll(allRights);
        } else {
            for (RightModel r : allRights) {
                if (r.category != null && r.category.equals(categoryName)) {
                    filtered.add(r);
                }
            }
        }
        rightsAdapter = new RightsAdapter(filtered);
        recyclerRights.setAdapter(rightsAdapter);
    }

    // 🔹 Search rights locally matching title, description, cause or punishment
    private void searchLocalRights(String query) {
        List<RightModel> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        for (RightModel r : allRights) {
            boolean matchesCategory = selectedCategory.equals("All") || (r.category != null && r.category.equalsIgnoreCase(selectedCategory));
            if (matchesCategory) {
                boolean matchesSearch = (r.title != null && r.title.toLowerCase().contains(lowerQuery)) ||
                        (r.description != null && r.description.toLowerCase().contains(lowerQuery)) ||
                        (r.cause != null && r.cause.toLowerCase().contains(lowerQuery)) ||
                        (r.punishment != null && r.punishment.toLowerCase().contains(lowerQuery));
                if (matchesSearch) {
                    filtered.add(r);
                }
            }
        }
        rightsAdapter = new RightsAdapter(filtered);
        recyclerRights.setAdapter(rightsAdapter);
    }

    private void showAISearchDialog(String query) {
        // Stop any active TTS before showing new dialog
        if (textToSpeech != null && isSpeaking) {
            textToSpeech.stop();
            isSpeaking = false;
        }
        
        // Stop any active typewriter
        if (typewriterRunnable != null) {
            typewriterHandler.removeCallbacks(typewriterRunnable);
        }

        aiSearchDialog = new Dialog(this);
        aiSearchDialog.setContentView(R.layout.dialog_ai_search);
        if (aiSearchDialog.getWindow() != null) {
            aiSearchDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            aiSearchDialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        View btnCloseDialog = aiSearchDialog.findViewById(R.id.btnCloseDialog);
        View layoutLoading = aiSearchDialog.findViewById(R.id.layoutLoading);
        View layoutResult = aiSearchDialog.findViewById(R.id.layoutResult);
        TextView tvSearchResult = aiSearchDialog.findViewById(R.id.tvSearchResult);
        Button btnSpeakResult = (Button) aiSearchDialog.findViewById(R.id.btnSpeakResult);
        Button btnOk = (Button) aiSearchDialog.findViewById(R.id.btnOk);

        btnCloseDialog.setOnClickListener(v -> {
            if (textToSpeech != null) {
                textToSpeech.stop();
                isSpeaking = false;
            }
            if (typewriterRunnable != null) {
                typewriterHandler.removeCallbacks(typewriterRunnable);
            }
            aiSearchDialog.dismiss();
        });

        btnOk.setOnClickListener(v -> {
            if (textToSpeech != null) {
                textToSpeech.stop();
                isSpeaking = false;
            }
            if (typewriterRunnable != null) {
                typewriterHandler.removeCallbacks(typewriterRunnable);
            }
            aiSearchDialog.dismiss();
        });

        aiSearchDialog.setOnDismissListener(dialog -> {
            if (textToSpeech != null) {
                textToSpeech.stop();
                isSpeaking = false;
            }
            if (typewriterRunnable != null) {
                typewriterHandler.removeCallbacks(typewriterRunnable);
            }
        });

        // Query Backend AI
        queryAI(query, layoutLoading, layoutResult, tvSearchResult, btnSpeakResult);

        aiSearchDialog.show();
    }

    private void queryAI(String query, View layoutLoading, View layoutResult, TextView tvSearchResult, Button btnSpeakResult) {
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("query", query); // FastAPI expects "query"

        okhttp3.MediaType JSON = okhttp3.MediaType.get("application/json; charset=utf-8");
        okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonBody.toString(), JSON);
        String url = NetworkConfig.BASE_URL + "/query"; // Call FastAPI via Cloudflare tunnel

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    layoutLoading.setVisibility(View.GONE);
                    layoutResult.setVisibility(View.VISIBLE);
                    tvSearchResult.setText("❌ Network error: Could not reach the AI server. Please check your connection.");
                    btnSpeakResult.setVisibility(View.GONE);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    layoutLoading.setVisibility(View.GONE);
                    layoutResult.setVisibility(View.VISIBLE);

                    if (!response.isSuccessful()) {
                        tvSearchResult.setText("❌ Server error: Failed to get response from AI assistant.");
                        btnSpeakResult.setVisibility(View.GONE);
                        return;
                    }

                    try {
                        JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                        String botReply = jsonResponse.get("answer").getAsString();

                        // Clean up markdown markers for TTS read aloud
                        String cleanTTSMessage = botReply.replaceAll("[*#_`~-]", "").trim();

                        btnSpeakResult.setOnClickListener(v -> {
                            if (textToSpeech != null) {
                                if (isSpeaking) {
                                    textToSpeech.stop();
                                    isSpeaking = false;
                                    btnSpeakResult.setText("Read Aloud");
                                } else {
                                    textToSpeech.speak(cleanTTSMessage, TextToSpeech.QUEUE_FLUSH, null, null);
                                    isSpeaking = true;
                                    btnSpeakResult.setText("Stop Reading");
                                }
                            }
                        });

                        // Start Typewriter effect with live markdown rendering
                        startTypewriter(tvSearchResult, botReply);

                    } catch (Exception e) {
                        tvSearchResult.setText("❌ Error parsing AI response.");
                        btnSpeakResult.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    private void startTypewriter(TextView textView, String fullText) {
        final int delayMs = 15; // smooth fast speed
        final int charsPerStep = 3; // appends 3 characters at a time for swift streaming
        
        final int[] index = {0};
        final Markwon markwon = Markwon.create(this);

        typewriterRunnable = new Runnable() {
            @Override
            public void run() {
                if (index[0] < fullText.length()) {
                    index[0] = Math.min(index[0] + charsPerStep, fullText.length());
                    String currentText = fullText.substring(0, index[0]);
                    
                    // Render current chunk with markdown support
                    markwon.setMarkdown(textView, currentText);
                    
                    typewriterHandler.postDelayed(this, delayMs);
                }
              }
          };
          typewriterHandler.post(typewriterRunnable);
      }

      @Override
      protected void onDestroy() {
          if (textToSpeech != null) {
              textToSpeech.stop();
              textToSpeech.shutdown();
          }
          if (typewriterRunnable != null) {
              typewriterHandler.removeCallbacks(typewriterRunnable);
          }
          super.onDestroy();
      }
  }
