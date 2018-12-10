package edu.cs.cs184.photoapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.zomato.photofilters.imageprocessors.Filter;
import com.zomato.photofilters.imageprocessors.SubFilter;
import com.zomato.photofilters.imageprocessors.subfilters.BrightnessSubFilter;
import com.zomato.photofilters.imageprocessors.subfilters.ContrastSubFilter;
import com.zomato.photofilters.imageprocessors.subfilters.SaturationSubFilter;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static edu.cs.cs184.photoapp.MainActivity.scaleBitmapDown;


// Filter fragment:

public class FilterFragment extends Fragment {

    // TODO: add save functionality
    // TODO: create some interactable to view which feature(s) corresponded and what their percentages were.

    public static final String ARG_PARAM1 = "param1";
    public static final String ARG_PARAM2 = "param2";
    public static final String ARG_PARAM3 = "param3";



    private TextView infoText;

    private String inFeature;
    private double inPercent;
    private byte[] inBitmap;
    private int inIndex;
    private String inFilterName;
    private String savedImageName;

    private String[] features;
    private Double[] certainties;


    // determines reasonable resolutions for the mipmap in order to maximize fidelty and framerate
    final int MIPMAP_MAX_DIMENSION = 1000;
    final int MIPMAP_MIN_DIMENSION = 350;
    final int MIPMAP_STEP = 150;

    private Bitmap originalBitmap;
    private Bitmap mipMap;
    private Bitmap cachedBitmap;

    // save sub filters to a map so we don't compound filters when adding the same type of subfilter
    private Map<String,ArrayList<SubFilter>> filterMap;

    private Filter autoFilter;


    private ImageView imageView;

    // Required empty public constructor
    public FilterFragment() {
    }

