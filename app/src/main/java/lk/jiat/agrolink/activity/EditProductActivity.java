package lk.jiat.agrolink.activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;

import lk.jiat.agrolink.R;
import lk.jiat.agrolink.model.Category;
import lk.jiat.agrolink.model.Product;
import lk.jiat.agrolink.network.ApiClient;
import lk.jiat.agrolink.network.ApiService;
import lk.jiat.agrolink.util.UserManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProductActivity extends AppCompatActivity {

    private static final String TAG = "EditProductActivity";
    private EditText editName, editPrice, editStock, editDescription;
    private Spinner spinnerCategory;
    private ImageView imgProduct;
    private Button btnUpdate, btnDelete;
    private Product product;
    private String token;
    private String[] categories = {"Vegetables", "Fruits", "Grains", "Others"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_product);

        editName = findViewById(R.id.editEditProductName);
        editPrice = findViewById(R.id.editEditProductPrice);
        editStock = findViewById(R.id.editEditProductStock);
        editDescription = findViewById(R.id.editEditProductDescription);
        spinnerCategory = findViewById(R.id.spinnerEditCategory);
        imgProduct = findViewById(R.id.imgEditProduct);
        btnUpdate = findViewById(R.id.btnUpdateProduct);
        btnDelete = findViewById(R.id.btnDeleteProduct);

        token = UserManager.getToken(this);

        // Setup Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        // Get Product details from Intent
        product = getIntent().getParcelableExtra("product");

        if (product != null) {
            editName.setText(product.getName());
            editPrice.setText(String.valueOf(product.getPrice()));
            editStock.setText(String.valueOf(product.getStock()));
            editDescription.setText(product.getDescription());
            
            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                Picasso.get().load(product.getImageUrl()).into(imgProduct);
            }

            // Set current category in spinner
            if (product.getCategory() != null) {
                for (int i = 0; i < categories.length; i++) {
                    if (categories[i].equalsIgnoreCase(product.getCategory().getName())) {
                        spinnerCategory.setSelection(i);
                        break;
                    }
                }
            }
        }

        btnUpdate.setOnClickListener(v -> updateProduct());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void updateProduct() {
        String name = editName.getText().toString().trim();
        String priceStr = editPrice.getText().toString().trim();
        String stockStr = editStock.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String selectedCategory = spinnerCategory.getSelectedItem().toString();

        if (name.isEmpty() || priceStr.isEmpty() || stockStr.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        product.setName(name);
        product.setPrice(Double.parseDouble(priceStr));
        product.setStock(Integer.parseInt(stockStr));
        product.setDescription(description);
        
        Category cat = new Category();
        cat.setName(selectedCategory);
        product.setCategory(cat);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.updateAdminProduct("Bearer " + token, product.getId(), product).enqueue(new Callback<Product>() {
            @Override
            public void onResponse(Call<Product> call, Response<Product> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EditProductActivity.this, "Product Updated Successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditProductActivity.this, "Update Failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Product> call, Throwable t) {
                Toast.makeText(EditProductActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete this product?")
                .setPositiveButton("Yes, Delete", (dialog, which) -> deleteProduct())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProduct() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.deleteAdminProduct("Bearer " + token, product.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EditProductActivity.this, "Product Deleted!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditProductActivity.this, "Delete Failed", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(EditProductActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
