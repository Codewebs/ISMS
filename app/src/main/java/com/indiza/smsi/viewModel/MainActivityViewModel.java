package com.indiza.smsi.viewModel;

import static com.indiza.smsi.view.adapter.ContactsAdapter.contactsToSend;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.indiza.smsi.ExcelsendActivity;
import com.indiza.smsi.common.Constants;
import com.indiza.smsi.common.ExcelUtils;
import com.indiza.smsi.common.FileShareUtils;
import com.indiza.smsi.contract.IMainActivityContract;
import com.indiza.smsi.data.ContactResponse;
import com.indiza.smsi.data.Message;
import com.indiza.smsi.data.response.BooleanResponse;
import com.indiza.smsi.data.response.DataResponse;
import com.indiza.smsi.data.response.ErrorData;
import com.indiza.smsi.data.response.StateDefinition;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;


public class MainActivityViewModel extends AndroidViewModel
        implements IMainActivityContract.ViewModel {
    private static final String TAG = MainActivityViewModel.class.getSimpleName();
    private final List<ContactResponse> contactResponseList;
    private final List<Message> messageList;
    private final List<Message> failedList;
    private List<ContactResponse> parsedExcelDataList;
    private final MutableLiveData<DataResponse<ContactResponse>> contactsMLD;
    public final MutableLiveData<DataResponse<Message>> messagesMLD;
    private final MutableLiveData<BooleanResponse> generateExcelMLD;
    private final MutableLiveData<DataResponse<ContactResponse>> excelContactsDataMLD;
    private DataResponse<ContactResponse> response;;
    public static int progress = 0;
    public static final int PICK_PDF_FILE = 2;

    private Object[] failed = new Object[3];
    // Constructor
    public MainActivityViewModel(@NonNull Application application) {
        super(application);
        messageList = new ArrayList<>();
        failedList = new ArrayList<>();
        contactResponseList = new ArrayList<>();
        parsedExcelDataList = new ArrayList<>();
        contactsMLD = new MutableLiveData<>();
        messagesMLD = new MutableLiveData<>();
        generateExcelMLD = new MutableLiveData<>();
        excelContactsDataMLD = new MutableLiveData<>();
    }
    @Override
    public void initiateImport() {
        Log.e(TAG, "initiateImport: ");
        DataResponse<ContactResponse> response;
        // Initially setting Status as 'LOADING' and set/post value to contactsMLD
        response = new DataResponse(StateDefinition.State.LOADING, null, null);
        setContactsMLD(response);

        queryContactsContentProvider();

        Log.e(TAG, "initiateImport SIZE: " + contactResponseList.size());

        if (contactResponseList.size() > 0) {
            response = new DataResponse(StateDefinition.State.SUCCESS, contactResponseList, null);
        } else {
            response = new DataResponse(StateDefinition.State.ERROR, null,
                    new ErrorData(StateDefinition.ErrorState.INTERNAL_ERROR, "No Contacts queried"));
        }
        setContactsMLD(response);
    }
    public void initiateSend(String msg){
        DataResponse<Message>  result;
        result = new DataResponse(StateDefinition.State.LOADING,null,null);
        setMeessagesMLD(result);
        if(ePureur(contactsToSend).size()>0){
            // when message sent and when delivered, or set to null.
            PendingIntent sentIntent = null, deliveryIntent = null;
            //checkForSmsPermission();
            SmsManager smsManager = SmsManager.getDefault();
            String scAddress = null;
            List<ContactResponse> listepurer = new ArrayList<>();
            listepurer = ePureur(contactsToSend);
            progress = 0;
//            for (int i = 0; i< listepurer.size(); i++){
            while(progress<listepurer.size()){
                String name  = listepurer.get(progress).getName();
                String message = msg.replace("{name}",name );
                try{
                    String usrphone = listepurer.get(progress).getPhoneNumberList().get(0).getNumber();
                    smsManager.sendTextMessage(usrphone, scAddress, message, sentIntent, deliveryIntent);
                    Toast.makeText(getApplication().getApplicationContext(), "SMS Sent Successfully", Toast.LENGTH_SHORT).show();
                    //ExcelsendActivity.smsEditText.setText(message);
                    Thread.sleep(Integer.parseInt(Constants.SEC_LATENCE_MSG)*1000);

                }catch(Exception ex){
                    failed[0] = failedList.size()+1;
                    failed[1] = listepurer.get(progress);
                    failed[2] = message;
                   Toast.makeText(getApplication().getApplicationContext(), "SMS to "+listepurer.get(progress).getPhoneNumberList().get(0).getNumber()+" Failed to Send, Please try again", Toast.LENGTH_SHORT).show();
                }
                progress++;
            }
           // progress = 0;
        }else{
            Toast.makeText(getApplication().getApplicationContext(), "Error No contact found", Toast.LENGTH_SHORT).show();
        }
        if(failedList.size()<0){
            result = new DataResponse(StateDefinition.State.SUCCESS, messageList, null);
        }else{
            result = new DataResponse(StateDefinition.State.ERROR, failedList,
                    new ErrorData(StateDefinition.ErrorState.INTERNAL_ERROR, " There is " + failedList.size() +" Messages wich has not been sent"));
        }
        setMeessagesMLD(result);
    }
    @Override
    public void initiateExport(List<ContactResponse> dataList) {
        Log.e(TAG, "initiateExport: ");
        BooleanResponse response;
        // Initially setting Status as 'LOADING' and set/post value to generateExcelMLD
        response = new BooleanResponse(StateDefinition.State.LOADING, false, null);
        setGenerateExcelMLD(response);
        boolean isExcelGenerated = ExcelUtils.exportDataIntoWorkbook(getApplication(),
                Constants.EXCEL_FILE_NAME, dataList);
        if (isExcelGenerated) {
            response = new BooleanResponse(StateDefinition.State.SUCCESS, true, null);
        } else {
            response = new BooleanResponse(StateDefinition.State.ERROR, false,
                    new ErrorData(StateDefinition.ErrorState.EXCEL_GENERATION_ERROR, "Excel not generated"));
        }
        setGenerateExcelMLD(response);
    }
    @Override
    public void initiateRead(Activity activity) {
        Log.e(TAG, "initiateRead: ");
        // Initially setting Status as 'LOADING' and set/post value to excelContactsDataMLD
        response = new DataResponse(StateDefinition.State.LOADING, null, null);
        readExcelMLD(response);
        openFile(activity);

    }
    public void parseFile(FileInputStream fileInputStream,Context file) {
        parsedExcelDataList = ExcelUtils.readFromExcelWorkbook(fileInputStream,file,
                Constants.EXCEL_FILE_NAME);
        if (parsedExcelDataList.size() > 0) {
            response = new DataResponse(StateDefinition.State.SUCCESS, parsedExcelDataList, null);
        } else {
            response = new DataResponse(StateDefinition.State.ERROR, null,
                    new ErrorData(StateDefinition.ErrorState.FILE_NOT_FOUND_ERROR, "Error reading data from excel"));
        }
        readExcelMLD(response);
    }
    private void openFile( Activity activity) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        String[] mimeTypes = {"application/vnd.ms-excel","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent.setType(mimeTypes.length == 1 ? mimeTypes[0] : "*/*");
            if (mimeTypes.length > 0) {
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            }
        } else {
            String mimeTypesStr = "";
            for (String mimeType : mimeTypes) {
                mimeTypesStr += mimeType + "|";
            }
            intent.setType(mimeTypesStr.substring(0,mimeTypesStr.length() - 1));
        }
        activity.startActivityForResult(intent, PICK_PDF_FILE);
    }
    @Override
    public Uri initiateSharing() {
        Log.e(TAG, "initiateSharing: ");
        return FileShareUtils.accessFile(getApplication(), Constants.EXCEL_FILE_NAME);
    }
    /**
     * Live Data for Querying of Content Provider
     */
    public LiveData<DataResponse<ContactResponse>> getContactsFromCPLiveData() {
        return contactsMLD;
    }
    public LiveData<DataResponse<Message>> sendMessageLiveData() {
        return messagesMLD;
    }
    /**
     * Live Data for status of Excel Workbook Generation
     */
    public LiveData<BooleanResponse> isExcelGeneratedLiveData() {
        return generateExcelMLD;
    }
    /**
     * Live Data for Reading Excel Workbook data
     */
    public LiveData<DataResponse<ContactResponse>> readContactsFromExcelLiveData() {
        return excelContactsDataMLD;
    }
    /**
     * Set/ Post Value for Contacts MLD
     */
    private void setContactsMLD(DataResponse<ContactResponse> response) {
        if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
            contactsMLD.setValue(response);
        } else {
            contactsMLD.postValue(response);
        }
    }
    private void setMeessagesMLD(DataResponse<Message> response) {
        if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
            messagesMLD.setValue(response);
        } else {
            messagesMLD.postValue(response);
        }
    }
    /**
     * Set/ Post Value for Generate Excel MLD
     */
    private void setGenerateExcelMLD(BooleanResponse response) {
        if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
            generateExcelMLD.setValue(response);
        } else {
            generateExcelMLD.postValue(response);
        }
    }
    /**
     * Set/ Post Value for Read Excel MLD
     */
    private void readExcelMLD(DataResponse<ContactResponse> response) {
        if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
            excelContactsDataMLD.setValue(response);
        } else {
            excelContactsDataMLD.postValue(response);
        }
    }
    /**
     * Method: Queries Contacts Content Provider to access contacts
     */
    private void queryContactsContentProvider() {
        contactResponseList.clear();
        ContentResolver contentResolver = getApplication().getContentResolver();
        Cursor contactCursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);
        if (contactCursor != null && contactCursor.getCount() > 0) {
            // Iterate
            while (contactCursor.moveToNext()) {
                String id = contactCursor.getString(contactCursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                String name = contactCursor.getString(contactCursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));

                if (name != null) {
                    // Check if current contact has phone numbers
                    if (contactCursor.getInt(contactCursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                        List<ContactResponse.PhoneNumber> phoneNumberList = new ArrayList<>();
                        // Query
                        Cursor phoneNumberCursor = contentResolver
                                .query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                        null,
                                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id},
                                        null);
                        // Iterate
                        while (phoneNumberCursor.moveToNext()) {
                            String phoneNumber = phoneNumberCursor.getString(phoneNumberCursor
                                    .getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));

                            phoneNumberList.add(new ContactResponse.PhoneNumber(phoneNumber));
                        }
                        contactResponseList.add(new ContactResponse(id, name, phoneNumberList));
                    }
                }
            }
        }
    }
    public static List<ContactResponse> ePureur(List<ContactResponse> contacts){
        List<ContactResponse> contactsToAdd = new ArrayList<>(); List<ContactResponse> contactsToRem = new ArrayList<>();
        for (int i=0;i<contacts.size();i++){
            if(contacts.get(i).getName().isEmpty() || contacts.get(i).getName() ==""){
                contacts.get(i).setName("*");
            }
            if(contacts.get(i).getPhoneNumberList().size()>0){
                    if (Constants.NBRE_CHIFFRE_BY_NUM >0){
                        if(contacts.get(i).getPhoneNumberList().get(0).getNumber().toString().length() == Constants.NBRE_CHIFFRE_BY_NUM){
                            contactsToAdd.add(contacts.get(i));
                        }else{
                            contactsToRem.add(contacts.get(i));
                        }
                    }else{
                        contactsToAdd.add(contacts.get(i));
                    }
                    //contactsToAdd.add(contacts.get(i));
                   // Toast.makeText(null, contacts.get(i).getName()+" dont have number in slot 1", Toast.LENGTH_SHORT).show();
            }else{
                contactsToRem.add(contacts.get(i));
                //Toast.makeText(getApplication().getApplicationContext(), " Contacts list is empty or invalid", Toast.LENGTH_SHORT).show();
            }
        }
        return contactsToAdd;
    }
    public boolean checkPermissionsAtRuntime(Activity view) {
        String[] PERMISSIONS = {
                //Manifest.permission.POST_NOTIFICATIONS;
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        Log.e(TAG, "checkPermissionsAtRuntime: ---%%---------%%---------%%-------%%---");
        ActivityCompat.requestPermissions(view, PERMISSIONS, Constants.REQUEST_PERMISSION_ALL);
        return true;
    }
    public boolean checkOnePermissions(Activity view, String[] permission) {
        Log.e(TAG, "checkPermissionsAtRuntime: ---%%---------%%---------%%-------%%---");
        ActivityCompat.requestPermissions(view, permission, Constants.REQUEST_PERMISSION_ALL);
        return true;
    }
    private void createNotificationChannel(String chanelID, String chanelName,String chanelDesc, Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(chanelID, chanelName, importance);
            channel.setDescription(chanelDesc);
            // Register the channel with the system. You can't change the importance
            // or other notification behaviors after this.
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
