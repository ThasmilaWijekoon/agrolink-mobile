package lk.jiat.agrolink.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

import lk.jiat.agrolink.R;
import lk.jiat.agrolink.model.Category;
import lk.jiat.agrolink.model.Product;
import lk.jiat.agrolink.network.ApiClient;
import lk.jiat.agrolink.network.ApiService;
import lk.jiat.agrolink.util.UserManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddProductActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView imgSelectProduct;
    private EditText editName, editPrice, editStock, editDescription;
    private Spinner spinnerCategory;
    private Button btnSelectImage, btnSaveProduct;
    private Uri imageUri;
    private String[] categories = {"Vegetables", "Fruits", "Grains", "Others"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        imgSelectProduct = findViewById(R.id.imgSelectProduct);
        editName = findViewById(R.id.editProductName);
        editPrice = findViewById(R.id.editProductPrice);
        editStock = findViewById(R.id.editProductStock);
        editDescription = findViewById(R.id.editProductDescription);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSaveProduct = findViewById(R.id.btnSaveProduct);

        // Setup Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        btnSelectImage.setOnClickListener(v -> openGallery());

        btnSaveProduct.setOnClickListener(v -> {
            if (validateForm()) {
                uploadImageToFirebase();
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            imgSelectProduct.setImageURI(imageUri);
        }
    }

    private boolean validateForm() {
        if (imageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            return false;
        }
        String name = editName.getText().toString().trim();
        String price = editPrice.getText().toString().trim();
        String stock = editStock.getText().toString().trim();

        if (name.isEmpty() || price.isEmpty() || stock.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void uploadImageToFirebase() {
        String fileName = UUID.randomUUID().toString();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("product_images/" + fileName);

        storageRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl = uri.toString();
                saveProductToBackend(imageUrl);
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Image Upload Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void saveProductToBackend(String imageUrl) {
        String name = editName.getText().toString().trim();
        String priceStr = editPrice.getText().toString().trim();
        String stockStr = editStock.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        
        // අලුත් Category ඔබ්ජෙක්ට් එකක් සාදා තෝරාගත් නම ලබා දෙමු
        String selectedCategoryName = spinnerCategory.getSelectedItem().toString();
        Category categoryObj = new Category();
        categoryObj.setName(selectedCategoryName);

        double price = Double.parseDouble(priceStr);
        int stock = Integer.parseInt(stockStr);

        // Product එක සාදන විට String එක වෙනුවට Category Object එක ලබා දෙමු
        Product product = new Product(0, name, description, price, stock, imageUrl, categoryObj);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        String token = UserManager.getToken(this);

        apiService.addAdminProduct("Bearer " + token, product).enqueue(new Callback<Product>() {
            @Override
            public void onResponse(Call<Product> call, Response<Product> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddProductActivity.this, "Product Saved Successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AddProductActivity.this, "Failed to save product: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Product> call, Throwable t) {
                Toast.makeText(AddProductActivity.this, "Network Error!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
