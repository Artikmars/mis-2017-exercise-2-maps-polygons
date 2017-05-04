package com.example.artam.mapspolygons;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.SphericalUtil;

import java.util.List;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    int markersCount;

    SharedPreferences sPref = null;
    Marker marker = null;
    String myLocation, markersCountStr, strButton;
    LatLng position1;
    Button btnStartPolygon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        btnStartPolygon = (Button) findViewById(R.id.startPolBtn);
    }

    @Override
    public void onMapLongClick(LatLng latLng) {

        EditText editT = (EditText) findViewById(R.id.editText);
        myLocation = editT.getText().toString();

        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title(myLocation)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

        marker = mMap.addMarker(options);
        editT.setText("");

        Toast.makeText(getApplicationContext(),
                "New marker added \n" + latLng.toString(), Toast.LENGTH_SHORT)
                .show();

        markersCount++;
        markersCountStr = Integer.toString(markersCount);

        // http://stackoverflow.com/questions/22814490/how-to-save-a-marker-onmapclick
        if (latLng.latitude != 0 && latLng.longitude != 0) {
            sPref.edit().putString("Lat" + Integer.toString((markersCount - 1)), Double.toString(latLng.latitude)).apply();
            sPref.edit().putString("Lng" + Integer.toString((markersCount - 1)), Double.toString(latLng.longitude)).apply();
            sPref.edit().putString("myLocation", myLocation).apply();
            sPref.edit().putInt("markersCount", markersCount).apply();
        }
    }

    public void extractMarkers() { //http://stackoverflow.com/questions/35868807/saving-google-map-markers-into-sharedpreferences-in-android-studios

        sPref = this.getSharedPreferences("location", 0);

        markersCount = sPref.getInt("markersCount", 0);


        if (markersCount != 0) {
            String lat, lng, myLocation;

            // Iterating through all the locations stored
            for (int i = 0; i < markersCount; i++) {
                lat = sPref.getString("Lat" + i, "0");
                lng = sPref.getString("Lng" + i, "0");
                myLocation = sPref.getString("myLocation", "0");

                Toast.makeText(this, "Location: " + myLocation + "\n " + "Latitude: " + lat + "\n "
                        + "Longitude: " + lng, Toast.LENGTH_LONG).show();

                double lat3 = Double.valueOf(lat);
                double lng3 = Double.valueOf(lng);

                position1 = new LatLng(lat3, lng3);

                mMap.addMarker(new MarkerOptions()
                        .position(position1)
                        .title(myLocation)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            }
        }
    }

    public void startPolygon(View v) {
        if (markersCount > 2) {

            strButton = btnStartPolygon.getText().toString();
            if (strButton.equals("Start Polygon")) {

                btnStartPolygon.setText(R.string.end_polygon);
                sPref = this.getSharedPreferences("location", 0);
                markersCount = sPref.getInt("markersCount", 0);

                PolygonOptions polygonOptions = new PolygonOptions();

                if (markersCount > 2) {

                    String lat;
                    String lng;

                    // Iterating through all the locations stored
                    for (int i = 0; i < markersCount; i++) {

                        // Getting the latitude of the i-th location
                        lat = sPref.getString("Lat" + i, "0");

                        // Getting the longitude of the i-th location
                        lng = sPref.getString("Lng" + i, "0");

                        double lat3 = Double.valueOf(lat);
                        double lng3 = Double.valueOf(lng);

                        position1 = new LatLng(lat3, lng3);

                        if (lat3 != 0 && lng3 != 0) {
                            polygonOptions.add(new LatLng(lat3, lng3));

                        }

                    }


                    //http://stackoverflow.com/questions/28838287/calculate-the-area-of-a-polygon-drawn-on-google-maps-in-an-android-application


                    LatLng centroid = computeCentroid(polygonOptions.getPoints());
                    mMap.addPolygon(polygonOptions.fillColor(Color.argb(125, 175, 194, 255))
                            .strokeColor(Color.BLUE));

                    double totalArea = SphericalUtil.computeArea(polygonOptions.getPoints());
                    String areaUnit = " m²";
                    if (totalArea > 1000000) {
                        totalArea = totalArea / 1000000;
                        areaUnit = " km²";
                    }

                    totalArea = Math.round(totalArea * 100.0) / 100.0;

                    mMap.addMarker(new MarkerOptions().position(centroid).title(String.valueOf(totalArea) + areaUnit));


                }
            } else {
                btnStartPolygon.setText(R.string.start_polygon);
                mMap.clear();
                sPref.edit().clear().apply();
                markersCountStr = null;
                markersCount = 0;
            }

        } else {

            Toast.makeText(getApplicationContext(), "Please add more than 2 markers", Toast.LENGTH_SHORT).show();
        }
    }

    //http://stackoverflow.com/questions/9752334/calculate-centroid-of-android-graphics-path-values-and-find-the-centroids-rela//http://www.androiddevelopersolutions.com/2015/02/android-calculate-center-of-polygon-in.html
    private LatLng computeCentroid(List<LatLng> positions) {
        double centerX = 0;
        double centerY = 0;
        for (LatLng position : positions) {
            centerX += position.latitude;
            centerY += position.longitude;
        }
        LatLng center = new LatLng(centerX / positions.size(), centerY / positions.size());
        Toast.makeText(this, "Centroid Lat: " + center.latitude + "\n Lng: " + center.longitude,
                Toast.LENGTH_SHORT).show();
        return center;
    }

    public void onClearMarkers(View view) { //Google Maps Android Samples

        btnStartPolygon.setText(R.string.start_polygon);
        mMap.clear();
        sPref.edit().clear().apply();
        markersCountStr = null;
        markersCount = 0;

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        extractMarkers();
        mMap.setOnMapLongClickListener(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                if (isGPSEnabled(getApplicationContext())) {
                    return false;
                } else Toast.makeText(getApplicationContext(),
                        "Please enable your GPS for detecting your current position!",
                        Toast.LENGTH_LONG).show();
                return true;
            }
        });
    }

    public boolean isGPSEnabled(Context mContext) {   //http://stackoverflow.com/questions/843675/how-do-i-find-out-if-the-gps-of-an-android-device-is-enabled
        LocationManager locationManager = (LocationManager)
                mContext.getSystemService(Context.LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
}