    public static FilterFragment newInstance(byte[] param1, int param2, String param3, Object[] feats, Object[] certs) {
        FilterFragment fragment = new FilterFragment();
        Filter test = new Filter();
        Bundle args = new Bundle();
        args.putByteArray(ARG_PARAM1, param1);
        args.putInt(ARG_PARAM2, param2);
        args.putString(ARG_PARAM3, param3);

        // TODO: clean this up
        args.putSerializable("feats", feats);
        args.putSerializable("certs", certs);



        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater,container,savedInstanceState);
        setRetainInstance(true);
        super.onCreate(savedInstanceState);

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_filter, container, false);


        return view;





    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){


        super.onViewCreated(view,savedInstanceState);

        filterMap = new HashMap<>();
        imageView = (ImageView) getView().findViewById(R.id.imageView);
        originalBitmap = Bitmap.createBitmap(10,10,Bitmap.Config.ARGB_8888);


        if (getArguments() != null) {
            Log.e("as", "arguments aren't null");
            inBitmap = getArguments().getByteArray(ARG_PARAM1);
            inIndex = getArguments().getInt(ARG_PARAM2);
            inFilterName = getArguments().getString(ARG_PARAM3);
            Object[] rawFeat = (Object[]) getArguments().getSerializable("feats");
            features = Arrays.copyOf(rawFeat, rawFeat.length, String[].class);
            Object[] rawCert = (Object[]) getArguments().getSerializable("certs");
            certainties = Arrays.copyOf(rawCert, rawCert.length, Double[].class);


            // try to decode the bitmap passed.
            // If it doesn't get passed successfully restart the app, because without the bitmap there is nothing to do.
            try {
                originalBitmap = BitmapFactory.decodeByteArray(inBitmap,0, inBitmap.length);
                imageView.setImageBitmap(originalBitmap);


            } catch(Exception e){Toast.makeText(getActivity().getApplicationContext(), "Error fetching image. Restarting...",Toast.LENGTH_LONG).show();
                Intent i = getActivity().getBaseContext().getPackageManager()
                        .getLaunchIntentForPackage( getActivity(). getBaseContext().getPackageName() );
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }


            // TODO: make a dialog with the features detected on info clicked




        }

        // TODO: potentially make these into an array, or something else more elegant (only if someone wants to put in the effort)


        // TODO: Don't actually do anything for brightness (making sure there isn't confusion)
        final SeekBar brightnessSlider = (SeekBar) getView().findViewById(R.id.seekBar1);
        final TextView brightnessLabel = (TextView) getView().findViewById(R.id.textView1);
        brightnessLabel.setText("Brightness: 0");
        brightnessSlider.setMax(200);
        brightnessSlider.setProgress(100);




        final SeekBar contrastSlider = (SeekBar) getView().findViewById(R.id.seekBar2);
        final TextView contrastLabel = (TextView) getView().findViewById(R.id.textView2);
        contrastLabel.setText("Contrast: 0");
        contrastSlider.setMax(200);
        contrastSlider.setProgress(100);


        final SeekBar saturationSlider = (SeekBar) getView().findViewById(R.id.seekBar3);
        final TextView saturationLabel = (TextView) getView().findViewById(R.id.textView3);
        saturationLabel.setText("Saturation: 0");
        saturationSlider.setMax(200);
        saturationSlider.setProgress(100);

        final Button infoButton = (Button) getView().findViewById(R.id.button);
        final Button button1 = (Button) getView().findViewById(R.id.button1);
        final Button ResetButton = (Button) getView().findViewById(R.id.button2);
        if(features.length < 1) infoButton.setVisibility(View.INVISIBLE);
        final ImageButton saveButton = (ImageButton) getView().findViewById(R.id.saveButton);



        // removed automatic filter from user edits, this way the user edits will edit the filtered picture instead of blending the edits
        autoFilter = CustomFilters.getFilter(inFilterName,FilterSelectorActivity.getContext());

        //addToFilterMap("myfilter",(ArrayList) mFilter.getSubFilters());
        //final Filter mFilter = CustomFilters.getFilter(inFilterName,FilterSelectorActivity.getContext());

        imageView.setImageBitmap(getBitmap());

        updateMipMap();
        
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(StorageHelper.isExternalStorageWritable()){
                    //choose filename
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("Save Image As");
                    final EditText textName = new EditText(getContext());
                    DateFormat date = new SimpleDateFormat("dd-MM-yyy HH:mm:ss z");
                    Date cal = Calendar.getInstance().getTime();
                    textName.setHint("PhotoApp_" + date.format(cal));
                    textName.setInputType(InputType.TYPE_CLASS_TEXT);
                    builder.setView(textName);
                    builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            savedImageName = textName.getText().toString();
                            if(savedImageName.equals(""))
                                savedImageName = "PhotoApp_" + date.format(cal);
                            cachedBitmap =  ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                            String url = StorageHelper.insertImage(getActivity().getContentResolver(), cachedBitmap, savedImageName + ".jpg");
                            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                            File f = new File(url);
                            Uri contentUri = Uri.fromFile(f);
                            mediaScanIntent.setData(contentUri);
                            getActivity().sendBroadcast(mediaScanIntent);
                            Toast.makeText(getContext(),"Image Saved", Toast.LENGTH_SHORT).show();

                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.show();

                }

                else
                    Toast.makeText(getContext(), "Image not saved", Toast.LENGTH_SHORT).show();

            }
        });


        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create(); //Read Update
                if(features.length!=0) {
                    alertDialog.setTitle("Features Applicable to This Filter:");
                    String message = "";
                    for (int i = 0; i < features.length; i++) {
                        DecimalFormat d = new DecimalFormat("0.00");
                        message = message + features[i] + " : " + d.format(certainties[i]) + "% certain \n";
                    }
                    alertDialog.setMessage(message);
                }
                else alertDialog.setTitle("No Applicable Features. \n\n");
                alertDialog.setButton("Continue..", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // here you can add functions
                    }
                });

                alertDialog.show();  //<-- See This!
            }
        });


        button1.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    //Bitmap currentBitmap = originalBitmap.copy( Bitmap.Config.ARGB_8888,true);
                    //imageView.setImageBitmap(getFilter().processFilter(currentBitmap));
                    imageView.setImageBitmap(cachedBitmap);
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    cachedBitmap =  ((BitmapDrawable) imageView.getDrawable()).getBitmap();

                    imageView.setImageBitmap(originalBitmap);
                    //cachedBitmap = getFilter().processFilter(getBitmap());

                }
                return true;
            }


        });

        ResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                brightnessSlider.setProgress(100);
                contrastSlider.setProgress(100);
                saturationSlider.setProgress(100);
                imageView.setImageBitmap(getBitmap());

            }
        });




        brightnessSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int brightness = i-100;
                Log.d("ad","brightness:" + brightness);
                brightnessLabel.setText("Brightness: " + brightness);
                ArrayList<SubFilter> a = new ArrayList<>();
                a.add(new BrightnessSubFilter(brightness));
                //Filter f = new Filter();
                //f.addSubFilter(a.get(0));
                addToFilterMap("brightness",a);
                imageView.setImageBitmap(getFilter().processFilter(getMipMap()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                updateMipMap();
                imageView.setImageBitmap(getBitmap());


            }
        });

        contrastSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                // minimum contrast is .5
                Float contrast = (float) ((i+50) * .01f);
                Float displayNum = (contrast - 1.5f) * 100;
                DecimalFormat d = new DecimalFormat("0");
                contrastLabel.setText("Contrast: "+d.format(displayNum));
                ArrayList<SubFilter> a = new ArrayList<>();
                a.add(new ContrastSubFilter(contrast));
                addToFilterMap("contrast", a);
                //Filter f = new Filter();
                //f.addSubFilter(a.get(0));
                imageView.setImageBitmap(getFilter().processFilter(getMipMap()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                updateMipMap();
                imageView.setImageBitmap(getBitmap());


            }
        });


        saturationSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                Float saturation = (float)(.0f + i * .01f);
                Float displayNum = (saturation - 1.0f) * 100;
                DecimalFormat d = new DecimalFormat("0");
                saturationLabel.setText("Saturation: " + d.format(displayNum));
                ArrayList<SubFilter> a = new ArrayList<>();
                a.add(new SaturationSubFilter(saturation));
                addToFilterMap("saturation",a);
                imageView.setImageBitmap(getFilter().processFilter(getMipMap()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                updateMipMap();
                imageView.setImageBitmap(getBitmap());
            }
        });




    }
    private Bitmap getBitmap(){
        return getFilter().processFilter(originalBitmap.copy(Bitmap.Config.ARGB_8888,true));
    }


    private void updateMipMap(){
        mipMap = scaleBitmapDown(originalBitmap,Math.max( MIPMAP_MAX_DIMENSION-MIPMAP_STEP*getMapSize(),MIPMAP_MIN_DIMENSION));
    }



    private Bitmap getMipMap(){
        return mipMap.copy(Bitmap.Config.ARGB_8888,true);
    }

    private void addToFilterMap(String s, ArrayList<SubFilter> a){
        filterMap.put(s, a);
    }


    private  void removeFromFilterMap(String s)
    {
        filterMap.remove(s);
    }

    private Filter getFilter() {
        Filter filters = new Filter();
        if(autoFilter != null){
            filters.addSubFilters(autoFilter.getSubFilters());
        }
        for( ArrayList<SubFilter> a: filterMap.values())
            filters.addSubFilters( a);
        return filters;
    }

    private int getMapSize(){
        int result = 0;
        for(ArrayList<SubFilter> a: filterMap.values()) result += a.size();
        return  result;
    }

}
