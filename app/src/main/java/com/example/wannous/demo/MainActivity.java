package com.example.wannous.demo;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_features2d;
import org.bytedeco.javacpp.opencv_ml;
import org.bytedeco.javacpp.opencv_nonfree;
import org.bytedeco.javacv.CanvasFrame;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static org.bytedeco.javacpp.opencv_features2d.*;
import static org.bytedeco.javacpp.opencv_highgui.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    // SIFT keypoint features
    private static final int N_FEATURES = 0;
    private static final int N_OCTAVE_LAYERS = 3;
    private static final double CONTRAST_THRESHOLD = 0.04;
    private static final double EDGE_THRESHOLD = 10;
    private static final double SIGMA = 1.6;

    private static final int CAMERA_REQUEST = 1;
    private static int LOAD_IMAGE = 2;
    private int READ_PERMISSION = 1;


    public opencv_core.Mat img;
    private opencv_nonfree.SIFT SiftDesc;
    private String ImageString;


    TextView mTextView;
    RequestQueue queue;
    JSONObject jsonObj;
    ImageView imageView;

    private static String dictionary;

    private static final String jsonFilename = "index.json";
    private static String url = new String();
    private static String urlSoda = "http://www-rech.telecom-lille.fr/nonfreesift/";
    private static String urlVoiture = "http://www-rech.telecom-lille.fr/freeorb/";


    private final int choix = 1;
    private String filePath;
    int classNumber;
    String[] class_names;


    protected boolean shouldAskPermissions() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    //cf. OnCreate, pb with permissions with gallery
    @TargetApi(Build.VERSION_CODES.M)
    protected void askPermissions() {
        String[] permissions = {
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"
        };
        int requestCode = 200;
        requestPermissions(permissions, requestCode);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(shouldAskPermissions()){
            askPermissions();
        }

        String refFile = "Sprite_1.png";
        this.filePath = this.ToCache(this, "images" + "/" + refFile, refFile).getPath();

        imageView = (ImageView) findViewById(R.id.imageView);
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
        imageView.setImageBitmap(bitmap);

        Button gallery = (Button) findViewById(R.id.Galley);
        Button analyse = (Button) findViewById(R.id.Analyse);
        Button photo = (Button) findViewById(R.id.Photo);

        gallery.setOnClickListener(this);
        analyse.setOnClickListener(this);
        photo.setOnClickListener(this);


        queue = Volley.newRequestQueue(this);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                switch (choix){
                    case 1:
                        url = urlSoda;
                        break;
                    case 2:
                        url = urlVoiture;
                        break;
                }
                getJSONFiles(url);
            }
        }, 2000);




        /*
        try {
            FileInputStream fis = new FileInputStream(new File(getCacheDir()+"/vocabulary.yml"));
            StringBuilder builder = new StringBuilder();
            int ch;
            while ((ch = fis.read()) != -1){
                builder.append((char)ch);
            }
            System.out.println(builder.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }*/


        //return bestMatch;

    }


    @Override
    public void onClick(View view) {

        int id = view.getId();

        switch (id) {

            case R.id.Photo:
                dispatchTakePictureIntent();
                break;
            //charge les différent fichier nécessaire à l'analyse comme le vocabulaire
            case R.id.Analyse:
                final opencv_core.Mat vocabulary;

                System.out.println("read vocabulary from file... ");
                //Loader.load(opencv_core.class);

                opencv_core.CvFileStorage storage = opencv_core.cvOpenFileStorage(getCacheDir() + "/vocabulary.yml", null, opencv_core.CV_STORAGE_READ);
                Pointer p = opencv_core.cvReadByName(storage, null, "vocabulary", opencv_core.cvAttrList());
                opencv_core.CvMat cvMat = new opencv_core.CvMat(p);
                vocabulary = new opencv_core.Mat(cvMat);
                System.out.println(vocabulary.toString());
                System.out.println("vocabulary loaded " + vocabulary.rows() + " x " + vocabulary.cols());
                opencv_core.cvReleaseFileStorage(storage);


                //create SIFT feature point extracter
                final opencv_nonfree.SIFT detector;
                // default parameters ""opencv2/features2d/features2d.hpp""
                detector = new opencv_nonfree.SIFT(0, 3, 0.04, 10, 1.6);

                //create a matcher with FlannBased Euclidien distance (possible also with BruteForce-Hamming)
                final FlannBasedMatcher matcher;
                matcher = new FlannBasedMatcher();

                //create BoF (or BoW) descriptor extractor
                final BOWImgDescriptorExtractor bowide;
                bowide = new BOWImgDescriptorExtractor(detector.asDescriptorExtractor(), matcher);

                //Set the dictionary with the vocabulary we created in the first step
                bowide.setVocabulary(vocabulary);
                System.out.println("Vocab is set");



                if (choix == 1) {
                    //reconnaissance des soda
                    classNumber = 3;
                    class_names = new String[classNumber];

                    class_names[0] = "Coca";
                    class_names[1] = "Pepsi";
                    class_names[2] = "Sprite";
                }else if (choix == 2) {
                    //reconnaissance des Voitures
                    classNumber = 6;
                    class_names = new String[classNumber];

                    class_names[0] = "Alpha Romeo";
                    class_names[1] = "Audi";
                    class_names[2] = "BMW";
                    class_names[3] = "Ferrari";
                    class_names[4] = "Skoda";
                    class_names[5] = "Toyota";
                }

                //Charge dans le cache les différent classifieur précédemment télécharger sur le serveur et enregistrés
                final opencv_ml.CvSVM[] classifiers;
                classifiers = new opencv_ml.CvSVM[classNumber];
                for (int i = 0; i < classNumber; i++) {
                    //System.out.println("Ok. Creating class name from " + className);
                    //open the file to write the resultant descriptor
                    classifiers[i] = new opencv_ml.CvSVM();
                    classifiers[i].load(getCacheDir() + "/" + class_names[i] + ".xml");
                }


                opencv_core.Mat response_hist = new opencv_core.Mat();
                KeyPoint keypoints = new KeyPoint();
                opencv_core.Mat inputDescriptors = new opencv_core.Mat();

                opencv_core.Mat imageTest = imread(filePath, 1);

                //Analyse it
                detector.detectAndCompute(imageTest, opencv_core.Mat.EMPTY, keypoints, inputDescriptors);
                bowide.compute(imageTest, keypoints, response_hist);

                // Finding best match
                float minf = Float.MAX_VALUE;
                String bestMatch = null;
                long timePrediction = System.currentTimeMillis();

                // loop for all classes
                for (int j = 0; j < classNumber; j++) {
                    // classifier prediction based on reconstructed histogram
                    float res = classifiers[j].predict(response_hist, true);
                    if (res < minf) {
                        minf = res;
                        bestMatch = class_names[j];
                    }
                }

                timePrediction = System.currentTimeMillis() - timePrediction;
                Toast.makeText(this, "Analysis "+ filePath + "  predicted as " + bestMatch + " in " + timePrediction + " ms", Toast.LENGTH_SHORT).show();
                Log.d("Analysis", filePath + "  predicted as " + bestMatch + " in " + timePrediction + " ms");
                break;
            //open an intent to select an image from gallery
            case R.id.Galley:
                Log.i("Gallery button", "User clicked the gallery Button");
                //gal.loadImagefromGallery();
                Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, LOAD_IMAGE);
                break;

            default:
                break;
        }


    }

    public void getJSONFiles(String urlChoosen) {

        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url + jsonFilename, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject json) {
                        try {
                            //getting vocabulary
                            final String dictionaryName = json.getString("vocabulary");
                            StringRequest stringRequest = new StringRequest(Request.Method.GET, url + dictionaryName,
                                    new Response.Listener<String>() {
                                        @Override
                                        public void onResponse(String response) {
                                            Log.i("DICTIONARY", dictionaryName + " DONE");
                                            //saveDictionnary(response);
                                            //System.out.println(response);
                                            stringToCache(response, "vocabulary.yml");
                                        }
                                    },
                                    new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            Log.e("DICTIONARY", dictionaryName + " FAIL");
                                        }
                                    });
                            queue.add(stringRequest);

                            JSONArray jsonArrayBrands = json.getJSONArray("brands");
                            Brand[] arrayBrands = new Brand[jsonArrayBrands.length()];

                            for (int i = 0; i < jsonArrayBrands.length(); i++) {
                                JSONObject brand = jsonArrayBrands.getJSONObject(i);

                                JSONArray imagesNames = brand.getJSONArray("images");
                                String[] images = new String[imagesNames.length()];

                                for (int j = 0; j < images.length; j++) {
                                    images[j] = imagesNames.getString(j);
                                }

                                //Create new brand and add to array
                                arrayBrands[i] = createBrand(brand, images, queue);
                            }
                        } catch (JSONException e) {
                            Log.e("getJSONFiles ERR", "onResponse: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("ERR", "onErrorResponse: " + error.getMessage());
                    }
                }
        );
        queue.add(jsonRequest);
    }
    //use to save the yml file
    public void saveDictionnary(String response) throws IOException {
        File file = new File(getCacheDir() + File.separator + "vocabulary.yml");
        //long fileSize = file.length();

        //overwrite the existing file
        FileOutputStream fout = new FileOutputStream(file, false);
        fout.write(response.getBytes());
        fout.flush();
        fout.close();
    }

    //create a Brand Object from the data register in the JSON File
    public Brand createBrand(JSONObject brand, String[] images, RequestQueue queue) throws JSONException {
        Brand brandItem = new Brand(
                brand.getString("brandname"),
                brand.getString("url"),
                brand.getString("classifier"),
                images,
                queue,
                this,
                choix
        );
        return brandItem;
    }

    public File stringToCache(String string, String fileName) {
        File file = new File(getCacheDir(), fileName);
        try {
            file.createNewFile();

            byte[] stringData = string.getBytes();

            FileOutputStream fos = new FileOutputStream(file, false);
            fos.write(stringData);

            fos.flush();
            fos.close();

            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Utilisée pour enregistré un fichier dans le cache (permettant de contourner les problème d'annalyse d'image de different format avec JavaCV
    public static File ToCache(Context context, String Path, String fileName) {
        InputStream input;
        FileOutputStream output;
        byte[] buffer;
        String filePath = context.getCacheDir() + "/" + fileName;
        File file = new File(filePath);
        AssetManager assetManager = context.getAssets();

        try {
            input = assetManager.open(Path);
            buffer = new byte[input.available()];
            input.read(buffer);
            input.close();

            output = new FileOutputStream(filePath);
            output.write(buffer);
            output.close();
            return file;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    //Affichage d'une image dans l'image view de la main view
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST);
            }
        }
    }
    // Use to save the picture taken from the application by the user.
    private File createImageFile() throws IOException {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        filePath = image.getAbsolutePath();
        return image;
    }

    //Sur le retour d'une activité lancer par la main activity
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            //if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            if (requestCode == CAMERA_REQUEST ) {
                //Make sure the picture has the right dimensions
                setPic();
            }
            // When an Image is picked
            else if (requestCode == LOAD_IMAGE && resultCode == RESULT_OK && null != data) {

                // Get the Image from data
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                // Get the cursor
                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                // Move to first row
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                filePath = cursor.getString(columnIndex);
                cursor.close();
                ImageView imgView = (ImageView) findViewById(R.id.imageView);

                //requests permission to read a files from user's device
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_PERMISSION);

                // Set the Image in ImageView after decoding the String
                imgView.setImageBitmap(BitmapFactory.decodeFile(filePath));


            }
        } catch (Exception e) {
            Log.e("ERR", "onActivityResult: " + e.getMessage());
        }
     }
    private void setPic() {
        // Get the dimensions of the View
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
       // BitmapFactory.decodeFile(filePath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(filePath, bmOptions);
        imageView.setImageBitmap(bitmap);
    }
}