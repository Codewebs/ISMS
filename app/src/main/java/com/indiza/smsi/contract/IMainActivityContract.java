package com.indiza.smsi.contract;

import android.app.Activity;
import android.net.Uri;

import com.airbnb.lottie.LottieAnimationView;
import com.indiza.smsi.data.ContactResponse;

import java.util.List;

/**
 * Contract to be implemented by MainActivity (View) and MainActivityViewModel (ViewModel)
 */
public interface IMainActivityContract {

    // View
    interface View {
        void initializeViews();
        void setupLottieAnimation(LottieAnimationView animationView, String animationName);
        void setupHandlerThreads();
        void destroyHandlerThreads();
        void onReadFromExcelButtonClicked();
        void switchVisibility(android.view.View view, int visibility);
        void enableUIComponent(android.view.View componentName);
        void disableUIComponent(android.view.View componentName);
        void setupRecyclerView();
        void displaySnackBar(String message);


    }

    // View-Model
    interface ViewModel {
        void initiateImport();
        void initiateExport(List<ContactResponse> dataList);
        void initiateRead(Activity activity);
        void initiateSend(String message);
        Uri initiateSharing();
    }
}
