package com.indiza.smsi;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ShareCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.indiza.smsi.common.Constants;
import com.indiza.smsi.common.DatabaseHelper;
import com.indiza.smsi.databinding.ActivitySettingsBinding;
import com.indiza.smsi.viewModel.MainActivityViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created By: Envy on 22/04/2023
 */
public class SettingActivity extends AppCompatActivity {
    private static final String TAG = InlineActivity.class.getSimpleName();
    private ActivitySettingsBinding mBinding;
    private MainActivityViewModel mViewModel;
    private FloatingActionButton shareButton;
    private Button buyCoffeeButton;
    private Button changeSimButton;
    private Button btnPermSms1;
    private Button btnPermStorage2;
    private Button btnPermContact3;
    private Button btnPermNotif4;
    private FloatingActionButton saveSetting;
    private static TextView text_simChoose;
    private EditText editTextNumberSecond;
    private EditText editTextHMTextAds;
    private EditText editTextNumCourt;
    private EditText editTextSMSC;
    private EditText editTextNBChiffreParNumero;
    private ConstraintLayout constraintLayout;
    public static CharSequence [] simSlots;
    private TextView textViewSmsc;
    final DatabaseHelper helper = new DatabaseHelper(this);
    ContextThemeWrapper gtw = new ContextThemeWrapper(this, R.style.SnackbarColor2);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_settings);
        mViewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);
        initializeViews();
        checkNumberOfSimSlot();
        getGrantedPermissions("com.indiza.smsi");
    }
    List<String> getGrantedPermissions(final String appPackage) {
        List<String> granted = new ArrayList<String>();
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(appPackage, PackageManager.GET_PERMISSIONS);
            for (int i = 0; i < pi.requestedPermissions.length; i++) {
                if ((pi.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                    granted.add(pi.requestedPermissions[i]);
                    if(pi.requestedPermissions[i].equals("android.permission.SEND_SMS")){
                       btnPermSms1.setEnabled(false); btnPermSms1.setText("ACTIVE");
                    }else if(pi.requestedPermissions[i].equals("android.permission.READ_EXTERNAL_STORAGE")){
                        btnPermStorage2.setEnabled(false);btnPermStorage2.setText("ACTIVE");
                    }else if(pi.requestedPermissions[i].equals("android.permission.READ_CONTACTS" )){
                        btnPermContact3.setEnabled(false);btnPermContact3.setText("ACTIVE");
                    }else if(pi.requestedPermissions[i].equals("android.permission.READ_PHONE_STATE")){
                        btnPermNotif4.setEnabled(false);btnPermNotif4.setText("ACTIVE");
                    }
                }
            }
        } catch (Exception e) {
        }
        return granted;
    }

    @Override
    protected void onResume() {
        super.onResume();
        ContextThemeWrapper ctw = new ContextThemeWrapper(this, R.style.CustomSnackbarTheme);
        shareButton.setOnClickListener(view -> onShareButtonClicked());
        btnPermSms1.setOnClickListener(view ->{
            String[] PERMISSIONS = {
                    Manifest.permission.READ_SMS,
                    Manifest.permission.SEND_SMS,
            };
            mViewModel.checkOnePermissions(this,PERMISSIONS);
        });
        changeSimButton.setOnClickListener(view -> {
                if (simSlots.length > 1) {
                    DDialogFragment st = new DDialogFragment();
                    st.show(getSupportFragmentManager(), "Change SIM");
                } else {
                    Snackbar.make(ctw, view, "Only one SIM slot connected, nothing to change", Snackbar.LENGTH_LONG).show();
                }
        });
        saveSetting.setOnClickListener(view -> {
            int latence = Integer.parseInt(editTextNumberSecond.getText().toString());
            int NBRE_MESSAGEPUB = Integer.parseInt(editTextHMTextAds.getText().toString());
            int NBRE_CHIFFRE_BY_NUM = Integer.parseInt(editTextNBChiffreParNumero.getText().toString());
            Constants.SEC_LATENCE_MSG = (latence<0 || latence >15) ? "7" : String.valueOf(latence) ;
            Constants.NBRE_MESSAGE_PUB = (NBRE_MESSAGEPUB<0 || NBRE_MESSAGEPUB >150) ? "50" : String.valueOf(NBRE_MESSAGEPUB);
            Constants.SMSC_ADDRESS = editTextSMSC.getText().toString();
            Constants.NUMERO_COURT = editTextNumCourt.getText().toString();
            Constants.NBRE_CHIFFRE_BY_NUM = (NBRE_CHIFFRE_BY_NUM<0 || NBRE_CHIFFRE_BY_NUM >20) ? 9 : NBRE_CHIFFRE_BY_NUM;
            if(helper.insertSettings()){

                Snackbar.make(gtw, view, "Save Successfull", Snackbar.LENGTH_LONG).show();
            }else{
                Snackbar.make(ctw, view, "Error while saving", Snackbar.LENGTH_LONG).show();
            }
        });
        textViewSmsc.setOnClickListener(view->{
            String url = "https://www.wikipedia.org/wiki/Short_Message_Service_Center";

            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        });

    }
    private int checkNumberOfSimSlot(){
        int subId = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {

            SubscriptionManager subscriptionManager = (SubscriptionManager) getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                mViewModel.checkPermissionsAtRuntime(this);
                return subId;
            }
            List<SubscriptionInfo> subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
             //subId = subscriptionInfoList.get(0).getSubscriptionId();// change index to 1 if you want to get Subscrption Id for slot 1.
             subId = subscriptionInfoList.size();// change index to 1 if you want to get Subscrption Id for slot 1.
            if(subId>1){
                simSlots = new String[2];
                if(Constants.SIM_FOR_SEND == 0){
                    text_simChoose.setText(subscriptionInfoList.get(0).getDisplayName());
                }else if(Constants.SIM_FOR_SEND==1){
                    text_simChoose.setText(subscriptionInfoList.get(1).getDisplayName());
                }else{
                    text_simChoose.setText(subscriptionInfoList.get(0).getDisplayName());
                }
                simSlots[0] = subscriptionInfoList.get(0).getDisplayName();
                simSlots[1] = subscriptionInfoList.get(1).getDisplayName();
            }else{
                simSlots = new String[1];
                simSlots[0] = subscriptionInfoList.get(0).getDisplayName();
                text_simChoose.setText(subscriptionInfoList.get(0).getDisplayName());
            }
        }
        return subId;
    }
    @Override
    protected void onStop() {
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    public static class DDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.action_pick)
                    .setItems(simSlots, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                         if(which==0) {
                             Constants.SIM_FOR_SEND = 0;
                             text_simChoose.setText(simSlots[0]);
                         }else {
                             Constants.SIM_FOR_SEND = 1;
                             text_simChoose.setText(simSlots[1]);
                         }
                        }
                    });
            return builder.create();
        }
    }
    public void initializeViews() {
        Log.e(TAG, "initializeViews: ");
        shareButton = mBinding.shareExcelFloatingButton;
        constraintLayout = mBinding.constraintLayout;
        buyCoffeeButton = mBinding.buttonBuyCoffee;
        changeSimButton = mBinding.buttonChangeSim;
        btnPermSms1 = mBinding.btnPermSms1;
        btnPermStorage2 = mBinding.btnPermStorage2;
        btnPermContact3 = mBinding.btnPermContact3;
        btnPermNotif4 = mBinding.btnPermNotif4;
        editTextHMTextAds = mBinding.editTextHMTextAds;
        editTextNumberSecond = mBinding.editTextNumberSecond;
        text_simChoose = mBinding.textSimChoose;
        saveSetting = mBinding.saveSetting;
        editTextNumCourt = mBinding.editTextNumCourt;
        editTextNBChiffreParNumero = mBinding.editTextNBChiffreParNumero;
        editTextSMSC = mBinding.editTextSMSC;
        textViewSmsc = mBinding.textViewSmsc;
        initData();
    }
    private void initData(){
        helper.DBsettings = helper.getDbSettings(helper.SETTINGS_TABLE_NAME);
        editTextNumberSecond.setText(Constants.SEC_LATENCE_MSG);
        editTextHMTextAds.setText(Constants.NBRE_MESSAGE_PUB);
        editTextSMSC.setText(Constants.SMSC_ADDRESS);
        editTextNumCourt.setText(!Constants.NUMERO_COURT.equals("")? Constants.NUMERO_COURT : "") ;
        editTextNBChiffreParNumero.setText(Constants.NBRE_CHIFFRE_BY_NUM+"");
        //editTextNBChiffreParNumero.setText(Constants.NBRE_CHIFFRE_BY_NUM);
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
    public void switchVisibility(View view, int visibility) {
        view.setVisibility(visibility);
    }

    public void enableUIComponent(View componentName) {
        componentName.setClickable(true);
        componentName.setAlpha(1);
    }
    public void disableUIComponent(View componentName) {
        componentName.setClickable(false);
        componentName.setAlpha((float) 0.4);
    }
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