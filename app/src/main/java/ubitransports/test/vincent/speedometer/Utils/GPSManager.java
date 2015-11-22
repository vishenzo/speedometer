package ubitransports.test.vincent.speedometer.Utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;

import java.util.List;

import ubitransports.test.vincent.speedometer.Activity.MainActivity;
import ubitransports.test.vincent.speedometer.R;

/**
 * Created by Vincent on 21/11/2015.
 */
public class GPSManager implements android.location.GpsStatus.Listener {

    private static final int gpsMinTime = 500;
    private static final int gpsMinDistance = 0;

    private static LocationManager locationManager = null;
    private static LocationListener locationListener = null;
    Context mContext;

    private Location mLastLocation; //Utile au calcul de la distance parcourue
    private double mDistance; //Distance parcourue

    public GPSManager(Context context) {

        mContext = context;
        mDistance = 0;

        GPSManager.locationListener = new LocationListener() { //LocationListener pour recevoir les changements de position.
            @Override
            public void onLocationChanged(final Location location) {
                double distance = 0;
                if (mLastLocation != null) { //Initialiser mLastLocation avec la position de départ du PDA
                    distance = mLastLocation.distanceTo(location);
                }
                if (location.getAccuracy() < distance) { //Comparaison permettant de pallier la marge d'erreur du GPS
                    addDistance(distance); //On cumule la distance
                }
                mLastLocation = location; //On met à jour la dernière position
                ((MainActivity) mContext).onMyLocationChanged(location); //Callback dans MainActivity
            }

            @Override
            public void onProviderDisabled(final String provider) {
            }

            @Override
            public void onProviderEnabled(final String provider) {
            }

            @Override
            public void onStatusChanged(final String provider, final int status, final Bundle extras) {
            }
        };
    }

    //Affichage d'une pop up lorsque le GPS n'est pas activéee
    public void displayAlert() {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
        alertDialog.setTitle(mContext.getString(R.string.gps_activation));
        alertDialog.setMessage(mContext.getString(R.string.ask_gps_activation));
        alertDialog.setPositiveButton(mContext.getString(R.string.gps_settings), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }
        });
        alertDialog.setNegativeButton(mContext.getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.show();
    }

    public void startListening() {

        //On s'abonne aux changements de position

        if (GPSManager.locationManager == null) {
            GPSManager.locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        }

        GPSManager.locationManager.addGpsStatusListener(this); //Permet d'avoir des informations relatives aux GPS

        //Critères de géolocalisation
        final Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setSpeedRequired(true);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);

        final String bestProvider = GPSManager.locationManager.getBestProvider(criteria, true); //On utilise le meilleur provider qui match avec nos critères

        if (bestProvider != null && bestProvider.length() > 0) {
            GPSManager.locationManager.requestLocationUpdates(bestProvider, GPSManager.gpsMinTime, GPSManager.gpsMinDistance, GPSManager.locationListener);
        } else {
            final List<String> providers = GPSManager.locationManager.getProviders(true);
            for (final String provider : providers) {
                GPSManager.locationManager.requestLocationUpdates(provider, GPSManager.gpsMinTime,
                        GPSManager.gpsMinDistance, GPSManager.locationListener);
            }
        }
    }

    //Méthode permettant d'arrêter l'abonnement aux changements de postion
    public void stopListening() {
        try {
            if (GPSManager.locationManager != null && GPSManager.locationListener != null) {
                GPSManager.locationManager.removeUpdates(GPSManager.locationListener);
            }
            GPSManager.locationManager = null;
        } catch (final Exception ex) {
        }
    }

    //Méthode permettant d'avoir des informations relatives aux GPS auxquels nous sommes connectés
    public void onGpsStatusChanged(int event) {
        int Satellites = 0;
        int SatellitesInFix = 0;
        int timetofix = locationManager.getGpsStatus(null).getTimeToFirstFix(); //Temps mis pour se connecter à tous les GPS disponibles
        for (GpsSatellite sat : locationManager.getGpsStatus(null).getSatellites()) {
            if (sat.usedInFix()) {
                SatellitesInFix++;
            }
            Satellites++;
        }
    }

    //Interface permettant de définir une "callback" dans la MainActivity et ansi de déclencher des évènements lorsque notre position change.
    public interface ILocation {
        public void onMyLocationChanged(Location location);
    }

    //Méthode pour calculer la distance parcourue entre un point A & un point B
    private void addDistance(double distance) {
        mDistance += distance;
    }

    public double getmDistance() {
        return mDistance;
    }

    public void setmDistance(double mDistance) {
        this.mDistance = mDistance;
    }


}
