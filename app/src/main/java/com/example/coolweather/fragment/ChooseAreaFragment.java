package com.example.coolweather.fragment;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.coolweather.MainActivity;
import com.example.coolweather.R;
import com.example.coolweather.WeatherActivity;
import com.example.coolweather.beans.City;
import com.example.coolweather.beans.County;
import com.example.coolweather.beans.Province;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import org.jetbrains.annotations.NotNull;
import org.litepal.LitePal;
import org.litepal.crud.LitePalSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {

    private static final String TAG = "ChooseAreaFragment";

    private static final int LEVEL_PROVINCE = 1,LEVEL_CITY = 2,LEVEL_COUNTY = 3;

    private ProgressDialog progressDialog;

    private Province selectedProvince;
    private City selectedCity;
    private County selectedCounty;

    private TextView title_view;
    private ImageButton back_btn;
    private ListView listview;
    private ArrayList<String > dataList = new ArrayList<>();
    private ArrayAdapter<String> arrayAdapter;

    /**
     * 省列表
     */
    private List<Province> provinceList;

    /**
     * 市列表
     */
    private List<City> cityList;

    /**
     * 县列表
     */
    private List<County> countyList;

    private int currentLevel;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container);
        back_btn = view.findViewById(R.id.back_button);
        title_view = view.findViewById(R.id.title_text);
        listview = view.findViewById(R.id.list_view);
        arrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1,dataList);
        listview.setAdapter(arrayAdapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel==LEVEL_PROVINCE){
                    selectedProvince = provinceList.get(position);
                    QueryCities();
                }else if(currentLevel==LEVEL_CITY){
                    selectedCity = cityList.get(position);
                    QueryCounties();
                }else if(currentLevel==LEVEL_COUNTY){
                    selectedCounty = countyList.get(position);
                    String weatherId = selectedCounty.getWeatherId();
                    if (getActivity() instanceof MainActivity){
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weatherId",weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }else if(getActivity() instanceof WeatherActivity){
                        WeatherActivity weatherActivity = (WeatherActivity) getActivity();
                        weatherActivity.drawerLayout.closeDrawers();
                        weatherActivity.swipeRefreshLayout.setRefreshing(true);
                        weatherActivity.RequestWeatherInfo(weatherId);
                    }


                }
            }
        });
        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel==LEVEL_COUNTY){
                    QueryCities();
                }else if(currentLevel==LEVEL_CITY){
                    QueryProvinces();
                }
            }
        });
        QueryProvinces();
    }

    /**
     * 查询所有的省份信息，首先从数据库内查询，如果没有则从网络端查询
     */
    private void QueryProvinces() {
        title_view.setText("中国");
        back_btn.setVisibility(View.GONE);
        provinceList = LitePal.findAll(Province.class);

        if (provinceList.size()>0){
            dataList.clear();
            for(Province province:provinceList){
                dataList.add(province.getProvinceName());
            }
            arrayAdapter.notifyDataSetChanged();
            listview.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        }else {
            String address = "http://guolin.tech/api/china";
            QueryFromServer(address,"province");
        }
    }

    /**
     * 查询所有的 城市 信息，首先从数据库内查询，如果没有则从网络端查询
     */
    private void QueryCities() {
        title_view.setText(selectedProvince.getProvinceName());
        back_btn.setVisibility(View.VISIBLE);
        cityList = LitePal
                .where("provinceid=?", String.valueOf(selectedProvince.getId()))
                .find(City.class);

        if (cityList.size()>0){
            dataList.clear();
            for(City city:cityList){
                dataList.add(city.getCityName());
            }
            arrayAdapter.notifyDataSetChanged();
            listview.setSelection(0);
            currentLevel = LEVEL_CITY;
        }else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            QueryFromServer(address,"city");
        }
    }

    /**
     * 查询所有的 区县信息，首先从数据库内查询，如果没有则从网络端查询
     */
    private void QueryCounties() {
        title_view.setText(selectedCity.getCityName());
        back_btn.setVisibility(View.VISIBLE);
        countyList = LitePal
                .where("cityid=?", String.valueOf(selectedCity.getId()))
                .find(County.class);

        if (countyList.size()>0){
            dataList.clear();
            for(County county:countyList){
                dataList.add(county.getCountyName());
            }
            arrayAdapter.notifyDataSetChanged();
            listview.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        }else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode+"/"+cityCode;
            QueryFromServer(address,"county");
        }
    }

    /**
     * 从服务器上查询相关的信息
     * @param address 服务器地址
     * @param type 查询类型
     */
    private void QueryFromServer(String address,final String type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String respText = response.body().string();
                Log.d(TAG, "onResponse: "+respText);
                boolean result = false;
                if ("province".equalsIgnoreCase(type)){
                    result = Utility.handleProvinceResponse(respText);
                }else if("city".equalsIgnoreCase(type)){
                    result = Utility.handleCityResponse(respText,selectedProvince.getId());
                }else if ("county".equalsIgnoreCase(type)){
                    result = Utility.handleCountyResponse(respText,selectedCity.getId());
                }
                if (result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equalsIgnoreCase(type)){
                                QueryProvinces();
                            }else if("city".equalsIgnoreCase(type)){
                                QueryCities();
                            }else if ("county".equalsIgnoreCase(type)){
                                QueryCounties();
                            }
                        }
                    });
                }
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
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setTitle("正在加载");
            progressDialog.setMessage("正在加载..........");
            progressDialog.setCancelable(false);
        }
        progressDialog.show();
    }
}
