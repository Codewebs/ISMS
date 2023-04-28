package com.indiza.smsi;

import static com.indiza.smsi.view.adapter.ContactsAdapter.contactsToSend;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ShareCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.indiza.smsi.contract.IMainActivityContract;
import com.indiza.smsi.data.ContactResponse;
import com.indiza.smsi.data.Message;
import com.indiza.smsi.data.response.DataResponse;
import com.indiza.smsi.data.response.StateDefinition;
import com.indiza.smsi.databinding.ActivitySenderBinding;
import com.indiza.smsi.view.adapter.ContactsAdapter;
import com.indiza.smsi.viewModel.MainActivityViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created By: Envy 19/04/2023
 */
public class SenderActivity extends AppCompatActivity implements IMainActivityContract.View,ContactBottomDialog.ItemClickListener {
    private static final String TAG = SenderActivity.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 1;
    private ActivitySenderBinding mBinding;
    private MainActivityViewModel mViewModel;
    private HandlerThread importContactsHandlerThread;
    private HandlerThread sentMessageHandlerThread;
    private Handler contactsHandler;
    private Handler sendHandler;
    private Button importContactsButton;

    private Button sendContactButton;
    private Button btnFragContact;
    private FloatingActionButton shareButton;
    private RecyclerView contactsRecyclerView;
    private Button button_selector;
    private ConstraintLayout constraintLayout;
    private LottieAnimationView lottieAnimationView;
    private LottieAnimationView importLottieView;
    private LottieAnimationView sendSmsLottie;
    private final String NO_DATA_ANIMATION = "no_data.json";
    private final String LOADING_ANIMATION = "loading.json";
    private final String ERROR_ANIMATION = "error.json";
    private final String DONE_ANIMATION = "done.json";
    private final String CANCEL_ANIMATION = "cancel.json";
    public List<ContactResponse> contactsList;
    public static EditText smsEditText;
    public static ProgressBar progressBar_cyclic;
    public static ProgressBar progressBar;
    public static TextView textViewProgress;
    Handler handler = new Handler();
    ContactsAdapter mAdapter;
    /**
     * Observer for getContactsFromCPLiveData
     */
    private final Observer<DataResponse<ContactResponse>> importContactsFromCPObserver = contactResponse ->  {
        Log.e(TAG, "importContactsFromCPObserver onChanged()");
        if (contactResponse.getState() == StateDefinition.State.SUCCESS) {
            setupLottieAnimation(lottieAnimationView, NO_DATA_ANIMATION);
            if (contactResponse.getData().size() > 0) {
                contactsList.clear();
                contactsList.addAll(contactResponse.getData());
                displaySnackBar("Retrieved "+contactsList.size()+" contacts from device.");
                // Disable Import button
                disableUIComponent(importContactsButton);
                setupLottieAnimation(importLottieView, DONE_ANIMATION);
                setupRecyclerView();
            } else {
                displaySnackBar("No contacts found");
                setupLottieAnimation(lottieAnimationView, ERROR_ANIMATION);
            }
        } else if (contactResponse.getState() == StateDefinition.State.ERROR) {
            setupLottieAnimation(lottieAnimationView, ERROR_ANIMATION);
            String errorMessage = (contactResponse.getErrorData().getErrorStatus()
                    + contactResponse.getErrorData().getErrorMessage());
            setupLottieAnimation(importLottieView, CANCEL_ANIMATION);
            displaySnackBar(errorMessage);
        } else {
            setupLottieAnimation(lottieAnimationView, LOADING_ANIMATION);
        }
    };
    /**
     * @param view View (message_icon) that was clicked.
     */
    private final Observer<DataResponse<Message>> SendMessageCPLObserver = messageDataResponse -> {

        if(messageDataResponse.getState() == StateDefinition.State.SUCCESS){
            setupLottieAnimation(lottieAnimationView, NO_DATA_ANIMATION);
            if(messageDataResponse.getData().size()>0){
                displaySnackBar("Sent : "+messageDataResponse.getData().size());
                setupLottieAnimation(importLottieView, DONE_ANIMATION);
            }else{
                displaySnackBar("Nothing sent");
            }
        }else if(messageDataResponse.getState() == StateDefinition.State.ERROR){
            setupLottieAnimation(importLottieView, ERROR_ANIMATION);

            setupLottieAnimation(importLottieView, CANCEL_ANIMATION);
            displaySnackBar(messageDataResponse.getErrorData().getErrorStatus() + messageDataResponse.getErrorData().getErrorMessage());
        }else{
            setupLottieAnimation(importLottieView, LOADING_ANIMATION);
        }

    };

