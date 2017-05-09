package com.sikeandroid.nationdaily.cosplay;

import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import com.sikeandroid.nationdaily.R;
import com.sikeandroid.nationdaily.utils.SettingsCamera;
import com.sikeandroid.nationdaily.utils.TakePhoto;
import java.io.File;

import static com.sikeandroid.nationdaily.utils.CameraParam.FLASH_CLOSE;
import static com.sikeandroid.nationdaily.utils.CameraParam.FLASh_OPEN;
import static com.sikeandroid.nationdaily.utils.CameraParam.flashFlag;

public class ARCamera extends AppCompatActivity implements View.OnClickListener {

  private static final String TAG = "ARCamera";
  private ImageButton takePhoto;
  private ImageButton changeCamera;
  private TakePhoto mPreview;
  private ImageView mediaPreview;

  public static ImageView clothes;
  public static FrameLayout mainInterface;
  private static int lastX;
  private static int lastY;
  int screenWidth, screenHeight;

  //民族服装相关信息
  public static int nationClothesId;
  public static String nationName;
  public static int clothesX;
  public static int clothesY;

  //public DrawerLayout drawClothes;
  private ImageButton flash;

  @Override protected void onCreate(Bundle savedInstanceState) {
    //满屏显示
    this.getWindow()
        .setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN );
    super.onCreate( savedInstanceState );
    setContentView( R.layout.activity_ar_camera );

    takePhoto = (ImageButton) findViewById( R.id.take_photo );
    mediaPreview = (ImageView) findViewById( R.id.preview );
    mainInterface = (FrameLayout) findViewById( R.id.main_interface );

    changeCamera = (ImageButton) findViewById( R.id.change_camera );
    if (Camera.getNumberOfCameras() == 1) {
      TakePhoto.cameraFlag = TakePhoto.BACK_CAMERA;
      //兼容只有一个摄像头
      changeCamera.setVisibility( View.GONE );
    }

    flash = (ImageButton) findViewById( R.id.flash );
    flashState();

    //传入衣服
    passClothes();

    takePhoto.setOnClickListener( this );

    //默认预览是上次拍的最后一张图片
    if (getMediaImageUri() != null) {
      mediaPreview.setImageURI( getMediaImageUri() );
    }
    //全屏预览图片
    mediaPreview.setOnClickListener( this );

    //翻转摄像机
    changeCamera.setOnClickListener( this );

    //侧滑功能
    final ClothesFragment clothesFragment = new ClothesFragment();
    getFragmentManager().beginTransaction().replace( R.id.pref_set, clothesFragment ).commit();

