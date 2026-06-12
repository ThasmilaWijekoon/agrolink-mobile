package lk.jiat.agrolink.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessaging;

import lk.jiat.agrolink.R;
import lk.jiat.agrolink.fragment.HomeFragment;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Notification Permission ලබා ගැනීමට Launcher එකක් හදමු (Android 13+)
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notification Permission Granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Notification Permission Denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Android 13 (API 33) හෝ ඊට ඉහළ නම් Permission ඉල්ලමු
        askNotificationPermission();

        // FCM Token එක ලබාගෙන Log එකේ පෙන්වමු
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    Log.d(TAG, "FCM_TOKEN_DEBUG: " + token);
                    // ඔබට පහසුවෙන් Copy කර ගැනීමට පහත Log එක බලන්න
                    System.out.println("COPY_THIS_TOKEN: " + token);
                });

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}
