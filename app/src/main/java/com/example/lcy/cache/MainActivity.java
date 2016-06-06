package com.example.lcy.cache;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private GridView mGridView;
    private String [] imageThumbUrls = Images.imageThumbUrls;
    private ImageAdapter mImageAdapter;
    private FileUtils fileUtils;
    private ImageView iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fileUtils = new FileUtils(this);
        mGridView = (GridView) findViewById(R.id.gridView);
        mImageAdapter = new ImageAdapter(this, mGridView, imageThumbUrls);
        mGridView.setAdapter(mImageAdapter);

        iv = (ImageView) findViewById(R.id.iv);

    }

    @Override
    protected void onResume() {
        /*
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    URL url = new URL("http://img.chinasexq.com/uploadfiles/news/2016-2/1811834411.jpg");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(20000);
                    connection.setReadTimeout(20000);
                    int ret = connection.getResponseCode();
                    System.out.println("------>"+ret);

                    final Bitmap bitmap = BitmapFactory.decodeStream(connection.getInputStream());

                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            iv.setImageBitmap(bitmap);
                        }
                    });

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        */
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        mImageAdapter.cancelTask();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("删除手机中图片缓存");
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                fileUtils.deleteFile();
                Toast.makeText(getApplication(), "清空缓存成功", Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
