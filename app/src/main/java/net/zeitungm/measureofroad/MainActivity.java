package net.zeitungm.measureofroad;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.app.Activity;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.location.Location;

public class MainActivity extends Activity implements LocationListener
{
    private LocationManager _location_manager;
    private static double _latitude  = 0.0;
    private static double _longitude = 0.0;

    private static boolean _isRangingNow = false;
    private static double _start_latitude  = 0.0;
    private static double _start_longitude = 0.0;
    private static double _end_latitude  = 0.0;
    private static double _end_longitude = 0.0;

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

        _location_manager.requestLocationUpdates( LocationManager.GPS_PROVIDER /*String*/, 1000/* long */, 50 /* float */, this /*LocationListener */);
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

    @Override
    public void onStatusChanged( String provide, int status, Bundle extras)
    {
        switch(status)
        {
            case LocationProvider.AVAILABLE:
                break;
            case LocationProvider.OUT_OF_SERVICE:
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                break;
        }
    }

    @Override
    // locationが変わった時呼ばれる
    public void onLocationChanged( android.location.Location location)
    {
        // TODO:あとでgetLastKnownLocationするやつも試してみる
        //とりあえず対症療法的に、locationが変わる度にクラス変数に格納するやつやる
        _latitude  = location.getLatitude();
        _longitude = location.getLongitude();

        TextView tmp_textView;
        tmp_textView = (TextView) findViewById(R.id.test_text);
        tmp_textView.setText("onLocationChanged");

        //緯度と経度を取得し、フォーマットを整えてTextViewに書く
        // 緯度
        tmp_textView = (TextView) findViewById(R.id.value_latitude);
        tmp_textView.setText(ToFormattedString(location.getLatitude()));
        // 経度
        tmp_textView = (TextView) findViewById(R.id.value_longitude);
        tmp_textView.setText(ToFormattedString(location.getLongitude()));

    }

    @Override
    public void onProviderEnabled( String provider)
    {

    }

    @Override
    public void onProviderDisabled( String provider)
    {

    }

    // 緯度・経度をフォーマットされた文字列に変換する
    // ex1: 123.456789 → "+123.456789"
    // ex2: 12.34      → "+ 12.34"
    // ex3: -5.6789    → "-  5.6789"
    public String ToFormattedString( double value)
    {
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

    //位置情報を取得する
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
        textView_latitude.setText(ToFormattedString(_latitude));
        // 経度
        TextView textView_longitude = (TextView) findViewById(R.id.value_longitude);
        textView_longitude.setText(ToFormattedString(_longitude));

        if(_isRangingNow)
        {
            _end_latitude  = _latitude;
            _end_longitude = _longitude;
            TextView tmp_textView;
            tmp_textView = (TextView) findViewById(R.id.value_ranging_finish_latitude);
            tmp_textView.setText(ToFormattedString(_end_latitude));
            tmp_textView = (TextView) findViewById(R.id.value_ranging_finish_longitude);
            tmp_textView.setText(ToFormattedString(_end_longitude));
            float[] distance = new float[3];
            location.distanceBetween( _start_latitude, _start_longitude, _end_latitude, _end_longitude, distance);
            tmp_textView = (TextView)findViewById(R.id.value_distance);
            tmp_textView.setText(""+distance[0]);
            //位置情報取得ボタンのテキストを元に戻す
            Button tmp_button = (Button)findViewById(R.id.button_get_location);
            tmp_button.setText("位置情報を取得する");
            _isRangingNow = false;
            tmp_textView = (TextView)findViewById(R.id.test_text);
            tmp_textView.setText("");
        }
        else
        {
            //現在地を開始地点として、距離の計測を開始する
            _isRangingNow = true;
            //現在地を距離計測開始地点とする
            _start_latitude  = _latitude;
            _start_longitude = _longitude;
            //距離計測開始地点をTextViewに表示する
            TextView tmp_textView;
            tmp_textView = (TextView) findViewById(R.id.value_ranging_start_latitude);
            tmp_textView.setText(ToFormattedString(_start_latitude));
            tmp_textView = (TextView) findViewById(R.id.value_ranging_start_longitude);
            tmp_textView.setText(ToFormattedString(_start_longitude));
            //位置情報取得ボタンのテキストを、「距離を計算する」に変更する
            Button tmp_button = (Button)findViewById(R.id.button_get_location);
            tmp_button.setText("距離を計算する");
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
