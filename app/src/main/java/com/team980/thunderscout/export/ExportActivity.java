package com.team980.thunderscout.export;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.team980.thunderscout.R;

public class ExportActivity extends AppCompatActivity implements View.OnClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        Button buttonExport = (Button) findViewById(R.id.buttonExport);
        buttonExport.setOnClickListener(this);

        Button buttonOpenFolder = (Button) findViewById(R.id.buttonOpenFolder);
        buttonOpenFolder.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.buttonExport) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                CSVExportTask exportTask = new CSVExportTask(this);
                exportTask.execute();
            } else {
                //Request permission
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1
                );
            }
        } else if (v.getId() == R.id.buttonOpenFolder) {
            Uri selectedUri = Uri.parse(Environment.getExternalStorageDirectory() + "/ThunderScout/");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(selectedUri, "resource/folder");

            if (intent.resolveActivityInfo(getPackageManager(), 0) != null)
            {
                startActivity(intent);
            }
            else
            {
                // if you reach this place, it means there is no any file
                // explorer app installed on your device
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                CSVExportTask exportTask = new CSVExportTask(this);
                exportTask.execute();
            } else {
                //Why would you ever deny the permission?
                Toast.makeText(this, "Please manually grant the permission", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivityForResult(intent, 0);
            }
        }
    }
}