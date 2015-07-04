package com.example.ryozo;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import org.achartengine.GraphicalView;

import com.example.graph.AverageCubicTemperatureChart;

import android.app.Activity;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;


public class RentaroMainActivity extends Activity  implements View.OnClickListener{

    AverageCubicTemperatureChart chart;
    GraphicalView view;
    Activity activity;
    LinearLayout graph;

    MyTimerTask timerTask = new MyTimerTask();  //onClickメソッドでインスタンス生成
    Timer   mTimer   = new Timer(true);         //onClickメソッドでインスタンス生成
    Handler mHandler = new Handler();           //UI Threadへのpost用ハンドラ

    final static int[] ONKAI = {
        0,1,2,3,4,5,6,7,8,9,10,11
    };

    final boolean DEBUG = false;

    final static int YSIZE = ViewGroup.LayoutParams.WRAP_CONTENT;
    //final static int YSIZE = 500;

    double[] history = new double[30];
    int time = 0;
    int base = 0;

    final static int SAMPLING_RATE = 44100;
    final static int MAX_I = 100;
    int FFT_SIZE;
    AudioRecord audioRec = null;
    Button btn = null;
    TextView lbl = null;
    TextView vlm = null;
    ImageButton btnStart = null;
    boolean bIsRecording = false;
    int bufSize;
    Handler handler;
    String labelText;

    final static private int START_ONKAI = 0;

    TextView comment = null;
    final static private int HYOUKA_INTERVAL = 30;


    protected void onCreate(Bundle savedInstanceState) {

        activity = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rentaro);

        graph = (LinearLayout)findViewById(R.id.layout_graph);

        if(DEBUG){
            findViewById(R.id.debug_linearLayout).setVisibility(View.VISIBLE);
        }
        else {
            findViewById(R.id.debug_linearLayout).setVisibility(View.GONE);
        }

        // 初期値設定
        for(int n=0; n<history.length; n++){
            history[n] = 0;
        }

        chart = new AverageCubicTemperatureChart();

        // 初期の音階設定
        view = chart.execute(getApplicationContext(),history,ONKAI[START_ONKAI]);

