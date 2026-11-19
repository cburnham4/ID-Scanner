package letshangllc.idscanner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * On-device AAMVA / ANSI DL/ID barcode parser (fallback when ML Kit's structured
 * {@code DriverLicense} fields are unavailable). Parses element IDs from the
 * PDF417 raw string without any native libraries or license keys.
 */
public final class AamvaParser {

    public static final class Result {
        public String firstName = "";
        public String middleName = "";
        public String lastName = "";
        public String birthdate = "";
        public String expirationDate = "";
        public String address1 = "";
        public String city = "";
        public String jurisdictionCode = "";
        public String postalCode = "";
        public String documentNumber = "";
    }

    /** Known DL/ID data element IDs used by this app. */
    private static final Pattern KNOWN_ELEMENT = Pattern.compile(
            "(DCS|DAB|DAC|DCT|DAD|DBB|DBA|DAG|DAL|DAI|DAN|DAJ|DAO|DAK|DAP|DAQ)"
                    + "([^\\r\\n]*?)(?=(?:DCS|DAB|DAC|DCT|DAD|DBB|DBA|DAG|DAL|DAI|DAN|DAJ|DAO|DAK|DAP|DAQ)|$)",
            Pattern.DOTALL
    );

    private static final Pattern LINE_ELEMENT =
            Pattern.compile("^([A-Z]{3})(.*)$");

    private AamvaParser() {
    }

    /**
     * @return parsed result, or {@code null} if the payload does not look like AAMVA data
     */
    @Nullable
    public static Result parse(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }

        String text = raw.trim();
        Map<String, String> elements = new HashMap<>();

        // Prefer line-oriented parsing (standard AAMVA layout).
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Matcher lineMatcher = LINE_ELEMENT.matcher(trimmed);
            if (lineMatcher.matches()) {
                String id = lineMatcher.group(1);
                String value = lineMatcher.group(2) != null ? lineMatcher.group(2).trim() : "";
                if (isKnownId(id) && !value.isEmpty()) {
                    elements.put(id, value);
                }
            }

            // Header lines often embed the first element after "DL", e.g. "...DLDAQ12345"
            int dlIndex = trimmed.indexOf("DL");
            if (dlIndex >= 0 && dlIndex + 2 < trimmed.length()) {
                String afterDl = trimmed.substring(dlIndex + 2);
                Matcher embedded = KNOWN_ELEMENT.matcher(afterDl);
                while (embedded.find()) {
                    String id = embedded.group(1);
                    String value = embedded.group(2) != null ? embedded.group(2).trim() : "";
                    // Strip trailing non-data if the match ran long
                    if (!value.isEmpty()) {
                        elements.putIfAbsent(id, value);
                    }
                }
            }
        }

        // Whole-document sweep for any missed known IDs (compact single-line payloads)
        if (elements.size() < 3) {
            Matcher matcher = KNOWN_ELEMENT.matcher(text);
            while (matcher.find()) {
                String id = matcher.group(1);
                String value = matcher.group(2) != null ? matcher.group(2).trim() : "";
                if (!value.isEmpty()) {
                    elements.putIfAbsent(id, value);
                }
            }
        }

        if (elements.isEmpty()) {
            return null;
        }

        Result result = new Result();
        result.lastName = firstNonEmpty(elements, "DCS", "DAB");
        result.firstName = firstNonEmpty(elements, "DAC", "DCT");
        result.middleName = firstNonEmpty(elements, "DAD");
        result.birthdate = formatDate(firstNonEmpty(elements, "DBB"));
        result.expirationDate = formatDate(firstNonEmpty(elements, "DBA"));
        result.address1 = firstNonEmpty(elements, "DAG", "DAL");
        result.city = firstNonEmpty(elements, "DAI", "DAN");
        result.jurisdictionCode = firstNonEmpty(elements, "DAJ", "DAO");
        result.postalCode = firstNonEmpty(elements, "DAK", "DAP");
        result.documentNumber = firstNonEmpty(elements, "DAQ");

        if (isBlank(result.firstName)
                && isBlank(result.lastName)
                && isBlank(result.birthdate)
                && isBlank(result.documentNumber)) {
            return null;
        }

        return result;
    }

    /**
     * Normalize ML Kit / AAMVA dates (often {@code MMDDYYYY} or {@code YYYYMMDD}) to
     * {@code MM/dd/yyyy} for display and age checks.
     */
    @NonNull
    public static String formatDate(@Nullable String rawDate) {
        if (rawDate == null) {
            return "";
        }
        String digits = rawDate.replaceAll("[^0-9]", "");
        if (digits.length() != 8) {
            if (rawDate.contains("/") || rawDate.contains("-")) {
                return rawDate.trim();
            }
            return rawDate.trim();
        }

        int prefix = Integer.parseInt(digits.substring(0, 4));
        String mm;
        String dd;
        String yyyy;
        if (prefix >= 1900 && prefix <= 2100) {
            yyyy = digits.substring(0, 4);
            mm = digits.substring(4, 6);
            dd = digits.substring(6, 8);
        } else {
            mm = digits.substring(0, 2);
            dd = digits.substring(2, 4);
            yyyy = digits.substring(4, 8);
        }
        return String.format(Locale.US, "%s/%s/%s", mm, dd, yyyy);
    }

    private static boolean isKnownId(String id) {
        switch (id) {
            case "DCS":
            case "DAB":
            case "DAC":
            case "DCT":
            case "DAD":
            case "DBB":
            case "DBA":
            case "DAG":
            case "DAL":
            case "DAI":
            case "DAN":
            case "DAJ":
            case "DAO":
            case "DAK":
            case "DAP":
            case "DAQ":
                return true;
            default:
                return false;
        }
    }

    @NonNull
    private static String firstNonEmpty(Map<String, String> elements, String... keys) {
        for (String key : keys) {
            String value = elements.get(key);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
