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
    private final String urlClassifieurSoda  = "http://www-rech.telecom-lille.fr/nonfreesift/classifiers/";
    private final String urlClassifieurVoiture = "http://www-rech.telecom-lille.fr/freeorb/classifiers/";

    //contructeur de la classe

    public Brand(String brandName, String url, String classifieur, String[] images, RequestQueue queue, Context context, int choix) {
        //Setting value fron JSON file

        this.brandName = brandName;
        this.url = choix == 1 ? urlClassifieurSoda : urlClassifieurVoiture ;
        this.classifieurName = classifieur;
        this.images = images;

        //settings from main activity
        this.queue = queue;
        this.context = context;

        //getting value from request
        getClassifieurFromServer(classifieur, choix);
    }

    //méthode utilisée pour récupérer l'ensemble des classifieur contenu sur le serveur de TL
    public void getClassifieurFromServer(String classifieur, int choix){
        StringRequest stringRequest = new StringRequest(Request.Method.GET, this.url + classifieurName,
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

    //utilisé pour enrigistré une string dans le cache 'contenu d'un fichier texte) afin de pouvoirs'en reservir dans d'autre classe
    // les données étant en cache et accessible via le nom donné
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

    //getter et setter

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

