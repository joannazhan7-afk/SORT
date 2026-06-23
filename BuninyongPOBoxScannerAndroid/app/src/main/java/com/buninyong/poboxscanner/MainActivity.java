package com.buninyong.poboxscanner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Size;
import android.view.Gravity;
import android.widget.*;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private PreviewView previewView;
    private TextView resultView, ocrView, statusView;
    private final List<Record> records = new ArrayList<>();
    private TextToSpeech tts;
    private long lastAnalyse = 0;
    private String lastSpokenBox = "";
    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private final TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    static class Record {
        String box, name, address; boolean cancelled;
        Record(String b, String n, String a, boolean c){ box=b; name=n; address=a; cancelled=c; }
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        tts = new TextToSpeech(this, this);
        loadData();
        buildUi();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera();
        else ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 10);
    }

    private void buildUi(){
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(0xfff6f7f8);
        statusView = new TextView(this); statusView.setText("Loaded " + records.size() + " records. Point camera at parcel address."); statusView.setPadding(18,16,18,10); statusView.setTextSize(15);
        previewView = new PreviewView(this); previewView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
        resultView = new TextView(this); resultView.setText("Waiting for scan..."); resultView.setTextSize(26); resultView.setGravity(Gravity.CENTER); resultView.setPadding(14,18,14,8);
        ocrView = new TextView(this); ocrView.setTextSize(13); ocrView.setPadding(14,4,14,16); ocrView.setMaxLines(4);
        root.addView(statusView); root.addView(previewView); root.addView(resultView); root.addView(ocrView); setContentView(root);
    }

    private void loadData(){
        try(InputStream is=getAssets().open("pobox_data.json")){
            byte[] buf=new byte[is.available()]; is.read(buf); JSONArray arr=new JSONArray(new String(buf, StandardCharsets.UTF_8));
            for(int i=0;i<arr.length();i++){ JSONObject o=arr.getJSONObject(i); records.add(new Record(o.optString("box"), o.optString("name"), o.optString("address"), o.optBoolean("cancelled"))); }
        } catch(Exception e){ e.printStackTrace(); }
    }

    private void startCamera(){
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get(); cameraProvider.unbindAll();
                Preview preview = new Preview.Builder().build(); preview.setSurfaceProvider(previewView.getSurfaceProvider());
                ImageAnalysis analysis = new ImageAnalysis.Builder().setTargetResolution(new Size(1280,720)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
                analysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    long now=System.currentTimeMillis(); if(now-lastAnalyse<1000){ imageProxy.close(); return; } lastAnalyse=now;
                    @SuppressWarnings("UnsafeOptInUsageError") android.media.Image mediaImage=imageProxy.getImage();
                    if(mediaImage!=null){ InputImage image=InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                        recognizer.process(image).addOnSuccessListener(text -> processText(text.getText())).addOnCompleteListener(t -> imageProxy.close());
                    } else imageProxy.close();
                });
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);
                statusView.setText("Camera running. Aim at name/address on parcel.");
            } catch(Exception e){ statusView.setText("Camera error: "+e.getMessage()); }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processText(String text){
        if(text == null || text.trim().length()<4) return;
        Match m = bestMatch(text);
        runOnUiThread(() -> {
            ocrView.setText("OCR: " + text.replace('\n',' '));
            if(m.score >= 35){
                resultView.setText("PO Box " + m.record.box + "\n" + m.record.name);
                if(!m.record.box.equals(lastSpokenBox)){ lastSpokenBox=m.record.box; speakBox(m.record.box); }
            } else {
                resultView.setText("No match yet");
            }
        });
    }

    static class Match { Record record; int score; Match(Record r,int s){record=r;score=s;} }
    private Match bestMatch(String query){
        int best=0; Record bestRec=null; String nq=norm(query); String[] toks=nq.split("\\s+");
        for(Record r:records){ if(r.cancelled) continue; String hay=norm(r.name+" "+r.address+" "+r.box); int s=0, hits=0;
            for(String t:toks){ if(t.length()<2) continue; if(hay.contains(t)){ hits++; s += t.length()>=4 ? 10 : 4; } }
            if(hits>=2) s += 15; if(nq.length()>10 && hay.contains(nq)) s += 40;
            if(s>best){ best=s; bestRec=r; }
        }
        return new Match(bestRec,best);
    }
    private static String norm(String s){ return s.toUpperCase(Locale.US).replaceAll("[^A-Z0-9]+"," ").trim(); }
    private void speakBox(String box){
        if(tts==null) return; tts.stop();
        StringBuilder sb=new StringBuilder("P O Box "); for(char c:box.toCharArray()){ sb.append(c).append(' '); }
        tts.speak(sb.toString(), TextToSpeech.QUEUE_FLUSH, null, "box");
    }
    @Override public void onInit(int status){ if(status==TextToSpeech.SUCCESS){ tts.setLanguage(Locale.ENGLISH); tts.setSpeechRate(0.85f); } }
    @Override public void onRequestPermissionsResult(int requestCode,@NonNull String[] perms,@NonNull int[] grants){ super.onRequestPermissionsResult(requestCode,perms,grants); if(requestCode==10 && grants.length>0 && grants[0]==PackageManager.PERMISSION_GRANTED) startCamera(); else statusView.setText("Camera permission denied."); }
    @Override protected void onDestroy(){ super.onDestroy(); cameraExecutor.shutdown(); if(tts!=null){tts.stop(); tts.shutdown();} recognizer.close(); }
}
