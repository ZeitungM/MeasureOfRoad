package net.zeitungm.measureofroad;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.app.Activity;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.location.Location;

//多分もう必要ないけど、コンパイルエラーが面倒臭いから位置情報が取得できることを確認してから削除する
//import android.location.LocationListener;

// Google APIs for Android
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.LocationListener;


public class MainActivity extends AppCompatActivity
                          implements GoogleApiClient.ConnectionCallbacks,
                                     GoogleApiClient.OnConnectionFailedListener,
                                     com.google.android.gms.location.LocationListener
{
    // 位置情報の更新間隔(mSec)
    public static final long _UPDATE_INTERVAL_MILLISEC = 10000;
    // 位置情報の最短更新間隔(mSec)
    public static final long _FASTEST_UPDATE_INTERVAL_MILLISEC = 10000;

    // TODO: これは一体何故10なの
    private static final int _REQUEST_CHECK_SETTINGS = 10;

    private LocationManager _location_manager;
    private Location _current_location;
    private GoogleApiClient _google_api_client;
    private LocationRequest _location_request;
    private static double _current_latitude  = 0.0;
    private static double _current_longitude = 0.0;

    private static boolean _isRangingNow = false;
    private static double _start_latitude  = 0.0;
    private static double _start_longitude = 0.0;
    //private static double _end_latitude  = 0.0;
    //private static double _end_longitude = 0.0;
    private static double _total_distance = 0.0;

    // Activity 生成時(初期化処理)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SetFont();

        //位置情報取得ボタンの設定
        Button button_get_location = (Button)findViewById(R.id.button_get_location);
        // 位置情報取得ボタンが押されたときの処理
        button_get_location.setOnClickListener(new View.OnClickListener()
                                               {
                                                   @Override
                                                   public void onClick(View v)
                                                   {
                                                       //位置情報取得ボタンが押されたときのメソッドを呼び出す
                                                       GetGeographicalCoordinate();
                                                   }
                                               }
        );

        // アプリケーションがACCESS_FINE_LOCATIONとACCESS_COARSE_LOCATIONのうち、一方でもパーミッションを持っていなかったら、
        if(ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions( this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);
        }
        else
        {
            //Toast.makeText( this, "両パーミッション許可済み", Toast.LENGTH_LONG).show();
            // ここにパーミッションが許可されているときの動作を書く
            LocationStart();
        }
    }

    protected synchronized void BuildGoogleApiClient()
    {
        _google_api_client = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        // todo: 何かインクルードが必要らしい
        createLocationRequest();
    }

    // LocationRequestのセットアップ
    protected void createLocationRequest()
    {
        _location_request = new LocationRequest();
        // 位置情報の更新間隔をミリ秒で指定
        _location_request.setInterval(_UPDATE_INTERVAL_MILLISEC);
        // 位置情報の最短更新間隔をミリ秒で指定
        _location_request.setFastestInterval(_FASTEST_UPDATE_INTERVAL_MILLISEC);
        // 位置情報の精度を(最高精度 PRIORITY_HIGH_ACCURACY に)設定
        _location_request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    // 権限チェック
    public void startLocationUpdates()
    {
        // LocationSettingRequest を build する builder を作成し、 LocationRequest を追加する
        LocationSettingsRequest.Builder location_settings_request_builder = new LocationSettingsRequest.Builder().addLocationRequest(_location_request);

        // 現在位置取得の前に位置情報の設定が有効になっているか確認
        // TODO: result が何の result か調べて rename する
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings( _google_api_client, location_settings_request_builder.build());

        result.setResultCallback( new ResultCallback<LocationSettingsResult>()
        {
            @Override
            public void onResult( @NonNull LocationSettingsResult locationSettingsResult )
            {
                final Status status = locationSettingsResult.getStatus();

                switch( status.getStatusCode() )
                {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // 設定が有効になっているので現在位置を取得する
                        if( ContextCompat.checkSelfPermission( MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION )==PackageManager.PERMISSION_GRANTED )
                        {
                            // TODO: 位置情報を取得する処理を書く
                            LocationServices.FusedLocationApi.requestLocationUpdates( _google_api_client, _location_request, MainActivity.this);
                        }
                        break;

                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try
                        {
                            status.startResolutionForResult( MainActivity.this, _REQUEST_CHECK_SETTINGS );
                        }
                        catch ( IntentSender.SendIntentException e )
                        {

                        }
                        break;

                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
    }

    // Activity 開始時
    @Override
    protected void onStart()
    {
        _google_api_client.connect();
        super.onStart();
    }

    // ユーザがActivityを離れたとき
    @Override
    protected void onStop()
    {
        _google_api_client.disconnect();
        super.onStop();
    }

    // パーミッションが許可されているときの処理 サンプルコードのlocationStart()
    private void LocationStart()
    {
        // TODO: 調べる(LocationManager, getSystemService, LOCATION_SERVICE)
        // 位置情報を取得するクラスLocationManagerをインスタンス化
        _location_manager = (LocationManager)getSystemService(LOCATION_SERVICE);

        //　GPS_PROVIDER(GPSが利用可能か)を調べる
        final boolean gps_enabled = _location_manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if(!gps_enabled)    //GPSが利用不可能な場合
        {
            // GPSを設定するように促す
            Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(settingsIntent);
        }

        if( ( ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION  )!=PackageManager.PERMISSION_GRANTED) &&
            ( ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED)    )
        {
            ActivityCompat.requestPermissions( this, new String[]{ Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);
            return ;
        }

    }

    @Override
    public void onRequestPermissionsResult( int requestCode, String[] permissions, int[] grantResults)
    {
        if(requestCode==1000)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                LocationStart();
                return;
            }
            else
            {
                Toast toast = Toast.makeText(this, "これ以上なにも出来ません", Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }

    // GoogleApiClient.ConnectionCallbacks インタフェースの抽象メソッド
    // onConnected() / onConnectionSUspended()
    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        // 接続時の処理
        Toast.makeText( this, "Connected to GoogleAPIClient", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        // 接続がなくなったときの処理
        Toast.makeText( this, "Connection to GoogleAPIClient Suspended", Toast.LENGTH_LONG).show();
        _google_api_client.connect();
    }

    // GoogleApiClient.OnConnectionFailedListener() の抽象メソッド
    // onConnectionFailed()
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connection_result)
    {
        //接続失敗時の処理
        Toast.makeText( this, "Connection to GoogleAPIClient Failed", Toast.LENGTH_LONG).show();
    }

    // com.google.android.gms.location.LocationListener() の抽象メソッド
    // onLocationChanged()
    @Override
    // locationが変わった時呼ばれる
    public void onLocationChanged( android.location.Location location)
    {
        //とりあえず対症療法的に、locationが変わる度にクラス変数に格納するやつやる
        _current_latitude = location.getLatitude();
        _current_longitude = location.getLongitude();

        // テスト用テキスト表示領域になんか出力してみる
        TextView tmp_textView;
        tmp_textView = (TextView) findViewById(R.id.test_text);
        tmp_textView.setText("onLocationChanged");
    }

    // このメソッドで経緯度を取得しているようだ( HACK: getLocation() の方がよくね？)
    public void setLocation()
    {
        _current_latitude  = _current_location.getLatitude();
        _current_longitude = _current_location.getLongitude();
    }

    // 緯度・経度をフォーマットされた文字列に変換する
    // ex: 12.34      → "+ 12.34"
    public String ToFormattedString( double value)
    {
        // ex1: 123.456789 → "+123.456789"
        // ex2: 12.34      → "+ 12.34"
        // ex3: -5.6789    → "-  5.6789"
        String formatted_string = "";

        // 符号をつける( 0より大きいなら"+"、0未満なら"-"をつける
        if (value > 0 )
            formatted_string += "+";
        else if (value < 0)
            formatted_string += "-";
        else
            formatted_string += " ";

        // 整数部の桁が3桁未満のときは、半角スペースで埋めて3桁にする
        if( value < 0 )
            value *= -1;
        if( value < 100.0 && value >= 10.0)
            formatted_string += " ";
        else if ( value < 10.0)
            formatted_string += "  ";

        // 最後に、緯度・経度の絶対値を連結する
        formatted_string += value;

        return formatted_string;
    }

    //位置情報を取得する: ボタンが押されたときの処理
    public void GetGeographicalCoordinate(  )
    {
        Toast.makeText( this, "You pushed button!", Toast.LENGTH_LONG).show();

        //何故か初期化できるけどちゃんと位置情報を取得できない
        //TODO:getLastKnownLocation を後で試す( そういやNULLかどうか確認してた例があったような？ )
        Location location = new Location("");

        //緯度と経度を取得し、フォーマットを整えてTextViewに書く
        // 対症療法的にクラス変数から引っ張ってくる
        // 緯度
        TextView textView_latitude = (TextView) findViewById(R.id.value_latitude);
        textView_latitude.setText(ToFormattedString( _current_latitude ));
        // 経度
        TextView textView_longitude = (TextView) findViewById(R.id.value_longitude);
        textView_longitude.setText(ToFormattedString( _current_longitude ));

        if(_isRangingNow)
        {
            TextView tmp_textView;
            tmp_textView = (TextView) findViewById(R.id.value_ranging_finish_latitude);
            tmp_textView.setText(ToFormattedString( _current_latitude ));
            tmp_textView = (TextView) findViewById(R.id.value_ranging_finish_longitude);
            tmp_textView.setText(ToFormattedString( _current_longitude ));
            float[] distance = new float[3];
            location.distanceBetween( _start_latitude, _start_longitude, _current_latitude, _current_longitude, distance);
            tmp_textView = (TextView)findViewById(R.id.value_distance);
            tmp_textView.setText(""+distance[0]);
            //_total_distance += distance[0];

            //距離測定前の状態に戻す(  ボタンラベル、 bool) テスト用テキスト領域
            Button tmp_button = (Button)findViewById(R.id.button_get_location);
            tmp_button.setText("新たに距離の計測を開始する");
            _isRangingNow = false;
            tmp_textView = (TextView)findViewById(R.id.test_text);
            tmp_textView.setText("");
        }
        else
        {
            //現在地を開始地点として、距離の計測を開始する
            _isRangingNow = true;
            //現在地を距離計測開始地点とする
            _start_latitude  = _current_latitude;
            _start_longitude = _current_longitude;
            //距離計測開始地点をTextViewに表示する
            TextView tmp_textView;
            tmp_textView = (TextView) findViewById(R.id.value_ranging_start_latitude);
            tmp_textView.setText(ToFormattedString(_start_latitude));
            tmp_textView = (TextView) findViewById(R.id.value_ranging_start_longitude);
            tmp_textView.setText(ToFormattedString(_start_longitude));
            //位置情報取得ボタンのテキストを、「距離の計測を終了する」に変更する
            Button tmp_button = (Button)findViewById(R.id.button_get_location);
            tmp_button.setText("距離の計測を終了する");
            tmp_textView = (TextView)findViewById(R.id.test_text);
            tmp_textView.setText("");
        }
    }

    //TextViewのフォントを設定する
    public void SetFont()
    {
        //TextView のフォントの設定
        TextView tmp_textView;
        // 現在地の経緯度のラベルと表示を等幅フォントにする
        // 緯度
        tmp_textView = (TextView) findViewById(R.id.label_latitude);
        tmp_textView.setTypeface(Typeface.MONOSPACE);
        tmp_textView = (TextView) findViewById(R.id.value_latitude);
        tmp_textView.setTypeface(Typeface.MONOSPACE);
        // 経度
        tmp_textView = (TextView) findViewById(R.id.label_longitude);
        tmp_textView.setTypeface(Typeface.MONOSPACE);
        tmp_textView = (TextView) findViewById(R.id.value_longitude);
        tmp_textView.setTypeface(Typeface.MONOSPACE);

        // 測距開始地点のラベルと表示を等幅フォントにする
        // 緯度
        tmp_textView = (TextView) findViewById(R.id.label_ranging_start_latitude);
        tmp_textView.setTypeface(Typeface.MONOSPACE);
        tmp_textView = (TextView) findViewById(R.id.value_ranging_start_latitude);
        tmp_textView.setTypeface(Typeface.MONOSPACE);
        // 経度
        tmp_textView = (TextView) findViewById(R.id.label_ranging_start_longitude);
        tmp_textView.setTypeface(Typeface.MONOSPACE);
        tmp_textView = (TextView) findViewById(R.id.value_ranging_start_longitude);
        tmp_textView.setTypeface(Typeface.MONOSPACE);

        // 測距終了地点のラベルと表示を等幅フォントにする
        // 緯度
        tmp_textView = (TextView) findViewById(R.id.label_ranging_finish_latitude);
        tmp_textView.setTypeface(Typeface.MONOSPACE);
        tmp_textView = (TextView) findViewById(R.id.value_ranging_finish_latitude);
        tmp_textView.setTypeface(Typeface.MONOSPACE);
        // 経度
        tmp_textView = (TextView) findViewById(R.id.label_ranging_finish_longitude);
        tmp_textView.setTypeface(Typeface.MONOSPACE);
        tmp_textView = (TextView) findViewById(R.id.value_ranging_finish_longitude);
        tmp_textView.setTypeface(Typeface.MONOSPACE);

        // テスト用のTextViewもとりあえず等幅フォントにしておく
        tmp_textView = (TextView) findViewById(R.id.test_text);
        tmp_textView.setTypeface(Typeface.MONOSPACE);
    }
}

//-----------------------
