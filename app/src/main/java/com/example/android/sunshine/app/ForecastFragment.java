package com.example.android.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/**
 * 이전 버전이랑 달리 현재 android studio는 Fragment.java파일이 분리되어있음 xml도 마찬가지
 */

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {
    ArrayAdapter<String> mForecastAdapter;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 현재 fragment가 메인메뉴에 추가할 item이 있다는것을 알림
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if(id == R.id.action_refresh) {
            FetchWeatherTask weatherTask = new FetchWeatherTask();
            //xml폴더의 pref_general에 저장된 key와 defaultvalue를 가져와서 location 인스턴스에 저장하고 실행함
            //항상 한쌍으로 사용되기 떄문에 String을 받아 올때도 key와 value를 쌍으로 받아옴
            //getDefaultSharedPreferences는 context유형의 인자를 사용하기때문에 getActivity로 context를 받아옴
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String location = prefs.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));

            //아래 코딩은 위의 코딩과 결과는 동일 하지만 location 인스턴스에
            /*String location = PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));*/


            //JSON에 특정주소에서 날씨를 받아오도록 변경하는 라인
            weatherTask.execute(location);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //ListView를 위한 페이크데이터를 추가함
        String[] data = {""};
        List<String> weekForecast = new ArrayList<String>(Arrays.asList(data));

        //ArrayAdapter가 데이터를 가져와서 실제로 ListView가(?) 원하는 형태로 뿌려준다.
        mForecastAdapter = new ArrayAdapter<String>(
                getActivity(), //activity의 context
                R.layout.list_item_forecast, //레이아웃
                R.id.list_item_forecast_textview, //id 요소(textview속성의 요소)
                weekForecast);
        //가장 기본이되는 레이아웃을 인플레이트함(rootView) 그리고 그값을 OnCreateView에 반드시 return 해야함
        View rootView = inflater.inflate(R.layout.fragment_main,container,false);

        //listview 참조 가져오고 아답터에 붙여넣음
        //ListView는 View의 자식클래스인데 케스팅할때 무리가 없는지 궁금하다.//2.6
        ListView listview = (ListView) rootView.findViewById(R.id.listview_forecast);
        listview.setAdapter(mForecastAdapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            //parnet : 클릭된 AdapterView, view : AdapterView내에서 선택된 view AdapterView에서 뿌려준것의 일부
            //position : adapter내에서 view의 위치, id : 클릭된 id
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String forecast = mForecastAdapter.getItem(position);

                //intent인스턴스 -   getActivity : 현재 activity의  context
                //Intent(현재 intent를 실행할 App package의 context 지금의 sunshine의 context, 인텐트로 사용될 부품클래스)
                Intent intent = new Intent(getActivity(), DetailActivity.class)
                        //인텐트를 보낼때 텍스트를 보내기때문에 EXTRA_TEXT상수를 사용해서 string type의 value forecast를 보낸다
                        .putExtra(Intent.EXTRA_TEXT, forecast);
                startActivity(intent);

                //토스트 예제 요렇게하면 listview에서 선택한 view가 토스트에 표시됨
                //Toast.makeText(getActivity(), forecast, Toast.LENGTH_SHORT).show();

            }
        });



        return rootView;
    }

    private void updateWeather() {
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        String location = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));


        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        //String location = prefs.getString(getString(R.string.pref_location_key),getString(R.string.pref_location_default));
        weatherTask.execute(location);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }



    //AsyncTask의 인자는  AsyncTask<Params, Progress, Result>으로 Prams는 doInBackground에
    //Progress는 onProgressUpdate에 Result는 onPostExecute에 사용된다.
    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        //모니터링할때 Tag에 FetchWeatherTask를 구분해주는 라인
        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();


        /* The date/time conversion code is going to be moved outside the asynctask later,
        * so for convenience we're breaking it out into its own method now.
        */
        private String getReadableDateString(long time){
            //시간셋팅
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("yyyy EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /**
         * Prepare the weather high/lows for presentation.
         * 온도 셋팅
         */
        private String formatHighLows(double high, double low, String unitType) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            if (unitType.equals(getString(R.string.pref_units_imperial))) {
                high = (high * 1.8) + 32;
                low = (low * 1.8) + 32;
            } else if (!unitType.equals(getString(R.string.pref_units_metric))) {
                Log.d(LOG_TAG, "Unit type not found: " + unitType);
            }
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }


        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            String[] resultStrs = new String[numDays];

            // Data is fetched in Celsius by default.
            // If user prefers to see in Fahrenheit, convert the values here.
            // We do this rather than fetching in Fahrenheit so that the user can
            // change this option without us having to re-fetch the data once
            // we start storing the values in a database.
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String units = sharedPref.getString(
                    getString(R.string.pref_units_key),getString(R.string.pref_units_metric)
            );

            for(int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime;
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                //JSON 포멧형식 내부에 다수의 Array로 object를 구분했기 때문에 getJSONArray
                //또는 getJOSNObject로 구분해서 해당 데이터를 불러옴
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                //위에 상수로 OWM_DESCRIPTION은 JSON Object의 main으로 지정했기 때문에 main값인 Clear, rainy등의 값을 불러옴
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low, units);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            /*for (String s : resultStrs) {
                Log.v(LOG_TAG, "Forecast entry: " + s);
            }*/


            return resultStrs;

        }

        @Override
        protected String[] doInBackground(String... params) {

            //인자를 확인함
            if(params.length == 0) {
                return null;
            }

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            String format = "json";
            String units = "metric";
            int numDays = 7;

            try {

                //api url이랑 api key 변수를 따로 만들어서 서버에 접속 -> api key확인 후 원하는 데이터를
                //받을 수 있도록 변경됨 추가로 build.gradle의 buildTypes.each를 추가해주고 api key값을 설정해줘야함
                /* 아래는 gradle 파일의 실제코드
                /buildTypes.each {
                /    it.buildConfigField 'String', 'OPEN_WEATHER_MAP_API_KEY', "\"f7fe6d79bfc2c54a90d229f0ddf393ee\""*/

                /* 기존의 api인증 코드 Uri.Builder를 사용하면서 더이상 사용하지 않음
                String baseUrl = "http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7";
                String apiKey = "&APPID=" + BuildConfig.OPEN_WEATHER_MAP_API_KEY;
                URL url = new URL(baseUrl.concat(apiKey));*/

                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are available at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast

                final String FORECAST_BASE_URL =
                        "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";
                final String APPID_PRAM = "APPID";

                //buildUpon()은 Uri.Build를 추상화한 메소드기 때문에 Uri.Build의 메소드인 appendQueryParameter가 사용가능한듯?
                //Uir를 통해 api사이트에 원하는 쿼리를 요청함
                //이때 위의 상수와 상수의 값을 정의한 변수를 쿼리(?)에 붙여서 요청하게됨
                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                        .appendQueryParameter(APPID_PRAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                        .build();

                URL url = new URL(builtUri.toString());

                //logcat에서 uri를 확인하기위한 라인
                Log.v(LOG_TAG, "Built URI " + builtUri.toString());

                // Create the request to OpenWeatherMap, and open the connection
                //실제로 접속요청을 보냄
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    forecastJsonStr = null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                //요청을 보낸 주소의 값을 읽어옴
                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    forecastJsonStr = null;
                }
                forecastJsonStr = buffer.toString();

                //인터넷에서 api JSON을 받을때 제대로 받았는지 TAG로 확인
                //Log.v이기 때문에 해당 TAG는 Text메뉴에 Forecast ~~ + JSON에서
                // 받아온 데이터로표시됨
                Log.v(LOG_TAG, "Forecast JSON String: " + forecastJsonStr);

            } catch (IOException e) {
                Log.e("PlaceholderFragment", "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                return  null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("PlaceholderFragment", "Error closing stream", e);
                    }
                }
            }

            try {
                //gwtWeatherDataFromJson 메소드를 실행해서 return값으로 resultStrs 배열을 받고
                //이를 onPostExcute의 인수로 전달해서 mForecastAdapter를 업데이트시킴
                return getWeatherDataFromJson(forecastJsonStr, numDays);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            //에러가 나지 않는 이상 try문에서 return까지 해버리므로 아래 라인은 실행되지 않는다
            //this will only happen if there was an error getting or parsing the forecast.
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if(result != null) {
                mForecastAdapter.clear();
                for (String dayForecastStr : result) {
                    mForecastAdapter.add(dayForecastStr);
                }
            }
        }
    }
}
