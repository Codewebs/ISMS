package com.indiza.smsi;

import static com.indiza.smsi.view.adapter.ContactsAdapter.contactsToSend;

import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ShareCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.indiza.smsi.common.Constants;
import com.indiza.smsi.common.GetFile;
import com.indiza.smsi.contract.IMainActivityContract;
import com.indiza.smsi.data.ContactResponse;
import com.indiza.smsi.data.response.DataResponse;
import com.indiza.smsi.data.response.StateDefinition;
import com.indiza.smsi.databinding.ActivityExcelsendBinding;
import com.indiza.smsi.view.adapter.ContactsAdapter;
import com.indiza.smsi.viewModel.MainActivityViewModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created By: Ranit Raj Ganguly on 15/04/2021
 */
public class ExcelsendActivity extends AppCompatActivity implements IMainActivityContract.View {
    private static final String TAG = ExcelsendActivity.class.getSimpleName();
    private ActivityExcelsendBinding mBinding;
    private static MainActivityViewModel mViewModel;
    private HandlerThread readExcelDataHandlerThread;
    private Handler readExcelHandler;
    private Button readExcelButton;
    private FloatingActionButton shareButton;
    private RecyclerView contactsRecyclerView;
    private ConstraintLayout constraintLayout;
    private LottieAnimationView lottieAnimationView;
    private LottieAnimationView readLottieView;
    private LottieAnimationView sendSmsLottie;
    private Button sendContactButton;
    private final String NO_DATA_ANIMATION = "no_data.json";
    private final String LOADING_ANIMATION = "loading.json";
    private final String ERROR_ANIMATION = "error.json";
    private final String DONE_ANIMATION = "done.json";
    private final String CANCEL_ANIMATION = "cancel.json";
    private static Handler sendHandler;
    private HandlerThread sentMessageHandlerThread;
    private HandlerThread progressHandlerTread;
    private List<ContactResponse> contactsList;
    private List<ContactResponse> importedExcelContactsList;
    public static EditText smsEditText;
    public static ProgressBar progressBar_cyclic;
    public static ProgressBar progressBar;
    public static TextView textViewProgress;


    long startTime = 0;
    Handler handler = new Handler();
    ContactsAdapter mAdapter;
    /**
     * Observer for readContactsFromExcelLiveData
     */
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            //timerTextView.setText(String.format("%d:%02d", minutes, seconds));

