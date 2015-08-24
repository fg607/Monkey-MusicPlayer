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

    private Handler handler = new Handler();//ѭ�������߳���Ϣ
    private UpdateTimeCallback updateTimeCallback = null;//����������
    private ArrayList<Queue> queues = null;//����ļ�����
    private long begin = 0;
    private long pauseTime = 0;
    private long nextTimeMill = 0;
    private long currentTimeMill = 0;
    private long pauseTimeMills = 0;
    private boolean hasLrc = false;
    private String message = null;//�㲥�ĸ��
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


        //�ж��Ƿ�Ϊ��һ�׸����������,ֹͣ���ŵĸ���
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

        //����֪ͨ��
        Notification notification = new Notification(R.drawable.notif_ico,
                getString(R.string.app_name), System.currentTimeMillis());


       // notification.flags=Notification.FLAG_AUTO_CANCEL;�û��������ܹ����֪ͨ


        //���֪ͨ�����غ�̨����Ĭ���½�
        Intent appIntent = new Intent(this, PlayerActivity.class);//ΪPlayerActivity�齨����Intent


        //ACTION_MAIN��CATEGORY_LAUNCHER���������齨PlayerActivity
        appIntent.setAction(Intent.ACTION_MAIN);
        appIntent.addCategory(Intent.CATEGORY_LAUNCHER);


        //��������ģʽΪ����̨PlayerActivityǰ̨����
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);


        PendingIntent pendingintent =PendingIntent.getActivity(this,0,appIntent,0);


        notification.setLatestEventInfo(this, FileUtil.getNameFromPath(mp3Path), "Monkey Music",
                pendingintent);
        startForeground(0x111, notification);//ʹService����ǰ̨���������ױ����

        flags = START_STICKY;//����START_STICKY��־������Service��ɱ������
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

            //ͬĿ¼��Ѱ��ͬ���Ʋ�ͬ��׺�ĸ���ļ�
            lrcPath = mp3Path.substring(0, mp3Path.length() - 1 - 2) + "lrc";

            //���׼������ļ��ɹ���˵���и���ļ����������false��˵��û�и���ļ�
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
                        //ֹͣ��ʸ��������¸��
                        handler.removeCallbacks(updateTimeCallback);
                        //��¼��ǰ��ͣʱ��
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
                        //�ָ����ź����¼����ʸ���������ʼʱ��
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
                //ֹͣ��ʸ�����

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

        public Queue times = null;//���ʱ�����
        public Queue messages = null;//������ݶ���
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
                        //�㲥�����Ϣ
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

        //����ʱֹͣǰ̨
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
