package com.indiza.smsi;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.indiza.smsi.common.Constants;
import com.indiza.smsi.viewModel.MainActivityViewModel;

public class HomeActivity extends AppCompatActivity {
    private MainActivityViewModel mViewModel;

    private static final String TAG = HomeActivity.class.getSimpleName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mViewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);
        setContentView(R.layout.activity_home);
        CardView send_byContactCardView= findViewById(R.id.send_contact_button_id);
        CardView send_exceltCardView= findViewById(R.id.send_excel_button_id);
        CardView send_inlineCardView= findViewById(R.id.send_inline_button_id);
        CardView settings_CardView= findViewById(R.id.settings_button);

        send_byContactCardView.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), SenderActivity.class);
            // start the Intent
            startActivity(intent);
        });
        send_exceltCardView.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), ExcelsendActivity.class);
            // start the Intent
            startActivity(intent);
        });
        send_inlineCardView.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), InlineActivity.class);
            startActivity(intent);
        });
        settings_CardView.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), SettingActivity.class);
            startActivity(intent);
        });
        mViewModel.checkPermissionsAtRuntime(HomeActivity.this);
    }


}
