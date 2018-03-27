package com.droidmentor.locationhelper;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.droidmentor.locationhelper.LocationUtil.PermissionUtils;
import com.droidmentor.locationhelper.LocationUtil.PermissionUtils.PermissionResultCallback;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MyLocationUsingLocationAPI extends AppCompatActivity implements
        ConnectionCallbacks,
        com.google.android.gms.location.LocationListener,
        OnConnectionFailedListener,
        OnRequestPermissionsResultCallback,
        PermissionResultCallback {


    @BindView(R.id.btnLocation)
    Button btnProceed;
    @BindView(R.id.tvAddress)
    TextView tvAddress;
    @BindView(R.id.tvEmpty)
    TextView tvEmpty;
    @BindView(R.id.rlPickLocation)
    RelativeLayout rlPick;
    final Handler ha = new Handler();
    Runnable runnable;

    ArrayList<MyLatLong> myLatLongArrayList = new ArrayList<>();

    // LogCat tag
    private static final String TAG = MyLocationUsingHelper.class.getSimpleName();

    private final static int PLAY_SERVICES_REQUEST = 1000;
    private final static int REQUEST_CHECK_SETTINGS = 2000;

    private Location mLastLocation;
    LocationListener myLocationListener;

    // Google client to interact with Google API

    private GoogleApiClient mGoogleApiClient;

    private static double latitude;
    private static double longitude;

    // list of permissions
    ArrayList<String> permissions = new ArrayList<>();
    PermissionUtils permissionUtils;

    boolean isPermissionGranted;
    //related to distance
    LocationManager lm;
    String parsedDistance;
    String response;
    TextView tvDistance;

    MyLatLong startLocation = null;
    MyLatLong lastLocation = null;
    float traveledDistance = 0.f;
    //
    double firstLat, firstLng;
    double lastLat, lastLng;
    boolean flag = true;
    float totalDistance = 0f;
    float totalDistanceGoogle = 0f;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_service);
        tvDistance = (TextView) findViewById(R.id.tvDistance);
        ButterKnife.bind(this);

        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        permissionUtils = new PermissionUtils(MyLocationUsingLocationAPI.this);

        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        permissionUtils.check_permission(permissions, "Need GPS permission for getting your location", 1);

        //RV click here
        rlPick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getLocation();

                if (mLastLocation != null) {
                    latitude = mLastLocation.getLatitude();
                    longitude = mLastLocation.getLongitude();
                    // getAddress();
                    Log.e("Lat==>", "" + mLastLocation.getLatitude());
                    Log.e("Long==>", "" + mLastLocation.getLongitude());
                    DecimalFormat df = new DecimalFormat("#.000000");
                    double f_latitude = Double.valueOf(df.format(mLastLocation.getLatitude()));
                    double f_longitude = Double.valueOf(df.format(mLastLocation.getLongitude()));

                    tvAddress.setVisibility(View.VISIBLE);
                    tvAddress.setText(f_latitude + "," + f_longitude);


                } else {

                    if (btnProceed.isEnabled())
                        btnProceed.setEnabled(false);

                    showToast("Couldn't get the location. Make sure location is enabled on the device");
                }
            }
        });


        btnProceed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showToast("Proceed to the next step");
                //stopLocationUpdates();
                //startActivity(new Intent(MyLocationUsingLocationAPI.this, DistanceActivity.class));
