package com.example.hw1

import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hw1.data.ScoreRepository
import com.example.hw1.databinding.ActivityHighScoresBinding
import com.example.hw1.databinding.ItemScoreBinding
import com.example.hw1.game.GameMode
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class HighScoresActivity : AppCompatActivity() {

    private lateinit var b: ActivityHighScoresBinding
    private lateinit var repo: ScoreRepository
    private lateinit var adapter: ScoreAdapter
    private var currentMode = GameMode.SLOW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityHighScoresBinding.inflate(layoutInflater)
        setContentView(b.root)

        repo = ScoreRepository.getInstance(applicationContext)
        adapter = ScoreAdapter()

        b.scoreList.layoutManager = LinearLayoutManager(this)
        b.scoreList.adapter = adapter

        b.btnModeSlow.setOnClickListener { setMode(GameMode.SLOW) }
        b.btnModeFast.setOnClickListener { setMode(GameMode.FAST) }
        b.btnModeSensor.setOnClickListener { setMode(GameMode.SENSOR) }

        if (getString(R.string.google_maps_key) == "YOUR_API_KEY") {
            android.widget.Toast.makeText(
                this,
                "Set google_maps_key in strings.xml to enable maps.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }

        setMode(GameMode.SLOW)
    }

    override fun onStart() {
        super.onStart()
        adapter.onParentStart()
    }

    override fun onResume() {
        super.onResume()
        adapter.onParentResume()
    }

    override fun onPause() {
        adapter.onParentPause()
        super.onPause()
    }

    override fun onStop() {
        adapter.onParentStop()
        super.onStop()
    }

    override fun onDestroy() {
        adapter.onParentDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        adapter.onParentLowMemory()
    }

    private fun setMode(mode: GameMode) {
        currentMode = mode
        b.modeTitle.text = "Top 10: ${mode.name}"
        loadScores()
    }

    private fun loadScores() {
        lifecycleScope.launch {
            val scores = withContext(Dispatchers.IO) {
                repo.getTopScores(currentMode)
            }
            val ui = scores.mapIndexed { index, s ->
                ScoreUi(
                    id = s.id,
                    rank = index + 1,
                    distance = s.distance,
                    coins = s.coins,
                    timestamp = s.timestamp,
                    latitude = s.latitude,
                    longitude = s.longitude
                )
            }
            adapter.submitList(ui)
        }
    }

    private data class ScoreUi(
        val id: Long,
        val rank: Int,
        val distance: Int,
        val coins: Int,
        val timestamp: Long,
        val latitude: Double?,
        val longitude: Double?
    )

    private class ScoreAdapter :
        androidx.recyclerview.widget.RecyclerView.Adapter<ScoreAdapter.ScoreViewHolder>() {

        private val mapViews = mutableSetOf<com.google.android.gms.maps.MapView>()
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        private val geocoderCache = mutableMapOf<String, String>()

        var items: List<ScoreUi> = emptyList()
            private set

        fun submitList(newItems: List<ScoreUi>) {
            items = newItems
            notifyDataSetChanged()
        }

        fun onParentStart() {
            mapViews.forEach { it.onStart() }
        }

        fun onParentResume() {
            mapViews.forEach { it.onResume() }
        }

        fun onParentPause() {
            mapViews.forEach { it.onPause() }
        }

        fun onParentStop() {
            mapViews.forEach { it.onStop() }
        }

        fun onParentDestroy() {
            mapViews.forEach { it.onDestroy() }
            mapViews.clear()
            scope.cancel()
        }

        fun onParentLowMemory() {
            mapViews.forEach { it.onLowMemory() }
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ScoreViewHolder {
            val inflater = android.view.LayoutInflater.from(parent.context)
            val binding = ItemScoreBinding.inflate(inflater, parent, false)
            val holder = ScoreViewHolder(binding)
            mapViews.add(holder.mapView)
            return holder
        }

        override fun onBindViewHolder(holder: ScoreViewHolder, position: Int) {
            val item = items[position]
            holder.bind(item)
        }

        override fun onViewRecycled(holder: ScoreViewHolder) {
            super.onViewRecycled(holder)
            holder.onRecycled()
        }

        override fun getItemCount(): Int = items.size

        inner class ScoreViewHolder(private val b: ItemScoreBinding) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(b.root) {

            val mapView: com.google.android.gms.maps.MapView = b.scoreMap
            private var googleMap: GoogleMap? = null
            private var pendingLatLng: LatLng? = null
            private var boundGeoKey: String? = null

            init {
                mapView.onCreate(null)
                mapView.getMapAsync { map ->
                    googleMap = map
                    map.uiSettings.isMapToolbarEnabled = false
                    map.uiSettings.isZoomControlsEnabled = false
                    map.uiSettings.setAllGesturesEnabled(false)
                    map.uiSettings.isMyLocationButtonEnabled = false
                    pendingLatLng?.let { updateMap(it) }
                }
            }

            fun bind(item: ScoreUi) {
                val dateFormat = DateFormat.getDateFormat(b.root.context)
                val timeFormat = DateFormat.getTimeFormat(b.root.context)
                b.rankText.text = "#${item.rank}"
                b.distanceText.text = "Distance: ${item.distance}"
                b.coinsText.text = "Coins: ${item.coins}"
                val date = java.util.Date(item.timestamp)
                b.timeText.text = "${dateFormat.format(date)} ${timeFormat.format(date)}"

                val lat = item.latitude
                val lng = item.longitude
                if (lat != null && lng != null) {
                    val geoKey = "%.4f,%.4f".format(Locale.US, lat, lng)
                    boundGeoKey = geoKey
                    val cachedCity = geocoderCache[geoKey]
                    if (cachedCity != null) {
                        b.locationText.text = "City: $cachedCity"
                    } else {
                        b.locationText.text = "City: locating..."
                        resolveCityAsync(lat, lng, geoKey)
                    }
                    b.scoreMap.visibility = View.VISIBLE
                    val latLng = LatLng(lat, lng)
                    pendingLatLng = latLng
                    updateMap(latLng)
                } else {
                    pendingLatLng = null
                    boundGeoKey = null
                    b.locationText.text = "Location unavailable"
                    b.scoreMap.visibility = View.GONE
                    googleMap?.clear()
                }

                mapView.onResume()
            }

            fun onRecycled() {
                mapView.onPause()
            }

            private fun updateMap(latLng: LatLng) {
                val map = googleMap ?: return
                map.clear()
                map.addMarker(MarkerOptions().position(latLng))
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13f))
            }

            private fun resolveCityAsync(lat: Double, lng: Double, geoKey: String) {
                val context = b.root.context.applicationContext
                scope.launch(Dispatchers.IO) {
                    val city = fetchCity(context, lat, lng)
                    scope.launch(Dispatchers.Main) {
                        geocoderCache[geoKey] = city
                        if (boundGeoKey == geoKey) {
                            b.locationText.text = "City: $city"
                        }
                    }
                }
            }

            private fun fetchCity(context: android.content.Context, lat: Double, lng: Double): String {
                val geocoder = Geocoder(context, Locale.getDefault())
                return try {
                    @Suppress("DEPRECATION")
                    val results: List<Address> = geocoder.getFromLocation(lat, lng, 1)
                        ?: emptyList()
                    val address = results.firstOrNull()
                    address?.locality
                        ?: address?.subAdminArea
                        ?: address?.adminArea
                        ?: "Unknown"
                } catch (_: Exception) {
                    "Unknown"
                }
            }
        }
    }
}
