package letshangllc.idscanner;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.mlkit.vision.barcode.common.Barcode;

/**
 * Structured driver's license fields returned from {@link ScanActivity}.
 */
public final class DlScanResult {
    public static final String EXTRA_FIRST_NAME = "dl_first_name";
    public static final String EXTRA_MIDDLE_NAME = "dl_middle_name";
    public static final String EXTRA_LAST_NAME = "dl_last_name";
    public static final String EXTRA_BIRTHDATE = "dl_birthdate";
    public static final String EXTRA_EXPIRATION = "dl_expiration";
    public static final String EXTRA_ADDRESS = "dl_address";
    public static final String EXTRA_CITY = "dl_city";
    public static final String EXTRA_JURISDICTION = "dl_jurisdiction";
    public static final String EXTRA_RAW = "dl_raw";

    public String firstName = "";
    public String middleName = "";
    public String lastName = "";
    public String birthdate = "";
    public String expirationDate = "";
    public String address1 = "";
    public String city = "";
    public String jurisdictionCode = "";
    public String rawBarcode = "";

    @NonNull
    public static DlScanResult fromMlKit(@NonNull Barcode.DriverLicense license,
                                         @Nullable String raw) {
        DlScanResult result = new DlScanResult();
        result.firstName = nullToEmpty(license.getFirstName());
        result.middleName = nullToEmpty(license.getMiddleName());
        result.lastName = nullToEmpty(license.getLastName());
        result.birthdate = AamvaParser.formatDate(license.getBirthDate());
        result.expirationDate = AamvaParser.formatDate(license.getExpiryDate());
        result.address1 = nullToEmpty(license.getAddressStreet());
        result.city = nullToEmpty(license.getAddressCity());
        result.jurisdictionCode = nullToEmpty(license.getAddressState());
        result.rawBarcode = nullToEmpty(raw);
        return result;
    }

    @NonNull
    public static DlScanResult fromAamva(@NonNull AamvaParser.Result parsed,
                                         @Nullable String raw) {
        DlScanResult result = new DlScanResult();
        result.firstName = nullToEmpty(parsed.firstName);
        result.middleName = nullToEmpty(parsed.middleName);
        result.lastName = nullToEmpty(parsed.lastName);
        result.birthdate = nullToEmpty(parsed.birthdate);
        result.expirationDate = nullToEmpty(parsed.expirationDate);
        result.address1 = nullToEmpty(parsed.address1);
        result.city = nullToEmpty(parsed.city);
        result.jurisdictionCode = nullToEmpty(parsed.jurisdictionCode);
        result.rawBarcode = nullToEmpty(raw);
        return result;
    }

    public void writeToIntent(@NonNull Intent intent) {
        intent.putExtra(EXTRA_FIRST_NAME, firstName);
        intent.putExtra(EXTRA_MIDDLE_NAME, middleName);
        intent.putExtra(EXTRA_LAST_NAME, lastName);
        intent.putExtra(EXTRA_BIRTHDATE, birthdate);
        intent.putExtra(EXTRA_EXPIRATION, expirationDate);
        intent.putExtra(EXTRA_ADDRESS, address1);
        intent.putExtra(EXTRA_CITY, city);
        intent.putExtra(EXTRA_JURISDICTION, jurisdictionCode);
        intent.putExtra(EXTRA_RAW, rawBarcode);
    }

    @Nullable
    public static DlScanResult fromIntent(@Nullable Intent intent) {
        if (intent == null) {
            return null;
        }
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return null;
        }
        DlScanResult result = new DlScanResult();
        result.firstName = extras.getString(EXTRA_FIRST_NAME, "");
        result.middleName = extras.getString(EXTRA_MIDDLE_NAME, "");
        result.lastName = extras.getString(EXTRA_LAST_NAME, "");
        result.birthdate = extras.getString(EXTRA_BIRTHDATE, "");
        result.expirationDate = extras.getString(EXTRA_EXPIRATION, "");
        result.address1 = extras.getString(EXTRA_ADDRESS, "");
        result.city = extras.getString(EXTRA_CITY, "");
        result.jurisdictionCode = extras.getString(EXTRA_JURISDICTION, "");
        result.rawBarcode = extras.getString(EXTRA_RAW, "");
        return result;
    }

    @NonNull
    private static String nullToEmpty(@Nullable String value) {
        return value == null ? "" : value.trim();
    }
}
