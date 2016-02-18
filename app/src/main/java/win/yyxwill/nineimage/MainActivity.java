package win.yyxwill.nineimage;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jude.library.imageprovider.ImageProvider;
import com.jude.library.imageprovider.OnImageSelectListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import win.yyxwill.nineimage.Util.ImagePiece;
import win.yyxwill.nineimage.Util.ImageSplitter;

import static android.view.View.OnClickListener;

public class MainActivity extends AppCompatActivity {
    private ImageProvider provider;
    //    Button mainbutton;
    ImageView mainimage;
    TextView maintext;
    ProgressBar mainprogressBar;
    //    ImageView mainimagesmall;
    Menu toolbarmenu;
    public static List<ImagePiece> imageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        init();

    }


    private void init() {
        final WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        provider = new ImageProvider(this);
//        mainbutton = (Button) findViewById(R.id.main_button);
//        mainimagesmall = (ImageView) findViewById(R.id.main_image_small);
        maintext = (TextView) findViewById(R.id.main_text);
        mainprogressBar = (ProgressBar) findViewById(R.id.main_progressBar);
        mainimage = (ImageView) findViewById(R.id.main_image);
        mainimage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                provider.getImageFromCameraOrAlbum(new OnImageSelectListener() {
                    @Override
                    public void onImageSelect() {

                    }

                    @Override
                    public void onImageLoaded(Uri uri) {

                        //获取说得图片尺寸
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        Bitmap bmp = BitmapFactory.decodeFile(uri.toString(), options);

                        wm.getDefaultDisplay().getWidth();

                        int width = wm.getDefaultDisplay().getWidth() < wm.getDefaultDisplay().getHeight() ? wm.getDefaultDisplay().getWidth() : wm.getDefaultDisplay().getHeight();
                        Log.i(options.outWidth + " 123123123", options.outWidth + "");

                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                        String option = prefs.getString("setting_image_quality_preference", "高");
                        String[] optionText = MainActivity.this.getResources().getStringArray(R.array.setting_image_quality_options);
                        Log.i("图片质量num", option);
                        Log.i("图片质量string", optionText[Integer.parseInt(option)]);


                        provider.corpImage(uri, (Integer.parseInt(option)+1)*500, (Integer.parseInt(option)+1)*500, new OnImageSelectListener() {
                            @Override
                            public void onImageSelect() {

                            }

                            @Override
                            public void onImageLoaded(Uri uri) {
                                mainimage.setImageURI(uri);
                                MenuItem item = (MenuItem) toolbarmenu.findItem(R.id.action_ok);
                                item.setVisible(true);
                                maintext.setVisibility(View.GONE);
                                Log.i("uriuriuriuriuri", uri.toString());
                                Bitmap bmp = ((BitmapDrawable) ((ImageView) mainimage).getDrawable()).getBitmap();
                                ImageSplitter imageSplitter = new ImageSplitter();
                                imageList = imageSplitter.split(bmp, 3, 3);
                            }

                            @Override
                            public void onError() {

                            }
                        });
                    }

                    @Override
                    public void onError() {

                    }
                });
            }
        });
    }

    public void saveBitmap(List<ImagePiece> imageList) {
        Log.e("TAG", "保存图片");
        mainprogressBar.setVisibility(View.VISIBLE);
        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-ddhhmm");
        final String date = sDateFormat.format(new java.util.Date());
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {

                File destDir = new File(Environment.getExternalStorageDirectory().getPath() + "/NineImage/" + date);
                if (!destDir.exists()) {
                    destDir.mkdirs();
                }
                for (int i = 0; i < 9; i++) {
                    File f = new File(Environment.getExternalStorageDirectory().getPath() + "/NineImage/" + date, i + ".png");
                    if (f.exists()) {
                        f.delete();
                    }
                    try {
                        FileOutputStream out = new FileOutputStream(f);
                        MainActivity.imageList.get(i).bitmap.compress(Bitmap.CompressFormat.PNG, 0, out);
                        out.flush();
                        out.close();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mainprogressBar.setProgress(mainprogressBar.getProgress() + (100 / 9));
                            }
                        });
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainprogressBar.setProgress(100);
                        Toast.makeText(MainActivity.this, "图片已导出至" + Environment.getExternalStorageDirectory().getPath() + "/NineImage/" + date, Toast.LENGTH_LONG).show();
                    }
                });
                ArrayList uriList = new ArrayList<>();
                for (int i = 0; i < 9; i++) {
                    uriList.add(Uri.fromFile(new File(Environment.getExternalStorageDirectory().getPath() + "/NineImage/" + date + "/" + i + ".png")));
                }
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
                shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList);
                shareIntent.setType("image/*");
                startActivity(Intent.createChooser(shareIntent, "分享到"));

            }
        });
        t.start();

    }

    public void delete(File file) {
        if (file.isDirectory()) {
            File[] childFiles = file.listFiles();
            if (childFiles == null || childFiles.length == 0) {
                file.delete();
                return;
            }

            for (int i = 0; i < childFiles.length; i++) {
                delete(childFiles[i]);
            }
            file.delete();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        toolbarmenu = menu;
        MenuItem item = (MenuItem) toolbarmenu.findItem(R.id.action_ok);
        item.setVisible(false);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        provider.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_settings:
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.action_ok:
                saveBitmap(imageList);
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
