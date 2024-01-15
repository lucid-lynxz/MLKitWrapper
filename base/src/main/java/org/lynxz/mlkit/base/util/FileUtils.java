package org.lynxz.mlkit.base.util;

import android.graphics.Bitmap;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class FileUtils {
    private static final String TAG = "FileUtils";

    // 带该前缀的路径表示 assets/ 下的资源
    public static final String ANDROID_ASSETS = "/android_assets/";

    /**
     * 检查 文件 / 文件夹 是否存在
     *
     * @param filepath 文件绝对路径
     */
    public static boolean checkFileExists(String filepath) {
        return !TextUtils.isEmpty(filepath) && new File(filepath).exists();
    }

    /**
     * 创建文件夹,若已存在则不重新创建
     *
     * @param dirpath 路径
     */
    public static boolean createDIR(String dirpath) {
        return createDIR(dirpath, false);
    }

    /**
     * 创建文件夹
     * forceRecreate 若文件存在,但非目录,则删除重建
     * 参考 {@link #createDIR(File, boolean)}
     */
    public static boolean createDIR(String dirpath, boolean forceRecreate) {
        return createDIR(new File(dirpath), forceRecreate);
    }

    /**
     * 创建文件夹
     * 若文件存在,但非目录,则删除重建
     *
     * @param targetFile    要创建的目标目录文件
     * @param forceRecreate 若目录已存在,是否要强制重新闯进(删除后,新建)
     * @return 是否创建成功
     */
    public static boolean createDIR(File targetFile, boolean forceRecreate) {
        if (targetFile == null) {
            LogWrapper.w(TAG, "createDIR fail: targetFile is null");
            return false;
        }

        boolean result = true;
        if (targetFile.exists()) { // 存在同名文件
            boolean isDir = targetFile.isDirectory();
            if (!isDir) { // 非目录,删除以便创建目录
                result = targetFile.delete();
            } else if (forceRecreate) { // 强制删除目录
                result = deleteDir(targetFile);
            } else { // 目录存在
                return true;
            }
        }

        if (!result) {
            LogWrapper.w(TAG, "createDIR fail:删除目录相关文件失败,创建失败,请排查");
            return false;
        }

        boolean success = targetFile.mkdirs();
        if (!success) {
            LogWrapper.w(TAG, "createDIR fail, path=" + targetFile.getAbsolutePath());
        }
        return success;
    }


    public static boolean createFile(String filepath) {
        return createFile(new File(filepath));
    }

    /**
     * 创建文件
     * 若存在同名文件/目录,则直接返回 true
     *
     * @return 创建文件结果
     */
    public static boolean createFile(File file) {
        if (file == null) {
            return false;
        }

        if (file.exists()) {
            return true;
        }
        boolean result = false;
        try {
            createDIR(file.getParent());

            result = file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            LogWrapper.w(TAG, "createFile fail: path=" + file.getAbsolutePath() + ",errorMsg:" + e.getMessage());
        }
        // LogWrapper.d(TAG, "createFile " + file.getAbsolutePath() + " , result = " + result);
        return result;
    }

    public static boolean writeToFile(String msg, String fileRelPathAndName, boolean append) {
        return writeToFile(msg, fileRelPathAndName, append, true);
    }

    /**
     * 写文件
     *
     * @param msg         写入的内容
     * @param filePath    绝对路径,如: /sdcard/amapauto20/aa/bb
     * @param append      是否是追加模式
     * @param autoAddCTRL 自动在结尾添加回测换行符
     */
    public static boolean writeToFile(String msg, String filePath, boolean append, boolean autoAddCTRL) {
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }

        filePath = recookPath(filePath);

        if (!createFile(filePath)) {
            LogWrapper.w(TAG, "writeToFile fail as create file fail, path=" + filePath);
            return false;
        }

        if (msg == null) {
            msg = "";
        }

        if (autoAddCTRL) {
            msg += "\r\n";
        }

        boolean success = false;
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
        try {
            File file = new File(filePath);
            fileWriter = new FileWriter(file, append);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(msg);
            bufferedWriter.flush();
            success = true;
        } catch (IOException e) {
            e.printStackTrace();
            LogWrapper.e(TAG, "writeToFile fail as IOException:" + e.getMessage()
                    + ",path=" + filePath);
        } finally {
            safetyClose(bufferedWriter);
            safetyClose(fileWriter);
        }
        return success;
    }

    /**
     * 存储二进制文件
     *
     * @param byteArr  byte数组
     * @param filePath 要写入的文件绝对路径, 如 /aa/b.bin
     * @param append   true-追加 false-覆盖原文件内容
     */
    public static boolean writeToFile(@NonNull byte[] byteArr, @NonNull String filePath, boolean append) {
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }

        filePath = filePath.replace("\\", "/")
                .replace("//", "/");

        createFile(filePath);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(filePath, append);
            fos.write(byteArr);
            fos.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            safetyClose(fos);
        }
    }

    /**
     * 保存文件
     *
     * @param filePath 要保存的路径,如 /sdcard/amapauto20/jniScreenshot/xxx.jpg
     */
    public static void saveImage(Bitmap bitmap, String filePath) {
        if (bitmap == null || TextUtils.isEmpty(filePath)) {
            LogWrapper.e(TAG, "保存图片失败,请检查参数后再试");
            return;
        }
        FileOutputStream out = null;
        try {
            File file = new File(filePath);
            createFile(file);
            out = new FileOutputStream(file);
            Bitmap.CompressFormat format =
                    Bitmap.Config.ARGB_8888 == bitmap.getConfig() ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
            bitmap.compress(format, 100, out);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            safetyClose(out);
        }
    }

    /**
     * 删除文件或者目录
     *
     * @return true-目标文件不存在(包括原本就不存在以及删除成功两种情况)
     * false-目标文件仍存在
     */
    public static boolean deleteFile(@NonNull String path) {
        return deleteFile(path, true);
    }

    /**
     * 删除文件或者目录
     *
     * @param deleteWhenIsDir path对应文件是目录时,是否要删除, true-删除 false-非目录时才删除
     * @return true-目标文件不存在(包括原本就不存在以及删除成功两种情况)
     * false-目标文件仍存在
     */
    public static boolean deleteFile(@NonNull String path, boolean deleteWhenIsDir) {
        File file = new File(path);
        if (!file.exists()) {
            return true;
        }

        if (file.isFile()) {
            return file.delete();
        } else if (deleteWhenIsDir) {
            return deleteDir(file);
        } else {
            return true;
        }
    }

    /**
     * 删除目录
     */
    public static boolean deleteDir(String pPath) {
        return deleteDir(new File(pPath));
    }

    /**
     * 删除指定目录
     * 若存在同名非目录文件,则不处理
     */
    public static boolean deleteDir(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return true;
        }

        // 重命名下再删除, 避免出现  Device or resource busy 等问题
        String srcPath = dir.getAbsolutePath();
        String destPath = srcPath + "_" + System.currentTimeMillis();
        if (rename(srcPath, destPath)) {
            dir = new File(destPath);
        } else {
            LogWrapper.w(TAG, "deleteDir dir fail: srcPath=" + srcPath + ",destPath=" + destPath);
        }
        for (File file : dir.listFiles()) {
            if (file.isFile()) {// 删除所有文件
                boolean delete = file.delete();
                if (!delete) {
                    LogWrapper.w(TAG, "deleteDir sub file fail,destPath=" + destPath + ",name=" + file.getName());
                }
            } else if (file.isDirectory()) { // 递归删除子目录
                deleteDir(file);
            }
        }
        return dir.delete();// 删除空目录本身
    }

    /**
     * 按行读取文件内容
     * 参考 {@link #readAllLine(File)}
     */
    @NonNull
    public static ArrayList<String> readAllLine(@Nullable String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return new ArrayList<>();
        }
        return readAllLine(new File(filePath));
    }

    /**
     * 按行读取指定文件的所有内容
     * 若文件不存在,则返回空list
     */
    @NonNull
    public static ArrayList<String> readAllLine(@Nullable File file) {
        ArrayList<String> contentList = new ArrayList<>();
        if (file == null || !file.exists()) {
            return contentList;
        }

        FileReader fr = null;
        BufferedReader bfr = null;
        try {
            fr = new FileReader(file);
            bfr = new BufferedReader(fr);

            String line = bfr.readLine();
            while (line != null) {
                contentList.add(line);
                line = bfr.readLine();
            }

            return contentList;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            safetyClose(bfr);
            safetyClose(fr);
        }
        return contentList;
    }

    public static void safetyClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String recookPath(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return filePath;
        }
        return filePath.trim().replace("\\", "/").replace("//", "/");
    }

    /**
     * 移动文件到指定位置并重命名
     *
     * @param oriFilePath  源文件绝对路径
     * @param destFilePath 要移动到的目标位置绝对路径
     */
    public static boolean rename(@NonNull String oriFilePath, @NonNull String destFilePath) {
        File srcFile = new File(oriFilePath);
        if (!srcFile.exists()) {
            LogWrapper.e(TAG, "rename fail as " + oriFilePath + " not exist");
            return false;
        }

        File dest = new File(destFilePath);
        dest.getParentFile().mkdirs();
        return srcFile.renameTo(dest);
    }

}
