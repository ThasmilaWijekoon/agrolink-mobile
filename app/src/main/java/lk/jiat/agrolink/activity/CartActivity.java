package lk.jiat.agrolink.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

import lk.jiat.agrolink.R;
import lk.jiat.agrolink.adapter.ProductAdapter;
import lk.jiat.agrolink.model.Order;
import lk.jiat.agrolink.model.Product;
import lk.jiat.agrolink.network.ApiClient;
import lk.jiat.agrolink.network.ApiService;
import lk.jiat.agrolink.util.CartManager;
import lk.jiat.agrolink.util.UserManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartActivity extends AppCompatActivity {

    private static final String TAG = "CartActivity";
    private static final int MAP_REQUEST_CODE = 100;
    private RecyclerView recyclerView;
    private TextView textTotal;
    private Button btnOrder;
    private Double selectedLat = null;
    private Double selectedLon = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        recyclerView = findViewById(R.id.recyclerViewCart);
        textTotal = findViewById(R.id.textTotal);
        btnOrder = findViewById(R.id.btnOrder);

        List<Product> cartList = CartManager.getCart();

        recyclerView.setLayoutManager(new LinearLayoutManager(CartActivity.this));
        recyclerView.setAdapter(new ProductAdapter(new ArrayList<>(cartList), true));

        double total = 0;
        for (Product p : cartList) {
            total += p.getPrice();
        }
        textTotal.setText("Total: Rs. " + String.format("%.2f", total));

        btnOrder.setOnClickListener(v -> {
            if (CartManager.getCart().isEmpty()) {
                Toast.makeText(this, "Cart is empty!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(CartActivity.this, MapActivity.class);
            startActivityForResult(intent, MAP_REQUEST_CODE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MAP_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            selectedLat = data.getDoubleExtra("lat", 0.0);
            selectedLon = data.getDoubleExtra("lon", 0.0);
            createOrderWithLocation();
        }
    }

    private void createOrderWithLocation() {
        final String userEmail = UserManager.getUser(this);
        final int userId = UserManager.getUserId(this);
        List<Product> cartList = CartManager.getCart();

        double tempTotal = 0;
        List<ApiService.CreateOrderItemRequest> orderItemRequests = new ArrayList<>();
        StringBuilder itemsDescriptionBuilder = new StringBuilder();

        for (Product p : cartList) {
            tempTotal += p.getPrice();
            orderItemRequests.add(new ApiService.CreateOrderItemRequest(p.getId(), 1, p.getPrice()));
            itemsDescriptionBuilder.append(p.getName()).append(", ");
        }
        if (itemsDescriptionBuilder.length() > 2) itemsDescriptionBuilder.setLength(itemsDescriptionBuilder.length() - 2);

        final double finalTotal = tempTotal;
        final String finalItemsDescription = itemsDescriptionBuilder.toString();

        ApiService.CreateOrderRequest orderRequest = new ApiService.CreateOrderRequest(
                finalTotal, "PENDING", userId, orderItemRequests, selectedLat, selectedLon
        );

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.createOrder(orderRequest).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Order createdOrder = response.body();
                    Toast.makeText(CartActivity.this, "Order Created! Opening Payment...", Toast.LENGTH_SHORT).show();
                    initiatePayHere(createdOrder.getId(), finalItemsDescription, userEmail, finalTotal);
                } else {
                    Toast.makeText(CartActivity.this, "Order Failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Order> call, Throwable t) {
                Toast.makeText(CartActivity.this, "Network Error!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initiatePayHere(int orderId, String items, String email, double amount) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        
        ApiService.PayHereRequest payHereRequest = new ApiService.PayHereRequest(
                orderId, items, "Customer", "User", email, "0771234567", "Colombo", "Colombo", "Sri Lanka"
        );

        apiService.createPayHereSession(payHereRequest).enqueue(new Callback<ApiService.PayHereCheckoutResponse>() {
            @Override
            public void onResponse(Call<ApiService.PayHereCheckoutResponse> call, Response<ApiService.PayHereCheckoutResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiService.PayHereCheckoutResponse pr = response.body();
                    
                    // ✅ නැවතත් PaymentActivity එකටම දත්ත යවමු
                    Intent intent = new Intent(CartActivity.this, PaymentActivity.class);
                    intent.putExtra("payhere_data", pr);
                    startActivity(intent);
                    
                    CartManager.getCart().clear();
                    finish();
                } else {
                    String message = "Payment initiation failed";
                    try {
                        if (response.errorBody() != null) {
                            String error = response.errorBody().string();
                            if (error != null && !error.trim().isEmpty()) {
                                message = error;
                            }
                        }
                    } catch (IOException ignored) {
                    }
                    Log.e(TAG, "PayHere session failed: code=" + response.code() + ", message=" + message);
                    Toast.makeText(CartActivity.this, message, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiService.PayHereCheckoutResponse> call, Throwable t) {
                Toast.makeText(CartActivity.this, "Connection Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
