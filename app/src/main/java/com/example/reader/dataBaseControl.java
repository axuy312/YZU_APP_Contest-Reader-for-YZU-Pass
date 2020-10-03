package com.example.reader;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class dataBaseControl extends AppCompatActivity {

    private EditText et_studentID, et_cardID;
    private NfcAdapter nfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_base_control);

        et_studentID = (EditText) findViewById(R.id.id);
        et_cardID = (EditText) findViewById(R.id.code);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null){
            Toast.makeText(this, "Nfc is not supported on this device", Toast.LENGTH_SHORT).show();
        }
        else if (!nfcAdapter.isEnabled()){
            Toast.makeText(this, "NFC disabled on this device. Turn on to proceed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null){
            enableForegroundDispatchSystem();
        }
    }

    private void enableForegroundDispatchSystem() {

        Intent intent = new Intent(this, dataBaseControl.class).addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        IntentFilter[] intentFilters = new IntentFilter[]{};

        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null);
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            byte[] paramArrayOfbyte = detectedTag.getId();
            long l2 = 0L;
            long l1 = 1L;
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0;; i++) {
                if (i >= paramArrayOfbyte.length)
                    break;
                l2 += (paramArrayOfbyte[i] & 0xFFL) * l1;
                l1 *= 256L;
            }
            et_cardID.setText(String.valueOf(l2));
        }
        else if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            NdefMessage inNdefMessage = (NdefMessage) parcelables[0];
            NdefRecord[] inNdefRecords = inNdefMessage.getRecords();
            NdefRecord ndefRecord_0 = inNdefRecords[0];

            String inMessage = new String(ndefRecord_0.getPayload());
            et_cardID.setText(inMessage);
        }
    }

    public void createAC(View view) {
        if (et_studentID.getText().toString().equals("") || et_cardID.getText().toString().equals("")) {
            Toast.makeText(dataBaseControl.this, "請輸入卡號和學號", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", et_studentID.getText().toString());
        data.put("code", et_cardID.getText().toString());
        data.put("token", "empty");

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference qrcodeRef = db.collection("User");
        qrcodeRef.document(et_cardID.getText().toString()).set(data);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("/User/"+et_cardID.getText().toString());
        myRef.child("id").setValue(et_studentID.getText().toString());
        myRef.child("code").setValue(et_cardID.getText().toString());
        myRef.child("nick").setValue("User");
        myRef.child("description").setValue("I am a human.");
        myRef.child("profile picture").setValue("default");
        et_cardID.setText("");
        et_studentID.setText("");
    }

    public void deleteACByCode(View view) {
        if (et_cardID.getText().toString().equals("")) {
            Toast.makeText(dataBaseControl.this, "請輸入卡號", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(dataBaseControl.this)
                .setTitle("Are you sure?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        //realtime data
                        FirebaseDatabase database = FirebaseDatabase.getInstance();
                        DatabaseReference myRef = database.getReference("/User/"+et_cardID.getText().toString());
                        myRef.removeValue();

                        //store data
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        CollectionReference qrcodeRef = db.collection("User");
                        qrcodeRef.document(et_cardID.getText().toString()).delete();

                        et_cardID.setText("");
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                }).show();
    }

    public void back(View view) {
        Intent intent = new Intent(dataBaseControl.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