        // 画面を乗せる。
        view.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                YSIZE));

        graph.addView(view);

        btn = ((Button)(findViewById(R.id.debug)));
        lbl = ((TextView)(findViewById(R.id.debug_text)));
        comment = ((TextView)(findViewById(R.id.comment)));
        vlm = ((TextView)(findViewById(R.id.volume)));
        btnStart = ((ImageButton)(findViewById(R.id.startBtn)));

        // debug用アニメーションスタートボタン
        btnStart.setOnClickListener(this);

        if(DEBUG){
            // debug用アニメーションスタートボタン
            btn.setOnClickListener(new View.OnClickListener (){
                @Override
                public void onClick(View v) {
                    // 初期値設定
                    for(int n=0; n<history.length; n++){
                        history[n] = 150;
                    }
                    mTimer.schedule( timerTask, 100, 50);
                    btn.setEnabled(false);
                    bIsRecording = false;
                }
            });
        }


        handler = new Handler();

        Button[] b = {
                (Button)(findViewById(R.id.button1)),
                (Button)(findViewById(R.id.button2)),
                (Button)(findViewById(R.id.button3)),
                (Button)(findViewById(R.id.button4)),
                (Button)(findViewById(R.id.button5)),
                (Button)(findViewById(R.id.button6)),
                (Button)(findViewById(R.id.button7)),
                (Button)(findViewById(R.id.button8)),
                (Button)(findViewById(R.id.button9)),
                (Button)(findViewById(R.id.button10)),
                (Button)(findViewById(R.id.button11)),
                (Button)(findViewById(R.id.button12))
        };

        for (int i = 0; i < b.length; i++) {
            b[i].setOnClickListener(this);
        }

        // ボタンの色を変更
        Button button = (Button) findViewById(BID[START_ONKAI]);
        button.setBackgroundColor(Color.parseColor("#0000FF"));
        button.setTextColor(Color.parseColor("#FFFFFF"));
        base = ONKAI[START_ONKAI];

        bufSize = AudioRecord.getMinBufferSize(
                SAMPLING_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        FFT_SIZE = getMin2Power(bufSize);
        if (FFT_SIZE > bufSize) bufSize = FFT_SIZE;
        audioRec = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLING_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufSize);

    }

    private void sayMessage(double[] history,double base){

        double sabun = 0;

        for (int n=0;n<history.length;n++){
            double sabun_kouho;
            double sabun_kouho1 = Math.abs(AverageCubicTemperatureChart.Hz2Graph(history[n]) - base * 10);
            double sabun_kouho2 = Math.abs(AverageCubicTemperatureChart.Hz2Graph(history[n]) - (base+12) * 10);
            double sabun_kouho3 = Math.abs(AverageCubicTemperatureChart.Hz2Graph(history[n]) - (base+24) * 10);
            double sabun_kouho4 = Math.abs(AverageCubicTemperatureChart.Hz2Graph(history[n]) - (base+36) * 10);
            sabun_kouho = Math.min(sabun_kouho1,sabun_kouho2);
            sabun_kouho = Math.min(sabun_kouho,sabun_kouho3);
            sabun_kouho = Math.min(sabun_kouho,sabun_kouho4);
            if(AverageCubicTemperatureChart.Hz2Graph(history[n]) <= 0.0){
                sabun += 60;
            }
            else {
                sabun += sabun_kouho;
            }
        }

        sabun/=history.length;

        if ( sabun < 4 ){
            comment.setText("最高や！");
        }
        else if (sabun <  8){
            comment.setText("すばらじゃぞ");
        }
        else if (sabun <  16){
            comment.setText("うむ、よい");
        }
        else if (sabun <  32){
            comment.setText("ふつう");
        }
        else if (sabun <  45){
            comment.setText("おぬし、ちと頑張ろうぜよ");
        }
        else if ( sabun < 55){
            comment.setText("(アカン)");
        }
        else {
            comment.setText("声、出していないね。");
        }


    }

    int[] BID = {
            R.id.button1, R.id.button2, R.id.button3, R.id.button4, R.id.button5, R.id.button6,
            R.id.button7, R.id.button8, R.id.button9, R.id.button10, R.id.button11, R.id.button12
    };

    String[] ONKAI_NAME = {
            "ド","ド#","レ","レ#","ミ","ファ","ファ#","ソ","ソ#","ラ","ラ#","シ",
    };


    @Override
    public void onClick(View v) {

        // TODO Auto-generated method stub
        for (int i = 0; i < BID.length; i++) {
            if (v.getId() == BID[i]) {
                Log.v("OnTouch", "Touch Down " + ONKAI[i]);

                // ボタンの色を変更
                Button button = (Button) findViewById(BID[i]);
                button.setBackgroundColor(Color.parseColor("#0000FF"));
                button.setTextColor(Color.parseColor("#FFFFFF"));

                // ベース音を変更
                base = ONKAI[i];
            } else {
                // ボタンの色を変更
                Button button = (Button) findViewById(BID[i]);
                button.setBackgroundColor(Color.parseColor("#CCCCCC"));
                button.setTextColor(Color.parseColor("#000000"));
            }
        }

        if (v.equals(btnStart)) {



            if (bIsRecording) {
                /*
                btn.setText(R.string.start_label);
                bIsRecording = false;
                 */
            } else {
                // 録音開始
                btnStart.setVisibility(View.GONE);
                comment.setText("スタートじゃ！！");
                Log.v("AudioRecord", "startRecording");
                audioRec.startRecording();
                bIsRecording = true;
                // 録音スレッド
                new Thread(new Runnable() {
                    @Override

                    public void run() {
                        float volumeBase = -1.0f;
                        FFTUtilFloat fft = new FFTUtilFloat(bufSize);
                        short buf[] = new short[fft.fftSize];
                        // TODO Auto-generated method stub
                        while (bIsRecording) {
                            // 録音データ読み込み
                            int readSize = audioRec.read(buf, 0, buf.length);
                            if (readSize < 0 || readSize > fft.fftSize) continue;
                            long sqsum = 0;
                            for (int i=0; i < readSize; ++i) {
                                sqsum += buf[i] * buf[i];
                            }
                            if (volumeBase < 0) {
                                volumeBase = (float)(1.0 / Math.sqrt((double)sqsum/(double)readSize));
                            }
                            float p = (float)(Math.sqrt((double)sqsum/(double)readSize)) * volumeBase;
                            if (p < 1.0f) { p = 1.0f; }
                            final float volume = (float) (20.0 * (float)Math.log10(p));
                            Arrays.fill(buf, readSize, fft.fftSize, (short)0);
                            final float healtz = fft.getHealtz(buf, SAMPLING_RATE);
                            Log.v("AudioRecord", "read " + buf.length + " bytes");
                            Log.v("AudioRecord", healtz + " Hz");

                            //データの中身出力
                            StringBuilder sb = new StringBuilder();
                            //for (float s : fft.tmpReal) {
                            for (short s : buf) {
                                sb.append(s);
                                sb.append(" ");
                            }

                            labelText = String.valueOf(healtz);
                            //labelText = sb.toString();
                            handler.post(new Runnable(){
                                @Override
                                public void run() {
                                    lbl.setText(labelText);
                                    for(int n=0; n<history.length -1; n++){
                                        history[n] = history[n+1];
                                    }
                                    history[history.length-1] = healtz;

                                    graph.removeView(view);
                                    view = chart.execute(getApplicationContext(),history,base);
                                    view.setLayoutParams(new ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.WRAP_CONTENT,
                                            YSIZE));
                                    graph.addView(view);
                                    time++;
                                    if(time%HYOUKA_INTERVAL == 0){
                                        sayMessage(history,base);
                                    }
                                    vlm.setText((int)volume + "");

                                }
                            });
                        }
                        // 録音停止
                        Log.v("AudioRecord", "stop");
                        audioRec.stop();
                    }
                }).start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        super.onDestroy();
        audioRec.release();
    }

    public int getMin2Power(int length) {
        int i = 1;
        while(i < MAX_I) {
            int power = (int)Math.pow(2.0, (double)i);
            if (length > power) {
                return length;
            }
            ++i;
        }
        return -1;
    }



    // デバッグ用
    class MyTimerTask extends TimerTask{

        @Override
        public void run() {
            // mHandlerを通じてUI Threadへ処理をキューイング
            mHandler.post( new Runnable() {
                public void run() {

                    if(bIsRecording){
                        timerTask.cancel();
                    }

                    for(int n=0; n<history.length -1; n++){
                        history[n] = history[n+1];
                    }
                    history[history.length-1] = history[history.length-1] + ((Math.random()-0.2) * 3);

                    graph.removeView(view);
                    view = chart.execute(getApplicationContext(),history,ONKAI[START_ONKAI]);
                    view.setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            YSIZE));
                    graph.addView(view);
                    time++;

                    if(time%HYOUKA_INTERVAL == 0){
                        sayMessage(history,base);
                    }

                }
            });
        }
    }


}