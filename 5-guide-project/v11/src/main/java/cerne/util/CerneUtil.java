package cerne.util;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/** Utility class being dismantled: no sun.* since the jump to 11; the rest goes away by session 4. */
public final class CerneUtil {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private CerneUtil() {
    }

    public static String formatDate(Date date) {
        return DATE_FORMAT.format(date);
    }

    /** DateTimeFormatter is immutable and thread-safe; it coexists with the Date version until that one goes away. */
    public static String formatDate(LocalDate date) {
        return DAY_MONTH_YEAR.format(date);
    }

    public static String formatDateTime(Date date) {
        return DATE_TIME_FORMAT.format(date);
    }

    public static Date parseDate(String text) throws Exception {
        return DATE_FORMAT.parse(text);
    }

    /** Builds a date at midnight in the JVM time zone (month from 1 to 12). */
    public static Date date(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month - 1, day);
        return calendar.getTime();
    }

    public static Date dateTime(int year, int month, int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month - 1, day, hour, minute);
        return calendar.getTime();
    }

    public static Date addDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }

    public static boolean isValidCpf(String cpf) {
        if (cpf == null) {
            return false;
        }
        String digits = cpf.replaceAll("\\D", "");
        if (digits.length() != 11) {
            return false;
        }
        boolean allEqual = true;
        for (int i = 1; i < 11; i++) {
            if (digits.charAt(i) != digits.charAt(0)) {
                allEqual = false;
            }
        }
        if (allEqual) {
            return false;
        }
        for (int position = 9; position <= 10; position++) {
            int sum = 0;
            for (int i = 0; i < position; i++) {
                sum += (digits.charAt(i) - '0') * (position + 1 - i);
            }
            int digit = (sum * 10) % 11;
            if (digit == 10) {
                digit = 0;
            }
            if (digit != digits.charAt(position) - '0') {
                return false;
            }
        }
        return true;
    }

    public static String formatCpf(String cpf) {
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "."
                + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    public static String formatAmount(BigDecimal amount) {
        return "R$ " + amount.setScale(2, BigDecimal.ROUND_HALF_EVEN).toString().replace(".", ",");
    }

    /** Duplicated fee rule (the "official" one is in FeeCalculator, with different numbers). */
    public static BigDecimal calculateFee(String transferType, BigDecimal amount) {
        if ("INTERNAL".equals(transferType)) {
            return BigDecimal.ZERO;
        } else if ("BETWEEN_CREDIT_UNIONS".equals(transferType)) {
            return new BigDecimal("1.50");
        } else if ("OTHER_BANK".equals(transferType)) {
            return amount.multiply(new BigDecimal("0.005")).max(new BigDecimal("3.00"));
        }
        return BigDecimal.ZERO;
    }

    /** Business hours: from 8am to 6pm, in the JVM time zone. */
    public static boolean isBusinessHours(Date instant) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(instant);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        return hour >= 8 && hour < 18;
    }

    public static String generateTransferIdentifier() {
        return "TRF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /** Signature of the reconciliation files. BASE64Encoder broke the line every 76 characters; getEncoder does not. */
    public static String encodeBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static void log(String message) {
        System.out.println(DATE_TIME_FORMAT.format(new Date()) + " " + message);
    }

    // ... in the real system, 1,700 more lines: branch validation, balance calculation,
    // property reading, hand-built XML, currency conversions etc.
}
