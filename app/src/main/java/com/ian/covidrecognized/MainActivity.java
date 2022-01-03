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
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class MainActivity extends AppCompatActivity {
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    //圖片RGB 轉換設定值
    private static final int RESULT_TO_SHOW = 4;
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128.0f;

    private int PERMISSION_REQUEST_CODE = 200;
    private File path;
    private RecyclerView recyclerView;
    private List<FileSturct> covid_files;
    private FileAdapter fileAdapter;
    private AppCompatImageView imageView;
    String TAG = MainActivity.class.getName();
    private static final String MODEL = "mobilenetv2";
    private Interpreter tflite = null;
    private boolean load_result = false;
    private TextView label1,label2,label3,label4;

    // 模型的初始設定
    private final  Interpreter.Options tfliteOptions = new Interpreter.Options();
    // 保存模型的標籤
    List<String> labelList;
    // 初始模型的輸入圖像尺寸
    private int DIM_IMG_SIZE_X = 32; //299
    private int DIM_IMG_SIZE_Y = 32; //299
    private int DIM_PIXEL_SIZE = 3;
    // 圖片保存為bytes
    private ByteBuffer imgData = null;
    // 保存標籤的機率
    private float[][]labelProbArray = null;
    // 保存最高機率的標籤
    private String[] topLabel = null;
    //最高機率的 array
    private String[] topConfidence = null;

    // 用於保留圖像數據
    private int [] intValues = new int[300*300*4];

    private PriorityQueue<Map.Entry<String,Float>> sortrdLabels =
            new PriorityQueue<>(RESULT_TO_SHOW,
                    new Comparator<Map.Entry<String, Float>>() {
                        @Override
                        public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
                            return (o1.getValue()).compareTo(o2.getValue());
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = findViewById(R.id.recycle_viewer);
        imageView = findViewById(R.id.image);
        path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString());
        load_modle();
        setupLabelList();
        /*Android 9 以上需要檢查權限，
          */

        if(checkPermisson()){
            covid_files=readTheFiles();
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
        init_view();

    }

    public void setupLabelList(){
        labelList = new ArrayList<>();
        labelList.add("COVID");
        labelList.add("Lung_Opacity");
        labelList.add("Normal");
        labelList.add("Viral Pneumonia");
    }

    /*載入tensorflow lite*/
    public void load_modle(){
        try {
            MappedByteBuffer map = null ;
            AssetFileDescriptor fileDescriptor = getApplicationContext().getAssets().openFd(MODEL + ".tflite");
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            map = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
            tflite = new Interpreter(map);
            Toast.makeText(MainActivity.this, MODEL + " model load success", Toast.LENGTH_SHORT).show();
//            Log.d(TAG, MODEL + " model load success");
            tflite.setNumThreads(4);
            load_result = true;
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, MODEL + " model load fail", Toast.LENGTH_SHORT).show();
//            Log.d(TAG, MODEL + " model load fail");
            load_result = false;
            e.printStackTrace();
        }
    }




    /*init_view
    *  利用recycleviewer 載入資料夾影片
    * */
    public void init_view(){
        recyclerView.setLayoutManager(new GridLayoutManager(this,4));
        recyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.HORIZONTAL));
        imgData =ByteBuffer.allocateDirect(4*DIM_IMG_SIZE_X*DIM_IMG_SIZE_Y*DIM_PIXEL_SIZE);
        imgData.order(ByteOrder.nativeOrder());
        labelProbArray = new float[1][labelList.size()];
        topLabel = new String[RESULT_TO_SHOW];
        topConfidence = new String[RESULT_TO_SHOW];

        label1 = findViewById(R.id.label1);
        label2 = findViewById(R.id.label2);
        label3 = findViewById(R.id.label3);
        label4 = findViewById(R.id.label4);


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
                Bitmap bitmapImg = getResizeBitmap(bImage,32,32);
                convertBitToByteBuffer(bitmapImg);
                tflite.run(imgData,labelProbArray);

                printTopKLabel();

            }
        });

    }

    private Bitmap getResizeBitmap(Bitmap bm, int dim_img_size_x, int dim_img_size_y) {
        Bitmap resizeBitmap = null;
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleW = ((float)dim_img_size_x)/width;
        float scaleH = ((float)dim_img_size_y)/height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleW,scaleH);
        resizeBitmap = Bitmap.createBitmap(bm,0,0,width,height,matrix,false);
//        Log.v(TAG,""+resizeBitmap.getWidth()+","+resizeBitmap.getWidth());
        return resizeBitmap;
    }

    private void convertBitToByteBuffer(Bitmap bitmap){
        if (imgData==null){
            return;
        }
        imgData.rewind();
        bitmap.getPixels(intValues,0,bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());
        int pixel = 0 ;
        for (int i=0;i<DIM_IMG_SIZE_X;++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final  int val = intValues[pixel++];
                imgData.putFloat((val>>16)&0xFF);
                imgData.putFloat((val>>8)&0xFF);
                imgData.putFloat((val)&0xFF);
//                imgData.putFloat((((val>>16)&0xFF)-IMAGE_MEAN)/IMAGE_STD);
//                imgData.putFloat((((val>>8)&0xFF)-IMAGE_MEAN)/IMAGE_STD);
//                imgData.putFloat((((val)&0xFF)-IMAGE_MEAN)/IMAGE_STD);
            }
        }
    }

   public void printTopKLabel(){
        for(int i=0; i<labelList.size();++i){
            sortrdLabels.add(
                    new AbstractMap.SimpleEntry<>(labelList.get(i),labelProbArray[0][i])
            );
            if(sortrdLabels.size()>RESULT_TO_SHOW){
                sortrdLabels.poll();
            }
        }
//        Log.v(TAG,sortrdLabels.toString());
        final int size = sortrdLabels.size();
        for (int i=0;i<size;++i){
            Map.Entry<String,Float> label = sortrdLabels.poll();
            topLabel[i] = label.getKey();
            topConfidence[i]= String.format("%.0f",label.getValue()*100);

        }
        label1.setText("1."+topLabel[3]+","+topConfidence[3]+"%");
        label2.setText("2."+topLabel[2]+","+topConfidence[2]+"%");
        label3.setText("3."+topLabel[1]+","+topConfidence[1]+"%");
        label4.setText("3."+topLabel[0]+","+topConfidence[0]+"%");

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

    /*讀資料夾影片檔名*/
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


    /*檢查權限*/
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