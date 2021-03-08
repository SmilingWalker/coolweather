package com.example.coolweather;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.coolweather.gson.Forecast;
import com.example.coolweather.gson.Weather;
import com.example.coolweather.service.AutoUpdateService;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Response;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class WeatherActivity extends AppCompatActivity {

    private static final String TAG = "WeatherActivity";

    private TextView title_cityName_view,title_updateTime_view;
    private TextView now_degree_view,now_info_view;
    private TextView api_index_view,pm25_index_view;
    private TextView suggestion_comfort_view,suggestion_carwash_view,suggestion_sport_view;
    private ScrollView weather_layout;
    private LinearLayout forecast_layout;

    private ProgressDialog progressDialog;

    private ImageView back_img_view;

    public SwipeRefreshLayout swipeRefreshLayout;
    private String weatherId;
    private ImageButton nav_btn;
    public DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT>=21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        setContentView(R.layout.activity_weather);
        //初始化空间
        weather_layout = findViewById(R.id.weather_layout);
        forecast_layout = findViewById(R.id.forecast_layout);
        title_cityName_view = findViewById(R.id.title_city);
        title_updateTime_view = findViewById(R.id.title_update_time);
        now_degree_view = findViewById(R.id.now_degree_text);
        now_info_view = findViewById(R.id.now_info_text);


        api_index_view = findViewById(R.id.aqi_text_view);
        pm25_index_view = findViewById(R.id.pm25_text_view);

        suggestion_carwash_view = findViewById(R.id.carwash_text_view);
        suggestion_comfort_view = findViewById(R.id.comfort_text_view);
        suggestion_sport_view = findViewById(R.id.sport_text_view);

        back_img_view = findViewById(R.id.background_img);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.design_default_color_primary_dark);

        nav_btn = findViewById(R.id.nav_btn);
        drawerLayout = findViewById(R.id.drawer_layout);

        //从缓存查询天气信息
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherStr = defaultSharedPreferences.getString("weather", null);
        String bing_img_url = defaultSharedPreferences.getString("bing_pic",null);
        if (!TextUtils.isEmpty(bing_img_url)){
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String now_date = simpleDateFormat.format(new Date());
            String buffer_date = defaultSharedPreferences.getString("bing_pic_date",null);
            if (now_date.equals(buffer_date)){
                Glide.with(this).load(bing_img_url).into(back_img_view);
            }else {
                Log.d(TAG, "Refresh Bing image from internet (has before)");
                loadBingPic();
            }

        }else {
            Log.d(TAG, "Refresh Bing image from internet  ");
            loadBingPic();
        }
        if (!TextUtils.isEmpty(weatherStr)){
            //显示信息
            Weather weather = Utility.handleWeatherResponse(weatherStr);
            weatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        }else {
            //查询信息

            Intent intent = getIntent();
            String WeatherId = intent.getStringExtra("weatherId");
            weatherId = WeatherId;
            RequestWeatherInfo(WeatherId);
        }
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                RequestWeatherInfo(weatherId);
            }
        });

        nav_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

    private void loadBingPic() {
        String request_url = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(request_url, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String bing_pic_url = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bing_pic_url);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                editor.putString("bing_pic_date",simpleDateFormat.format(new Date()));
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bing_pic_url).into(back_img_view);
                    }
                });

            }
        });
    }

    private void showWeatherInfo(Weather weather) {
        if (weather!=null){
            title_updateTime_view.setText(weather.basic.update.updateTime.split(" ")[1]);
            title_cityName_view.setText(weather.basic.cityName);
            now_info_view.setText(weather.now.more.info);
            now_degree_view.setText(weather.now.temperature+"℃");

            forecast_layout.removeAllViews();
            for(Forecast forecast:weather.forecastList){
                View view = LayoutInflater.from(this).inflate(R.layout.forecast_item,forecast_layout,false);
                TextView forecast_date_view,forecast_info_view,forecast_max_view,forecast_min_view;
                forecast_date_view = view.findViewById(R.id.forecast_date_view);
                forecast_info_view = view.findViewById(R.id.forecast_info_view);
                forecast_min_view = view.findViewById(R.id.forecast_date_min);
                forecast_max_view = view.findViewById(R.id.forecast_date_max);
                forecast_date_view.setText(forecast.date);
                forecast_info_view.setText(forecast.more.info);
                forecast_max_view.setText(forecast.temperature.max);
                forecast_min_view.setText(forecast.temperature.min);
                forecast_layout.addView(view);
            }
            if (weather.aqi!=null){
                api_index_view.setText(weather.aqi.city.aqi);
                pm25_index_view.setText(weather.aqi.city.pm25);
            }
            String comfort = "舒适度: "+weather.suggestion.comfort.info;
            String sport = " 运动建议: " + weather.suggestion.sport.info;
            String carwash = " 洗车指数: " + weather.suggestion.carWash.info;
            suggestion_sport_view.setText(sport);
            suggestion_comfort_view.setText(comfort);
            suggestion_carwash_view.setText(carwash);
            weather_layout.setVisibility(View.VISIBLE);
            Intent intent = new Intent(this, AutoUpdateService.class);
            startService(intent);
        }else {
            Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
        }

    }

    public void RequestWeatherInfo(String weatherId){
        showProgressDialog();
        String url = "http://guolin.tech/api/weather?cityid="+weatherId+"&key="+R.string.API_KEY;
        HttpUtil.sendOkHttpRequest(url, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                closeProgressDialog();
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String respText = response.body().string();
                Weather weather = Utility.handleWeatherResponse(respText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (weather!=null){
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather",respText);
                            editor.apply();
                            showWeatherInfo(weather);
                        }else {
                            Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                        }
                        closeProgressDialog();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });

    }

    private void closeProgressDialog() {
        if (progressDialog!=null){
            progressDialog.dismiss();
        }
    }

    private void showProgressDialog() {
        if (progressDialog==null){
            progressDialog = new ProgressDialog(WeatherActivity.this);
            progressDialog.setTitle("正在加载");
            progressDialog.setMessage("正在加载..........");
            progressDialog.setCancelable(false);
        }
        progressDialog.show();
    }


}