package com.fg607.lrc;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Administrator on 2015/6/4.
 */
public class LrcProcessor {

    public ArrayList<Queue> process(InputStream inputStream){

        Queue<Long> timeMills = new LinkedList<Long>();
        Queue<String> messages = new LinkedList<String>();
        ArrayList<Queue> queues = new ArrayList<Queue>();

        try {
            InputStreamReader inputReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputReader);
            String temp = null;
            int i = 0;
            Pattern p =Pattern.compile("\\[([^\\[\\]]*)\\]");//匹配[]字符串
            String result = null;
            while((temp = bufferedReader.readLine())!=null)
            {

                i++;
                Matcher m = p.matcher(temp);
                if(m.find())
                {
                    if(result != null)
                    {
                        messages.add(result);
                    }
                    String timeStr = m.group();

                    Long timeMill = time2Long(timeStr.substring(1,timeStr.length()-1));

                    if(timeMill != -1L)//防止出现[我爱你]的时间转换错误
                    {

                        timeMills.offer(timeMill);
                        String msg = temp.substring(10);
                        result = ""+msg+"\n";
                    }
                    else {
                            timeMill = 0L;
                            timeMills.offer(timeMill);
                            result = "送给我心爱滴小老鼠" + "\n";
                    }


                }
                else{
                    result = result + temp + "\n";

                }
            }
            messages.add(result);
            queues.add(timeMills);
            queues.add(messages);


        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


        return queues;
    }

    public Long time2Long(String timeStr){

        int min ,sec,mill;
        try{
            String s[] = timeStr.split(":");
            min = Integer.parseInt(s[0]);
            String ss[] = s[1].split("\\.");
            sec = Integer.parseInt(ss[0]);
            mill = Integer.parseInt(ss[1]);
        }
        catch (Exception e){
            e.printStackTrace();
            return -1L;

        }



        return min*60*1000 + sec*1000 + mill*10L;

    }
}
