package elephantiza.elizaface;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.support.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private static final int TAKE_PICTURE = 0;
    private static final int PICK_PHOTO = 1;
    private static final int REQUEST_READ_PERMISSION = 786;
    private static final int REQUEST_WRITE_PERMISSION = 787;
    private FaceOverlayView faceView;
    private FloatingActionButton camera;
    private FloatingActionButton gallery;
    private FloatingActionButton download;
    Intent takePicture;
    Intent pickPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        requestReadPermission();

        setContentView(R.layout.activity_main);
        camera = findViewById(R.id.camera);
        gallery = findViewById(R.id.gallery);
        download = findViewById(R.id.download);
        faceView = (FaceOverlayView) findViewById( R.id.face_overlay );
        faceView.setDrawingCacheEnabled(true);
        faceView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        pickPhoto = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        InputStream stream = getResources().openRawResource( R.raw.face );
        Bitmap bitmap = BitmapFactory.decodeStream(stream);
        faceView.setBitmap(bitmap);

        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(takePicture, TAKE_PICTURE);
            }
        });
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(pickPhoto , PICK_PHOTO);
            }
        });
        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveImage();
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch(requestCode) {
            case TAKE_PICTURE:
                if(resultCode == RESULT_OK){
                    Bundle extras = imageReturnedIntent.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    faceView.setBitmap(imageBitmap);
                }

                break;
            case PICK_PHOTO:
                pickPhoto(resultCode, imageReturnedIntent);
                break;
        }
    }
    private void requestReadPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_PERMISSION);
        }
    }

    private void requestWritePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION);
        }
    }

    private void pickPhoto(int resultCode,Intent imageReturnedIntent) {
        if(resultCode == RESULT_OK){
            try {
                Uri selectedImage = imageReturnedIntent.getData();

                String[] filePath = {MediaStore.Images.Media.DATA};

                Cursor cursor = getContentResolver().query(selectedImage, filePath, null, null, null);
                cursor.moveToFirst();
                String mImagePath = cursor.getString(cursor.getColumnIndex(filePath[0]));
                cursor.close();
                InputStream stream = getContentResolver().openInputStream(selectedImage);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap yourSelectedImage = BitmapFactory.decodeStream(stream, null, options);
                stream.close();
                //orientation
                try {
                    int rotate = 0;
                    try {
                        ExifInterface exif = new ExifInterface(
                                mImagePath);
                        int orientation = exif.getAttributeInt(
                                ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_NORMAL);

                        switch (orientation) {
                            case ExifInterface.ORIENTATION_ROTATE_270:
                                rotate = 270;
                                break;
                            case ExifInterface.ORIENTATION_ROTATE_180:
                                rotate = 180;
                                break;
                            case ExifInterface.ORIENTATION_ROTATE_90:
                                rotate = 90;
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotate);
                    Bitmap imageBitmap = Bitmap.createBitmap(yourSelectedImage , 0, 0, yourSelectedImage.getWidth(), yourSelectedImage.getHeight(), matrix, true);
                    faceView.setBitmap(imageBitmap);
                }
                catch (Exception e) {}
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Could not open file.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void saveImage() {
        int ctime = Calendar.getInstance().get(Calendar.MILLISECOND);
        faceView.destroyDrawingCache();
        faceView.buildDrawingCache();
        Bitmap bitmap = faceView.getDrawingCache();

        File file = new File(android.os.Environment.getExternalStorageDirectory(),"ElizaFace");
        if (!file.exists()) {
            file.mkdirs();
        }
        String path = file.getAbsolutePath() + file.separator + ctime + ".png";
        File f = new File(path);
        MediaScannerConnection.scanFile(this, new String[]{f.getPath()},
                null, new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                    }
                });
        FileOutputStream ostream = null;
        try {
            ostream = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, ostream);

        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Intent intent=new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", new File(path)), "image/*");
            PendingIntent pendingIntent=PendingIntent.getActivity(getApplicationContext(),0,intent, PendingIntent.FLAG_UPDATE_CURRENT);
            ostream.flush();
            ostream.close();
            NotificationManager notif=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notify= new NotificationCompat.Builder(getApplicationContext(),"1")
                    .setContentTitle("THEOBESC, THEO! <3")
                    .setContentText("Image Saved Successfully")
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setAutoCancel(true)
                    .addAction(R.drawable.eliza,"Go",pendingIntent)//to add action button
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(R.drawable.eliza).build();
            notify.flags |= Notification.FLAG_AUTO_CANCEL;
            notif.notify(0, notify);
            Toast.makeText(getApplicationContext(), "Image saved",Toast.LENGTH_LONG).show();
        }

        catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Image failed to save. Try again.", Toast.LENGTH_LONG).show();
            Log.e("error",e.getMessage());
        }
    }
}