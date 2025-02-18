package com.MultiCamPack;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
/*
    AddVideosActivity, created by Frankie McDonagh
    Date:

    This Activity contains code from from:
    Chikka Anddev, accessed from https://stackoverflow.com/questions/4943629/how-to-delete-a-whole-folder-and-content/4943771.
    Miša Peić, accessed from https://stackoverflow.com/questions/3401579/get-filename-and-path-from-uri-from-mediastore.
 */

public class AddVideosActivity extends AppCompatActivity
{

    Uri selectedUri;
    ListView videoList;
    CustomAdapter customAdapter;
    ArrayList<VideoListItemModel> videoArray;
    ArrayList<VideoListItemModel> newArray;
    ArrayList<ProductionVideoModel> productionVideoModelArrayList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_videos);
        setToolbar();
        videoList = findViewById(R.id.videoList);
        //create video array.
        videoArray = new ArrayList<>();
        //filling video list if there is a bundle present.
        fillList();
        //setting custom adapter and list
        customAdapter = new CustomAdapter(this, R.layout.addvideo_item, videoArray);
        videoList.setAdapter(customAdapter);

    }

    private void setToolbar() {
        Toolbar toolbar = findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
    }
    public void btnInfo(View view) {
        AlertDialog.Builder mbuilder = new AlertDialog.Builder(AddVideosActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.dialog_add_videos_help, null);
        mbuilder.setView(mView);
        AlertDialog dialog = mbuilder.create();
        dialog.show();
    }
    @Override
    protected void onResume(){
        super.onResume();
    }

    public void fillList(){
        Intent i = getIntent();
        Bundle bundle = i.getExtras();
        if(bundle != null) {
            newArray = bundle.getParcelableArrayList("list");
        }
        String mainVideo = i.getStringExtra("VideoPath");
        String startTime = i.getStringExtra("Average Time");
        if(mainVideo != null){
            for(VideoListItemModel video : newArray)
            {
                if(mainVideo.equals(video.getName())){
                    video.setStartTime(startTime);
                    break;
                }

            }
        }
        if(newArray != null) videoArray.addAll(newArray);
    }

    public void addVideo(View v)
    {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        i.setType("video/*");

        startActivityForResult(i, 100);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 100 && resultCode == RESULT_OK)
        {
            selectedUri = data.getData();
            VideoListItemModel Entry = new VideoListItemModel(selectedUri.toString(), "0");
            videoArray.add(Entry);
            customAdapter.notifyDataSetChanged();

        }
    }

    public void btnConvertVideos(View view) {
        productionVideoModelArrayList.clear();
        try {
            prepareVideoArray();
            DeleteOldProductionVideos();
            Intent i = new Intent(this, ProgressBarActivity.class);
            Bundle b = new Bundle();
            b.putParcelableArrayList("productionList", productionVideoModelArrayList);
            i.putExtras(b);
            this.startActivity(i);
        }
        catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Video list must not be empty", Toast.LENGTH_LONG).show();
        }
    }

    //See above re: Chikka Andev
    private void DeleteOldProductionVideos() {
        //get the production folder
        File dir = new File(Environment.getExternalStorageDirectory()+"/TempProductionVideos");
        //delete children
        if (dir.isDirectory())
        {
            String[] children = dir.list();
            if(children != null) {

                for (int i = 0; i < children.length; i++) {
                new File(dir, children[i]).delete();
                }
            }
        }
    }

    private void prepareVideoArray() {
        //get shortest duration.
        Double shortestDuration = getShortestDuration();
        int counter = 0;
        File folder = new File(Environment.getExternalStorageDirectory() + "/TempProductionVideos");
        if(!folder.exists()){
            folder.mkdir();
        }
        String fileExt = ".mp4";
        for(VideoListItemModel video : videoArray)
        {
            counter++;

            String videoName = "Screen " + Integer.toString(counter);
            String videoPath = video.getName();
            Uri videoUri = Uri.parse(videoPath);
            String realVideoPath = getRealPathFromUri(getApplicationContext(), videoUri);
            DecimalFormat df = new DecimalFormat(".##");
            Double startTime = (double)(Integer.parseInt(video.getStartTime())) / 1000;
            startTime = Double.parseDouble(df.format(startTime));
            Double duration = (startTime + shortestDuration) / 1000;
            duration = Double.parseDouble(df.format(duration));
            File dest = new File(folder, videoName+fileExt);
            String[] Command  = new String[] {"-ss",""+startTime,"-y","-i",realVideoPath,"-t",""+duration,"-vcodec","mpeg4","-b:v","2097152","-b:a","48000","-ac","2","-ar","22050","-vf","scale=1280x720",dest.getAbsolutePath()};
            ProductionVideoModel pvm = new ProductionVideoModel(Command, duration);
            productionVideoModelArrayList.add(pvm);
        }
    }

    private Double getShortestDuration() {
        ArrayList<Integer> durations = new ArrayList<>();
        if(videoArray != null) {
            for (VideoListItemModel video : videoArray) {
                String videoName = video.getName();
                Uri videoUri = Uri.parse(videoName);
                int startTime = Integer.parseInt(video.getStartTime());
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(getApplicationContext(), videoUri);
                String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                int timeInMillisec = Integer.parseInt(time);
                int durationAfterStartTime = timeInMillisec - startTime;
                durations.add(durationAfterStartTime);
                retriever.release();
                //ii
            }
            Collections.sort(durations);

        }
        Double returnDouble = (double) durations.get(0);
        return returnDouble;

    }
    //See above re: Miša Peić
    private String getRealPathFromUri(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        finally{
            if(cursor!=null)
            {
                cursor.close();
            }
        }

    }
}
