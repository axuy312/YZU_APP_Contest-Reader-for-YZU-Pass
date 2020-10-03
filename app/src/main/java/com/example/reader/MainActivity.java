package com.example.reader;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import android.os.Handler;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    private Handler mHandlerTime = new Handler();
    private TextView studentID, cardID, time, count;
    private Spinner spinner;
    private String code;
    private int step = 0;
    int cu_time = 0;
    Button qrPage;
    APIService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<String> siteList = new ArrayAdapter<>(MainActivity.this, R.layout.support_simple_spinner_dropdown_item, getResources().getStringArray(R.array.place));
        spinner.setAdapter(siteList);

        studentID = (TextView) findViewById(R.id.tv_id);
        cardID = (TextView) findViewById(R.id.tv_code);
        time = (TextView) findViewById(R.id.tv_time);
        count = (TextView) findViewById(R.id.count);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);

        qrPage = (Button) findViewById(R.id.qr_page);
        qrPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent in = new Intent(MainActivity.this, QRcodeActivity.class);
                startActivity(in);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        //Toast.makeText(MainActivity.this, "onResume", Toast.LENGTH_SHORT).show();
        if (nfcAdapter != null){
            enableForegroundDispatchSystem();
        }
    }

    private void enableForegroundDispatchSystem() {

        Intent intent = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        IntentFilter[] intentFilters = new IntentFilter[]{};

        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //Toast.makeText(MainActivity.this, "onNewIntent", Toast.LENGTH_SHORT).show();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            byte[] paramArrayOfbyte = detectedTag.getId();
            long l2 = 0L;
            long l1 = 1L;
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; ; i++) {
                if (i >= paramArrayOfbyte.length)
                    break;
                l2 += (paramArrayOfbyte[i] & 0xFFL) * l1;
                l1 *= 256L;
            }
            code = String.valueOf(l2);
        } else if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            NdefMessage inNdefMessage = (NdefMessage) parcelables[0];
            NdefRecord[] inNdefRecords = inNdefMessage.getRecords();
            NdefRecord ndefRecord_0 = inNdefRecords[0];

            String inMessage = new String(ndefRecord_0.getPayload());
            code = inMessage;
        } else {
            return;
        }
        swipe(code);
    }

    private String getTime() {

        Calendar c = Calendar.getInstance();

        int year = c.get(Calendar.YEAR);

        int month = c.get(Calendar.MONTH) + 1;

        int day = c.get(Calendar.DAY_OF_MONTH);

        int hour = c.get(Calendar.HOUR_OF_DAY);

        int minute = c.get(Calendar.MINUTE);
        String str;
        str = String.format("%04d", year) + "/" + String.format("%02d", month) + "/" + String.format("%02d", day) + " " + String.format("%02d", hour) + ":" + String.format("%02d", minute);
        return str;
    }

    private void counter() {
        if (cu_time != 0) {
            count.setText("關門倒數: " + String.valueOf(cu_time));
        } else {
            count.setText("我是讀卡機~");
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
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists() && documentSnapshot.get("code").toString().equals(cardid)){

                            String t, site;
                            site = spinner.getSelectedItem().toString();
                            t = getTime();
                            studentID.setText("Student ID: " + documentSnapshot.get("id").toString());
                            cardID.setText("Card ID: " + cardid);
                            time.setText("Time: " + t);
                            sentRecord(cardid, site, t, documentSnapshot.get("token").toString());
                            if (cu_time != 0) {
                                cu_time = 10;
                            } else {
                                cu_time = 10;
                                mHandlerTime.postDelayed(timerRun, 0);
                            }
                        }
                    }
                });
    }

    private void sentRecord(final String c, final String s, String t, String to) {
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

    public void next(View view) {
        if (step < 2) {
            step++;
        } else if (step < 4) {
            step++;
            Toast.makeText(MainActivity.this, String.valueOf(step), Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(MainActivity.this, dataBaseControl.class);
            startActivity(intent);
        }
    }
}