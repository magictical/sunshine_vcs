/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends ActionBarActivity {

    private final  String LOG_TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "is onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new ForecastFragment())
                    .commit();
        }
    }

    @Override
    protected void onStart() {
        Log.v(LOG_TAG, "is onStart");
        super.onStart();

    }

    @Override
    protected  void onResume() {
        Log.v(LOG_TAG, "is onResume");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.v(LOG_TAG, "is onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.v(LOG_TAG, "is onStop");
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        Log.v(LOG_TAG, "is onDestroy");
        super.onDestroy();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        //메뉴 옵션의 location map을 터치했을때 실행할 메소드를 설정
        if(id ==R.id.location_map) {
            openPreferredLocationInMap();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void openPreferredLocationInMap() {
        //implicit intent로 map을 사용할텐데 아래 라인은 SharedPreference 인스턴스를 생성해서
        //getDefaultSharedPreference메소드로 설정값들에 접근할 수 있도록 만든다.
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        //이제 Preference에 접근이 가능하므로 location 인스턴스에 해당값을 담는다. (key, value 쌍으로)
        String location = sharedPrefs.getString(
                getString(R.string.pref_location_key),
                getString(R.string.pref_location_default)
        );
        //https://developer.android.com/intl/ko/guide/components/intents-common.html#Maps map관련 정보
        //implicit intent로 map을 사용하고 사용될 데이터를 담기위해서 Uri를 사용햇음
        //appendQuearyParameter의 q는 data map의 scheme이고 location은 위의 key에서 받아온 값이 담길 곳이다.
        // Uri에게 전달될 map의 data scheme형식은 "geo:0:0?q=location(초기값은 94043 구글본사위치임 ㅋㅋ)
        Uri geoLocation = Uri.parse("geo:0:0?").buildUpon()
                .appendQueryParameter("q", location)
                .build();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);

        if(intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d(LOG_TAG, "couldn't call" + location + ", no receiving apps installed!");
        }

    }
}
