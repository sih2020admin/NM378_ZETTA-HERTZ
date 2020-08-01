package com.example.agrolens.CropDetect;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.agrolens.GPSTracker;
import com.example.agrolens.R;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.nio.MappedByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class DetectResult extends AppCompatActivity {
    private ImageView imageView,select_loc;
    private TextView textView, coordinates;
    WifiManager wifiManager;
    private final static int PLACE_PICKER_REQUEST = 999;

    private EditText editText;
    Bitmap bitmap;
    Uri myUri;
    String lat, lng;
    FirebaseAuth auth;
    FirebaseUser user;
    String cropName;
    String uid;
    StorageReference storageReference;
    DatabaseReference reference,reference2,reference3;
    ProgressDialog pd;
    String generatedtext;
    String currentDate;
    String currentTime;
    String Description;
    String username;






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect_result);
        textView = findViewById(R.id.bottom_crop_name);
        imageView = findViewById(R.id.result_crop);
        select_loc=findViewById(R.id.selectloc);
        editText = findViewById(R.id.description_text);
        coordinates = findViewById(R.id.coordintestext);
        auth=FirebaseAuth.getInstance();
        user=auth.getCurrentUser();
        uid=user.getUid();
        wifiManager= (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);



        FirebaseDatabase database=FirebaseDatabase.getInstance();
        reference=database.getReference("Users");

        reference2=database.getReference("marker");
        updateusername();



        currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        select_loc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wifiManager.setWifiEnabled(false);
                openPlacePicker();

            }
        });


        pd=new ProgressDialog(DetectResult.this);
        pd.setMessage("saving...");
        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editText.setFocusable(true);
                editText.setFocusableInTouchMode(true);
            }
        });


        Bundle extras = getIntent().getExtras();
        cropName = extras.getString("crop");
        //byte[] byteArray = extras.getByteArray("bitmap");
        // bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        myUri = Uri.parse(extras.getString("imageUri"));
        imageView.setImageURI(myUri);
        textView.setText(cropName);
        storageReference= FirebaseStorage.getInstance().getReference("cropsimages");
        generatedtext=generateString(8);

        //imageView.setImageBitmap(bitmap);
        GPSTracker gps = new GPSTracker(this);
        double latitude = gps.getLatitude();
        double longitude= gps.getLongitude();
        lat=Double.toString(latitude);
        lng=Double.toString(longitude);
        coordinates.setText(lat+", "+lng);








    }
    private void openPlacePicker() {
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        try {
            startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST);

            //Enable Wifi
            wifiManager.setWifiEnabled(true);

        } catch (GooglePlayServicesRepairableException e) {
            Log.d("Exception",e.getMessage());

            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            Log.d("Exception",e.getMessage());

            e.printStackTrace();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode){
                case PLACE_PICKER_REQUEST:
                    Place place = PlacePicker.getPlace(DetectResult.this, data);

                    double latitude = place.getLatLng().latitude;
                    double longitude = place.getLatLng().longitude;

                    lat=Double.toString(latitude);
                    lng=Double.toString(longitude);
                    coordinates.setText(lat+", "+lng);


            }
        }
    }



    private void updateusername() {
        FirebaseDatabase database=FirebaseDatabase.getInstance();

        reference3=database.getReference("Users");

        reference3.child(uid).child("userdetails").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                username=snapshot.child("name").getValue().toString();




            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private String generateString(int len){
        char[] chars="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
        StringBuilder stringBuilder=new StringBuilder();
        Random random=new Random();
        for (int i=0;i<len;i++){
            char c=chars[random.nextInt(chars.length)];
            stringBuilder.append(c);

        }
        return stringBuilder.toString();
    }
    private void uploadPrfileCoverPhoto(Uri uri) {
        pd.show();


        StorageReference storageReference2=storageReference.child(uid).child(generatedtext);
        storageReference2.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Task<Uri> uriTask=taskSnapshot.getStorage().getDownloadUrl();
                while(!uriTask.isSuccessful());
                Uri downloaduri=uriTask.getResult();

                if(uriTask.isSuccessful()){
                    HashMap<String,Object> results=new HashMap<>();
                    results.put("image",downloaduri.toString());
                    reference2.child(generatedtext).updateChildren(results);
                    reference.child(uid).child("crophistory").child(generatedtext).updateChildren(results).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            pd.dismiss();
                            Toast.makeText(DetectResult.this,"Details saved successfully",Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(DetectResult.this,DetectCrop.class));
                            finish();

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            pd.dismiss();
                            Toast.makeText(DetectResult.this,"image updation error",Toast.LENGTH_SHORT).show();

                        }
                    });

                }
                else{
                    pd.dismiss();
                    Toast.makeText(DetectResult.this,"Some error occured",Toast.LENGTH_SHORT).show();
                }

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }


    public void saveDetails(View view) {
        if(TextUtils.isEmpty(editText.getText().toString())){
            Description="No Data";

        }else {
            Description=editText.getText().toString().trim();

        }





        HashMap<Object,String> hashMap=new HashMap<>();
        hashMap.put("cropname",cropName);
        hashMap.put("desc",Description);
        hashMap.put("cropage","");
        hashMap.put("latitude",lat);
        hashMap.put("longitude",lng);
        hashMap.put("image","");
        hashMap.put("date",currentDate);
        hashMap.put("time",currentTime);
        hashMap.put("generatetext",generatedtext);
        hashMap.put("username",username);

        reference.child(uid).child("crophistory").child(generatedtext).setValue(hashMap);
        reference2.child(generatedtext).setValue(hashMap);
        uploadPrfileCoverPhoto(myUri);

    }
}






