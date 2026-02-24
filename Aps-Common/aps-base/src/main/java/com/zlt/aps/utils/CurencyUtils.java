package com.zlt.aps.utils;

/**
 * 货币工具


 */
public class CurencyUtils {
    /**
     * 获取币种对应符号，没有则返回币种本身码值
     * @return
     */
    public static String getCurencyStr4Sale(String currency){
        if ("USD".equals(currency)) {
            return "US$";
        }else if ("CNY".equals(currency)) {
            return "¥";
        }else if ("EUR".equals(currency)) {
            return "EUR€";
        }else if ("VND".equals(currency)) {
            return "VND";
        }
        return currency;
    }
    /**
     * 获取币种对应符号，没有则返回币种本身码值
     * @return
     */
    public static String getCurencyStr4Sale1(String currency){
        if ("USD".equals(currency)) {
            return "USD$";
        }else if ("CNY".equals(currency)) {
            return "¥";
        }else if ("EUR".equals(currency)) {
            return "EUR€";
        }else if ("VND".equals(currency)) {
            return "VND";
        }
        return currency;
    }

    /**
     * 获取币种对应符号，没有则返回币种本身码值
     *
     * @return
     */
    public static String getCurencyStr(String currency) {
        if ("USD".equals(currency)) {
            return "$";
        } else if ("CNY".equals(currency)) {
            return "¥";
        } else if ("EUR".equals(currency)) {
            return "€";
        } else if ("VND".equals(currency)) {
            return "₫";
        }
        return currency;
    }
}
