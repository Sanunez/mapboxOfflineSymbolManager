package omegaonedevelopers.client.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionError;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

	private final String TAG = "MainActivity";

	//Basic Map Components
	private MapView mapView;
	private MapboxMap mapboxMap;

	//SymbolManager
	private SymbolManager symbolManager;

	//Offline Map Components
	private OfflineManager offlineManager;

	// JSON encoding/decoding
	public static final String JSON_CHARSET = "UTF-8";
	public static final String JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME";

	//UI Components
	private ProgressDialog progressBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Mapbox.getInstance(this, getString(R.string.mapbox_token));
		setContentView(R.layout.activity_main);
		mapView = findViewById(R.id.mapView);
		mapView.onCreate(savedInstanceState);
		mapView.getMapAsync(this);
	}

	@Override
	public void onMapReady(@NonNull MapboxMap mapboxMap) {
		MainActivity.this.mapboxMap = mapboxMap;
		MainActivity.this.mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
			symbolManager = new SymbolManager(mapView, MainActivity.this.mapboxMap, style);
			symbolManager.setIconAllowOverlap(true);
			symbolManager.setTextAllowOverlap(true);
			offlineManagerSetup(style);
		});
	}

	private void offlineManagerSetup(Style style){
		//Create offline Manager
		offlineManager = OfflineManager.getInstance(MainActivity.this);
		//Bound area to download
		LatLngBounds latLngBounds = new LatLngBounds.Builder()
						.include(new LatLng(25.7803,-100.1007)) //SouthWest
						.include(new LatLng(25.5775, -100.4449))//NorthEast
						.build();
		//Define the area to download
		OfflineTilePyramidRegionDefinition definition = new OfflineTilePyramidRegionDefinition(
						style.getUrl(),
						latLngBounds,
						0,
						22,
						MainActivity.this.getResources().getDisplayMetrics().density);
		//Set metadata
		byte[] metadata;
		try{
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("LocalTest", "Monterrey, Guadalupe");
			String json = jsonObject.toString();
			metadata = json.getBytes("UTF8");
		} catch(Exception exception){
			Log.e(TAG, "Failed to encode metadata: " + exception.getMessage());
			metadata = null;
		}
		//Asynchronously create region
		offlineManager.createOfflineRegion(definition,metadata,offlineDownloadCallback);
	}

	OfflineManager.CreateOfflineRegionCallback offlineDownloadCallback = new OfflineManager.CreateOfflineRegionCallback() {
		@Override
		public void onCreate(OfflineRegion offlineRegion) {
			offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE);
			progressBar = new ProgressDialog(MainActivity.this);
			startProgress(null, "Loading offline map");
			offlineRegion.setObserver(offlineDownloadObserver);
		}

		@Override
		public void onError(String error) {
			Log.e(TAG, " offlineDownloadCallback: " + error);
		}
	};

	OfflineRegion.OfflineRegionObserver offlineDownloadObserver = new OfflineRegion.OfflineRegionObserver() {
		@Override
		public void onStatusChanged(OfflineRegionStatus status) {
			double percentage = status.getRequiredResourceCount() >= 0
							? (100.0 * status.getCompletedResourceCount() / status.getRequiredResourceCount()) :
							0.0;

			if (status.isComplete()){
				endProgress();
				addMarkersToOfflineMap();
				Log.d(TAG, " offlineDownloadObserver: Offline Maps Download Complete");
			}else{
				setPercentage((int)Math.round(percentage));
				Log.d(TAG, " offlineDownloadObserver: " + percentage);
			}
		}

		@Override
		public void onError(OfflineRegionError error) {
			Log.e(TAG, " offlineDownloadObserver: onError reason: " + error.getReason());
			Log.e(TAG, " offlineDownloadObserver: onError message: " + error.getMessage());

		}

		@Override
		public void mapboxTileCountLimitExceeded(long limit) {
			Log.e(TAG, "Mapbox tile count limit exceeded: " + limit);
		}
	};

	private void addMarkersToOfflineMap(){
		SymbolOptions options = new SymbolOptions()
						.withLatLng(new LatLng(25.681901, -100.301183))
						.withIconImage("airport-15")
						.withIconSize(2f)
						.withTextColor("#FFFFFF")
						.withTextField("TEST")
						.withTextOffset(new Float[]{0f,0f})
						.withTextHaloColor("#000000")
						.withTextHaloWidth(1f)
						.withTextHaloBlur(1f);
		Symbol symbol = symbolManager.create(options);
	}

	//ProgressBar methods
	private void startProgress(String title, String message) {
		if(title != null){progressBar.setTitle(title);}
		if(message != null){progressBar.setMessage(message);}
		progressBar.setIndeterminate(true);
		progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressBar.show();
	}
	private void setPercentage(final int percentage){
		progressBar.setIndeterminate(false);
		progressBar.setProgress(percentage);
	}
	private void endProgress(){
		progressBar.setIndeterminate(false);
		progressBar.dismiss();
	}

	//LifeCycle Methods
	@Override
	public void onStart(){
		super.onStart();
		mapView.onStart();
	}

	@Override
	public void onResume(){
		super.onResume();
		mapView.onResume();
	}

	@Override
	public void onPause(){
		super.onPause();
		mapView.onPause();
		if (false) {
			offlineManager.listOfflineRegions(new OfflineManager.ListOfflineRegionsCallback() {
				@Override
				public void onList(OfflineRegion[] offlineRegions) {
					if (offlineRegions.length > 0) {
						// delete the last item in the offlineRegions list which will be yosemite offline map
						offlineRegions[(offlineRegions.length - 1)].delete(new OfflineRegion.OfflineRegionDeleteCallback() {
							@Override
							public void onDelete() {
								Toast.makeText(MainActivity.this, "Offline Map Deleted", Toast.LENGTH_LONG).show();
							}

							@Override
							public void onError(String error) {
								Log.e(TAG, "On delete error: " + error);
							}
						});
					}
				}

				@Override
				public void onError(String error) {
					Log.e(TAG, "onListError: " + error);
				}
			});
		}

	}

	@Override
	public void onStop(){
		super.onStop();
		mapView.onStop();
	}

	@Override
	public void onLowMemory(){
		super.onLowMemory();
		mapView.onLowMemory();
	}

	@Override
	protected void onDestroy(){
		super.onDestroy();
		mapView.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		mapView.onSaveInstanceState(outState);
	}
}
