package com.example.handwriting;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.github.gcacace.signaturepad.views.SignaturePad;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private SignaturePad signaturePad;
    private Button confirmButton;
    private Button clearButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        signaturePad = findViewById(R.id.signature_pad);
        signaturePad.setMinWidth(15f);  // 最小笔触宽度
        signaturePad.setMaxWidth(20f); // 最大笔触宽度


        confirmButton = findViewById(R.id.confirm_button);
        clearButton = findViewById(R.id.clear_button);

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!signaturePad.isEmpty()) {
                    sendOcrRequest(signaturePad.getSignatureBitmap());
                } else {
                    showToast("请先签名");
                }
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signaturePad.clear();
            }
        });
    }

    private void sendOcrRequest(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT);

        RequestBody body = new FormBody.Builder()
                .add("image", encodedImage)
                .build();

        Request request = new Request.Builder()
                .url("https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic?access_token=24.acb1d6aaca78a37782e7751676a30300.2592000.1706079051.282335-45703283")
                .post(body)
                .build();

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    showToast("请求失败: " + response);
                } else {
                    try {
                        final String responseData = response.body().string();
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONArray wordsResult = jsonObject.getJSONArray("words_result");
                        StringBuilder recognizedText = new StringBuilder();
                        for (int i = 0; i < wordsResult.length(); i++) {
                            JSONObject word = wordsResult.getJSONObject(i);
                            recognizedText.append(word.getString("words"));
                            recognizedText.append("\n");
                        }
                        Log.d("OCRResponse", responseData);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showToast("你是" + recognizedText.toString());
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }
}
