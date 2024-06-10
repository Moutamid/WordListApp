package com.moutamid.wordlistapp;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.List;
import java.util.Random;

public class ResendNotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "word_app_channel";
    private List<String[]> wordsList;
    private Random random = new Random();

    @Override
    public void onReceive(Context context, Intent intent) {
        wordsList = WordListSingleton.getInstance().getWordsList();
        sendNotification(context);
    }

    private void sendNotification(Context context) {
        if (wordsList != null && !wordsList.isEmpty()) {
            int index = random.nextInt(wordsList.size());
            String[] wordPair = wordsList.get(index);

            Intent mainIntent = new Intent(context, MainActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mainIntent.putExtra("WORD", wordPair[0]);
            mainIntent.putExtra("TRANSLATION", wordPair[1]);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Intent deleteIntent = new Intent(context, NotificationDismissedReceiver.class);
            PendingIntent deletePendingIntent = PendingIntent.getBroadcast(context, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle(wordPair[0])
                    .setContentText("Translation: " + wordPair[1])
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setDeleteIntent(deletePendingIntent);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(1, builder.build());
        }
    }
}
