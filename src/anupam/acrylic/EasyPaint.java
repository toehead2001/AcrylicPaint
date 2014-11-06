/*
 * Copyright (C) 2014 Valerio Bozzolan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package anupam.acrylic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("ClickableViewAccessibility")
public class EasyPaint extends GraphicsActivity implements
		ColorPickerDialog.OnColorChangedListener {

	private Paint mPaint;
	private MaskFilter mEmboss;
	private MaskFilter mBlur;
	
	public static int DEFAULT_BRUSH_SIZE = 10;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//it removes the title from the actionbar(more space for icons?)
		//this.getActionBar().setDisplayShowTitleEnabled(false); 
		
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

		//this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION); 
		//it removes the navigation bar, but you can't paint without it and once it is shown again, it doesn't hide again
		
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		setContentView(new MyView(this));

		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setDither(true);
		mPaint.setColor(Color.GREEN);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeWidth(DEFAULT_BRUSH_SIZE);

		mEmboss = new EmbossMaskFilter(new float[] { 1, 1, 1 }, 0.4f, 6, 3.5f);

		mBlur = new BlurMaskFilter(5, BlurMaskFilter.Blur.NORMAL);

		if (isFirstTime()) {

			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle(R.string.app_name);

			alert.setMessage(R.string.app_description);

			alert.setNegativeButton(R.string.continue_fuck,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {

							Toast.makeText(getApplicationContext(),
									R.string.here_is_your_canvas,
									Toast.LENGTH_SHORT).show();

						}
					});

			alert.show();
		} else {
			Toast.makeText(getApplicationContext(),
					R.string.here_is_your_canvas, Toast.LENGTH_SHORT).show();
		}

	}

	public void colorChanged(int color) {
		mPaint.setColor(color);
	}

	public class MyView extends View {

		private Bitmap mBitmap;
		private Canvas mCanvas;
		private Path mPath;
		private Paint mBitmapPaint;

		public MyView(Context c) {
			super(c);

			Display display = getWindowManager().getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			int width = size.x;
			int height = size.y;

			mBitmap = Bitmap.createBitmap(width, height,
					Bitmap.Config.ARGB_8888);
			mCanvas = new Canvas(mBitmap);
			mPath = new Path();
			mBitmapPaint = new Paint(Paint.DITHER_FLAG);
		}

		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
		}

		@Override
		protected void onDraw(Canvas canvas) {
			canvas.drawColor(0xFFFFFFFF);

			canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);

			canvas.drawPath(mPath, mPaint);
		}

		private float mX, mY;
		private static final float TOUCH_TOLERANCE = 4;

		private void touch_start(float x, float y) {
			mPath.reset();
			mPath.moveTo(x, y);
			mX = x;
			mY = y;
		}

		private void touch_move(float x, float y) {
			float dx = Math.abs(x - mX);
			float dy = Math.abs(y - mY);
			if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
				mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
				mX = x;
				mY = y;
			}
		}

		private void touch_up() {
			mPath.lineTo(mX, mY);
			// commit the path to our offscreen
			mCanvas.drawPath(mPath, mPaint);
			// kill this so we don't double draw
			mPath.reset();
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			float x = event.getX();
			float y = event.getY();

			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				touch_start(x, y);
				invalidate();
				break;
			case MotionEvent.ACTION_MOVE:
				touch_move(x, y);
				invalidate();
				break;
			case MotionEvent.ACTION_UP:
				touch_up();
				invalidate();
				break;
			}
			return true;
		}
	}

	private static final int COLOR_MENU_ID = Menu.FIRST;
	private static final int SIZE_MENU_ID = Menu.FIRST + 1;
	private static final int ERASE_MENU_ID = Menu.FIRST + 2;
	private static final int CLEAR_ALL = Menu.FIRST + 3;
	private static final int NORMAL_BRUSH = Menu.FIRST + 4;
	private static final int EMBOSS_MENU_ID = Menu.FIRST + 5;
	private static final int BLUR_MENU_ID = Menu.FIRST + 6;
	private static final int SAVE = Menu.FIRST + 7;
	private static final int SHARE = Menu.FIRST + 8;
	private static final int ABOUT = Menu.FIRST + 9;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, COLOR_MENU_ID, 0, R.string.color).setIcon(R.drawable.color).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, SIZE_MENU_ID, 0, R.string.brush_size).setIcon(R.drawable.size).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, ERASE_MENU_ID, 0, R.string.erase).setIcon(R.drawable.erase).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, CLEAR_ALL, 0, R.string.clear_all).setIcon(R.drawable.clear_all).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, NORMAL_BRUSH, 0, R.string.normal).setIcon(R.drawable.size).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, EMBOSS_MENU_ID, 0, R.string.emboss).setIcon(R.drawable.emboss).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, BLUR_MENU_ID, 0, R.string.blur).setIcon(R.drawable.blur).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, SAVE, 0, R.string.save).setIcon(R.drawable.save).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, SHARE, 0, R.string.share).setIcon(R.drawable.share).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, ABOUT, 0, R.string.about).setIcon(R.drawable.about).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

		/****
		 * Is this the mechanism to extend with filter effects? Intent intent =
		 * new Intent(null, getIntent().getData());
		 * intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
		 * menu.addIntentOptions( Menu.ALTERNATIVE, 0, new ComponentName(this,
		 * NotesList.class), null, intent, 0, null);
		 *****/
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		mPaint.setXfermode(null);
		mPaint.setAlpha(0xFF);

		switch (item.getItemId()) {
		case NORMAL_BRUSH:
			mPaint.setMaskFilter(null);
			return true;
		case COLOR_MENU_ID:
			new ColorPickerDialog(this, this, mPaint.getColor()).show();
			return true;
		case EMBOSS_MENU_ID:
			mPaint.setMaskFilter(mEmboss);
			return true;
		case BLUR_MENU_ID:
			mPaint.setMaskFilter(mBlur);
			return true;
		case SIZE_MENU_ID:
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View layout = inflater.inflate(R.layout.brush,
					(ViewGroup) findViewById(R.id.root));
			AlertDialog.Builder builder = new AlertDialog.Builder(this)
					.setView(layout);
			final AlertDialog alertDialog = builder.create();
			alertDialog.show();
			SeekBar sb = (SeekBar) layout.findViewById(R.id.seekBar1);
			sb.setProgress(getStrokeSize());
			final Button done = (Button) layout.findViewById(R.id.select_size);
			final TextView txt = (TextView) layout
					.findViewById(R.id.size_value);
			txt.setText(String
					.format(getResources().getString(
							R.string.default_size_is), getStrokeSize()));
			sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				public void onProgressChanged(SeekBar seekBar,
						final int progress, boolean fromUser) {
					// Do something here with new value
					mPaint.setStrokeWidth(progress);
					txt.setText(String.format(getResources().getString(R.string.your_selected_size_is), progress));
				}
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
				}
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
				}
			});
			done.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					alertDialog.dismiss();
				}
			});
			return true;
		case ERASE_MENU_ID:
			LayoutInflater inflater_e = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View layout_e = inflater_e.inflate(R.layout.brush,
					(ViewGroup) findViewById(R.id.root));
			AlertDialog.Builder builder_e = new AlertDialog.Builder(this)
					.setView(layout_e);
			final AlertDialog alertDialog_e = builder_e.create();
			alertDialog_e.show();
			SeekBar sb_e = (SeekBar) layout_e.findViewById(R.id.seekBar1);
			sb_e.setProgress(getStrokeSize());
			final Button done_e = (Button) layout_e.findViewById(R.id.select_size);
			final TextView txt_e = (TextView) layout_e
					.findViewById(R.id.size_value);
			txt_e.setText(String
					.format(getResources().getString(
							R.string.default_size_is), getStrokeSize()));
			sb_e.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				public void onProgressChanged(SeekBar seekBar,
						final int progress, boolean fromUser) {
					// Do something here with new value
					mPaint.setStrokeWidth(progress);
					txt_e.setText(String.format(getResources().getString(R.string.your_selected_size_is), progress));
				}
				public void onStartTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
				}
				public void onStopTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
				}
			});
			done_e.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					alertDialog_e.dismiss();
				}
			});
			// mPaint.setColor(bgColor);
			mPaint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
			return true;
		case CLEAR_ALL:
			Intent intent = getIntent();
			overridePendingTransition(0, 0);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			finish();
			overridePendingTransition(0, 0);
			startActivity(intent);
			return true;
		case SAVE:
			takeScreenshot(true);
			break;
		case SHARE:
			File screenshotPath = takeScreenshot(false);
			Intent i = new Intent();
			i.setAction(Intent.ACTION_SEND);
			i.setType("image/png");
			i.putExtra(Intent.EXTRA_SUBJECT,
					getString(anupam.acrylic.R.string.share_title_template));
			i.putExtra(Intent.EXTRA_TEXT,
					getString(anupam.acrylic.R.string.share_text_template));
			i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(screenshotPath));
			try {
				startActivity(Intent.createChooser(i,
						getString(anupam.acrylic.R.string.toolbox_share_title)));
			} catch (android.content.ActivityNotFoundException ex) {
				Toast.makeText(this.getApplicationContext(),
						anupam.acrylic.R.string.no_way_to_share,
						Toast.LENGTH_LONG).show();
			}
			break;
		case ABOUT:
			try {
				AlertDialog.Builder alert = new AlertDialog.Builder(this);

				alert.setTitle(R.string.about);

				alert.setMessage(R.string.app_description);

				alert.setPositiveButton(R.string.ok, null);

				alert.show();
			} catch (Exception e) {
				// TODO: handle exception
			}

		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * This takes the screenshot of the whole screen.
	 * Is this a good thing?
	 */
	private File takeScreenshot(boolean showToast) {
		View v = getWindow().getDecorView();

		v.setDrawingCacheEnabled(true);
		Bitmap cachedBitmap = v.getDrawingCache();
		Bitmap copyBitmap = cachedBitmap.copy(Bitmap.Config.RGB_565, true);
		FileOutputStream output = null;
		File file = null;
		try {
			File path = Places.getScreenshotFolder();
			Calendar cal = Calendar.getInstance();

			file = new File(path,

			cal.get(Calendar.YEAR) + "_" + (1 + cal.get(Calendar.MONTH)) + "_"
					+ cal.get(Calendar.DAY_OF_MONTH) + "_"
					+ cal.get(Calendar.HOUR_OF_DAY) + "_"
					+ cal.get(Calendar.MINUTE) + "_" + cal.get(Calendar.SECOND)
					+ ".png");
			output = new FileOutputStream(file);
			copyBitmap.compress(CompressFormat.PNG, 100, output);
		} catch (FileNotFoundException e) {
			file = null;
			e.printStackTrace();
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}

		if (file != null) {
			if (showToast)
				Toast.makeText(
						getApplicationContext(),
						String.format(
								getResources().getString(
										R.string.saved_your_location_to),
								file.getAbsolutePath()), Toast.LENGTH_LONG)
						.show();
			// sending a broadcast to the media scanner so it will scan the new
			// screenshot.
			Intent requestScan = new Intent(
					Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
			requestScan.setData(Uri.fromFile(file));
			sendBroadcast(requestScan);

			return file;
		} else {
			return null;
		}
	}

	private boolean isFirstTime() {
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		boolean ranBefore = preferences.getBoolean("RanBefore", false);
		if (!ranBefore) {
			// first time
			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean("RanBefore", true);
			editor.commit();
		}
		return !ranBefore;
	}

	private int getStrokeSize() {
		return (int) mPaint.getStrokeWidth();
	}
}