    /**
     * start sending messages Runnable data in a Background HandlerThread
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_sender);

        contactsList = new ArrayList<>();
        mViewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);
        mViewModel.getContactsFromCPLiveData().observe(this, importContactsFromCPObserver);
        mViewModel.sendMessageLiveData().observe(this, SendMessageCPLObserver);
        initializeViews();
        sendContactButton.setEnabled(false);
        importContactsButton.setEnabled(true);
        setupHandlerThreads();
    }
    private final Runnable MessageRunnable = new Runnable() {
        @Override
        public void run() {
            Log.e(TAG, "MessageRunnable run: ");
            mViewModel.initiateSend(smsEditText.getText().toString());
        }
    };
    private final Runnable importContactsRunnable = new Runnable() {
        @Override
        public void run() {
            Log.e(TAG, "importContactsRunnable run: ");
            mViewModel.initiateImport();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        contactsHandler.post(importContactsRunnable);
            btnFragContact.setOnClickListener(view -> showBottomSheet(view));
        shareButton.setOnClickListener(view -> onShareButtonClicked());
        sendContactButton.setOnClickListener(view -> {

            ContextThemeWrapper ctw = new ContextThemeWrapper(this, R.style.CustomSnackbarTheme);
            if(smsEditText.getText().length()<1){
                smsEditText.setBackgroundColor(Color.parseColor("#D81B65"));
                Snackbar.make(ctw, view, "No content in the message areaText", Snackbar.LENGTH_LONG).show();
            }else if(mAdapter.contactsToSend.size()<1){
                Snackbar.make(ctw, view, "No contact to send sms", Snackbar.LENGTH_LONG).show();
            }else if(mAdapter.contactsToSend.size()>0 && smsEditText.getText().length()>0) {
                ExcelsendActivity.StartDialogFragment st = new ExcelsendActivity.StartDialogFragment();
                st.show(getSupportFragmentManager(),"Send Message");
                smsEditText.setEnabled(false);
                contactsRecyclerView.setEnabled(false);
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                switchVisibility(contactsRecyclerView,View.GONE);
                switchVisibility(sendContactButton,View.GONE);
                importContactsButton.setEnabled(false);
                switchVisibility(progressBar_cyclic,View.VISIBLE);
                progressBar.setMax(contactsToSend.size());
                switchVisibility(progressBar,View.VISIBLE);
                switchVisibility(textViewProgress,View.VISIBLE);
                setupLottieAnimation(sendSmsLottie, CANCEL_ANIMATION);
                setupLottieAnimation(importLottieView, NO_DATA_ANIMATION);
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
                                        importContactsButton.setEnabled(true);
                                        sendContactButton.setEnabled(false);
                                        switchVisibility(sendContactButton,View.VISIBLE);
                                        try {Thread.sleep(3000);} catch (InterruptedException e) {e.printStackTrace();}
                                        contactsList.clear();
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

        sendContactButton.setOnClickListener(view -> sendHandler.post(MessageRunnable) );


    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyHandlerThreads();
        mViewModel.getContactsFromCPLiveData().removeObservers(this);
    }

    /**
     * Makes the sms button (message icon) invisible so that it can't be used,
     * and makes the Retry button visible.
     */
    private void disableSmsButton() {
        Toast.makeText(this, R.string.sms_disabled, Toast.LENGTH_LONG).show();
        MaterialButton smsButton = (MaterialButton) findViewById(R.id.send_contact_button);
        smsButton.setVisibility(View.INVISIBLE);
        Button retryButton = (Button) findViewById(R.id.button_retry);
        retryButton.setVisibility(View.VISIBLE);
    }
    /**
     * Makes the sms button (message icon) visible so that it can be used.
     */
    private void enableSmsButton() {
        MaterialButton smsButton = (MaterialButton) findViewById(R.id.send_contact_button);
        smsButton.setVisibility(View.VISIBLE);
    }
    /**
     * Sends an intent to start the activity
     * @param view  View (Retry button) that was clicked.
     */
    public void retryApp(View view) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        startActivity(intent);
    }
    public void showBottomSheet(View view) {
        ContactBottomDialog addPhotoBottomDialogFragment =ContactBottomDialog.newInstance();
        addPhotoBottomDialogFragment.show(getSupportFragmentManager(), ContactBottomDialog.TAG);
    }
    public void selector(View view) {
        ContactsAdapter cta = new ContactsAdapter(contactsList);
        for (int i=0; i<contactsList.size(); i++){
            if(contactsList.get(i).isSelected()){
                cta.selectAll(false);
                cta.notifyDataSetChanged();
            }else{
                cta.selectAll(true);
                cta.notifyDataSetChanged();
            }
            break;
        }
    }
    @Override
    public void initializeViews() {
        Log.e(TAG, "initializeViews: ");
        importContactsButton = mBinding.importContactButton;
        btnFragContact = mBinding.BtnFragContact;
        shareButton = mBinding.shareExcelFloatingButton;
        contactsRecyclerView = mBinding.displayContactsRecyclerView;
        constraintLayout = mBinding.constraintLayout;
        lottieAnimationView = mBinding.lottieAnimationView;
        importLottieView = mBinding.importContactLottie;
        button_selector = mBinding.buttonSelector;
        sendSmsLottie = mBinding.sentSmsLottie;
        sendContactButton = mBinding.sendContactButton;
        smsEditText = mBinding.smsMessage;
        progressBar_cyclic = mBinding.progressBarCyclic;
        progressBar = mBinding.progressBar;
        textViewProgress = mBinding.textViewProgress;
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
        importContactsHandlerThread = new HandlerThread("ImportContactsThread", Process.THREAD_PRIORITY_BACKGROUND);
        importContactsHandlerThread.start();
        contactsHandler = new Handler(importContactsHandlerThread.getLooper());

        sentMessageHandlerThread = new HandlerThread("sentMessageThread", Process.THREAD_PRIORITY_BACKGROUND);
        sentMessageHandlerThread.start();
        sendHandler = new Handler(sentMessageHandlerThread.getLooper());
    }
    @Override
    public void destroyHandlerThreads() {
        Log.e(TAG, "destroyHandlerThreads: ");
        importContactsHandlerThread.quitSafely();
        sentMessageHandlerThread.quitSafely();
  //      readExcelDataHandlerThread.quitSafely();
    }
    /**
     * Importing contacts Runnable to parse data in a Background HandlerThread
     */

    @Override
    public void onReadFromExcelButtonClicked() {

    }
    public void onShareButtonClicked() {
        Log.e(TAG, "onShareButtonClicked: ");
        Uri fileUri = mViewModel.initiateSharing();

        if (fileUri == null) {
            displaySnackBar("Generate Excel before sharing");
        } else {
            launchShareFileIntent(fileUri);
        }
    }
    @Override
    public void switchVisibility(View view, int visibility) {
        view.setVisibility(visibility);
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
        switchVisibility(button_selector, View.VISIBLE);
        ContactsAdapter mAdapter = new ContactsAdapter(contactsList);
        contactsRecyclerView.setHasFixedSize(true);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        contactsRecyclerView.setAdapter(mAdapter);
    }
    @Override
    public void displaySnackBar(String message) {
        Snackbar.make(constraintLayout, message, BaseTransientBottomBar.LENGTH_SHORT).show();
    }
    /**
     * Method: Launch Share file screen
     */
    private void launchShareFileIntent(Uri uri) {
        Intent intent = ShareCompat.IntentBuilder.from(this)
                .setType("application/pdf").setStream(uri).setChooserTitle("Select application to share file")
                .createChooserIntent().addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }
    @Override
    public void onItemClick(String item) {
    }
}