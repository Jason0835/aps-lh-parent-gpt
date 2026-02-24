package com.tlt.aps.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 数字转成越南语大写
 */
public class NumberToVietnameseWords {
    private static final String[] vietnameseUnits = {"", "nghìn", "triệu", "tỷ"};  // 千、百万、亿
    private static final String[] vietnameseNumbers = {"không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"}; // 0~9


    public static String convert(long number) {
        if (number == 0) {
            return vietnameseNumbers[0];
        }
        List<String> words = new ArrayList<>();
        int unitIndex = 0;
        while (number > 0) {
            long part = number % 1000;  //个十百
            number = number / 1000;  //去掉 ”个十百“ 后下次循环的数字
            if (part > 0) {

                List<String> partWords = new ArrayList<>();
                long hundreds = part / 100;  //百位上的数字
                part = part % 100;
                long tens = part / 10;  //十位上的数字
                long units = part % 10;  //个位上的数字


                //百位
                if (hundreds > 0) {
                    partWords.add(vietnameseNumbers[(int) hundreds]);
                    partWords.add("trăm");  //百
                }else if(hundreds==0 && number>0){
                    //百位、十万、亿、千亿上的0 要拼接 零百(không trăm)
                    partWords.add("không trăm");
                }

                //十位
                if (tens > 1) {
                    partWords.add(vietnameseNumbers[(int) tens]);
                    partWords.add("mươi");  //十
                } else if (tens == 1) {
                    partWords.add("mười");  //十
                }else if(tens == 0){
                    if (hundreds>0 || number>0){
                        //十位、万、千万、百亿位上的0 要拼接 lẻ
                        partWords.add("lẻ");
                    }
                }

                //个位
                if (units > 0 && tens!= 1) {
                    partWords.add(vietnameseNumbers[(int) units]);
                }

                //三位数字后的单位
                if (unitIndex > 0 && partWords.size() > 0) {
                    partWords.add(vietnameseUnits[unitIndex]);
                }

                words.addAll(0, partWords);
            }
            unitIndex++;
        }
        words.add("đồng");  //最后拼接单位
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            result.append(word).append(" ");
        }
        return result.toString().trim();
    }

//    public static void main(String[] args) {
//        long number = 255602124L;
        // 255百万,602千,124
        //三位数的起始位上是0的，需要说“零百(không trăm)，也就是百位、十万、亿、千亿“
        //三位数的中间位上是0的，只说“零(lẻ)，也就是十位、万、千万、百亿
//        long number = 1001001001L;
        // 1.001.001.001：một tỷ không trăm lẻ một triệu không trăm lẻ một nghìn không trăm lẻ một đồng
//        System.out.println(convert(number));
//    }


}