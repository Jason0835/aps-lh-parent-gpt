package com.zlt.aps.utils;

/**
 * 数字转字符串
 */
public class NumberToWordsConverter {

    private static final String[] units = {
            "", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "NINE", "TEN",
            "ELEVEN", "TWELVE", "THIRTEEN", "FOURTEEN", "FIFTEEN", "SIXTEEN", "SEVENTEEN", "EIGHTEEN", "NINETEEN"
    };

    private static final String[] tens = {
            "", "", "TWENTY", "THIRTY", "FORTY", "FIFTY", "SIXTY", "SEVENTY", "EIGHTY", "NINETY"
    };

    private static final String[] thousands = {
            "", "THOUSAND", "MILLION", "BILLION"
    };

    public static String convert(double amount) {
        if (amount == 0) {
            return "ZERO";
        }

        long dollars = (long) amount;
        int cents = (int) Math.round((amount - dollars) * 100);

        return convertDollars(dollars) + " AND " + convertCents(cents) + " ONLY";
    }

    private static String convertDollars(long number) {
        if (number == 0) {
            return "ZERO DOLLARS";
        }

        String words = "";
        int thousandCount = 0;

        while (number > 0) {
            if (number % 1000 != 0) {
                words = convertLessThanOneThousand((int) (number % 1000)) + " " + thousands[thousandCount] + " " + words;
            }
            number /= 1000;
            thousandCount++;
        }

        return words.trim() + " DOLLARS";
    }

    private static String convertLessThanOneThousand(int number) {
        if (number < 20) {
            return units[number];
        } else if (number < 100) {
            return tens[number / 10] + " " + units[number % 10];
        } else {
            return units[number / 100] + " HUNDRED " + convertLessThanOneThousand(number % 100);
        }
    }

    private static String convertCents(int number) {
        if (number < 20) {
            return units[number] + " CENTS";
        } else {
            return tens[number / 10] + " " + units[number % 10] + " CENTS";
        }
    }

}
