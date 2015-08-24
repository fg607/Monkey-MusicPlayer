package com.fg607.mp3player.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;

import com.fg607.fileutil.FileUtil;
import com.fg607.lrc.LrcProcessor;
import com.fg607.mp3player.DataClass;
import com.fg607.mp3player.PlayerActivity;
import com.fg607.mp3player.R;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Queue;

public class PlayerService extends Service {

    MediaPlayer mediaPlayer = null;
    private String mp3Path = null;

    private boolean isPlaying = false;
    private boolean isPause = false;
    private boolean isReleased = false;

    private Handler handler = new Handler();//循环处理线程消息
    private UpdateTimeCallback updateTimeCallback = null;//歌曲更新器
    private ArrayList<Queue> queues = null;//歌词文件保存
    private long begin = 0;
    private long pauseTime = 0;
    private long nextTimeMill = 0;
    private long currentTimeMill = 0;
    private long pauseTimeMills = 0;
    private boolean hasLrc = false;
    private String message = null;//广播的歌词
    private int seekTime = -1;
    private String lrcPath = null;

    public PlayerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        //判断是否为另一首歌曲，如果是,停止播放的歌曲
        if(mediaPlayer != null&&!mp3Path.equals(intent.getStringExtra("mp3path")))
        {
            mp3Path = intent.getStringExtra("mp3path");
            stop();
            DataClass.lrcStr = null;
            isPlaying = false;
            isPause = true;
            isReleased = false;

        }

        mp3Path = intent.getStringExtra("mp3path");
        int action = intent.getIntExtra("action",0);
        seekTime = intent.getIntExtra("seektime",-1);
        switch (action)
        {
            case DataClass.START:
            {
                start();
                break;
            }
            case DataClass.PAUSE:
            {
                pause();
                break;
            }
            case DataClass.STOP:
            {
                stop();
                break;
            }
            case DataClass.RESTART:
            {
                restart();
                break;
            }
            case DataClass.SEEK: {
                seek();
                break;
            }
            default:
                break;
        }

        //设置通知栏
        Notification notification = new Notification(R.drawable.notif_ico,
                getString(R.string.app_name), System.currentTimeMillis());


       // notification.flags=Notification.FLAG_AUTO_CANCEL;用户点击清除能够清除通知


        //点击通知栏返回后台程序，默认新建
        Intent appIntent = new Intent(this, PlayerActivity.class);//为PlayerActivity组建创建Intent


        //ACTION_MAIN和CATEGORY_LAUNCHER设置启动组建PlayerActivity
        appIntent.setAction(Intent.ACTION_MAIN);
        appIntent.addCategory(Intent.CATEGORY_LAUNCHER);


