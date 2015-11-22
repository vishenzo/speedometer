package ubitransports.test.vincent.speedometer.Activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Chronometer;
import android.widget.TextView;

import com.cardiomood.android.controls.gauge.SpeedometerGauge;

import java.math.BigDecimal;

import butterknife.Bind;
import butterknife.ButterKnife;
import ubitransports.test.vincent.speedometer.R;
import ubitransports.test.vincent.speedometer.Utils.GPSManager;


public class MainActivity extends AppCompatActivity implements GPSManager.ILocation { //On respecte le contrat de l'interface définit dans la classe GPSManager

    //Business
    private GPSManager mGPSManager;
    private LocationManager mLocationManager;
    Long mElapsedTime; //Temps écoulé depuis le lancement du chronomètre
    private final int MAX_SPEED_GAUGE = 300;
    private final int MINOR_TICK_GAUGE = 2;
    private final int MAJOR_TICK_GAUGE = 30;
    private boolean isChronometerRunning;
    private boolean isStoped;

    //Views
    @Bind(R.id.tv_speed)
    TextView mTvSpeed;
    @Bind(R.id.speedometer)
    SpeedometerGauge mSpeedometerGauge;
    @Bind(R.id.chronometer)
    Chronometer mChronometer;
    @Bind(R.id.tv_distance)
    TextView mTvDistance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initViews();

        isChronometerRunning = false;
        isStoped = true;

        mGPSManager = new GPSManager(this); //Logique métier du GPS dans GPSManager.java (principe de responsabilité unique, principes SOLID)
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mGPSManager.displayAlert(); //Si le GPS n'est pas activé, on affiche une pop up pour demander l'activation
        }
        mGPSManager.startListening(); //On s'abonne aux changements de position

        mElapsedTime = 0L; //Initialisation du temps écoulé (au départ 0)
        mChronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {

                mElapsedTime = SystemClock.elapsedRealtime() - chronometer.getBase(); //Calcul du temps écoulé

                int h = (int) (mElapsedTime / 3600000);
                int m = (int) (mElapsedTime - h * 3600000) / 60000;
                int s = (int) (mElapsedTime - h * 3600000 - m * 60000) / 1000;

                String hh = h < 10 ? "0" + h : h + "";
                String mm = m < 10 ? "0" + m : m + "";
                String ss = s < 10 ? "0" + s : s + "";

                chronometer.setText(hh + ":" + mm + ":" + ss); //Mise à jour de l'affichage sur le chronomètre
            }
        });

    }

    private void initViews() {
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mSpeedometerGauge.setLabelConverter(new SpeedometerGauge.LabelConverter() {
            @Override
            public String getLabelFor(double progress, double maxProgress) {
                return String.valueOf((int) Math.round(progress));
            }
        });

        //Configuration des valeurs affichées par la jauge de vitesse
        mSpeedometerGauge.setMaxSpeed(MAX_SPEED_GAUGE);
        mSpeedometerGauge.setMajorTickStep(MAJOR_TICK_GAUGE);
        mSpeedometerGauge.setMinorTicks(MINOR_TICK_GAUGE);

        mSpeedometerGauge.addColoredRange(30, 90, Color.GREEN);
        mSpeedometerGauge.addColoredRange(90, 180, Color.YELLOW);
        mSpeedometerGauge.addColoredRange(180, 300, Color.RED);

        //Initialisation de l'affichage du chronomètre
        mChronometer.setText(getString(R.string.chrono_base));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    @Override
    public void onMyLocationChanged(Location location) { //Callback définie par l'interface

        float speed = location.getSpeed(); //Vitesse instantanée en mètres
        double currentSpeed = round(speed * 3.6, 3, BigDecimal.ROUND_HALF_UP); //Vitesse instantanée en km/h

        mTvSpeed.setText(currentSpeed + " km/h"); //Affichage de la vitesse
        mSpeedometerGauge.setSpeed(currentSpeed, true); //Placement de l'aiguille sur la jauge
        mTvDistance.setText(round(mGPSManager.getmDistance() / 1000, 3, BigDecimal.ROUND_HALF_UP) + " km"); //Affichage de la distance parcourue

        if (speed > 0) {
            if (!isChronometerRunning) {
                startChronometer();
            }
            isStoped = false;
            Intent i = new Intent();
            i.setAction("restart");
            sendBroadcast(i);
        }

        if (currentSpeed == 0 && mGPSManager.getmDistance() > 0) {

            if (!isStoped) {
                //On passe dans cette condition si la vitesse est égale à 0 (arrêt) et que la distance parcourue est > 0 (cela permet de gérer le cas
                //du premier lancement de l'application

                //On stop le chrono
                mChronometer.stop();
                isChronometerRunning = false;

                double distance = mGPSManager.getmDistance();

                Intent i = new Intent(MainActivity.this, AverageSpeedActivity.class); //On change automatiquement d'écran
                i.putExtra("distance", distance); //On passe la distance en paramètre
                i.putExtra("elapsedTime", mElapsedTime); //On passe le temps écoulé entre les deux derniers arrêts
                startActivityForResult(i, 0);

                isStoped = true;
            }
        }
    }

    //Méthode pour arrondir la vitesse
    public static double round(double unrounded, int precision, int roundingMode) {
        BigDecimal bd = new BigDecimal(unrounded);
        BigDecimal rounded = bd.setScale(precision, roundingMode);
        return rounded.doubleValue();
    }

    //Méthode appellée lorsque l'on redémarre (vitesse > 0)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (resultCode == 1) {
                mGPSManager.setmDistance(0);
                mElapsedTime = 0L;

            }
        }
    }

    private void startChronometer() {
        mChronometer.setBase(SystemClock.elapsedRealtime());
        mChronometer.start();
        isChronometerRunning = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGPSManager.stopListening();
    }
}
