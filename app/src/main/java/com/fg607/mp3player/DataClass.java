package com.fg607.mp3player;

/**
 * Created by Administrator on 2015/6/4.
 */
public class DataClass {

      public static final int START = 1;
    public static final int PAUSE = 2;
    public static final int STOP = 3;
    public static final int RESTART = 4;
    public static final int SEEK = 5;

    public static final String SERVICEACTION = "com.fg607.player_msg_action";
    public static String lrcStr = null;
    public static String mp3Name = null;
    public static boolean isExit = false ;
    public static int seekBarMax = -1;
}