            handler.postDelayed(this, 500);
        }
    };
    private final Observer<DataResponse<ContactResponse>> readExcelDataObserver = dataResponse -> {
        Log.e(TAG, "readExcelDataObserver onChanged()");

        if (dataResponse.getState() == StateDefinition.State.SUCCESS) {

            if (dataResponse.getData().size() > 0) {
                importedExcelContactsList.clear();
                importedExcelContactsList.addAll(dataResponse.getData());
                displaySnackBar("Fetched "+importedExcelContactsList.size()+" contacts from Excel.");

                // Disable Read button
                disableUIComponent(readExcelButton);
                setupLottieAnimation(readLottieView, DONE_ANIMATION);

                setupRecyclerView();
            } else {
                displaySnackBar("No contacts found");
                setupLottieAnimation(lottieAnimationView, ERROR_ANIMATION);
            }

        } else if (dataResponse.getState() == StateDefinition.State.ERROR) {
            setupLottieAnimation(lottieAnimationView, ERROR_ANIMATION);

            String errorMessage = (dataResponse.getErrorData().getErrorStatus()
                    + dataResponse.getErrorData().getErrorMessage());

            setupLottieAnimation(readLottieView, CANCEL_ANIMATION);
            displaySnackBar(errorMessage);
        } else {
            setupLottieAnimation(lottieAnimationView, LOADING_ANIMATION);
        }
    };
    public static final Runnable MessageRunnable = new Runnable() {
        @Override
        public void run() {
            Log.e(TAG, "MessageRunnable run: ");
            mViewModel.initiateSend(smsEditText.getText().toString());
        }
    };
    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(timerRunnable);
    }
    /**
     * Read Excel data runnable
     */
    private final Runnable readExcelDataRunnable = new Runnable() {
        @Override
        public void run() {
            Log.e(TAG, "readExcelDataRunnable run: ");
            mViewModel.initiateRead(ExcelsendActivity.this);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_excelsend);

        contactsList = new ArrayList<>();
        importedExcelContactsList = new ArrayList<>();
        mViewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);
        mViewModel.readContactsFromExcelLiveData().observe(this, readExcelDataObserver);
        initializeViews();
        sendContactButton.setEnabled(false);
        switchVisibility(progressBar,View.GONE);
        switchVisibility(textViewProgress,View.GONE);
        setupHandlerThreads();
    }
    @Override
    protected void onResume() {
        super.onResume();
        readExcelButton.setOnClickListener(view -> onReadFromExcelButtonClicked());
        sendContactButton.setOnClickListener(view -> {
            ContextThemeWrapper ctw = new ContextThemeWrapper(this, R.style.CustomSnackbarTheme);
            if(smsEditText.getText().length()<1){
                smsEditText.setBackgroundColor(Color.parseColor("#D81B65"));
                Snackbar.make(ctw, view, "No content in the message areaText", Snackbar.LENGTH_LONG).show();
            }else if(mAdapter.contactsToSend.size()<1){
                Snackbar.make(ctw, view, "No contact to send sms", Snackbar.LENGTH_LONG).show();
            }else if(mAdapter.contactsToSend.size()>0 && smsEditText.getText().length()>0) {
                StartDialogFragment st = new StartDialogFragment();
                st.show(getSupportFragmentManager(),"Send Message");
                smsEditText.setEnabled(false);
                contactsRecyclerView.setEnabled(false);
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                switchVisibility(contactsRecyclerView,View.GONE);
                switchVisibility(sendContactButton,View.GONE);
                readExcelButton.setEnabled(false);
                switchVisibility(progressBar_cyclic,View.VISIBLE);
                progressBar.setMax(contactsToSend.size());
                switchVisibility(progressBar,View.VISIBLE);
                switchVisibility(textViewProgress,View.VISIBLE);
                setupLottieAnimation(sendSmsLottie, CANCEL_ANIMATION);
                setupLottieAnimation(readLottieView, NO_DATA_ANIMATION);
                new Thread(new Runnable() {
                    public void run() {
                        while (MainActivityViewModel.progress < contactsToSend.size()) {
                            handler.post(new Runnable() {
                                public void run() {
                                    setupLottieAnimation(sendSmsLottie, DONE_ANIMATION);
                                    progressBar.setProgress(MainActivityViewModel.progress+1);
                                    if(MainActivityViewModel.progress==progressBar.getMax()-1){
                                        textViewProgress.setText("All messages sent");
                                        switchVisibility(progressBar_cyclic,View.GONE);
                                        try {Thread.sleep(5000);} catch (InterruptedException e) {e.printStackTrace();}
                                        readExcelButton.setEnabled(true);
                                        sendContactButton.setEnabled(false);
                                        switchVisibility(sendContactButton,View.VISIBLE);
                                        try {Thread.sleep(3000);} catch (InterruptedException e) {e.printStackTrace();}
                                        importedExcelContactsList.clear();
                                        setupRecyclerView();
                                    }else{
                                        textViewProgress.setText(MainActivityViewModel.progress+"/"+contactsToSend.size());
                                        // setupLottieAnimation(sendSmsLottie, NO_DATA_ANIMATION);
                                    }
                                }
                            });
                            try {
                                // Sleep for 200 milliseconds.
                                    Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
        });
        smsEditText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                smsEditText.setBackgroundColor(Color.parseColor("#FFFFFF"));
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

    }

    public static class StartDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.dialog_start_send)
                    .setPositiveButton(R.string.start, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            sendHandler.post(MessageRunnable);
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
    }
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.chanel_notif_excel);
            String description = getString(R.string.chanel_Desc_excel);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("1", name, importance);
            channel.setDescription(description);
            // Register the channel with the system. You can't change the importance
            // or other notification behaviors after this.
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyHandlerThreads();
        mViewModel.getContactsFromCPLiveData().removeObservers(this);
        mViewModel.isExcelGeneratedLiveData().removeObservers(this);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        Context context = getBaseContext();
        if(requestCode == MainActivityViewModel.PICK_PDF_FILE && resultCode == RESULT_OK){
            if (data != null){
                Log.d("Excel Picker", data.getDataString());
                Uri uri = data.getData();
                String filepath;
                GetFile gfle = new GetFile();
                File file = new File(String.valueOf(gfle.GetFile(context,uri)));

                Cursor cursor = getContentResolver().query(uri,null,null,null,null);
                if (cursor==null){
                    filepath= uri.getPath();
                }else{
                    cursor.moveToFirst();
                    int index = cursor.getColumnIndex("_display_name");
                    filepath = cursor.getString(index);
                    cursor.close();
                }
                FileInputStream fileInputStream = null;
                try {
                    fileInputStream = new FileInputStream(file);
                } catch (IOException e) {
                    Log.e(TAG, "Error Reading Exception: ", e);
                }
               Constants.EXCEL_FILE_NAME = filepath;
                sendContactButton.setEnabled(true);
               mViewModel.parseFile(fileInputStream,context);
            }
        }
    }
    @Override
    public void initializeViews() {
        Log.e(TAG, "initializeViews: ");
        readExcelButton = mBinding.readExcelDataButton;
        shareButton = mBinding.shareExcelFloatingButton;
        contactsRecyclerView = mBinding.displayContactsRecyclerView;
        constraintLayout = mBinding.constraintLayout;
        lottieAnimationView = mBinding.lottieAnimationView;
        readLottieView = mBinding.readContactLottie;
        sendSmsLottie = mBinding.sentSmsLottie;
        sendContactButton = mBinding.sendContactButton;
        smsEditText = mBinding.smsMessage;
        progressBar_cyclic = mBinding.progressBarCyclic;
        progressBar = mBinding.progressBar;
        textViewProgress = mBinding.textViewProgress;

        //disableUIComponent(readExcelButton);
        setupLottieAnimation(lottieAnimationView, NO_DATA_ANIMATION);
    }

    @Override
    public void setupLottieAnimation(LottieAnimationView lottieView, String animationName) {
        if (lottieView.isAnimating()) {
            lottieView.cancelAnimation();
        }
        lottieView.setAnimation(animationName);
        lottieView.playAnimation();
    }

    @Override
    public void setupHandlerThreads() {
        Log.e(TAG, "setupHandlerThreads: ");
        // Read Excel handler thread
        readExcelDataHandlerThread = new HandlerThread("ReadExcelHandlerThread",
                Process.THREAD_PRIORITY_BACKGROUND);
        readExcelDataHandlerThread.start();
        readExcelHandler = new Handler(readExcelDataHandlerThread.getLooper());

        sentMessageHandlerThread = new HandlerThread("sentMessageThread", Process.THREAD_PRIORITY_BACKGROUND);
        sentMessageHandlerThread.start();
        sendHandler = new Handler(sentMessageHandlerThread.getLooper());

        progressHandlerTread = new HandlerThread("progressHandlerTread", Process.THREAD_PRIORITY_BACKGROUND);
        progressHandlerTread.start();
        sendHandler = new Handler(progressHandlerTread.getLooper());
    }

    @Override
    public void destroyHandlerThreads() {
        Log.e(TAG, "destroyHandlerThreads: ");
        readExcelDataHandlerThread.quitSafely();
        sentMessageHandlerThread.quitSafely();
    }
    @Override
    public void onReadFromExcelButtonClicked() {
        Log.e(TAG, "onReadFromExcelButtonClicked: ");
        readExcelHandler.post(readExcelDataRunnable);
    }
    @Override
    public void switchVisibility(View view, int visibility) {
        view.setVisibility(visibility);
    }
    public void switchClicable(View view, boolean enable){
        view.setEnabled(enable);
    }
    @Override
    public void enableUIComponent(View componentName) {
        componentName.setClickable(true);
        componentName.setAlpha(1);
    }

    @Override
    public void disableUIComponent(View componentName) {
        componentName.setClickable(false);
        componentName.setAlpha((float) 0.4);
    }

    @Override
    public void setupRecyclerView() {
        Log.e(TAG, "setupRecyclerView: ");
        switchVisibility(lottieAnimationView, View.GONE);
        switchVisibility(contactsRecyclerView, View.VISIBLE);
        mAdapter = new ContactsAdapter(importedExcelContactsList);
        contactsRecyclerView.setHasFixedSize(true);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        contactsRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void displaySnackBar(String message) {
        Snackbar.make(constraintLayout, message, BaseTransientBottomBar.LENGTH_SHORT)
                .show();
    }
    /**
     * Method: Show Alert Dialog when User denies permission permanently
     */
    private void inflateAlertDialog(boolean isTrue) {
        if (isTrue) {
            // Inflate Alert Dialog
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Permissions Mandatory")
                    .setMessage("Kindly enable all permissions through Settings")
                    .setPositiveButton("OKAY", (dialogInterface, i) -> {
                        launchAppSettings();
                        dialogInterface.dismiss();
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    /**
     * Method: Launch App-Settings Screen
     */
    private void launchAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, Constants.REQUEST_PERMISSION_SETTING);
    }

    /**
     * Method: Launch Share file screen
     */
    private void launchShareFileIntent(Uri uri) {
        Intent intent = ShareCompat.IntentBuilder.from(this)
                .setType("application/pdf")
                .setStream(uri)
                .setChooserTitle("Select application to share file")
                .createChooserIntent()
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(intent);
    }
}