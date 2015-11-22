package ubitransports.test.vincent.speedometer.Activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.math.BigDecimal;

import butterknife.Bind;
import butterknife.ButterKnife;
import ubitransports.test.vincent.speedometer.R;

public class AverageSpeedActivity extends AppCompatActivity {

    //Views
    @Bind(R.id.tv_average_speed)
    TextView mTvAverageSpeed;

    //Business
    private double mDistance;
    private long mElapsedTime;
    private BroadcastReceiver mBroadcastReceiver;
    private IntentFilter mIntentFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initViews();
        initBroadcastReceiver();

        //On récupère la distance parcourue & le temps écoulé depuis le dernier arrêt
        mDistance = getIntent().getDoubleExtra("distance", 0);
        mElapsedTime = getIntent().getLongExtra("elapsedTime", 0);

        //On sait que Vitesse moyenne = distance / temps
        double averageSpeed = (mDistance / (mElapsedTime / 1000)) * 3.6;

        try {
            double average = round(averageSpeed, 3, BigDecimal.ROUND_HALF_UP);
            mTvAverageSpeed.setText(average + "");
        } catch (Exception e) {
            mTvAverageSpeed.setText("");
        }
    }

    private void initViews() {
        setContentView(R.layout.activity_average_speed);
        ButterKnife.bind(this);

    }

    private void initBroadcastReceiver() {
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction("restart");
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null) {
                    if ("restart".equals(action)) {
                        AverageSpeedActivity.this.setResult(1);
                        finish(); //Vitesse >0, on switch sur le premier écran
                    }
                }
            }
        };
        this.registerReceiver(mBroadcastReceiver, mIntentFilter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_average_speed, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    //Méthode pour arrondir la vitesse
    public static double round(double unrounded, int precision, int roundingMode) {
        BigDecimal bd = new BigDecimal(unrounded);
        BigDecimal rounded = bd.setScale(precision, roundingMode);
        return rounded.doubleValue();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }
}
