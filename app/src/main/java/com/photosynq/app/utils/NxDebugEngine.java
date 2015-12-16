package com.photosynq.app.utils;

import android.os.Environment;
import android.util.Log;

import com.photosynq.app.PhotoSyncApplication;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * (c) Nexus-Computing GmbH Switzerland, 2015
 * Created by Manuel Di Cerbo on 10.11.15.
 */
public class NxDebugEngine {

    public static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault());

    public static final long MAX_FILE_SIZE = 1000000; // limit to 1M files
    public static final int MAX_FILES = 15; // limit to 15 Files per category
    public static final File STORAGE = Environment.getExternalStorageDirectory();
    public static final File DIR = new File(STORAGE, "photosynq-dbg");

    public void dbg(String message, String buffer, String fileName) {
        checkDir();

        File outFile = selectOutFile(fileName, buffer);

        Log.d(" NX PHOTOSYNQ ", String.format("outfile is %s", outFile.getAbsolutePath()));

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outFile, true));
            writer.write(String.format(">==< %s >==< collected %s\n\n", message, FMT.format(new Date())));
            writer.write(buffer);
            writer.write("\n\n");
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public NxDebugEngine() {
        if (sUploadThread == null) {
            sUploadThread = new Thread(mUploadRunnable, "Log Upload Thread");
            sUploadThread.start();
        }
    }

    public void checkDir() {
        if (!DIR.exists()) {
            DIR.mkdirs();
            return;
        } else {
            if (DIR.isDirectory()) {
                return;
            }
            throw new IllegalArgumentException(String.format("%s is not a directory", DIR.getAbsolutePath()));
        }
    }

    public File selectOutFile(String fileName, String buffer) {
        int num = 0;

        for (String s : DIR.list()) {
            if (s.matches(String.format("^%s-[0-9]{3}.txt$", fileName))) {
                int n = Integer.valueOf(s.substring(s.length() - 7, s.length() - 4));
                if (n > num) {
                    num = n;
                }
            }
        }

        if (num > MAX_FILES) {
            rotate(fileName);
            num = MAX_FILES;
        }

        File out = new File(DIR, String.format("%s-%03d.txt", fileName, num));
        if (out.length() + buffer.length() > MAX_FILE_SIZE) {
            out = new File(DIR, String.format("%s-%03d.txt", fileName, num + 1));
        }

        return out;
    }

    public void rotate(String fileName) {

        final ArrayList<String> files = new ArrayList<>();
        for (String s : DIR.list()) {
            if (s.matches(String.format("^%s-[0-9]{3}.txt$", fileName))) {
                files.add(s);
            }
        }

        if (files.size() <= MAX_FILES) {
            return;
        }

        Collections.sort(files);



        log("exceeded max files for debug %s", fileName);

        for (int i = 0; i < MAX_FILES; i++) {
            String saveFile = files.get(files.size() - 1 - i);
            File origFile = new File(DIR, saveFile);
            File tmpFile = new File(DIR, String.format("%s-%03d.txt.save", fileName, MAX_FILES - i));
            origFile.renameTo(tmpFile);
        }

        for (String s : DIR.list()) {
            if (s.matches(String.format("^%s-[0-9]{3}.txt$", fileName))) {
                new File(DIR, s).delete();
            }
        }

        for (String s : DIR.list()) {
            if (s.matches(String.format("^%s-[0-9]{3}.txt.save$", fileName))) {
                File renamed = new File(DIR, s.substring(0, s.length() - 5));
                new File(DIR, s).renameTo(renamed);
                log("renamed %s to %s", s, renamed.getName());
            }
        }


    }

    public void createZip(String inputFolderPath, String outZipPath) throws IOException {
        collectLogcat();
        FileOutputStream fos;
        fos = new FileOutputStream(outZipPath);
        ZipOutputStream zos;
        zos = new ZipOutputStream(fos);
        File srcFile = new File(inputFolderPath);
        File[] files = srcFile.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (!files[i].getName().endsWith(".txt")) {
                continue;
            }

            byte[] buffer = new byte[1024];
            FileInputStream fis = new FileInputStream(files[i]);
            zos.putNextEntry(new ZipEntry(files[i].getName()));
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
            zos.closeEntry();
            fis.close();
        }
        zos.close();
    }

    public void collectLogcat() {
        try {
            Process proc = Runtime.getRuntime().exec("logcat -v time -d -f" + new File(DIR, "logcat.txt").getAbsolutePath());
            proc.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static Thread sUploadThread = null;
    private final Runnable mUploadRunnable = new Runnable() {
        @Override
        public void run() {
            for (; ; ) {
                try {
                    String file = mQueue.take();
                    if (file.equals(POISON)) {
                        break;
                    }

                    try {
                        upload(new File(file));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            sUploadThread = null;
        }
    };

    private final static String POISON = "____POISON";

    private final ArrayBlockingQueue<String> mQueue = new ArrayBlockingQueue<>(3);

    private void upload(File file) throws IOException {

        dbg("uploading file %s", file.getAbsolutePath());
        URL url;
        URLConnection urlConn;
        url = new URL("http://app.nexus-computing.com/photosynq/upload.php");
        urlConn = url.openConnection();
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        urlConn.setRequestProperty("Content-Type", "application/octet-stream");
        urlConn.setRequestProperty("File-Name", file.getName());
        urlConn.connect();

        FileInputStream is = new FileInputStream(file);
        byte[] buf = new byte[4096];
        int len;

        while ((len = is.read(buf)) > 0) {
            urlConn.getOutputStream().write(buf, 0, len);
        }

        String res = "";

        while ((len = urlConn.getInputStream().read(buf)) > 0) {
            res += new String(buf, 0, len);
        }

        dbg("received %s from http endpoint", res);
        urlConn.getInputStream().close();

        if (res.equals("OK")) {
            for (File f : DIR.listFiles()) {
                if (f.getName().endsWith(".txt")) {
                    f.delete();
                }
            }

            PhotoSyncApplication.sApplication.toast("Thank you for uploading a debug log");
        }
        file.delete();
    }

    public void uploadLogs() {
        File zip = new File(DIR, String.format("upload-%s.zip", FMT.format(new Date())));
        try {
            dbg("creating zip: %s", zip.getAbsolutePath());
            createZip(DIR.getAbsolutePath(), zip.getAbsolutePath());
            mQueue.offer(zip.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void dbg(String fmt, String... args) {
        Log.d(">==< NX DBG >==<", String.format(fmt, args));
    }

    public static void log(String fmt, Object... args) {
        Log.d(">==< NX DBG >==<", String.format(fmt, args));
    }
}
