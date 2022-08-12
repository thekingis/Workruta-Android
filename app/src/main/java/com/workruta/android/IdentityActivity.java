package com.workruta.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.workruta.android.Utils.Constants;
import com.workruta.android.Utils.Functions;
import com.workruta.android.Utils.SharedPrefMngr;
import com.workruta.android.Utils.StringUtils;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.workruta.android.PaymentsActivity.listenForUpdates;
import static com.workruta.android.Utils.Constants.validIDs;
import static com.workruta.android.Utils.Constants.www;
import static com.workruta.android.Utils.Util.convertStringToArrayInt;

public class IdentityActivity extends SharedCompatActivity implements AdapterView.OnItemSelectedListener {

    RelativeLayout mainView;
    LinearLayout whiteFade, blackFade;
    TextView headText, next;
    EditText idNumber, passwordET;
    @SuppressLint("StaticFieldLeak")
    static ImageView imgView;
    static String filePath;
    Spinner monthIs, dayIs, yearIs, monthEx, dayEx, yearEx, idType;
    int user, minYr, maxYr, curYr, mnthIsIndex, dayIsIndex, yearIsIndex, mnthExIndex, dayExIndex, yearExIndex, idIndex;
    boolean requesting;
    SharedPrefMngr sharedPrefMngr;
    List<String> days, yearsIs, yearsEx, types;
    String[] months;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identity);
        sharedPrefMngr = new SharedPrefMngr(this);
        user = sharedPrefMngr.getMyId();
        requesting = false;

        mainView = findViewById(R.id.mainView);
        blackFade = findViewById(R.id.blackFade);
        whiteFade = findViewById(R.id.whiteFade);
        next = findViewById(R.id.next);
        headText = findViewById(R.id.headText);
        imgView = findViewById(R.id.imgView);
        passwordET = findViewById(R.id.passwordET);
        idNumber = findViewById(R.id.idNumber);
        monthIs = findViewById(R.id.monthIs);
        dayIs = findViewById(R.id.dayIs);
        yearIs = findViewById(R.id.yearIs);
        monthEx = findViewById(R.id.monthEx);
        dayEx = findViewById(R.id.dayEx);
        yearEx = findViewById(R.id.yearEx);
        idType = findViewById(R.id.idType);

        months = new String[]{"Month", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        days = new ArrayList<>();
        yearsIs = new ArrayList<>();
        yearsEx = new ArrayList<>();
        types = Arrays.asList(validIDs);
        String dy = "Day", yr = "Year";
        yearsIs.add(yr);
        yearsEx.add(yr);
        days.add(dy);
        Date date = new Date();
        Calendar calndr = new GregorianCalendar();
        calndr.setTime(date);
        curYr = calndr.get(Calendar.YEAR);
        minYr = curYr - 5;
        maxYr = curYr + 5;
        for(int i = 1; i < 32; i++){
            String day = Integer.toString(i);
            days.add(day);
        }
        for(int i = curYr; i >= minYr; i--){
            String year = Integer.toString(i);
            yearsIs.add(year);
        }
        for(int i = maxYr; i >= curYr; i--){
            String year = Integer.toString(i);
            yearsEx.add(year);
        }

        ArrayAdapter<String> adapterM = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, months);
        monthIs.setAdapter(adapterM);
        monthEx.setAdapter(adapterM);
        monthIs.setOnItemSelectedListener(this);
        monthEx.setOnItemSelectedListener(this);
        ArrayAdapter<String> adapterD = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, days);
        dayIs.setAdapter(adapterD);
        dayEx.setAdapter(adapterD);
        dayIs.setOnItemSelectedListener(this);
        dayEx.setOnItemSelectedListener(this);
        ArrayAdapter<String> adapterYIs = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, yearsIs);
        yearIs.setAdapter(adapterYIs);
        yearIs.setOnItemSelectedListener(this);
        ArrayAdapter<String> adapterEx = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, yearsEx);
        yearEx.setAdapter(adapterEx);
        yearEx.setOnItemSelectedListener(this);
        ArrayAdapter<String> adapterType = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        idType.setAdapter(adapterType);
        idType.setOnItemSelectedListener(this);

        imgView.setOnClickListener((v) -> {
            Intent intent = new Intent(context, CameraAct.class);
            Bundle bundle = new Bundle();
            bundle.putInt("activity", 2);
            intent.putExtras(bundle);
            startActivity(intent);
        });
        next.setOnClickListener((v) -> submitForm());
        headText.setOnClickListener((v) -> onBackPressed());
        whiteFade.setOnClickListener((v) -> {
            return;
        });

        setupUI(mainView);
        getDetails();
    }

    @SuppressLint("InflateParams")
    private void getDetails() {
        RelativeLayout layout = (RelativeLayout) getLayoutInflater().inflate(R.layout.slide_loader, null);
        whiteFade.addView(layout);
        new android.os.Handler().postDelayed(() -> {
            RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("user", String.valueOf(getMyId()))
                    .addFormDataPart("cat", "identity")
                    .build();
            Request request = new Request.Builder()
                    .url(Constants.editorUrl)
                    .post(requestBody)
                    .build();

            OkHttpClient okHttpClient = new OkHttpClient();
            Call call = okHttpClient.newCall(request);
            try(Response response = call.execute()) {
                if(response.isSuccessful()) {
                    String responseString = Objects.requireNonNull(response.body()).string();
                    JSONObject object = new JSONObject(responseString);
                    boolean available = object.getBoolean("available");
                    if(available) {
                        String number = object.getString("number");
                        String type = object.getString("type");
                        String isDate = object.getString("isDate");
                        String exDate = object.getString("exDate");
                        filePath = object.getString("filePath");
                        List<String> list = Arrays.asList(validIDs);
                        idIndex = list.indexOf(type);
                        int[] isInts = convertStringToArrayInt(isDate, "-");
                        int[] exInts = convertStringToArrayInt(exDate, "-");
                        int yrIs = isInts[0];
                        yearIsIndex = yearsIs.indexOf(String.valueOf(yrIs));
                        mnthIsIndex = isInts[1];
                        dayIsIndex = isInts[2];
                        int yrEx = exInts[0];
                        yearExIndex = yearsEx.indexOf(String.valueOf(yrEx));
                        mnthExIndex = exInts[1];
                        dayExIndex = exInts[2];
                        idNumber.setText(number);
                        idType.setSelection(idIndex);
                        monthIs.setSelection(mnthIsIndex);
                        dayIs.setSelection(dayIsIndex);
                        yearIs.setSelection(yearIsIndex);
                        monthEx.setSelection(mnthExIndex);
                        dayEx.setSelection(dayExIndex);
                        yearEx.setSelection(yearExIndex);
                        imageLoader.displayImage(www + filePath, imgView);
                    }
                    whiteFade.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 2000);
    }

    @SuppressLint("InflateParams")
    private void submitForm() {
        String number = idNumber.getText().toString(),
                password = passwordET.getText().toString();
        if(StringUtils.isEmpty(number) || StringUtils.isEmpty(password) || mnthIsIndex == 0 || dayIsIndex == 0
                || yearIsIndex == 0 || idIndex == 0 || mnthExIndex == 0 || dayExIndex == 0 || yearExIndex == 0){
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_LONG).show();
            return;
        }
        if(filePath == null){
            Toast.makeText(this, "Please add a picture of your driver's licence", Toast.LENGTH_LONG).show();
            return;
        }
        requesting = true;
        String type = validIDs[idIndex];
        File file = new File(filePath);
        whiteFade.setVisibility(View.VISIBLE);
        if(whiteFade.getChildCount() > 0)
            whiteFade.removeAllViews();
        RelativeLayout layout = (RelativeLayout) getLayoutInflater().inflate(R.layout.slide_loader, null);
        whiteFade.addView(layout);
        new android.os.Handler().postDelayed(() -> {
            MultipartBody.Builder multipartBody = new MultipartBody.Builder().setType(MultipartBody.FORM);
            if(file.exists()) {
                Uri uris = Uri.fromFile(file);
                String fileExt = MimeTypeMap.getFileExtensionFromUrl(uris.toString());
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExt.toLowerCase());
                multipartBody.addFormDataPart("file", file.getName(), RequestBody.create(file, MediaType.parse(mimeType)));
            } else
                multipartBody.addFormDataPart("filePath", filePath);
            multipartBody.addFormDataPart("user", String.valueOf(user))
                    .addFormDataPart("type", type)
                    .addFormDataPart("number", number)
                    .addFormDataPart("password", password)
                    .addFormDataPart("mnthIsIndex", String.valueOf(mnthIsIndex))
                    .addFormDataPart("dayIsIndex", String.valueOf(dayIsIndex))
                    .addFormDataPart("yearIsIndex", String.valueOf(yearIsIndex))
                    .addFormDataPart("mnthExIndex", String.valueOf(mnthExIndex))
                    .addFormDataPart("dayExIndex", String.valueOf(dayExIndex))
                    .addFormDataPart("yearExIndex", String.valueOf(yearExIndex));
            RequestBody requestBody = multipartBody.build();
            Request request = new Request.Builder()
                    .url(Constants.submitIDUrl)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .post(requestBody)
                    .build();

            OkHttpClient okHttpClient = new OkHttpClient();
            Call call = okHttpClient.newCall(request);
            try(Response response = call.execute()) {
                requesting = false;
                whiteFade.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    String responseString = Objects.requireNonNull(response.body()).string();
                    JSONObject object = new JSONObject(responseString);
                    boolean noError = object.getBoolean("noError");
                    String dataStr = object.getString("dataStr");
                    if(noError) {
                        if(file.exists()){
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                try {
                                    Files.delete(file.toPath());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            else
                                file.delete();
                        }
                        JSONObject jsonObject = new JSONObject(dataStr);
                        filePath = jsonObject.getString("filePath");
                        Toast.makeText(context, "Data Saved", Toast.LENGTH_LONG).show();
                        listenForUpdates(context, "idCard", "pending");
                    } else
                        Toast.makeText(this, dataStr, Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 200);
    }

    public static void displayImage(String fPath){
        filePath = fPath;
        Bitmap bitmap = Functions.decodeFiles(fPath, false);
        imgView.setImageBitmap(bitmap);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(parent == idType)
            idIndex = position;
        else if(parent == monthIs)
            mnthIsIndex = position;
        else if(parent == dayIs)
            dayIsIndex = position;
        else if(parent == yearIs && position > 0)
            yearIsIndex = Integer.parseInt((String) parent.getItemAtPosition(position));
        else if(parent == monthEx)
            mnthExIndex = position;
        else if(parent == dayEx)
            dayExIndex = position;
        else if(parent == yearEx && position > 0)
            yearExIndex = Integer.parseInt((String) parent.getItemAtPosition(position));
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    public void onBackPressed(){
        if(!requesting) {
            if(blackFade.getVisibility() == View.VISIBLE){
                blackFade.setVisibility(View.GONE);
                return;
            }
            finish();
        }
    }

    public void hideSoftKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setupUI(View view) {
        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener((v, event) -> {
                hideSoftKeyboard(v);
                idNumber.clearFocus();
                passwordET.clearFocus();
                return false;
            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
    }

}