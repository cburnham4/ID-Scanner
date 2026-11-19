package letshangllc.idscanner;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AamvaParserTest {

    private static final String SAMPLE_AAMVA =
            "@\n"
                    + "\n"
                    + "ANSI 636000090002DL00410278ZV03190008DLDAQD1234567\n"
                    + "DCSDOE\n"
                    + "DACJOHN\n"
                    + "DADJAMES\n"
                    + "DBB01151990\n"
                    + "DBA01152030\n"
                    + "DAG123 MAIN ST\n"
                    + "DAIANYTOWN\n"
                    + "DAJCA\n"
                    + "DAK90210\n";

    @Test
    public void parsesUsAamvaFields() {
        AamvaParser.Result result = AamvaParser.parse(SAMPLE_AAMVA);
        assertNotNull(result);
        assertEquals("JOHN", result.firstName);
        assertEquals("JAMES", result.middleName);
        assertEquals("DOE", result.lastName);
        assertEquals("01/15/1990", result.birthdate);
        assertEquals("01/15/2030", result.expirationDate);
        assertEquals("123 MAIN ST", result.address1);
        assertEquals("ANYTOWN", result.city);
        assertEquals("CA", result.jurisdictionCode);
        assertEquals("D1234567", result.documentNumber);
    }

    @Test
    public void formatsUsAndCanadianDates() {
        assertEquals("01/15/1990", AamvaParser.formatDate("01151990"));
        assertEquals("01/15/1990", AamvaParser.formatDate("19900115"));
        assertEquals("01/15/1990", AamvaParser.formatDate("01/15/1990"));
    }

    @Test
    public void returnsNullForEmptyOrUnrelated() {
        assertNull(AamvaParser.parse(null));
        assertNull(AamvaParser.parse(""));
        assertNull(AamvaParser.parse("just some random text"));
    }

    @Test
    public void addition_isCorrect() {
        // Keep a trivial smoke test for the module
        assertEquals(4, 2 + 2);
    }
}
