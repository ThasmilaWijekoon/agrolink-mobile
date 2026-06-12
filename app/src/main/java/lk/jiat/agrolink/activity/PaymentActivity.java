package lk.jiat.agrolink.activity;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import lk.jiat.agrolink.R;
import lk.jiat.agrolink.network.ApiService;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

public class PaymentActivity extends AppCompatActivity {

    private static final String TAG = "PaymentActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        WebView webView = findViewById(R.id.webViewPayment);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        
        webView.setWebViewClient(new WebViewClient());

        ApiService.PayHereCheckoutResponse data = (ApiService.PayHereCheckoutResponse) getIntent().getSerializableExtra("payhere_data");

        if (data != null) {
            try {
                String merchantId = safe(data.merchantId);
                String checkoutUrl = safe(data.checkoutUrl);
                String orderId = safe(data.orderId);
                String amount = safe(data.amount);
                String currency = safe(data.currency, "LKR");
                String hash = safe(data.hash);

                if (isBlank(checkoutUrl) || isBlank(merchantId) || isBlank(orderId) || isBlank(amount) || isBlank(hash)) {
                    Toast.makeText(this, "Payment setup is incomplete. Please try again.", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Invalid PayHere payload: checkoutUrl=" + checkoutUrl + ", merchantId=" + merchantId + ", orderId=" + orderId + ", amount=" + amount);
                    finish();
                    return;
                }

                // Formatting data exactly as PayHere expects for POST
                StringBuilder postData = new StringBuilder();
                postData.append("merchant_id=").append(URLEncoder.encode(merchantId, "UTF-8"));
                postData.append("&return_url=").append(URLEncoder.encode(safe(data.returnUrl), "UTF-8"));
                postData.append("&cancel_url=").append(URLEncoder.encode(safe(data.cancelUrl), "UTF-8"));
                postData.append("&notify_url=").append(URLEncoder.encode(safe(data.notifyUrl), "UTF-8"));
                postData.append("&order_id=").append(URLEncoder.encode(orderId, "UTF-8"));
                postData.append("&items=").append(URLEncoder.encode(safe(data.items), "UTF-8"));
                postData.append("&currency=").append(URLEncoder.encode(currency, "UTF-8"));
                postData.append("&amount=").append(URLEncoder.encode(amount, "UTF-8"));
                postData.append("&first_name=").append(URLEncoder.encode(safe(data.firstName), "UTF-8"));
                postData.append("&last_name=").append(URLEncoder.encode(safe(data.lastName), "UTF-8"));
                postData.append("&email=").append(URLEncoder.encode(safe(data.email), "UTF-8"));
                postData.append("&phone=").append(URLEncoder.encode(safe(data.phone), "UTF-8"));
                postData.append("&address=").append(URLEncoder.encode(safe(data.address), "UTF-8"));
                postData.append("&city=").append(URLEncoder.encode(safe(data.city), "UTF-8"));
                postData.append("&country=").append(URLEncoder.encode(safe(data.country, "Sri Lanka"), "UTF-8"));
                postData.append("&hash=").append(URLEncoder.encode(hash, "UTF-8"));

                Log.d(TAG, "Posting to: " + checkoutUrl);
                Log.d(TAG, "Data: " + postData.toString());

                webView.postUrl(checkoutUrl, postData.toString().getBytes(StandardCharsets.UTF_8));

            } catch (Exception e) {
                Log.e(TAG, "Error: " + e.getMessage());
                Toast.makeText(this, "Payment initialization failed", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            finish();
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(String value, String fallback) {
        String clean = safe(value);
        return clean.isEmpty() ? fallback : clean;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
