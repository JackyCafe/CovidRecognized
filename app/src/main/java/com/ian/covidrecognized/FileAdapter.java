package com.ian.covidrecognized;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.VH>{

    private Context context;
    private List<FileSturct> datas;
    private String TAG = FileAdapter.class.getName();
    private OnRecyclerViewClickListener listener;



    public FileAdapter(Context context, List<FileSturct> datas){
            this.context = context;
            this.datas = datas;
    }

    public void setItemClickListener(OnRecyclerViewClickListener itemClickListener) {
        listener = itemClickListener;
    }

    public static class  VH extends RecyclerView.ViewHolder{
        private ImageView thumbnail;
        private TextView label;
        public VH(@NonNull View v) {
            super(v);
            thumbnail = v.findViewById(R.id.thumbnail);
            label = v.findViewById(R.id.label);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.filelayout,parent,false);
        VH vh = new VH(v);
        if(listener != null) {
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onItemClickListener(view);
                }
            });
        }
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        String filename=datas.get(position).getFile_name();
        Bitmap bImage = BitmapFactory.decodeFile(filename);
        String type = datas.get(position).getType();
        holder.label.setText(type);
        holder.thumbnail.setImageBitmap(bImage);

    }

    @Override
    public int getItemCount() {
        return datas.size();
    }
}
