package com.omerakcinar.kotlinplacesapp_basic.view

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.omerakcinar.kotlinplacesapp_basic.R
import com.omerakcinar.kotlinplacesapp_basic.databinding.ActivityMapsBinding
import com.omerakcinar.kotlinplacesapp_basic.model.Place
import com.omerakcinar.kotlinplacesapp_basic.roomdb.PlaceDao
import com.omerakcinar.kotlinplacesapp_basic.roomdb.PlaceDb
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.lang.Exception

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var isFirst = true
    private var selectedLatitude : Double = 0.0
    private var selectedLongitude : Double = 0.0
    val compositeDisposable = CompositeDisposable()
    private lateinit var db : PlaceDb
    private lateinit var placeDao : PlaceDao
    var savedPlace : Place? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        registerLauncher()
        db = Room.databaseBuilder(applicationContext,PlaceDb::class.java,"Place").build()
        placeDao = db.placeDao()
        binding.deleteButton.visibility = View.GONE
        binding.saveButton.isEnabled = false
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLongClickListener(this)

        val intent = intent
        val info = intent.getStringExtra("info")

        if (info == "new"){
            locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager
            locationListener = object : LocationListener{
                override fun onLocationChanged(p0: Location) {
                    if (isFirst){
                        val userLocation = LatLng(p0.latitude,p0.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,15f))
                        isFirst = false
                    }
                }
            }

            if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
                    Snackbar.make(binding.root,"Permission required.",Snackbar.LENGTH_INDEFINITE).setAction("Allow"){
                        //request permission
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }.show()
                } else {
                    //request permission
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            } else {
                //permission granted
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener)
                val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastLocation != null){
                    val lastUserLocation = LatLng(lastLocation.latitude,lastLocation.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,15f))
                }
                mMap.isMyLocationEnabled = true
            }
        }else{
            mMap.clear()
            savedPlace = intent.getSerializableExtra("selectedPlace") as? Place
            savedPlace?.let {
                val latlng = LatLng(it.latitude,it.longitude)
                mMap.addMarker(MarkerOptions().position(latlng).title(it.placeName))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng,15f))

                binding.placeNameText.setText(it.placeName)
                binding.saveButton.visibility = View.GONE
                binding.deleteButton.visibility = View.VISIBLE
            }
        }
    }

    fun registerLauncher(){
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){
            if (it){
                if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    //permission granted
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener)
                    val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (lastLocation != null){
                        val lastUserLocation = LatLng(lastLocation.latitude,lastLocation.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,15f))
                    }
                    mMap.isMyLocationEnabled = true
                } else {
                    Toast.makeText(this,"Permission required!",Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun savePlace(view: View){
        if (binding.placeNameText.text.toString() == ""){
            Toast.makeText(this,"Place name can't be empty.",Toast.LENGTH_SHORT).show()
        }else{
            val placeName = binding.placeNameText.text.toString()
            val latitude = selectedLatitude
            val longitude = selectedLongitude
            val placeToSave = Place(placeName,latitude,longitude)

            compositeDisposable.add(placeDao.insert(placeToSave)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleResponse))
        }
    }

    fun handleResponse(){
        val intent = Intent(this,MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    fun deletePlace(view: View){
        val alert = AlertDialog.Builder(this)
        alert.setTitle("Delete")
        alert.setMessage("You sure? This action can't be taken back.")
        alert.setPositiveButton("Delete",DialogInterface.OnClickListener { dialogInterface, i ->
            savedPlace?.let {
                compositeDisposable.add(placeDao.delete(it)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse))
            }
        })
        alert.setNegativeButton("Cancel",DialogInterface.OnClickListener { dialogInterface, i ->  })
        alert.show()
    }

    override fun onMapLongClick(p0: LatLng) {
        binding.saveButton.isEnabled = true
        mMap.clear()
        mMap.addMarker(MarkerOptions().position(p0))
        selectedLatitude = p0.latitude
        selectedLongitude = p0.longitude
    }

}