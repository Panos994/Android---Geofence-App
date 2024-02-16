package com.example.geofence_app2;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

// DisplayDataActivity.java
public class DisplayDataActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_data);

        EditText etDisplayTableName = findViewById(R.id.etDisplayTableName);
        EditText etDisplayRecordId = findViewById(R.id.etDisplayRecordId);
        Button btnSubmit = findViewById(R.id.btnSubmit);
        TextView tvDisplayResult = findViewById(R.id.tvDisplayResult);

        Button btnBack = findViewById(R.id.btnBack);




        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Λαμβάνουμε τα δεδομένα από τα πεδία κειμένου
                String tableName = etDisplayTableName.getText().toString();
                String recordId = etDisplayRecordId.getText().toString();

                // Ελέγχουμε αν τα πεδία είναι κενά
                if (TextUtils.isEmpty(tableName) || TextUtils.isEmpty(recordId)) {
                    Toast.makeText(DisplayDataActivity.this, "Please enter table name and record ID", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Καλούμε τη μέθοδο για εμφάνιση των δεδομένων
                displayData(tableName, recordId, tvDisplayResult);
            }
        });

        //για να επιστρεψω στο mainactivity
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Επιστρέφουμε στο MainActivity
                Intent intent = new Intent(DisplayDataActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });




    }

    // Μέθοδος για την εμφάνιση των δεδομένων
    private void displayData(String tableName, String recordId, TextView tvDisplayResult) {
        // Εκτελούμε το query στο ContentProvider για επιστροφή μίας εγγραφής με βάση το ID
        Cursor cursor = getContentResolver().query(
                MyContentProvider.CONTENT_URI.buildUpon().appendPath(recordId).build(),

        null,
                null,
                null,
                null
        );

        // Ελέγχουμε αν υπάρχουν δεδομένα στον Cursor
        if (cursor != null && cursor.moveToFirst()) {
            // Λαμβάνουμε τα δεδομένα από τον Cursor
            int columnIndex = cursor.getColumnIndex(DatabaseHelper.KEY_LATITUDE);
            double latitude = cursor.getDouble(columnIndex);
            // επαναλαμβανω τη διαδικασία για τα υπόλοιπα πεδία



            int longitudeIndex = cursor.getColumnIndex(DatabaseHelper.KEY_LONGITUDE);
            double longitude = cursor.getDouble(longitudeIndex);

            int radiusIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_RADIUS);
            float radius = cursor.getFloat(radiusIndex);

            int pointTypeIndex = cursor.getColumnIndex(DatabaseHelper.KEY_POINT_TYPE);
            String pointType = cursor.getString(pointTypeIndex);







            // Εμφανίζουμε τα δεδομένα στην οθόνη
            String result = "Latitude: " + latitude + "\nLongitude: " + longitude + "\nRadius: " + radius + "\nPoint Type: " + pointType;



            tvDisplayResult.setText(result);
        } else {
            tvDisplayResult.setText("No data found for the specified record ID");
        }

        // Κλείσιμο του Cursor
        if (cursor != null) {
            cursor.close();
        }
    }
}

