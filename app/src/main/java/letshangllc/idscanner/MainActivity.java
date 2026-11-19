package letshangllc.idscanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import net.idscan.android.dlparser.DLParser;
import net.idscan.android.pdf417scanner.PDF417ScanActivity;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private DLParser parser = null;

    /* Views */
    private TextView tvAge;
    private TextView tvName;
    private TextView tvBirthday;
    private TextView tvExpires;
    private TextView tvAddress;
    private TextView tvCity;
    private LinearLayout cvLinLayout;
    private TextView tvNoData;
    private EditText etAge;

    /* Required Age */
    private int requiredAge = 21;
    private int scannedAge = 0;

    private AdsHelper adsHelper;
    private Timer adRefreshTimer;

    private final ActivityResultLauncher<String> requestCameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchScanner();
                } else {
                    Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<Intent> scanLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                int resultCode = result.getResultCode();
                Intent data = result.getData();

                if (resultCode == PDF417ScanActivity.RESULT_OK) {
                    if (data != null) {
                        tvName.setText(data.getStringExtra(PDF417ScanActivity.BARCODE_DATA));
                        parseData();
                    }
                } else if (resultCode == PDF417ScanActivity.ERROR_INVALID_CAMERA_NUMBER) {
                    tvName.setText(R.string.err_invalid_camera_number);
                    Toast.makeText(this, R.string.err_invalid_camera_number, Toast.LENGTH_SHORT).show();
                } else if (resultCode == PDF417ScanActivity.ERROR_CAMERA_NOT_AVAILABLE) {
                    tvName.setText(R.string.err_camera_not_available);
                    Toast.makeText(this, R.string.err_camera_not_available, Toast.LENGTH_SHORT).show();
                } else if (resultCode == PDF417ScanActivity.ERROR_INVALID_CAMERA_ACCESS) {
                    tvName.setText(R.string.err_invalid_camera_access);
                    Toast.makeText(this, R.string.err_invalid_camera_access, Toast.LENGTH_SHORT).show();
                } else if (resultCode == PDF417ScanActivity.ERROR_INVALID_LICENSE_KEY) {
                    tvName.setText(R.string.err_invalid_license_key);
                    Toast.makeText(this, R.string.err_invalid_license_key, Toast.LENGTH_SHORT).show();
                } else if (resultCode != RESULT_CANCELED) {
                    Toast.makeText(this, R.string.scan_failed, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.root_layout);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        tvAge = findViewById(R.id.tvAge);
        tvName = findViewById(R.id.tvName);
        tvBirthday = findViewById(R.id.tvBirthday);
        tvExpires = findViewById(R.id.tvExpires);
        tvAddress = findViewById(R.id.tvAddress);
        tvCity = findViewById(R.id.tvCity);
        etAge = findViewById(R.id.etAge);
        cvLinLayout = findViewById(R.id.cvLinLayout);
        tvNoData = findViewById(R.id.tvNoData);

        Button btnScan = findViewById(R.id.btn_scan);
        btnScan.setOnClickListener(view -> startScanWithPermission());

        setupToolbar();
        runAds();
    }

    private void startScanWithPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchScanner();
        } else {
            Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show();
            requestCameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchScanner() {
        Intent i = new Intent(MainActivity.this, PDF417ScanActivity.class);
        i.putExtra(PDF417ScanActivity.EXTRA_LICENSE_KEY, getString(R.string.scanner_key));
        scanLauncher.launch(i);
    }

    private void parseData() {
        parser = new DLParser();
        cvLinLayout.setVisibility(View.VISIBLE);
        tvNoData.setVisibility(View.GONE);
        try {
            parser.setup(getApplicationContext(), getString(R.string.parser_key));
            DLParser.DLResult res = parser.parse(tvName.getText().toString().getBytes("UTF8"));

            tvName.setText(String.format(Locale.getDefault(), "%s %s %s",
                    res.firstName, res.middleName, res.lastName));

            if (!isLegal(res.birthdate)) {
                Toast.makeText(this, R.string.not_of_age, Toast.LENGTH_SHORT).show();
                tvBirthday.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                tvAge.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            } else {
                tvAge.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light));
                tvBirthday.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light));
            }

            if (scannedAge == 0) {
                tvAge.setText(R.string.not_available);
            } else {
                tvAge.setText(String.format(Locale.getDefault(), "%d", scannedAge));
            }

            if (isExpired(res.expirationDate)) {
                Toast.makeText(this, R.string.expired, Toast.LENGTH_SHORT).show();
                tvExpires.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            } else {
                tvExpires.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light));
            }

            tvBirthday.setText(res.birthdate);
            tvExpires.setText(res.expirationDate);
            tvAddress.setText(res.address1);
            tvCity.setText(res.city + ", " + res.jurisdictionCode);

        } catch (DLParser.DLParserException e) {
            Log.e(TAG, "Parser error", e);
            Toast.makeText(this, R.string.parse_failed, Toast.LENGTH_SHORT).show();
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Encoding error", e);
        }
    }

    private boolean isLegal(String birthdate) {
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

        try {
            Date date = formatter.parse(birthdate);
            if (date == null) {
                return true;
            }
            Date dateNow = new Date();

            this.scannedAge = getDiffYears(date, dateNow);
            if (scannedAge >= requiredAge) {
                Log.i(TAG, "LEGAL");
                return true;
            } else {
                return false;
            }
        } catch (ParseException e) {
            Log.e(TAG, "Age parse error", e);
            Toast.makeText(this, R.string.age_undetermined, Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    private boolean isExpired(String expirationDate) {
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

        try {
            Date date = formatter.parse(expirationDate);
            if (date == null) {
                return true;
            }
            Date dateNow = new Date();
            if (dateNow.after(date)) {
                Log.i(TAG, "Expired");
                return true;
            } else {
                return false;
            }
        } catch (ParseException e) {
            Log.e(TAG, "Expiration parse error", e);
            Toast.makeText(this, R.string.expiration_undetermined, Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    public static int getDiffYears(Date first, Date last) {
        Calendar a = Calendar.getInstance();
        a.setTime(first);
        Calendar b = Calendar.getInstance();
        b.setTime(last);
        int diff = b.get(Calendar.YEAR) - a.get(Calendar.YEAR);
        if (a.get(Calendar.MONTH) > b.get(Calendar.MONTH) ||
                (a.get(Calendar.MONTH) == b.get(Calendar.MONTH) && a.get(Calendar.DATE) > b.get(Calendar.DATE))) {
            diff--;
        }
        return diff;
    }

    public void subOneOnClick(View view) {
        String ageString = etAge.getText().toString().trim();
        if (ageString.isEmpty()) {
            Toast.makeText(this, R.string.required_age_blank, Toast.LENGTH_SHORT).show();
        } else {
            try {
                int age = Integer.parseInt(ageString);
                etAge.setText(String.format(Locale.getDefault(), "%d", --age));
                this.requiredAge = age;
            } catch (Exception e) {
                Log.e(TAG, "Age decrement error", e);
            }
        }
    }

    public void addOneOnClick(View view) {
        String ageString = etAge.getText().toString().trim();
        if (ageString.isEmpty()) {
            Toast.makeText(this, R.string.required_age_blank, Toast.LENGTH_SHORT).show();
        } else {
            try {
                int age = Integer.parseInt(ageString);
                etAge.setText(String.format(Locale.getDefault(), "%d", ++age));
                this.requiredAge = age;
            } catch (Exception e) {
                Log.e(TAG, "Age increment error", e);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (adsHelper != null) {
            adsHelper.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        if (adRefreshTimer != null) {
            adRefreshTimer.cancel();
            adRefreshTimer = null;
        }
        if (adsHelper != null) {
            adsHelper.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adsHelper != null) {
            adsHelper.onResume();
        }
    }

    public void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.app_name);
    }

    public void runAds() {
        adsHelper = new AdsHelper(
                getWindow().getDecorView(),
                getString(R.string.admob_banner_unit_id),
                this
        );
        adsHelper.setUpAds();

        int delay = 1000;
        int period = getResources().getInteger(R.integer.ad_refresh_rate);
        adRefreshTimer = new Timer();
        adRefreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (adsHelper != null) {
                    adsHelper.refreshAd();
                }
            }
        }, delay, period);
    }
}
