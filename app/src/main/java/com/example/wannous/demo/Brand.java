package com.example.wannous.demo;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by YTABEL12 on 20/02/2017.
 */

public class Brand {
    private Context context;
    private String brandName;
    private String url;
    private String classifieurName;
    private String classifieur;
    private String[] images;
    private RequestQueue queue;
    private final String urlClassifieur = "http://www-rech.telecom-lille.fr/nonfreesift/classifiers/";

    public Brand(String brandName, String url, String classifieur, String[] images, RequestQueue queue, Context context) {
        //Setting value fron JSON file
        this.brandName = brandName;
        this.url = url;
        this.classifieurName = classifieur;
        this.images = images;

        //settings from main activity
        this.queue = queue;
        this.context = context;

        //getting value from request
        getClassifieurFromServer(classifieur);
    }

    public void getClassifieurFromServer(String classifieur){
        StringRequest stringRequest = new StringRequest(Request.Method.GET, urlClassifieur + classifieurName,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.i("CLASSIFIEUR", classifieurName + " DONE");
                    saveResponse(response);
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("CLASSIFIEUR", classifieurName + "FAIL");
                }
            }
        );
        queue.add(stringRequest);
    }
    public void saveResponse(String response){
        stringToCache(response, classifieurName);
    }

    public File stringToCache(String string, String fileName) {
        File file = new File(context.getCacheDir(), fileName);
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

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getClassifieur() {
        return classifieurName;
    }

    public void setClassifieur(String classifieur) {
        this.classifieurName = classifieur;
    }

    public String[] getImages() {
        return images;
    }

    public void setImages(String[] images) {
        this.images = images;
    }

    public RequestQueue getQueue() {
        return queue;
    }

    public void setQueue(RequestQueue queue) {
        this.queue = queue;
    }
}

