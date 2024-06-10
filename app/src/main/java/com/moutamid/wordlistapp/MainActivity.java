package com.moutamid.wordlistapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private TextView wordTextView, translationTextView;
    private List<String[]> wordsList = new ArrayList<>();
    private Random random = new Random();
    private Handler inactivityHandler = new Handler(Looper.getMainLooper());
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable inactivityRunnable, timerRunnable;
    private TextToSpeech textToSpeech;
    private GestureDetector gestureDetector;
    private static final String CHANNEL_ID = "word_app_channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wordTextView = findViewById(R.id.wordTextView);
        translationTextView = findViewById(R.id.translationTextView);

        loadWords();
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("WORD") && intent.hasExtra("TRANSLATION")) {
            String word = intent.getStringExtra("WORD");
            String translation = intent.getStringExtra("TRANSLATION");
            displayWord(word, translation);
        } else {
            displayRandomWord();
        }

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US); // Set language to US English
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "Language not supported", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Text-to-Speech initialization failed", Toast.LENGTH_SHORT).show();
            }
        });

        gestureDetector = new GestureDetector(this, new GestureListener());
        checkApp(MainActivity.this);
        inactivityRunnable = this::minimizeApp;
        timerRunnable = this::notifyNewWord;
        startInactivityTimer();
        startAppTimer();
        createNotificationChannel();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    private void loadWords() {
        try {
            InputStream inputStream = getResources().openRawResource(R.raw.words); // Load words from res/raw/words.txt
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    wordsList.add(parts);
                }
            }
            reader.close();
            WordListSingleton.getInstance().setWordsList(wordsList);  // Set the list in singleton
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void displayRandomWord() {
        if (!wordsList.isEmpty()) {
            int index = random.nextInt(wordsList.size());
            String[] wordPair = wordsList.get(index);
            displayWord(wordPair[0], wordPair[1]);
        }
    }

    private void displayWord(String word, String translation) {
        wordTextView.setText(word);
        translationTextView.setText(translation);
    }
    private void speakWord() {
        String word = wordTextView.getText().toString();
        if (!word.isEmpty()) {
            textToSpeech.speak(word, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void resetInactivityTimer() {
        inactivityHandler.removeCallbacks(inactivityRunnable);
        inactivityHandler.postDelayed(inactivityRunnable, 60000); // 1 minute
    }

    private void startInactivityTimer() {
        inactivityHandler.postDelayed(inactivityRunnable, 10000); // 10 sec
    }

    private void minimizeApp() {
        moveTaskToBack(true);
    }

    private void startAppTimer() {
        timerHandler.postDelayed(timerRunnable, 60000); // 1 minute
    }

    public void notifyNewWord() {
        if (!wordsList.isEmpty()) {
            int index = random.nextInt(wordsList.size());
            String[] wordPair = wordsList.get(index);
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Intent deleteIntent = new Intent(this, NotificationDismissedReceiver.class);
            PendingIntent deletePendingIntent = PendingIntent.getBroadcast(this, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle(wordPair[0])
                    .setContentText("Translation: " + wordPair[1])
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setDeleteIntent(deletePendingIntent);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify(1, builder.build());
        }
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = "Word Notification Channel";
            String description = "Channel for Word App notifications";
            int importance = android.app.NotificationManager.IMPORTANCE_HIGH;
            android.app.NotificationChannel channel = new android.app.NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            android.app.NotificationManager notificationManager = getSystemService(android.app.NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            speakWord();
            resetInactivityTimer();
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            displayRandomWord();
            return true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startInactivityTimer();
        startAppTimer();
    }

    public static void checkApp(Activity activity) {
        String appName = "WordListApp";

        new Thread(() -> {
            URL google = null;
            try {
                google = new URL("https://raw.githubusercontent.com/Moutamid/Moutamid/main/apps.txt");
            } catch (final MalformedURLException e) {
                e.printStackTrace();
            }
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(google != null ? google.openStream() : null));
            } catch (final IOException e) {
                e.printStackTrace();
            }
            String input = null;
            StringBuffer stringBuffer = new StringBuffer();
            while (true) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        if ((input = in != null ? in.readLine() : null) == null) break;
                    }
                } catch (final IOException e) {
                    e.printStackTrace();
                }
                stringBuffer.append(input);
            }
            try {
                if (in != null) {
                    in.close();
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
            String htmlData = stringBuffer.toString();

            try {
                JSONObject myAppObject = new JSONObject(htmlData).getJSONObject(appName);

                boolean value = myAppObject.getBoolean("value");
                String msg = myAppObject.getString("msg");

                if (value) {
                    activity.runOnUiThread(() -> {
                        new AlertDialog.Builder(activity).setMessage(msg).setCancelable(false).show();
                    });
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }).start();
    }


}
