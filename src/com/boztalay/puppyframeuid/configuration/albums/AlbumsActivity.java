package com.boztalay.puppyframeuid.configuration.albums;

import android.app.ActionBar;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import com.boztalay.puppyframeuid.R;
import com.boztalay.puppyframeuid.configuration.editalbum.EditAlbumActivity;
import com.boztalay.puppyframeuid.persistence.Album;
import com.boztalay.puppyframeuid.persistence.PuppyFramePersistenceManager;
import com.boztalay.puppyframeuid.widget.WidgetUpdater;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

public class AlbumsActivity extends Activity implements AdapterView.OnItemClickListener {
	private static final int EDIT_ALBUM_ACTIVITY_EDIT_REQUEST_CODE = 1;
    private static final int EDIT_ALBUM_ACTIVITY_ADD_REQUEST_CODE = 2;
	private static final int FADE_DURATION_MILLIS = 75;
	
	private PuppyFramePersistenceManager persistenceManager;
	private Album currentAlbum;

    private AlbumsAdapter albumsAdapter;

    private int appWidgetId;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_albums);

        Log.d("PuppyFrame", "AlbumsActivity: onCreate called");

		persistenceManager = new PuppyFramePersistenceManager(this);
        appWidgetId = getAppWidgetId();

        Log.d("PuppyFrame", "AlbumsActivity: Showing preferences for AppWidgetId: " + appWidgetId);

        initializeUniversalImageLoader();
        setUpViewsAndTitle();
        prepareResult();
	}

    private void initializeUniversalImageLoader() {
        Log.d("PuppyFrame", "AlbumsActivity: Initializing the universal image loader");

        DisplayImageOptions displayOptions = new DisplayImageOptions.Builder()
                .cacheInMemory()
                .displayer(new FadeInBitmapDisplayer(FADE_DURATION_MILLIS))
                .showImageForEmptyUri(R.drawable.missing_picture_default)
                .showImageOnFail(R.drawable.missing_picture_default)
                .build();

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
                .defaultDisplayImageOptions(displayOptions)
                .build();

        ImageLoader.getInstance().init(config);
    }
	
	private void setUpViewsAndTitle() {
        ActionBar actionBar = getActionBar();
        if(actionBar != null) {
            actionBar.setTitle(getString(R.string.albums_title));
        }

        View currentAlbumView = findViewById(R.id.current_album);

		if(persistenceManager.getAlbumIds().size() == 0) {
            Log.d("PuppyFrame", "AlbumsActivity: No albums found, setting the screen up for no albums");
			setUpViewsForNoAlbums(currentAlbumView);
		} else {
            Log.d("PuppyFrame", "AlbumsActivity: Found albums, setting up the screen to display them");
			setUpViewsForAlbums(currentAlbumView);
		}

        currentAlbumView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startEditAlbumActivity();
            }
        });

        GridView albumsGrid = (GridView) findViewById(R.id.albums_grid);
        albumsAdapter = new AlbumsAdapter(this, appWidgetId);
        albumsGrid.setAdapter(albumsAdapter);
        albumsGrid.setOnItemClickListener(this);
	}
	
	private void setUpViewsForNoAlbums(View currentAlbumView) {
		currentAlbum = null;
		
		ImageView currentAlbumThumbnail = (ImageView)currentAlbumView.findViewById(R.id.album_thumbnail);
		currentAlbumThumbnail.setImageResource(R.drawable.missing_picture_default);
		
		TextView currentAlbumTitle = (TextView)currentAlbumView.findViewById(R.id.album_title);
		currentAlbumTitle.setText("Make an album!");
	}
	
	private void startEditAlbumActivity() {
		Intent editAlbumIntent = new Intent(AlbumsActivity.this, EditAlbumActivity.class);
        editAlbumIntent.putExtra(EditAlbumActivity.APP_WIDGET_ID_KEY, appWidgetId);

		if(currentAlbum != null) {
            Log.d("PuppyFrame", "AlbumsActivity: Starting the Edit activity to edit an album");

			editAlbumIntent.putExtra(EditAlbumActivity.ALBUM_ID_KEY, currentAlbum.getId());
            startActivityForResult(editAlbumIntent, EDIT_ALBUM_ACTIVITY_EDIT_REQUEST_CODE);
		} else {
            Log.d("PuppyFrame", "AlbumsActivity: Starting the Edit activity to create an album");

            startActivityForResult(editAlbumIntent, EDIT_ALBUM_ACTIVITY_ADD_REQUEST_CODE);
        }
	}

    private void startEditAlbumActivityForNewAlbum() {
        Log.d("PuppyFrame", "AlbumsActivity: Starting the Edit activity to create an album");

        Intent editAlbumIntent = new Intent(AlbumsActivity.this, EditAlbumActivity.class);
        startActivityForResult(editAlbumIntent, EDIT_ALBUM_ACTIVITY_ADD_REQUEST_CODE);
    }

    private void setUpViewsForAlbums() {
        View currentAlbumView = findViewById(R.id.current_album);
        setUpViewsForAlbums(currentAlbumView);
    }
	
	private void setUpViewsForAlbums(View currentAlbumView) {
		String currentAlbumId = persistenceManager.getCurrentAlbumIdForAppWidgetId(appWidgetId);
        if(currentAlbumId == null) {
            Log.d("PuppyFrame", "AlbumsActivity: Couldn't find an album associated with this AppWidgetId, setting it to the default album");

            currentAlbum = persistenceManager.getDefaultAlbum();
            persistenceManager.setCurrentAlbumForAppWidgetId(currentAlbum, appWidgetId);
        } else {
            Log.d("PuppyFrame", "AlbumsActivity: Found an album associated with this AppWidgetId: " + currentAlbumId);

            currentAlbum = persistenceManager.getAlbumWithId(currentAlbumId);
        }

        ImageView currentAlbumThumbnail = (ImageView)currentAlbumView.findViewById(R.id.album_thumbnail);
        ImageLoader.getInstance().displayImage(currentAlbum.getThumbnailPath(), currentAlbumThumbnail);

        TextView currentAlbumTitle = (TextView)currentAlbumView.findViewById(R.id.album_title);
        currentAlbumTitle.setText(currentAlbum.getTitle());
	}

    private int getAppWidgetId() {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if(extras == null) {
            throw new RuntimeException("Couldn't find the widget id!");
        }

        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if(appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            throw new RuntimeException("Couldn't find the widget id!");
        }

        return appWidgetId;
    }

	private void prepareResult() {
		Intent configurationResult = createConfigurationResultIntent(appWidgetId);
		setResult(RESULT_OK, configurationResult);
	}

	private Intent createConfigurationResultIntent(int appWidgetId) {
		Intent configurationResult = new Intent();
		configurationResult.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

		return configurationResult;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.albums_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
		    case R.id.add_new_album_menu_action:
		    	startEditAlbumActivityForNewAlbum();
		        return true;
		    case R.id.settings_menu_action:
		    	return true;
		}
	    return super.onOptionsItemSelected(item);
	}

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        persistenceManager.setCurrentAlbumForAppWidgetId((Album) parent.getAdapter().getItem(position), appWidgetId);

        refreshAndUpdateEverything();
    }

    private void refreshAndUpdateEverything() {
        setUpViewsForAlbums();
        albumsAdapter.refreshAlbums();
        prepareResult();
    }
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK) {
            if(requestCode == EDIT_ALBUM_ACTIVITY_ADD_REQUEST_CODE) {
                Log.d("PuppyFrame", "AlbumsActivity: Just came back from creating an album, refreshing everything");
                refreshAndUpdateEverything();
            } else if(requestCode == EDIT_ALBUM_ACTIVITY_EDIT_REQUEST_CODE) {
                Log.d("PuppyFrame", "AlbumsActivity: Just came back from editing an album, refreshing everything");
                setUpViewsForAlbums();
            }
        }
	}

    @Override
    public void onStop() {
        Log.d("PuppyFrame", "AlbumsActivity: AlbumsActivity is stopping, updating all widgets with their first images");

        WidgetUpdater.updateAllWidgets(this);

        super.onStop();
    }
}
