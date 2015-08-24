package com.fg607.fileutil;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Administrator on 2015/6/3.
 */
public class FileUtil {

    public static String sdcardPath = Environment.getExternalStorageDirectory().getAbsolutePath();


    public static String getParent(String filename){
        File file = new File(filename);

        return  file.getParent();

    }

    public static File[] getFils (String folder) {


        File dirpath = new File(folder);

        File[] files = dirpath.listFiles();

        return files;
    }

    public static File[] getParentFiles(String filename){

        File file = new File(filename);

        File pFolder = file.getParentFile();

        File[] files = pFolder.listFiles();

        return files;

    }

    //获取文件扩展名
    public static String getExtensionName(String filename){

        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot >-1) && (dot < (filename.length() - 1))) {
                return filename.substring(dot + 1);
            }
        }
        return filename;
    }
    //获取不带扩展的文件名
    public static String getFileNameNoEx(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot >-1) && (dot < (filename.length()))) {
                return filename.substring(0, dot);
            }
        }
        return filename;
    }

    public static String getSimpleName(String mp3path){

        File file = new File(mp3path);

        return  file.getName();

    }

    public static String getNameFromPath(String mp3path){


        return getFileNameNoEx(getSimpleName(mp3path));


    }

    public static void createFolder(String foldername){

        File folder = new File(sdcardPath+"/"+foldername);

        if(!folder.exists())
            folder.mkdir();
    }

    public  static File createSdcardFile(String foldername , String filename){


        File file = new File(sdcardPath+"/"+foldername+"/"+filename);

        if(!file.exists())
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        return file;

    }

    public static void writeFileData(File file,String data){


        FileOutputStream fileopstream = null;
        try {
             fileopstream = new FileOutputStream(file);
            fileopstream.write(data.getBytes());

        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {

            if (fileopstream != null)
                try {
                    fileopstream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }


    }

    public static String readFileData(File file){


        String data = null;
        FileInputStream fileinputstream = null;
        try {
            fileinputstream = new FileInputStream(file);
            InputStreamReader isreader = new InputStreamReader(fileinputstream);
            BufferedReader reader = new BufferedReader(isreader);
            data = reader.readLine();

        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {

            if (fileinputstream != null)
                try {
                    fileinputstream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }



        return data;

    }
}
