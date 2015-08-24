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

     private SlidingMenu mSlidingMenu;//由ViewGroup组织的View视图
     private  AlertDialog  myDialog;
     private List<HashMap<String,String>> list;
     private List<Map<String,Object>> folderList;

     private String mp3Path = null;
     private String folder = null;
     private int cureentMp3Number = -1;
     private  boolean isMusicEnd = true;

     private BroadcastReceiver receiver = null;//广播用来接收service传送过来的歌词内容
     private IntentFilter intentFilter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;

        //viewmain为由主页布局生成的View视图
       mainView = LayoutInflater.from(this).inflate(R.layout.activity_player, null);

        //viewmenu为由侧边栏布局生成的View视图
        View viewmenu = LayoutInflater.from(this).inflate(R.layout.menulayout, null);

        //viewDlg为由对话框布局生成的View视图
        viewDlg = LayoutInflater.from(this).inflate(R.layout.filedialog, null);


        //mSlidingMenu将主页视图和侧栏视图组织成一个视图
        mSlidingMenu = new SlidingMenu(this,mainView,viewmenu);

        setContentView(mSlidingMenu);//注意setContentView需要换为我们的SlidingMenu

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
                //手动调节进度
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



        loadFolder();//载入上次退出程序前的状态


        //首次打开程序使用储存卡根目录下的“/musics”作为默认播放目录
        if (folder == null)
        {
            FileUtil.createFolder("Monkey Music");
            folder = Environment.getExternalStorageDirectory() + "/Monkey Music";
            Toast.makeText(PlayerActivity.this, "点击右上角文件夹选择音乐播放目录", Toast.LENGTH_LONG).show();
        }


        updateListView();


        //显示歌曲名称
        if(mp3Path != null)
        {
            textViewName.setText(FileUtil.getNameFromPath(mp3Path));

        }
        else {

            textViewName.setText("");//首次启动
        }


        //侧边栏条目响应点击
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

        //文件管理器响应点击
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
                    Toast.makeText(PlayerActivity.this, "播放列表已更新", Toast.LENGTH_SHORT).show();


                }


            }
        });

        receiver = new ServiceBroadcastReceiver();//设置Service接收广播
        registerReceiver(receiver, getIntentFilter());//注册广播

        initPopupWindow();


    }


    //更新侧边栏条目
    private void updateListView(){

        //清空menu列表
        list.clear();
        SimpleAdapter adapterclr = new SimpleAdapter(this,list,R.layout.mp3infor_item,new String[]{"mp3_name"},new int[]{R.id.mp3_name});
        listview.setAdapter(adapterclr);
        //遍历musics目录下的所有歌曲
        File[] files = FileUtil.getFils(folder);

        //将所有歌曲名字、大小、路径存入map并添加到list
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


        //ListActivity 必须使用setListAdapter设置一个SimpleAdapter将一个List<>关联到ListView上
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
       // unregisterReceiver(receiver);//暂停广播歌词接收

    }

    @Override
    protected void onResume() {

        super.onResume();
        if(DataClass.mp3Name!=null)
        textViewName.setText(FileUtil.getNameFromPath(DataClass.mp3Name));//刷新歌曲名称
        textView.setText(DataClass.lrcStr);//刷新歌词显示



    }



    public void postMsg(String path,int action){//向service发送控制歌曲及动作

        Intent intent = new Intent();
        intent.putExtra("mp3path",path);
        intent.putExtra("action",action);
        intent.setClass(PlayerActivity.this, PlayerService.class);
        startService(intent);

    }

    //Service广播接受
    class ServiceBroadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {

            //获取当前歌词文本，更新UI
            String lrcMsg = intent.getStringExtra("lrcmsg");

            if(lrcMsg != null)
            textView.setText(lrcMsg);

            isMusicEnd = intent.getBooleanExtra("isEnd", false);

            DataClass.seekBarMax = intent.getIntExtra("seekbarmax", -1);

            int seedBarPosition = intent.getIntExtra("seekbarposition",-1);

            isInvalidMp3 = intent.getBooleanExtra("invalidmp3",false);

            if(isInvalidMp3)
            {
                Toast.makeText(PlayerActivity.this,"不是有效的MP3文件！",Toast.LENGTH_SHORT).show();
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
                Toast.makeText(PlayerActivity.this, "已是最后一首歌曲", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(PlayerActivity.this, "已是第一首歌曲", Toast.LENGTH_SHORT).show();

        }


    }
    //自定义广播过滤
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




    //程序被任务管理器清除时调用,程序退出前清除工作
    @Override
    protected void onDestroy() {

        clearAPP();

        super.onDestroy();

    }



    private void showPopupWindow(){



        if(!popupWindow.isShowing()){

            //产生背景变暗效果
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
        //初始化popupwindow，绑定显示view，设置该view的宽度/高度

        popupWindow = new PopupWindow(mPopupWindowView, ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);

        //设置透明背景
        ColorDrawable cd = new ColorDrawable(0x000000);
        popupWindow.setBackgroundDrawable(cd);



        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        // 这个是为了点击“返回Back”也能使其消失，并且并不会影响你的背景；使用该方法点击窗体之外，才可关闭窗体
       // ColorDrawable dw = new ColorDrawable(0xb0000000);
       // popupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.menu_back));
        //Background不能设置为null，dismiss会失效
//		popupWindow.setBackgroundDrawable(null);
        //设置渐入、渐出动画效果
//		popupWindow.setAnimationStyle(R.style.popupwindow);
        popupWindow.update();
        //popupWindow调用dismiss时触发，设置了setOutsideTouchable(true)，点击view之外/按键back的地方也会触发
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




    //响应实体按键
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {


        if (keyCode == KeyEvent.KEYCODE_BACK) {

            if (mSlidingMenu.isOpen())
            {
                mSlidingMenu.close();

            } else {

                //返回键让程序进入后台运行
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

    //显示目录选择对话框
    public void showDlg(){

        updateFolderList("/");
        if(myDialog == null)
        {
            //不能多次调用setview(),否则会父视图已经包含子视图的异常。
            AlertDialog dialog = new AlertDialog.Builder(this).setTitle("选择音乐目录").setView(viewDlg).create();
            dialog.setCanceledOnTouchOutside(false);//使除了dialog以外的地方不能被点击
            myDialog = dialog;
        }

        myDialog.show();

    }

    //刷新文件管理器内容
    public void updateFolderList(String currentFolder)
    {

        //清除旧的条目
        folderList.clear();
        SimpleAdapter adapter = new SimpleAdapter(PlayerActivity.this,folderList,R.layout.filedialog,
                new String[]{"name","image"},new int[]{R.id.adapter_filename,R.id.adapter_image});
        dlg_listView.setAdapter(adapter);


        File f = new File(currentFolder);
        File[] file = f.listFiles();

        if(!currentFolder.equals("/")){//如果不是根目录的话就在要显示的列表中加入此项
            Map<String,Object> map1=new HashMap<String,Object>();
            map1.put("name", "返回上一级目录");
            map1.put("image", R.drawable.folder_back);
            map1.put("path",f.getParent());
            map1.put("isDire", true);
            folderList.add(map1);
        }

        if(file != null){//必须判断 否则目录为空的时候会报错
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
            HashMap<String, String> map = list.get(mp3Number);//获取选择条目的歌曲信息


            //启动service播放音乐
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



        //启动service播放音乐
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

    public void clearAPP(){//应用退出前停止service服务，并清除资源占用

        saveCurrentFolder();//保存状态
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


        //通过点击音乐打开播放器

        Uri uri = intent.getData();
        if(uri != null)
        {

            if(!isMp3File(uri.getPath()))
            {
                Toast.makeText(PlayerActivity.this,"不是有效的mp3文件",Toast.LENGTH_LONG).show();
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

