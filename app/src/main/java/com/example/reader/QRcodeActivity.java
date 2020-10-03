package com.example.reader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.reader.Notification.APIService;
import com.example.reader.Notification.Client;
import com.example.reader.Notification.Data;
import com.example.reader.Notification.MyResponse;
import com.example.reader.Notification.Sender;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.okhttp.internal.Internal;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QRcodeActivity extends AppCompatActivity {

    SurfaceView surfaceView;        //display camera area
    TextView textView;              //save scan words
    CameraSource cameraSource;      //camera
    BarcodeDetector barcodeDetector;//google vision
    Button nfcPage;                 //nfc page
    Spinner spinner;                //place menu
    TextView studentID, cardID, time;//post to server
    int cu_time = 0;
    String code;
    Handler mHandlerTime = new Handler();
    APIService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);

        getCameraPermission();

        surfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        textView = (TextView) findViewById(R.id.scan_result);

        nfcPage = (Button) findViewById(R.id.nfc_page);
        spinner = (Spinner) findViewById(R.id.spinnerTwo);
        ArrayAdapter<String> placeList = new ArrayAdapter<>(QRcodeActivity.this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.place));
        placeList.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(placeList);
        apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);

        studentID = (TextView) findViewById(R.id.tv_id);
        cardID = (TextView) findViewById(R.id.tv_code);
        time = (TextView) findViewById(R.id.tv_time);

        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE).build();
        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(720, 1200)
                .setAutoFocusEnabled(true)
                .build();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED)
                    return;
                try {
                    cameraSource.start(holder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                //shutdown camera
                if (cameraSource != null){
                    cameraSource.stop();
                }
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {

            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> qrCodes = detections.getDetectedItems();
                if (qrCodes.size() != 0) {
                    textView.post(new Runnable() {
                        @Override
                        public void run() {
                            if (cameraSource != null){
                                cameraSource.stop();
                            }
                            code = qrCodes.valueAt(0).displayValue;

                            //textView.setText(code);

                            if (cameraSource != null) {
                                cameraSource.release();
                                cameraSource = null;
                            }

                            //code is Card Id
                            swipe(code);
                        }
                    });
                }
            }
        });

        nfcPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    public void getCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(QRcodeActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
            }
        }
    }

    private String getTime() {

        Calendar c = Calendar.getInstance();

        int year = c.get(Calendar.YEAR);

        int month = (c.get(Calendar.MONTH)) + 1;

        int day = c.get(Calendar.DAY_OF_MONTH);

        int hour = c.get(Calendar.HOUR_OF_DAY);

        int minute = c.get(Calendar.MINUTE);
        String str;
        str = String.format("%04d", year) + "/" + String.format("%02d", month) + "/" + String.format("%02d", day) + " " + String.format("%02d", hour) + ":" + String.format("%02d", minute);
        return str;
    }

    private void counter() {
        if (cu_time != 0) {
            textView.setText("關門倒數: " + String.valueOf(cu_time));
        } else {
            textView.setText("QR 掃描機");

            Intent intent = new Intent(getBaseContext(), QRcodeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    private final Runnable timerRun = new Runnable() {
        public void run() {
            counter();
            if (cu_time != 0) {
                --cu_time;
                mHandlerTime.postDelayed(timerRun, 1000);
            }
        }
    };

    private void swipe(final String cardid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference userRef = db.collection("User");
        userRef.document(cardid)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {//帳號是否存在
                        if (documentSnapshot.exists() && documentSnapshot.get("code").toString().equals(cardid)){

                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            CollectionReference userRef = db.collection("QRcode");
                            final String sid = documentSnapshot.get("id").toString();
                            final String to = documentSnapshot.get("token").toString();

                            userRef.document(cardid)
                                    .get()
                                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot documentSnapshot) {//QRcode是否過期
                                            if (documentSnapshot.exists()){
                                                Long qrtime = Long.valueOf(documentSnapshot.get("time").toString());
                                                Long currtime = System.currentTimeMillis();
                                                Log.d("----Time----", String.valueOf(currtime)+" - " +String.valueOf(qrtime)+" = "+String.valueOf(currtime-qrtime));
                                                if (0 < currtime - qrtime && currtime - qrtime < 60000){//about 60 seconds

                                                    String t, site;
                                                    site = spinner.getSelectedItem().toString();
                                                    t = getTime();
                                                    studentID.setText("Student ID: " + sid);
                                                    cardID.setText("Card ID: " + cardid);
                                                    time.setText("Time: " + t);
                                                    sentRecord(cardid, site, t, to);
                                                    if (cu_time != 0) {
                                                        cu_time = 10;
                                                    } else {
                                                        cu_time = 10;
                                                        mHandlerTime.postDelayed(timerRun, 0);
                                                    }
                                                }
                                                else {
                                                    Intent intent = new Intent(getBaseContext(), QRcodeActivity.class);
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                    startActivity(intent);
                                                }
                                            }
                                            else {
                                                Intent intent = new Intent(getBaseContext(), QRcodeActivity.class);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                startActivity(intent);
                                            }
                                        }
                                    });



                        }
                    }
                });
    }

    private void sentRecord(final String c, final String s, final String t, String to) {

        String time = t.replace("/","").replace(":","").replace(" ","");
        Log.d("--RUN--", "sentRecord");
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("/Record/"+c+"/"+time);
        HashMap<String, Object>data = new HashMap<>();
        data.put("location", s);
        data.put("time", t);
        myRef.setValue(data);

        String context = "時間: " + t + "\n地點: " + s;

        Data sendData = new Data("刷卡通知: ", context);
        Sender sender = new Sender(sendData, to);


        apiService.sendNotification(sender)
                .enqueue(new Callback<MyResponse>() {
                    @Override
                    public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                        if (response.code() == 200){
                            if (response.body().success != 1){
                                Log.d("--sendNotification--", "Failed!");
                            }
                            else {
                                Log.d("--sendNotification--", "Success!");
                            }
                        }else{
                            Log.d("--sendNotification--", "Response: " + String.valueOf(response.code()));
                        }
                    }

                    @Override
                    public void onFailure(Call<MyResponse> call, Throwable t) {
                        Log.d("--sendNotification--", "onFailure: " + t.getMessage());
                    }
                });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