        //设置启动模式为将后台PlayerActivity前台运行
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);


        PendingIntent pendingintent =PendingIntent.getActivity(this,0,appIntent,0);


        notification.setLatestEventInfo(this, FileUtil.getNameFromPath(mp3Path), "Monkey Music",
                pendingintent);
        startForeground(0x111, notification);//使Service处于前台，避免容易被清除

        flags = START_STICKY;//设置START_STICKY标志，用于Service被杀后重启
        return super.onStartCommand(intent, flags, startId);
    }


    public void start(){

        if(!isPlaying)
        {
            mediaPlayer = MediaPlayer.create(PlayerService.this, Uri.parse("file://" + mp3Path));

            if(mediaPlayer == null)
            {
                sendMsg("invalidmp3",true);
                return;
            }
            mediaPlayer.setLooping(false);

            sendMsg("seekbarmax",mediaPlayer.getDuration());

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {


                    handler.removeCallbacks(updateTimeCallback);
                    sendMsg("isEnd",true);
                    DataClass.lrcStr = null;
                    isPlaying = false;
                    isPause = true;


                }
            });
            mediaPlayer.start();

            //同目录下寻找同名称不同后缀的歌词文件
            lrcPath = mp3Path.substring(0, mp3Path.length() - 1 - 2) + "lrc";

            //如果准备歌词文件成功，说明有歌词文件，如果返回false，说明没有歌词文件
            if(prepareLrc(lrcPath))
            {
                hasLrc = true;
                begin = System.currentTimeMillis();
              //  handler.postDelayed(updateTimeCallback, 5);
            }
            else
            {
                hasLrc = false;
                sendMsg("lrcmsg","");
                DataClass.lrcStr = null;

                updateTimeCallback = new UpdateTimeCallback();
            }

            handler.postDelayed(updateTimeCallback, 5);
            isPlaying = true;
            isPause = false;
            isReleased = false;
        }

    }
    public void pause(){

        if(mediaPlayer!=null)
        {
            if(!isReleased) {
                if (!isPause) {
                    mediaPlayer.pause();

                    if(hasLrc)
                    {
                        //停止歌词更新器更新歌词
                        handler.removeCallbacks(updateTimeCallback);
                        //记录当前暂停时间
                        pauseTime = System.currentTimeMillis();
                    }
                    isPlaying = false;
                    isPause = true;
                    isReleased = false;
                } else
                {
                    mediaPlayer.start();
                    if(hasLrc)
                    {
                        //恢复播放后重新计算歌词更新器的起始时间
                        begin = begin + System.currentTimeMillis() - pauseTime;
                        handler.postDelayed(updateTimeCallback, 10);
                    }
                    isPlaying = true;
                    isPause = false;
                    isReleased = false;
                }

            }


        }

    }
    public void stop(){

        if(mediaPlayer!=null)
        {
            if(!isReleased)
            {
                mediaPlayer.stop();
                if(hasLrc)
                {
                    handler.removeCallbacks(updateTimeCallback);
                }
                //停止歌词更新器

                DataClass.lrcStr = null;
                mediaPlayer = null;
                isPlaying = false;
                isPause  = true;
                isReleased = true;
            }

        }

    }

    public void restart(){

        stop();
        start();

    }

    public boolean prepareLrc(String lrcName){

            try {
                InputStream inputStream = new FileInputStream(lrcName);
                LrcProcessor lrcProcessor = new LrcProcessor();
                queues = lrcProcessor.process(inputStream);
                updateTimeCallback = new UpdateTimeCallback(queues);
                begin = 0;
                nextTimeMill = 0;
                currentTimeMill = 0;

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            }
            return true;

    }

    class UpdateTimeCallback implements Runnable{

        public Queue times = null;//歌词时间队列
        public Queue messages = null;//歌词内容队列
        public boolean isRest = false;


        public UpdateTimeCallback(ArrayList<Queue> queues) {

            times = queues.get(0);
            messages = queues.get(1);

        }

        public UpdateTimeCallback(){


             times = null;
             messages = null;
        }

        @Override
        public void run() {

            if(times != null && !times.isEmpty())
            {
                long offset = System.currentTimeMillis() - begin;
                if (currentTimeMill == 0) {
                    nextTimeMill = (Long) times.poll();
                    message = (String) messages.poll();
                }
                if (offset >= nextTimeMill) {

                    if((offset-nextTimeMill) <= 10000)
                    {
                        //广播歌词信息
                        Intent intent = new Intent();
                        intent.setAction(DataClass.SERVICEACTION);
                        intent.putExtra("lrcmsg", message);
                        DataClass.lrcStr = message;
                        sendBroadcast(intent);
                    }

                    nextTimeMill = (Long) times.poll();
                    message = (String) messages.poll();
                }
                currentTimeMill = currentTimeMill + 10;
            }

            sendMsg("seekbarposition",mediaPlayer.getCurrentPosition());

            handler.postDelayed(updateTimeCallback, 10);

        }
        public void initQueue(){


            times = getQueue(lrcPath).get(0);
            messages = getQueue(lrcPath).get(1);
            nextTimeMill = (Long) times.poll();
            message = (String) messages.poll();

        }
    }

    @Override
    public void onDestroy() {

        //销毁时停止前台
        stopForeground(true);
        super.onDestroy();
    }

    public void sendMsg(String key,String msg){


        Intent intent = new Intent();
        intent.setAction(DataClass.SERVICEACTION);
        intent.putExtra(key, msg);
        sendBroadcast(intent);

    }
    public void sendMsg(String key,boolean msg){


        Intent intent = new Intent();
        intent.setAction(DataClass.SERVICEACTION);
        intent.putExtra(key, msg);
        sendBroadcast(intent);

    }
    public void sendMsg(String key,int msg){


        Intent intent = new Intent();
        intent.setAction(DataClass.SERVICEACTION);
        intent.putExtra(key, msg);
        sendBroadcast(intent);

    }

    public void seek(){


        if(mediaPlayer != null)
        {
            int currentPosition = mediaPlayer.getCurrentPosition();
            mediaPlayer.seekTo(seekTime);

            if(hasLrc)
            {
                begin = begin + (currentPosition - seekTime);

                if((currentPosition - seekTime) >= 0)
                    updateTimeCallback.initQueue();
                sendMsg("lrcmsg","");
            }

        }




    }

    public ArrayList<Queue> getQueue(String lrcName ){


        ArrayList<Queue> queue = null;
        try {
            InputStream inputStream = new FileInputStream(lrcName);
            LrcProcessor lrcProcessor = new LrcProcessor();
            queue = lrcProcessor.process(inputStream);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return queue;
        }
        return queue;

    }
}
