package com.example.coolweather.util;

import android.text.TextUtils;

import com.example.coolweather.beans.City;
import com.example.coolweather.beans.County;
import com.example.coolweather.beans.Province;
import com.example.coolweather.gson.Weather;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Response;

public class Utility {

    public static boolean handleProvinceResponse(String response){
        if (!TextUtils.isEmpty(response)){
            try {
                JSONArray provinces = new JSONArray(response);
                for (int i = 0; i < provinces.length() ; i++) {
                    JSONObject jsonObject = (JSONObject) provinces.get(i);
                    Province province = new Province();
                    province.setProvinceCode(jsonObject.getInt("id"));
                    province.setProvinceName(jsonObject.getString("name"));
                    province.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean handleCityResponse(String response,int provinceId){
        if (!TextUtils.isEmpty(response)){
            try {
                JSONArray cities = new JSONArray(response);
                for (int i = 0; i < cities.length() ; i++) {
                    JSONObject jsonObject = (JSONObject) cities.get(i);
                    City city  = new City();
                    city.setCityCode(jsonObject.getInt("id"));
                    city.setCityName(jsonObject.getString("name"));
                    city.setProvinceId(provinceId);
                    city.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean handleCountyResponse(String response,int cityId){
        if (!TextUtils.isEmpty(response)){
            try {
                JSONArray counties = new JSONArray(response);
                for (int i = 0; i < counties.length() ; i++) {
                    JSONObject jsonObject = (JSONObject) counties.get(i);
                    County county = new County();
                    county.setWeatherId(jsonObject.getString("weather_id"));
                    county.setCountyName(jsonObject.getString("name"));
                    county.setCityId(cityId);
                    county.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 处理天气信息的查询结果，
     * @param response 网络请求返回的请求体
     * @return 封装成功的一个weather对象
     */
    public static Weather handleWeatherResponse(String response){
        if (!TextUtils.isEmpty(response)){
            try {
                JSONObject jsonObject = new JSONObject(response);
                JSONArray jsonArray = (JSONArray) jsonObject.get("HeWeather");
                String status = (String) jsonArray.getJSONObject(0).get("status");
                if ("ok".equalsIgnoreCase(status)){
                    String weather = jsonArray.getJSONObject(0).toString();
                    return new Gson().fromJson(weather,Weather.class);
                }else {
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();

            }
        }
        return null;
    }
}
