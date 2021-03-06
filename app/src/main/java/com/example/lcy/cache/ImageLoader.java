package com.example.lcy.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.support.v4.util.LruCache;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by luchunyang on 16/6/5.
 */

//ImageDownLoader类，异步下载的核心类，保存图片到手机缓存，将图片加入LruCache中等等
public class ImageLoader {

    private Context context;
    private ExecutorService mImageThreadPool = null;
    private FileUtils fileUtils = null;

    //缓存Image的类，当存储Image的大小大于LruCache设定的值，系统自动释放内存
    private LruCache<String,Bitmap> lruCache;

    public ImageLoader(Context context) {
        this.context = context;
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int mCacheSize = maxMemory/8;
        LinkedHashMap<String,String>s;

        lruCache = new LruCache<String, Bitmap>(mCacheSize){

            //必须重写此方法，来测量Bitmap的大小
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes()*value.getHeight();
            }

            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                super.entryRemoved(evicted, key, oldValue, newValue);
            }
        };

        fileUtils = new FileUtils(context);
    }


    /**
     * 获取线程池的方法，因为涉及到并发的问题，我们加上同步锁
     * @return
     */
    public ExecutorService getThreadPool(){
        if(mImageThreadPool == null){
            synchronized(ExecutorService.class){
                if(mImageThreadPool == null){
                    //为了下载图片更加的流畅，我们用了2个线程来下载图片
                    mImageThreadPool = Executors.newFixedThreadPool(2);
                }
            }
        }

        return mImageThreadPool;

    }

    /**
     * 添加Bitmap到内存缓存
     * @param key
     * @param bitmap
     */
    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null && bitmap != null) {
            lruCache.put(key, bitmap);
        }
    }

    /**
     * 从内存缓存中获取一个Bitmap
     * @param key
     * @return
     */
    public Bitmap getBitmapFromMemCache(String key) {
        return lruCache.get(key);
    }

    /**
     * 获取Bitmap, 内存中没有就去手机或者sd卡中获取，这一步在getView中会调用，比较关键的一步
     * @param url
     * @return
     */
    public Bitmap showCacheBitmap(String url) {
        if (getBitmapFromMemCache(url) != null) {
            return getBitmapFromMemCache(url);
        } else if (fileUtils.isFileExists(url) && fileUtils.getFileSize(url) != 0) {
            //从SD卡获取手机里面获取Bitmap
            Bitmap bitmap = fileUtils.getBitmap(url);

            //将Bitmap 加入内存缓存
            addBitmapToMemoryCache(url, bitmap);
            return bitmap;
        }

        return null;
    }


    /**
     * 从Url中获取Bitmap
     * @param url
     * @return
     */
    private Bitmap getBitmapFormUrl(String url) {
        Bitmap bitmap = null;
        HttpURLConnection con = null;
        try {
            URL mImageUrl = new URL(url);
            con = (HttpURLConnection) mImageUrl.openConnection();
            con.setConnectTimeout(10 * 1000);
            con.setReadTimeout(10 * 1000);
            con.setDoInput(true);
            bitmap = BitmapFactory.decodeStream(con.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return bitmap;
    }


    /**
         * 先从内存缓存中获取Bitmap,如果没有就从SD卡或者手机缓存中获取，SD卡或者手机缓存
         * 没有就去下载
         */
    public Bitmap downloadImage(final String url, final onImageLoaderListener listener){
        //替换Url中非字母和非数字的字符，这里比较重要，因为我们用Url作为文件名，比如我们的Url
        //是Http://xiaanming/abc.jpg;用这个作为图片名称，系统会认为xiaanming为一个目录，
        //我们没有创建此目录保存文件就会报错
        final String subUrl = url.replaceAll("[^\\w]", "");
        Bitmap bitmap = showCacheBitmap(subUrl);
        if(bitmap != null){
            return bitmap;
        }else {

            final Handler handler = new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    listener.onImageLoader((Bitmap)msg.obj, url);
                }
            };


            getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    Bitmap bitmap = getBitmapFormUrl(url);
                    Message msg = handler.obtainMessage();
                    msg.obj = bitmap;
                    handler.sendMessage(msg);

                    try {
                        fileUtils.savaBitmap(subUrl,bitmap);
                    }catch (Exception e){

                    }

                    addBitmapToMemoryCache(subUrl,bitmap);
                }
            });
        }

        return null;
    }


    /**
     * 取消正在下载的任务
     */
    public synchronized void cancelTask() {
        if(mImageThreadPool != null){
            mImageThreadPool.shutdownNow();
            mImageThreadPool = null;
        }
    }

    /**
     * 异步下载图片的回调接口
     * @author len
     */
    public interface onImageLoaderListener{
        void onImageLoader(Bitmap bitmap, String url);
    }

}
