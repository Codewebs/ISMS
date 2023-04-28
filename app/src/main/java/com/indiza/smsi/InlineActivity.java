package com.indiza.smsi;

import static com.indiza.smsi.view.adapter.ContactsAdapter.contactsToSend;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.indiza.smsi.common.Constants;
import com.indiza.smsi.contract.IMainActivityContract;
import com.indiza.smsi.data.ContactResponse;
import com.indiza.smsi.databinding.ActivityInlineBinding;
import com.indiza.smsi.view.adapter.ContactsAdapter;
import com.indiza.smsi.viewModel.MainActivityViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created By: Envy on 24/04/2023
 */
public class InlineActivity extends AppCompatActivity implements IMainActivityContract.View {
    private static final String TAG = InlineActivity.class.getSimpleName();
    private ActivityInlineBinding mBinding;
    private MainActivityViewModel mViewModel;
    private Button btnAddContact;
    private EditText editTextPhone;
    private EditText editTextName;
    private EditText smsEditText;

    private Button sendContactButton;
    private LottieAnimationView inlineLottieView;

    private LottieAnimationView sendSmsLottie;
    private FloatingActionButton shareButton;
    private RecyclerView contactsRecyclerView;
    private ConstraintLayout constraintLayout;
    private LottieAnimationView lottieAnimationView;
    private final String NO_DATA_ANIMATION = "no_data.json";
    private final String LOADING_ANIMATION = "loading.json";
    private final String ERROR_ANIMATION = "error.json";
    private final String DONE_ANIMATION = "done.json";
    private final String CANCEL_ANIMATION = "cancel.json";
    ContactsAdapter mAdapter;
    Handler handler = new Handler();
    public static ProgressBar progressBar_cyclic;
    public static ProgressBar progressBar;
    public static TextView textViewProgress;

    ContextThemeWrapper gtw = new ContextThemeWrapper(this, R.style.SnackbarColor2);
    ContextThemeWrapper ctw = new ContextThemeWrapper(this, R.style.CustomSnackbarTheme);
    private final String[] PERMISSIONS = {
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
   // private List<ContactResponse> contactsList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_inline);
        ContactsAdapter.contactsToSend = new ArrayList<>();
        mViewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);
        initializeViews();
        btnAddContact.setEnabled(false);
        sendContactButton.setEnabled(false);
        setupHandlerThreads();
    }

    @Override
    protected void onResume() {
        super.onResume();
        shareButton.setOnClickListener(view -> onShareButtonClicked());
        btnAddContact.setOnClickListener(view -> addToContactSend());

        editTextPhone.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                if(isPhoneValid(editTextPhone.getText().toString())){
                    editTextPhone.setBackgroundColor(Color.parseColor("#FFFFCC"));
                    btnAddContact.setEnabled(true);
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });

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
                btnAddContact.setEnabled(false);
                switchVisibility(progressBar_cyclic,View.VISIBLE);
                progressBar.setMax(contactsToSend.size());
                switchVisibility(progressBar,View.VISIBLE);
                switchVisibility(textViewProgress,View.VISIBLE);
                setupLottieAnimation(sendSmsLottie, CANCEL_ANIMATION);
                setupLottieAnimation(inlineLottieView, NO_DATA_ANIMATION);
                int max = contactsToSend.size();
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
                                        btnAddContact.setEnabled(true);
                                        sendContactButton.setEnabled(false);
                                        switchVisibility(sendContactButton,View.VISIBLE);
                                        try {Thread.sleep(3000);} catch (InterruptedException e) {e.printStackTrace();}
                                        contactsToSend = new ArrayList<ContactResponse>();
                                    }else{
                                        textViewProgress.setText(MainActivityViewModel.progress+"/"+max);
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

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyHandlerThreads();
    }
    @Override
    public void initializeViews() {
        Log.e(TAG, "initializeViews: ");
        shareButton = mBinding.shareExcelFloatingButton;
        contactsRecyclerView = mBinding.displayContactsRecyclerView;
        constraintLayout = mBinding.constraintLayout;
        btnAddContact = mBinding.btnAddContact;
        editTextName = mBinding.editTextName;
        editTextPhone = mBinding.editTextPhone;
        smsEditText = mBinding.smsEditText;
        lottieAnimationView = mBinding.lottieAnimationView;
        inlineLottieView = mBinding.inlineContactLottie;
        progressBar_cyclic = mBinding.progressBarCyclic;
        progressBar = mBinding.progressBar;
        textViewProgress = mBinding.textViewProgress;
        sendSmsLottie = mBinding.sentSmsLottie;
        sendContactButton = mBinding.sendContactButton;
        setupLottieAnimation(lottieAnimationView, NO_DATA_ANIMATION);
        smsEditText.setEnabled(false);

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
    public void setupHandlerThreads() {}
    @Override
    public void destroyHandlerThreads() {}
    @Override
    public void onReadFromExcelButtonClicked() {}
    public void onShareButtonClicked() {
        Log.e(TAG, "onShareButtonClicked: ");
        Uri fileUri = mViewModel.initiateSharing();

        if (fileUri == null) {
            displaySnackBar("Generate Excel before sharing");
        } else {
            launchShareFileIntent(fileUri);
        }
    }
    public static boolean isNumeric(String value){
        if(value.matches("\\d+(?:\\.\\d+)?")){
            return true;
        }
        return false;
    }
    private boolean isPhoneValid(String phone){
        if (!phone.isEmpty() && !phone.equals("") ) {
            if((phone.length() == Constants.NBRE_CHIFFRE_BY_NUM) && isNumeric(phone)){
                return true;
            }else{
                setupLottieAnimation(inlineLottieView, CANCEL_ANIMATION);
                //displaySnackBar("Check the conformmity of number");
                return false;
            }
        }
        return false;
    }
    public void addToContactSend() {
        try {
            String name = editTextName.getText().toString();
            String phone = editTextPhone.getText().toString();
            List<ContactResponse.PhoneNumber> phoneNumbers = new ArrayList<>();
            phoneNumbers.add(new ContactResponse.PhoneNumber(phone));
            String CSize = String.valueOf(ContactsAdapter.contactsToSend.size() + 1);
            if (isPhoneValid(phone)) {
                ContactsAdapter.contactsToSend.add(new ContactResponse(CSize, name, phoneNumbers));
                setupLottieAnimation(inlineLottieView, DONE_ANIMATION);
                switchVisibility(contactsRecyclerView, View.VISIBLE);
                setupRecyclerView();
                editTextPhone.setText("");
                editTextName.setText("");
                smsEditText.setEnabled(true);
                sendContactButton.setEnabled(true);
            } else {
                setupLottieAnimation(inlineLottieView, CANCEL_ANIMATION);
                displaySnackBar("Error in Number, Cannot add it");
            }
        }catch(Exception ex){
           // ex.printStackTrace();
            displaySnackBar("Error While Adding this contaxt");
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
        ContactsAdapter mAdapter = new ContactsAdapter(ContactsAdapter.contactsToSend);
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