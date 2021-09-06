package com.riche.shoottheballoons;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Point;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.collision.Ray;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.Texture;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private Scene scene;//場景物件
    private Camera camera;
    private ModelRenderable bulletRendable;
    private boolean shouldStartTimer = true;
    private int balloonsLeft = 20;
    private Point point;
    private TextView balloonsLeftTxt;
    private SoundPool soundPool;
    private int sound;

    private static final String TAG = "CompassActivity";

    private Compass compass;
    private ImageView arrowView;
    private TextView sotwLabel;  // SOTW is for "side of the world"

    private float currentAzimuth;
    private SOTWFormatter sotwFormatter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Display display = getWindowManager().getDefaultDisplay();//取得螢幕寬跟高度
        point = new Point();//point物件用來儲存目前螢幕高度和寬度
        display.getRealSize(point);//取得螢幕寬跟高度以前用.getDefaultDisplay().getWidth()跟.getDefaultDisplay().getHeight()

        setContentView(R.layout.activity_main);

        balloonsLeftTxt = findViewById(R.id.balloonsCntTxt);
        CustomArFragment arFragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);

        loadSoundPool();
        scene = arFragment.getArSceneView().getScene();//取得arfragment的場景
        camera = scene.getCamera();//取得相機使用

        sotwFormatter = new SOTWFormatter(this);

        arrowView = findViewById(R.id.main_image_hands);
        sotwLabel = findViewById(R.id.sotw_label);
        setupCompass();

        addBalloonsToScene();
        buildBulletModel();


        Button shoot = findViewById(R.id.shootButton);
        shoot.setOnClickListener(v -> {
            if (shouldStartTimer) {
                startTimer();
                shouldStartTimer = false;
            }
            
            shoot();
        });

    }

    private void loadSoundPool() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()//建立AudioAttributes物件
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)//設定內容型態為聲音
                .setUsage(AudioAttributes.USAGE_GAME)//設定這個物件是給遊戲使用
                .build();//建立

        soundPool = new SoundPool.Builder()//建立soundPool物件來讓app使用音樂
                .setMaxStreams(1)//設定同時播放的音樂數量為1
                .setAudioAttributes(audioAttributes)//把audioattributes設定好的資料給soundpool物件
                .build();//建立

        sound = soundPool.load(this, R.raw.blop_sound, 1);//讀取blop_sound檔案給(int)sound變數最後一個parameter沒有影響 設置1即可
    }

    private void shoot() {
        Ray ray = camera.screenPointToRay(point.x / 2f, point.y / 2f);//做十字準心
        Node node = new Node();
        node.setRenderable(bulletRendable);
        scene.addChild(node);

        new Thread(() -> {//建立一個執行緒
            for (int i = 0; i < 200; i++) {
                int finalI = i;
                runOnUiThread(() -> {
                    Vector3 vector3 = ray.getPoint(finalI * 0.1f);
                    node.setWorldPosition(vector3);

                    Node nodeInContact = scene.overlapTest(node);//檢查子彈有沒有撞到氣球

                    if (nodeInContact != null) {
                        balloonsLeft--;
                        balloonsLeftTxt.setText("Balloons Left : " + balloonsLeft);
                        scene.removeChild(nodeInContact);

                        soundPool.play(sound, 1f, 1f, 1, 0, 1f);
                    }
                });

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            runOnUiThread(() -> {
                scene.removeChild(node);
            });
        }).start();
    }

    //計時
    private void startTimer() {
        TextView timer = findViewById(R.id.timerText);

        new Thread(() -> {
            int seconds = 0;
            while (balloonsLeft > 0) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                seconds++;

                int minutesPassed = seconds / 60;
                int secondsPassed = seconds % 60;

                runOnUiThread(() -> timer.setText(minutesPassed + " : " + secondsPassed));
            }
        }).start();
    }

    //建立子彈模型
    private void buildBulletModel() {
        Texture
                .builder()
                .setSource(this, R.drawable.texture)//設定子彈的資源
                .build()
                .thenAccept(texture -> {//lambda語法
                    MaterialFactory
                            .makeOpaqueWithTexture(this, texture)
                            .thenAccept(material -> {
                                bulletRendable = ShapeFactory
                                        .makeSphere(0.01f,
                                                new Vector3(0f, 0f, 0f ),
                                                material);
                            });
                });
    }

    //將氣球丟到場景中
    private void addBalloonsToScene () {
        ModelRenderable//利用Node渲染3D模型
                .builder()
                .setSource(this, Uri.parse("balloon.sfb"))//設定Model的資源
                .build()
                .thenAccept(renderable -> { //lambda語法 input -> body
                    for (int i = 0; i < 20; i++) {
                        Node node = new Node();
                        node.setRenderable(renderable);
                        scene.addChild(node);

                        Random random = new Random();
                        int x = random.nextInt(10);
                        int y = random.nextInt(20);
                        int z = random.nextInt(10);

                        z = -z;//把z成負數不然氣球會被場景下而不是在場景上

                        node.setWorldPosition(new Vector3(
                                (float) x,
                                y / 10f,
                                (float) z
                        ));
                    }

                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "start compass");
        compass.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        compass.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        compass.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "stop compass");
        compass.stop();
    }

    private void setupCompass() {
        compass = new Compass(this);
        Compass.CompassListener cl = getCompassListener();
        compass.setListener(cl);
    }

    private void adjustArrow(float azimuth) {
        Log.d(TAG, "will set rotation from " + currentAzimuth + " to "
                + azimuth);

        Animation an = new RotateAnimation(-currentAzimuth, -azimuth,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        currentAzimuth = azimuth;

        an.setDuration(500);
        an.setRepeatCount(0);
        an.setFillAfter(true);

        arrowView.startAnimation(an);
    }

    private void adjustSotwLabel(float azimuth) {
        sotwLabel.setText(sotwFormatter.format(azimuth));
    }

    private Compass.CompassListener getCompassListener() {
        return new Compass.CompassListener() {
            @Override
            public void onNewAzimuth(final float azimuth) {
                // UI updates only in UI thread
                // https://stackoverflow.com/q/11140285/444966
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adjustArrow(azimuth);
                        adjustSotwLabel(azimuth);
                    }
                });
            }
        };
    }
}