//                ha.removeCallbacks(runnable);
//                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,this);
                //endGPS();

            }
        });


        // check availability of play services
        if (checkPlayServices()) {

            // Building the GoogleApi client
            buildGoogleApiClient();
        }


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        //main logic here
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // TODO Auto-generated method stub

                try {
                    Log.e("From Lat==>", "" + location.getLatitude());
                    Log.e("From Long==>", "" + location.getLongitude());

                    DecimalFormat df = new DecimalFormat("#.000000");
                    double latitude = Double.valueOf(df.format(location.getLatitude()));
                    double longitude = Double.valueOf(df.format(location.getLongitude()));

                    ///adding  into array
                    myLatLongArrayList.add(new MyLatLong(latitude, longitude));

                    StringBuilder builder = new StringBuilder();
                    for (MyLatLong details : myLatLongArrayList) {
                        builder.append(details.getLat() + "," + details.getLon() + "\n");
                    }
                    tvAddress.setText(builder.toString());
                    if (startLocation == null) {
                        startLocation = new MyLatLong();
                        lastLocation = new MyLatLong();
                        startLocation.setLat(location.getLatitude());
                        startLocation.setLon(location.getLongitude());
                    } else if (location != null) {
                        Location startPoint = new Location("pointA");
                        startPoint.setLatitude(lastLocation.getLat());
                        startPoint.setLongitude(lastLocation.getLon());
////                        startPoint.setLatitude(23.017427);
////                        startPoint.setLongitude(72.529772);

                        Location endPoint = new Location("pointB");
                        endPoint.setLatitude(location.getLatitude());
                        endPoint.setLongitude(location.getLongitude());
//                        //endPoint.setLatitude(23.030513);
//                        //endPoint.setLongitude(72.507540);

                        //simple using location
                        float distanceKm = endPoint.distanceTo(startPoint) / 1000;

                        DecimalFormat decimalFormat = new DecimalFormat();
                        decimalFormat.setRoundingMode(RoundingMode.CEILING);
                        decimalFormat.setMaximumFractionDigits(2);
                        distanceKm = Float.parseFloat(decimalFormat.format(distanceKm));
                        Log.e("L_LocationKm", "" + distanceKm);
                        traveledDistance = traveledDistance + distanceKm;
                        Log.e("L_LocationFKMT", "" + traveledDistance);
                        Log.e("L_End", "-------------------------------------------");

                        //tvDistance.setText("" + traveledDistance);
//                        //using google api
                        String strKm = getDistance(
                                (float) startPoint.getLatitude(),
                                (float) startPoint.getLongitude(),
                                (float) endPoint.getLatitude(),
                                (float) endPoint.getLongitude());
                        String[] arrKm = strKm.split(" ");
                        float km = Float.parseFloat(arrKm[0].trim());
                        Log.e("L_GoogleKm", "" + totalDistanceGoogle);
                        totalDistanceGoogle = totalDistanceGoogle + km;
                        Log.e("L_GoogleFKMT", "" + totalDistanceGoogle);
                        Log.e("L_End", "-------------------------------------------");
                        tvDistance.setText("From Location=>" + traveledDistance + "km" + "\n" +
                                "From Google=>" + totalDistanceGoogle + "km");
                    }
                    lastLocation = new MyLatLong(location.getLatitude(), location.getLongitude());

                } catch (Exception e) {
                    Log.e("Catch=>", e.getMessage());
                    e.printStackTrace();
                }


            }

            private double meterDistanceBetweenPoints(float lat_a, float lng_a, float lat_b, float lng_b) {


                //request
                /* double temp=meterDistanceBetweenPoints((float) startPoint.getLatitude(),
//                                (float) startPoint.getLongitude(),
//                                (float) endPoint.getLatitude(),
//                                (float) endPoint.getLongitude());
//                        tvDistance.setText("" + traveledDistance);
*/
                float pk = (float) (180.f / Math.PI);

                float a1 = lat_a / pk;
                float a2 = lng_a / pk;
                float b1 = lat_b / pk;
                float b2 = lng_b / pk;

                double t1 = Math.cos(a1) * Math.cos(a2) * Math.cos(b1) * Math.cos(b2);
                double t2 = Math.cos(a1) * Math.sin(a2) * Math.cos(b1) * Math.sin(b2);
                double t3 = Math.sin(a1) * Math.sin(b1);
                double tt = Math.acos(t1 + t2 + t3);

                return 6366000 * tt;
            }

            @Override
            public void onProviderDisabled(String provider) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProviderEnabled(String provider) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStatusChanged(String provider, int status,
                                        Bundle extras) {
                // TODO Auto-generated method stub
            }
        });


    }


    /**
     * Method to display the location on UI
     */

    private void getLocation() {

        if (isPermissionGranted) {
            try {
                mLastLocation = LocationServices.FusedLocationApi
                        .getLastLocation(mGoogleApiClient);
            } catch (SecurityException e) {
                e.printStackTrace();
            }

        }

    }

    public Address getAddress(double latitude, double longitude) {
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            return addresses.get(0);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }


    public void getAddress() {

        Address locationAddress = getAddress(latitude, longitude);

        if (locationAddress != null) {
            String address = locationAddress.getAddressLine(0);
            String address1 = locationAddress.getAddressLine(1);
            String city = locationAddress.getLocality();
            String state = locationAddress.getAdminArea();
            String country = locationAddress.getCountryName();
            String postalCode = locationAddress.getPostalCode();

            String currentLocation;

            if (!TextUtils.isEmpty(address)) {
                currentLocation = address;

                if (!TextUtils.isEmpty(address1))
                    currentLocation += "\n" + address1;

                if (!TextUtils.isEmpty(city)) {
                    currentLocation += "\n" + city;

                    if (!TextUtils.isEmpty(postalCode))
                        currentLocation += " - " + postalCode;
                } else {
                    if (!TextUtils.isEmpty(postalCode))
                        currentLocation += "\n" + postalCode;
                }

                if (!TextUtils.isEmpty(state))
                    currentLocation += "\n" + state;

                if (!TextUtils.isEmpty(country))
                    currentLocation += "\n" + country;

                tvEmpty.setVisibility(View.GONE);
                tvAddress.setText(currentLocation + "" + latitude + "," + longitude);
                tvAddress.setVisibility(View.VISIBLE);


                if (!btnProceed.isEnabled())
                    btnProceed.setEnabled(true);


            }

        }

    }

    /**
     * Creating google api client object
     */

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

        mGoogleApiClient.connect();

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult locationSettingsResult) {

                final Status status = locationSettingsResult.getStatus();

                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location requests here
                        getLocation();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(MyLocationUsingLocationAPI.this, REQUEST_CHECK_SETTINGS);

                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });


    }


    /**
     * Method to verify google play services on the device
     */

    private boolean checkPlayServices() {

        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();

        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode,
                        PLAY_SERVICES_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made
                        getLocation();
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        break;
                    default:
                        break;
                }
                break;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();
    }

    /**
     * Google api callback methods
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle arg0) {

        // Once connected with google api, get the location
        getLocation();
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }


    // Permission check functions


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // redirects to utils
        permissionUtils.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }


    @Override
    public void PermissionGranted(int request_code) {
        Log.i("PERMISSION", "GRANTED");
        isPermissionGranted = true;
    }

    @Override
    public void PartialPermissionGranted(int request_code, ArrayList<String> granted_permissions) {
        Log.i("PERMISSION PARTIALLY", "GRANTED");
    }

    @Override
    public void PermissionDenied(int request_code) {
        Log.i("PERMISSION", "DENIED");
    }

    @Override
    public void NeverAskAgain(int request_code) {
        Log.i("PERMISSION", "NEVER ASK AGAIN");
    }

    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    //Distance using Google URL
    public String getDistance(final double lat1,
                              final double lon1,
                              final double lat2,
                              final double lon2) {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    URL url = new URL("http://maps.googleapis.com/maps/api/directions/json?origin=" + lat1 + "," + lon1 + "&destination=" + lat2 + "," + lon2 + "&sensor=false&units=metric&mode=driving");
                    final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    InputStream in = new BufferedInputStream(conn.getInputStream());
                    response = org.apache.commons.io.IOUtils.toString(in, "UTF-8");

                    JSONObject jsonObject = new JSONObject(response);
                    JSONArray array = jsonObject.getJSONArray("routes");
                    JSONObject routes = array.getJSONObject(0);
                    JSONArray legs = routes.getJSONArray("legs");
                    JSONObject steps = legs.getJSONObject(0);
                    JSONObject distance = steps.getJSONObject("distance");
                    parsedDistance = distance.getString("text");


                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return parsedDistance;
    }

    //    @Override
//    public void onLocationChanged(final Location location) {
//        try {
//            Log.e("From Lat==>", "" + location.getLatitude());
//            Log.e("From Long==>", "" + location.getLongitude());
//
//
//            ha.postDelayed(new Runnable() {
//
//                @Override
//                public void run() {
//                    runnable = this;
//                    //call function
//                    ha.postDelayed(this, 1000);
//                    getLocation();
//
//                    if (mLastLocation != null) {
//                        latitude = mLastLocation.getLatitude();
//                        longitude = mLastLocation.getLongitude();
//                        myLatLongArrayList.add(new MyLatLong(location.getLatitude(), location.getLongitude()));
//                        //** getAddress();
//                        Log.e("Lat==>", "" + latitude);
//                        Log.e("Long==>", "" + longitude);
//                        StringBuilder builder = new StringBuilder();
//                        for (MyLatLong details : myLatLongArrayList) {
//                            builder.append(details.getLat() + "," + details.getLon() + "\n");
//                        }
////                                for (int i = 0; i <10 ; i++) {
////
////                                    builder.append("this>"+i);
////                                }
//
//                        tvAddress.setText(builder.toString());
//                    } else {
//
//                        if (btnProceed.isEnabled())
//                            btnProceed.setEnabled(false);
//
//                        showToast("Couldn't get the location. Make sure location is enabled on the device");
//                    }
//
//
//                }
//
//            }, 1000);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onLocationChanged(Location location) {

    }
}


