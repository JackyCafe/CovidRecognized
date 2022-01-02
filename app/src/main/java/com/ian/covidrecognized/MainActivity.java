package com.ian.covidrecognized;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private int PERMISSION_REQUEST_CODE = 200;
    private File path;
    private RecyclerView recyclerView;
    private List<FileSturct> covid_files;
    private FileAdapter fileAdapter;
    private AppCompatImageView imageView;

    String TAG = MainActivity.class.getName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = findViewById(R.id.recycle_viewer);
        imageView = findViewById(R.id.image);
        path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString());
        /*Android 9 以上需要檢查權限，
         */

        if(checkPermisson()){
            covid_files=readTheFiles();
            init_view();
        }else{
            //Android 10 API 29 以上檢查權限的方法
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s",getApplicationContext().getPackageName())));
                startActivityForResult(intent, 2296);
            }else{
                // API 29 以下檢查權限的方法
                ActivityCompat.requestPermissions(this,  PERMISSIONS_STORAGE,PERMISSION_REQUEST_CODE);
            }
        }
    }
    /*init_view*/
    public void init_view(){
        recyclerView.setLayoutManager(new GridLayoutManager(this,4));
        recyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.HORIZONTAL));
        fileAdapter = new FileAdapter(this,covid_files);
        recyclerView.setAdapter(fileAdapter);
        fileAdapter.setItemClickListener(new OnRecyclerViewClickListener() {
            @Override
            public void onItemClickListener(View view) {
                int position = recyclerView.getChildAdapterPosition(view);
                int width = imageView.getWidth();
                int height = imageView.getHeight();
                String filename = covid_files.get(position).getFile_name();
                Bitmap bImage = BitmapFactory.decodeFile(filename);
                imageView.setImageBitmap(bImage);
                imageView.setMaxHeight(height);
                imageView.setMaxWidth(width);
                Log.v(TAG,""+bImage.getWidth());

            }
        });

    }



    /* 要權限的callback */
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode== PERMISSION_REQUEST_CODE){
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED
                    && grantResults[1]==PackageManager.PERMISSION_GRANTED){
                readTheFiles();
            }
            else{
                Toast.makeText(this,"Permission Denied",Toast.LENGTH_SHORT).show();
            }
        }
    }

    /*讀資料夾*/
    private List<FileSturct> readTheFiles() {
        File files = new File(path.getPath()+"/COVID-19_Radiography_Dataset");
        List<FileSturct> covid_files = new ArrayList<>();
        for(File dir: files.listFiles()){
                FileSturct file_type;
                for(File f: dir.listFiles()){
                    file_type=new FileSturct(dir.getName(),f.getAbsolutePath()) ;
                    covid_files.add(file_type);
                }
        }
        return covid_files;

    }


    public boolean checkPermisson(){
        if (Build.VERSION.SDK_INT  >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }else{
            int writePerm =   ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int readPerm = ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE);
            return writePerm== PackageManager.PERMISSION_GRANTED && readPerm==PackageManager.PERMISSION_GRANTED;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2296) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    readTheFiles();
                } else {
                    Toast.makeText(this, "Allow permission for storage access!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}