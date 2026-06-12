package lk.jiat.agrolink.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import lk.jiat.agrolink.R;
import lk.jiat.agrolink.model.User;
import lk.jiat.agrolink.network.ApiClient;
import lk.jiat.agrolink.network.ApiService;
import lk.jiat.agrolink.util.UserManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private static final int MAP_REQUEST_CODE = 2001;
    
    private EditText editName, editEmail, editPhone, editAddress;
    private Button btnUpdate, btnOpenMap;
    private int userId;
    private String token;
    
    // To store selected coordinates
    private Double selectedLat = null;
    private Double selectedLon = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        editName = findViewById(R.id.editProfileName);
        editEmail = findViewById(R.id.editProfileEmail);
        editPhone = findViewById(R.id.editProfilePhone);
        editAddress = findViewById(R.id.editProfileAddress);
        btnUpdate = findViewById(R.id.btnSaveProfile);
        btnOpenMap = findViewById(R.id.btnOpenMap);

        userId = UserManager.getUserId(this);
        token = UserManager.getToken(this);

        if (userId == -1) {
            Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadUserDetails();

        btnUpdate.setOnClickListener(v -> updateProfile());
        
        btnOpenMap.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, MapActivity.class);
            startActivityForResult(intent, MAP_REQUEST_CODE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MAP_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            selectedLat = data.getDoubleExtra("lat", 0);
            selectedLon = data.getDoubleExtra("lon", 0);
            
            Log.d(TAG, "Selected Lat: " + selectedLat + ", Lon: " + selectedLon);
            getAddressFromLocation(selectedLat, selectedLon);
        }
    }

    private void getAddressFromLocation(double lat, double lon) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String fullAddress = addresses.get(0).getAddressLine(0);
                editAddress.setText(fullAddress);
                Toast.makeText(this, "Location coordinates updated!", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder error: " + e.getMessage());
        }
    }

    private void loadUserDetails() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        String authHeader = (token != null) ? "Bearer " + token : "";

        apiService.getUserDetails(authHeader, userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    editName.setText(user.getName());
                    editEmail.setText(user.getEmail());
                    editPhone.setText(user.getPhone() != null ? user.getPhone() : "");
                    editAddress.setText(user.getAddress() != null ? user.getAddress() : "");
                    
                    // Keep existing coordinates if any
                    selectedLat = user.getLatitude();
                    selectedLon = user.getLongitude();
                }
            }
            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Log.e(TAG, "Error: " + t.getMessage());
            }
        });
    }

    private void updateProfile() {
        String name = editName.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        String address = editAddress.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create updated user object including coordinates
        User updatedUser = new User();
        updatedUser.setName(name);
        updatedUser.setPhone(phone);
        updatedUser.setAddress(address);
        updatedUser.setLatitude(selectedLat);
        updatedUser.setLongitude(selectedLon);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        String authHeader = (token != null) ? "Bearer " + token : "";

        apiService.updateUserProfile(authHeader, userId, updatedUser).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ProfileActivity.this, "Profile and Location saved!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ProfileActivity.this, "Update Failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