    Display dis = this.getWindowManager().getDefaultDisplay();
    screenWidth = dis.getWidth();
    screenHeight = dis.getHeight();
    clothes.setOnTouchListener( new View.OnTouchListener() {
      @Override public boolean onTouch(View v, MotionEvent event) {

        switch (event.getAction()) {
          case MotionEvent.ACTION_DOWN:
            lastX = (int) event.getRawX();
            lastY = (int) event.getRawY();

            break;

          case MotionEvent.ACTION_MOVE:
            int dx = (int) event.getRawX() - lastX;
            int dy = (int) event.getRawY() - lastY;

            int top = v.getTop() + dy;

            int left = v.getLeft() + dx;

            //if (top <= 0) {
            //  top = 0;
            //}
            //if (top >= screenHeight - v.getHeight()) {
            //  top = screenHeight - v.getHeight();
            //}
            //if (left >= screenWidth - v.getWidth()) {
            //  left = screenWidth - v.getWidth();
            //}
            //
            //if (left <= 0) {
            //  left = 0;
            //}

            v.layout( left, top, left + v.getWidth(), top + v.getHeight() );
            lastX = (int) event.getRawX();
            lastY = (int) event.getRawY();
            clothesX = (int) v.getX();
            clothesY = (int) v.getY();

            break;
          case MotionEvent.ACTION_UP:

            break;
        }

        return true;
      }
    } );
  }

  private void changeCamera() {
    if (TakePhoto.cameraFlag == TakePhoto.BACK_CAMERA) {
      if (TakePhoto.flashFlag == TakePhoto.FLASh_OPEN) {
        TakePhoto.flashFlag = TakePhoto.FLASH_CLOSE;
        mPreview.closeFlashLight();
      }
      TakePhoto.cameraFlag = TakePhoto.FRONT_CAMERA;
      mPreview.changeCamera();
      SettingsCamera.passCamera( mPreview.getCameraInstance() );
      SettingsCamera.initTakePhoto();
      mPreview.changePreview();
    } else {
      TakePhoto.cameraFlag = TakePhoto.BACK_CAMERA;
      mPreview.changeCamera();
      SettingsCamera.passCamera( mPreview.getCameraInstance() );
      SettingsCamera.initTakePhoto();
      mPreview.changePreview();
      flash.setBackgroundResource( R.drawable.flash_off );
    }
    flashState();
  }

  private void mediaPreview() {
    if (TakePhoto.cameraFlag == TakePhoto.BACK_CAMERA) {
      if (flashFlag == FLASh_OPEN) {
        flashFlag = FLASH_CLOSE;
        flash.setBackgroundResource( R.drawable.flash_off );
      }
    }
    Intent intent = new Intent( ARCamera.this, ShowPhoto.class );
    if (mPreview.getOutputMediaFileType() == null) {
      if (getMediaImageUri() == null) {
        return;
      }
      intent.setDataAndType( getMediaImageUri(), "image/*" );
      //return;
    } else {
      intent.setDataAndType( mPreview.getOutputMediaFileUri(), mPreview.getOutputMediaFileType() );
    }
    startActivityForResult( intent, 0 );
  }

  private void flashState() {
    if (TakePhoto.cameraFlag == TakePhoto.BACK_CAMERA) {
      flash.setVisibility( View.VISIBLE );
      flash.setOnClickListener( new View.OnClickListener() {
        @Override public void onClick(View v) {
          mPreview.flashLightUtils();
          if (flashFlag == FLASh_OPEN) {
            flash.setBackgroundResource( R.drawable.flash_on );
          } else {
            flash.setBackgroundResource( R.drawable.flash_off );
          }
        }
      } );
    } else {
      flash.setVisibility( View.GONE );
    }
  }

  private void passClothes() {
    clothes = new ImageView( this );
    //获取民族相关的信息
    Intent intent = getIntent();
    nationClothesId = intent.getIntExtra( Cloth.NATION_CLOTH_ID, R.drawable.bai_clothes );
    nationName = intent.getStringExtra( Cloth.NATION_NAME );
    setPosition();
  }

  public static void setPosition() {
    clothes.setImageResource( nationClothesId );
    //定义布局
    FrameLayout.LayoutParams params =
        new FrameLayout.LayoutParams( FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT );
    mainInterface.addView( clothes, 1, params );
    if (nationName.endsWith( "衣服" )) {
      //clothes.layout( 20, 300, 20 + clothes.getWidth(), 300 + clothes.getWidth() );
      clothesX = 20;
      clothesY = 300;
    } else if (nationName.endsWith( "帽子" )) {
      //clothes.layout( 300, 100, 300 + clothes.getWidth(), 100 + clothes.getWidth() );
      clothesX = 300;
      clothesY = 100;
    }
    clothes.setX( clothesX );
    clothes.setY( clothesY );
    Log.d( "ARCamera", clothes.getX() + "," + clothes.getY() );

    clothesX = (int) clothes.getX();
    clothesY = (int) clothes.getY();
  }

  public static void setClothesPosition() {
    //定义布局
    clothes.layout( clothesX, clothesY, clothesX + clothes.getWidth(), clothes.getHeight() );
    Log.d( "ARCamera:", clothes.getX() + "," + clothes.getY() );
  }

  private void initCamera() {

    mPreview = new TakePhoto( this );
    FrameLayout preview = (FrameLayout) findViewById( R.id.camera_preview );
    preview.addView( mPreview );
    SettingsCamera.passCamera( mPreview.getCameraInstance() );
    SettingsCamera.initTakePhoto();
  }

  private Uri getMediaImageUri() {
    File file = new File( "/sdcard/com.sike.android/DCIM" );
    Uri uri;
    if (!file.exists()) {
      return null;
    }
    File[] files = file.listFiles();
    if (files.length == 0) {
      return null;
    } else {
      uri = Uri.fromFile( files[files.length - 1] );
    }
    return uri;
  }

  @Override protected void onPause() {
    super.onPause();
    mPreview = null;
  }

  @Override protected void onResume() {
    super.onResume();
    if (mPreview == null) {
      initCamera();
      if (TakePhoto.cameraFlag == TakePhoto.BACK_CAMERA) {
        if (TakePhoto.flashFlag == TakePhoto.FLASh_OPEN) {
          mPreview.closeFlashLight();
          TakePhoto.flashFlag = TakePhoto.FLASH_CLOSE;
        }
      }
    }
  }

  @Override public void onClick(View v) {
    switch (v.getId()) {
      case R.id.take_photo:
        Log.d( TAG, "clothes:X=" + clothes.getX() + ",Y=" + clothes.getY() );
        Log.d( TAG, "clothesX=" + clothesX + ",clothesY=" + clothesY );
        clothesX = (int) clothes.getX();
        clothesY = (int) clothes.getY();
        mPreview.takePicture( mediaPreview );
        break;
      case R.id.preview:
        mediaPreview();
        break;
      case R.id.change_camera:
        changeCamera();
        break;
    }
  }
}
