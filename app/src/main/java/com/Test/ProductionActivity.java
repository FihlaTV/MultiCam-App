package com.Test;

import android.content.DialogInterface;
import android.content.Intent;
import android.icu.text.AlphabeticIndex;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class ProductionActivity extends AppCompatActivity {

    public int currentposition;
    public VideoView videoView;
    public String ClickedPath;
    public int counter;
    final ArrayList<String> RecorderPaths = new ArrayList<>();
    final ArrayList<Integer> RecordedStartTimes = new ArrayList<>();
    ArrayList<ProductionVideoModel> productionVideoModelArrayList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_production);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DeleteOldSelections();
        ClickedPath = null;
        counter = 0;



        ListView list = findViewById(R.id.listViewVideos);
        videoView = findViewById(R.id.videoViewPlayer);
        final ArrayList<String> varray = new ArrayList<>();
        String path = Environment.getExternalStorageDirectory().toString()+"/TempProductionVideos";
        Log.d("Files", "Path: " + path);
        File f = new File(path);
        File file[] = f.listFiles();
        Log.d("Files", "Size: "+ file.length);
        for (int i=0; i < file.length; i++)
        {
            //here populate your listview
            Log.d("Files", "FileName:" + file[i].getName());
            varray.add(file[i].toString());
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, varray);
        list.setAdapter(arrayAdapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(ClickedPath == null)
                {
                    ClickedPath = varray.get(position);
                }
                if(varray.get(position) != ClickedPath || counter == 0) {
                    ClickedPath = varray.get(position);
                    currentposition = videoView.getCurrentPosition();
                    RecordedStartTimes.add(currentposition);
                    RecorderPaths.add(ClickedPath);
                    Uri uri = Uri.parse(ClickedPath);
                    videoView.setVideoURI(uri);
                    videoView.pause();
                    videoView.seekTo(currentposition);
                    videoView.start();
                }
                counter++;
            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                openDialog();
            }
        });



    }

    private void openDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Times Saved")
                .setMessage("Do you want to continue?")
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        videoView.seekTo(0);
                        videoView.pause();
                        videoView.setVideoURI(null);
                        RecordedStartTimes.clear();
                        RecorderPaths.clear();
                    }
                })
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        productionVideoModelArrayList.clear();
                        DeleteOldSelections();
                        prepareArray();
                        goToNextPage();
                    }

                    private void prepareArray() {
                        ArrayList<String> commands = new ArrayList<>();
                        File folder = new File(Environment.getExternalStorageDirectory() + "/TempSelectionVideos");
                        if(!folder.exists()){
                            folder.mkdir();
                        }
                        for(int i = 0; i < RecordedStartTimes.size(); i++){
                            String videoName = "Selection "+ i ;
                            String fileExt = ".mp4";
                            int startTime;
                            int duration;
                            startTime = RecordedStartTimes.get(i);
                            int i2 = i+1;
                            try{
                                duration = RecordedStartTimes.get(i2) - startTime;
                            }
                            catch (Exception e) {
                                Uri videoUri = Uri.parse(RecorderPaths.get(i));
                                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                                retriever.setDataSource(getApplicationContext(), videoUri);
                                String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                                int timeInMillisec = Integer.parseInt(time);
                                duration = timeInMillisec - startTime;
                                retriever.release();
                            }
                            DecimalFormat df = new DecimalFormat(".##");
                            double DoubleStartTime = (double)(startTime) / 1000;
                            DoubleStartTime = Double.parseDouble(df.format(DoubleStartTime));
                            Double DoubleDuration = (double) duration / 1000;
                            DoubleDuration = Double.parseDouble(df.format(DoubleDuration));
                            File dest = new File(folder, videoName+fileExt);
                            String[] Command  = new String[] {"-ss",""+DoubleStartTime,"-y","-i",RecorderPaths.get(i),"-t",""+DoubleDuration,"-vcodec","mpeg4","-b:v","2097152","-b:a","48000","-ac","2","-ar","22050",dest.getAbsolutePath()};
                            ProductionVideoModel pvm = new ProductionVideoModel(Command, DoubleDuration);
                            productionVideoModelArrayList.add(pvm);

                        }
                    }
                    private void goToNextPage() {
                        Intent i = new Intent(ProductionActivity.this, ProgressBarConcatActivity.class);
                        Bundle b = new Bundle();
                        b.putParcelableArrayList("videoSelections",productionVideoModelArrayList);
                        i.putExtras(b);
                        ProductionActivity.this.startActivity(i);
                    }
                }).create().show();


    }

    private void DeleteOldSelections() {
        //get the production folder
        File dir = new File(Environment.getExternalStorageDirectory()+"/TempSelectionVideos");
        //delete children
        if (dir.isDirectory())
        {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++)
            {
                new File(dir, children[i]).delete();
            }
        }
    }


}
