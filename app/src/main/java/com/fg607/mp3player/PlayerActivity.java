package com.fg607.mp3player;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.fg607.fileutil.FileUtil;
import com.fg607.mp3player.service.PlayerService;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PlayerActivity extends Activity{

    private static final int EXITAPP = 1;
    private static final int ABOUT = 2;

    boolean pauseState = true;

     ImageButton btnpre = null;
     ImageButton btnpause = null;
     ImageButton btnnext = null;
     TextView textView = null;
     TextView textViewName = null;
     TextView playlist_name = null;
     SeekBar seekBar = null;
    MediaPlayer judgeMedia = null;
    boolean isInvalidMp3 = false;

     ImageView menuBtn = null;
     ImageView folderBtn = null;

    private PopupWindow popupWindow ;
    private View mPopupWindowView;
    private static Context mContext;
    private View mainView;
    WindowManager.LayoutParams lp;




    ListView listview = null;
     View viewDlg = null;
     ListView dlg_listView = null;

     private SlidingMenu mSlidingMenu;//��ViewGroup��֯��View��ͼ
     private  AlertDialog  myDialog;
     private List<HashMap<String,String>> list;
     private List<Map<String,Object>> folderList;

     private String mp3Path = null;
     private String folder = null;
     private int cureentMp3Number = -1;
     private  boolean isMusicEnd = true;

     private BroadcastReceiver receiver = null;//�㲥��������service���͹����ĸ������
     private IntentFilter intentFilter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;

        //viewmainΪ����ҳ�������ɵ�View��ͼ
       mainView = LayoutInflater.from(this).inflate(R.layout.activity_player, null);

        //viewmenuΪ�ɲ�����������ɵ�View��ͼ
        View viewmenu = LayoutInflater.from(this).inflate(R.layout.menulayout, null);

        //viewDlgΪ�ɶԻ��򲼾����ɵ�View��ͼ
        viewDlg = LayoutInflater.from(this).inflate(R.layout.filedialog, null);


        //mSlidingMenu����ҳ��ͼ�Ͳ�����ͼ��֯��һ����ͼ
        mSlidingMenu = new SlidingMenu(this,mainView,viewmenu);

        setContentView(mSlidingMenu);//ע��setContentView��Ҫ��Ϊ���ǵ�SlidingMenu

        list = new ArrayList<HashMap<String,String>>();
        folderList = new  ArrayList<Map<String,Object>>();

        listview = (ListView) viewmenu.findViewById(R.id.menulist);
        dlg_listView = (ListView)viewDlg.findViewById(R.id.adapter_listView1);
        playlist_name = (TextView)findViewById(R.id.playlist_name);

        textView = (TextView)findViewById(R.id.lrc_textview);
        textViewName = (TextView)findViewById(R.id.mp3name);
        seekBar = (SeekBar)findViewById(R.id.seekbar);


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //�ֶ����ڽ���
                // TODO Auto-generated method stub

                if(!isMusicEnd)
                {
                    int dest = seekBar.getProgress();

                    Intent serviceIntent = new Intent();
                    serviceIntent.putExtra("mp3path", mp3Path);
                    serviceIntent.putExtra("seektime", dest);
                    serviceIntent.putExtra("action", DataClass.SEEK);
                    serviceIntent.setClass(PlayerActivity.this, PlayerService.class);
                    startService(serviceIntent);
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                // TODO Auto-generated method stub

            }
        });



        btnpre = (ImageButton)findViewById(R.id.playpre);
        btnpause = (ImageButton)findViewById(R.id.pause);
        btnnext = (ImageButton)findViewById(R.id.playnext);
        menuBtn = (ImageView)findViewById(R.id.menu_img);
        folderBtn = (ImageView)findViewById(R.id.folder_img);

        btnpre.setOnClickListener(new BtnPlayPreListener());
        btnpause.setOnClickListener(new BtnPauseListener());
        btnnext.setOnClickListener(new BtnPlayNextListener());
        menuBtn.setOnClickListener(new ImgMenuListener());
        folderBtn.setOnClickListener(new ImgFolderListenr());



        loadFolder();//�����ϴ��˳�����ǰ��״̬


        //�״δ򿪳���ʹ�ô��濨��Ŀ¼�µġ�/musics����ΪĬ�ϲ���Ŀ¼
        if (folder == null)
        {
            FileUtil.createFolder("Monkey Music");
            folder = Environment.getExternalStorageDirectory() + "/Monkey Music";
            Toast.makeText(PlayerActivity.this, "������Ͻ��ļ���ѡ�����ֲ���Ŀ¼", Toast.LENGTH_LONG).show();
        }


        updateListView();


        //��ʾ��������
        if(mp3Path != null)
        {
            textViewName.setText(FileUtil.getNameFromPath(mp3Path));

        }
        else {

            textViewName.setText("");//�״�����
        }


        //�������Ŀ��Ӧ���
        listview.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {




                if(cureentMp3Number != arg2)
                {
                    cureentMp3Number = arg2;

                    playMusic(arg2);

                    pauseState = false;
                    isMusicEnd = false;
                    btnpause.setImageResource(R.drawable.pause);
                }

                mSlidingMenu.close();


            }
        });

        //�ļ���������Ӧ���
        dlg_listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {


                if ((Boolean) folderList.get(arg2).get("isDire")) {

                    updateFolderList((String) folderList.get(arg2).get("path"));

                } else {
                    String strfile = ((String) folderList.get(arg2).get("path"));
                    folder = FileUtil.getParent(strfile);
                    File file = new File(strfile);
                    playlist_name.setText("/" + file.getParentFile().getName());
                    updateListView();
                    myDialog.cancel();

                    cureentMp3Number = -1;
                    Toast.makeText(PlayerActivity.this, "�����б��Ѹ���", Toast.LENGTH_SHORT).show();


                }


            }
        });

        receiver = new ServiceBroadcastReceiver();//����Service���չ㲥
        registerReceiver(receiver, getIntentFilter());//ע��㲥

        initPopupWindow();


    }


    //���²������Ŀ
    private void updateListView(){

        //���menu�б�
        list.clear();
        SimpleAdapter adapterclr = new SimpleAdapter(this,list,R.layout.mp3infor_item,new String[]{"mp3_name"},new int[]{R.id.mp3_name});
        listview.setAdapter(adapterclr);
        //����musicsĿ¼�µ����и���
        File[] files = FileUtil.getFils(folder);

        //�����и������֡���С��·������map����ӵ�list
        int mp3Number = 0;
        for(File file:files){

            if(FileUtil.getExtensionName(file.getName()).equals("mp3")) {

                mp3Number++;
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("mp3_name", mp3Number + "." + " " + FileUtil.getFileNameNoEx(file.getName()));
                map.put("mp3_path", file.getPath());
                list.add(map);
            }
        }


        //ListActivity ����ʹ��setListAdapter����һ��SimpleAdapter��һ��List<>������ListView��
        SimpleAdapter adapter = new SimpleAdapter(this,list,R.layout.mp3infor_item,new String[]{"mp3_name"},new int[]{R.id.mp3_name});
        listview.setAdapter(adapter);


        File file = new File(folder);
        playlist_name.setText("/"+file.getName());


    }



    class BtnPlayPreListener implements OnClickListener {

        @Override
        public void onClick(View v) {


            playPreMp3();


        }
    }

    class BtnPauseListener implements OnClickListener {

        @Override
        public void onClick(View v) {

            if(isMusicEnd&&mp3Path != null)
            {

                playMusic(mp3Path);

                /*if((cureentMp3Number >= 0)&&!isTempMp3)
                    playMusic(cureentMp3Number);
                else
                    playMusic(mp3Path);*/
            }

            else if(mp3Path != null)
            {
                postMsg(mp3Path,DataClass.PAUSE);

                if(!pauseState)
                {
                    pauseState = true;
                    btnpause.setImageResource(R.drawable.go);
                }
                else {
                    pauseState = false;
                    btnpause.setImageResource(R.drawable.pause);
                }

            }

        }
    }

    class BtnPlayNextListener implements OnClickListener {

        @Override
        public void onClick(View v) {

           playNextMp3();


        }
    }


    class ImgMenuListener implements OnClickListener{

        @Override
        public void onClick(View v) {

            if (mSlidingMenu.isOpen())
            {
                mSlidingMenu.close();

            } else {

                mSlidingMenu.open();
            }

        }
    }

    class ImgFolderListenr implements OnClickListener{


        @Override
        public void onClick(View v) {
            showDlg();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
       // unregisterReceiver(receiver);//��ͣ�㲥��ʽ���

    }

    @Override
    protected void onResume() {

        super.onResume();
        if(DataClass.mp3Name!=null)
        textViewName.setText(FileUtil.getNameFromPath(DataClass.mp3Name));//ˢ�¸�������
        textView.setText(DataClass.lrcStr);//ˢ�¸����ʾ



    }



    public void postMsg(String path,int action){//��service���Ϳ��Ƹ���������

        Intent intent = new Intent();
        intent.putExtra("mp3path",path);
        intent.putExtra("action",action);
        intent.setClass(PlayerActivity.this, PlayerService.class);
        startService(intent);

    }

    //Service�㲥����
    class ServiceBroadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {

            //��ȡ��ǰ����ı�������UI
            String lrcMsg = intent.getStringExtra("lrcmsg");

            if(lrcMsg != null)
            textView.setText(lrcMsg);

            isMusicEnd = intent.getBooleanExtra("isEnd", false);

            DataClass.seekBarMax = intent.getIntExtra("seekbarmax", -1);

            int seedBarPosition = intent.getIntExtra("seekbarposition",-1);

            isInvalidMp3 = intent.getBooleanExtra("invalidmp3",false);

            if(isInvalidMp3)
            {
                Toast.makeText(PlayerActivity.this,"������Ч��MP3�ļ���",Toast.LENGTH_SHORT).show();
                playNextMp3();
            }

            if(seedBarPosition != -1)
            {
                seekBar.setProgress(seedBarPosition);
            }

            if(DataClass.seekBarMax != -1)
            {
                seekBar.setMax(DataClass.seekBarMax);
            }

            if(isMusicEnd)
            {
                btnpause.setImageResource(R.drawable.go);
                pauseState = true;
                playNextMp3();
            }
        }
    }


    public void playNextMp3(){

        if(cureentMp3Number > -1)
        {
            if (cureentMp3Number+1 <= list.size()-1)
            {
                cureentMp3Number++;
                playMusic(cureentMp3Number);

            }
            else
                Toast.makeText(PlayerActivity.this, "�������һ�׸���", Toast.LENGTH_SHORT).show();
        }



    }

    public void playPreMp3(){

        if(cureentMp3Number > -1)
        {
            if (cureentMp3Number-1>=0)
            {
                cureentMp3Number--;
                playMusic(cureentMp3Number);
            }
            else
                Toast.makeText(PlayerActivity.this, "���ǵ�һ�׸���", Toast.LENGTH_SHORT).show();

        }


    }
    //�Զ���㲥����
    public IntentFilter getIntentFilter(){

        if(intentFilter == null)
        {
            intentFilter = new IntentFilter();
            intentFilter.addAction(DataClass.SERVICEACTION);
        }
        return intentFilter;

    }

    public void  exitApp(){


        clearAPP();
    }




    //����������������ʱ����,�����˳�ǰ�������
    @Override
    protected void onDestroy() {

        clearAPP();

        super.onDestroy();

    }



    private void showPopupWindow(){



        if(!popupWindow.isShowing()){

            //���������䰵Ч��
            lp=getWindow().getAttributes();
            lp.alpha = 0.4f;
            getWindow().setAttributes(lp);

            popupWindow.showAtLocation(mainView, Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL ,0,0);
            //popupWindow.showAsDropDown(mainView,0,0);
        }else {
            popupWindow.dismiss();
        }
    }


    private void initPopupWindow(){
        initPopupWindowView();
        //��ʼ��popupwindow������ʾview�����ø�view�Ŀ��/�߶�

        popupWindow = new PopupWindow(mPopupWindowView, ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);

        //����͸������
        ColorDrawable cd = new ColorDrawable(0x000000);
        popupWindow.setBackgroundDrawable(cd);



        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        // �����Ϊ�˵��������Back��Ҳ��ʹ����ʧ�����Ҳ�����Ӱ����ı�����ʹ�ø÷����������֮�⣬�ſɹرմ���
       // ColorDrawable dw = new ColorDrawable(0xb0000000);
       // popupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.menu_back));
        //Background��������Ϊnull��dismiss��ʧЧ
//		popupWindow.setBackgroundDrawable(null);
        //���ý��롢��������Ч��
//		popupWindow.setAnimationStyle(R.style.popupwindow);
        popupWindow.update();
        //popupWindow����dismissʱ������������setOutsideTouchable(true)�����view֮��/����back�ĵط�Ҳ�ᴥ��
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {

            @Override
            public void onDismiss() {
                // TODO Auto-generated method stub
//				 WindowManager.LayoutParams lp=getWindow().getAttributes();
                lp.alpha = 1f;
                getWindow().setAttributes(lp);
            }

        });
    }


    private void initPopupWindowView(){

        mPopupWindowView = LayoutInflater.from(mContext).inflate(R.layout.popupwindowmenu, null);
        TextView textview_edit = (TextView) mPopupWindowView.findViewById(R.id.exit);
        textview_edit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                exitApp();

            }
        });
    }




    //��Ӧʵ�尴��
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {


        if (keyCode == KeyEvent.KEYCODE_BACK) {

            if (mSlidingMenu.isOpen())
            {
                mSlidingMenu.close();

            } else {

                //���ؼ��ó�������̨����
                moveTaskToBack(false);
            }


           // return true;
            return super.onKeyDown(keyCode, event);
        }
        if(keyCode == KeyEvent.KEYCODE_MENU)
        {
            showPopupWindow();
        }
        return super.onKeyDown(keyCode, event);

    }

    //��ʾĿ¼ѡ��Ի���
    public void showDlg(){

        updateFolderList("/");
        if(myDialog == null)
        {
            //���ܶ�ε���setview(),����Ḹ��ͼ�Ѿ���������ͼ���쳣��
            AlertDialog dialog = new AlertDialog.Builder(this).setTitle("ѡ������Ŀ¼").setView(viewDlg).create();
            dialog.setCanceledOnTouchOutside(false);//ʹ����dialog����ĵط����ܱ����
            myDialog = dialog;
        }

        myDialog.show();

    }

    //ˢ���ļ�����������
    public void updateFolderList(String currentFolder)
    {

        //����ɵ���Ŀ
        folderList.clear();
        SimpleAdapter adapter = new SimpleAdapter(PlayerActivity.this,folderList,R.layout.filedialog,
                new String[]{"name","image"},new int[]{R.id.adapter_filename,R.id.adapter_image});
        dlg_listView.setAdapter(adapter);


        File f = new File(currentFolder);
        File[] file = f.listFiles();

        if(!currentFolder.equals("/")){//������Ǹ�Ŀ¼�Ļ�����Ҫ��ʾ���б��м������
            Map<String,Object> map1=new HashMap<String,Object>();
            map1.put("name", "������һ��Ŀ¼");
            map1.put("image", R.drawable.folder_back);
            map1.put("path",f.getParent());
            map1.put("isDire", true);
            folderList.add(map1);
        }

        if(file != null){//�����ж� ����Ŀ¼Ϊ�յ�ʱ��ᱨ��
            for(int i = 0; i < file.length; i++){
                Map<String,Object> map=new HashMap<String,Object>();
                map.put("name", file[i].getName());
                map.put("image", (file[i].isDirectory()) ? R.drawable.format_folder: R.drawable.file);
                map.put("path",file[i].getPath());
                map.put("isDire", file[i].isDirectory());
                folderList.add(map);
            }
        }

        dlg_listView.setAdapter(adapter);
    }


    public void playMusic(int mp3Number){

        if(mp3Number > -1)
        {
            HashMap<String, String> map = list.get(mp3Number);//��ȡѡ����Ŀ�ĸ�����Ϣ


            //����service��������
            Intent serviceIntent = new Intent();
            serviceIntent.putExtra("mp3path", map.get("mp3_path"));
            serviceIntent.putExtra("action", DataClass.START);
            serviceIntent.setClass(PlayerActivity.this, PlayerService.class);
            startService(serviceIntent);

            mp3Path = map.get("mp3_path");
            DataClass.mp3Name = mp3Path;
            textViewName.setText(FileUtil.getNameFromPath(mp3Path));
            textView.setText("");
            btnpause.setImageResource(R.drawable.pause);
            pauseState = false;
            isMusicEnd = false;
        }


    }

    public void playMusic(String path){



        //����service��������
        Intent serviceIntent = new Intent();
        serviceIntent.putExtra("mp3path", path);

        if(cureentMp3Number == -1|cureentMp3Number==list.size()-1)
        serviceIntent.putExtra("action", DataClass.RESTART);
        else
            serviceIntent.putExtra("action", DataClass.START);
        serviceIntent.setClass(PlayerActivity.this, PlayerService.class);
        startService(serviceIntent);

        mp3Path = path;
        DataClass.mp3Name = mp3Path;
        textViewName.setText(FileUtil.getNameFromPath(mp3Path));
        textView.setText("");
        btnpause.setImageResource(R.drawable.pause);
        pauseState = false;
        isMusicEnd = false;



    }

    public void clearAPP(){//Ӧ���˳�ǰֹͣservice���񣬲������Դռ��

        saveCurrentFolder();//����״̬
        Intent serviceIntent = new Intent();
        serviceIntent.setClass(PlayerActivity.this, PlayerService.class);
        stopService(serviceIntent);
        unregisterReceiver(receiver);
        android.os.Process.killProcess(android.os.Process.myPid());

    }

    public void saveCurrentFolder(){

        if(list.size()>0|mp3Path != null)
        {

            File file = FileUtil.createSdcardFile("Monkey Music","state.dat");

            String data = folder+"&"+mp3Path+"&"+cureentMp3Number;

            FileUtil.writeFileData(file, data);
        }


    }

    public void loadFolder(){


        File file = FileUtil.createSdcardFile("Monkey Music", "state.dat");
        String data =  FileUtil.readFileData(file);

        if( data != null)
        {
            int first = data.indexOf("&");
            int last = data.lastIndexOf("&");

            folder = data.substring(0,first);
            mp3Path = data.substring(first + 1, last);
            cureentMp3Number = Integer.parseInt(data.substring(last+1));
            if(mp3Path.equals("null"))
            {
                mp3Path = null;
            }

        }


    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);


        //ͨ��������ִ򿪲�����

        Uri uri = intent.getData();
        if(uri != null)
        {

            if(!isMp3File(uri.getPath()))
            {
                Toast.makeText(PlayerActivity.this,"������Ч��mp3�ļ�",Toast.LENGTH_LONG).show();
                return;
            }
            mp3Path = uri.getPath();
            playMusic(mp3Path);
        }

    }

    public boolean isMp3File(String path)
    {
        judgeMedia = MediaPlayer.create(PlayerActivity.this, Uri.parse("file://" + path));


        if(judgeMedia != null)
        {
            judgeMedia = null;
            if(FileUtil.getExtensionName(FileUtil.getSimpleName(path)).equals("mp3")|FileUtil.getExtensionName(FileUtil.getSimpleName(path)).equals("MP3"))
            return true;
        }

        return false;
    }
}

