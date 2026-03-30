package com.zlt.aps.common;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtils;

import java.math.BigDecimal;
import java.util.Date;

public class CommonUtils {

    /**
     * 计算一个时间段的产能
     * @param durationSec
     * @param lhTime
     * @param moldNum
     * @return
     */
    public static int calcPeriodCapacity(BigDecimal durationSec, BigDecimal lhTime, int moldNum) {
        BigDecimal periodCapacity = BigDecimalUtils.div(durationSec,lhTime,2,true,BigDecimal.ROUND_DOWN);
        int capacity = periodCapacity.intValue();
        capacity = capacity * moldNum;
        if (moldNum == 2 && capacity % 2!=0){
            capacity = capacity+1;
        }
        return capacity;
    }

    /**
     * 判断夏季
     * @param summerStartDay 夏季开始日期
     * @param winterStartDay 冬季开始日期
     * @return
     */
    public static boolean isSummerSeason(String summerStartDay,String winterStartDay){
        boolean isSummer = false;
        Date nowDate = new Date();
        //1.判断夏季
        int iYear = DateUtils.getYear(nowDate);
        String strNowDate = DateUtils.dateTime();
        if (StringUtils.isNotEmpty(summerStartDay)){
            String strSummerStartDay = iYear+""+summerStartDay;
            if (strNowDate.compareTo(strSummerStartDay)>=0){
                isSummer = true;
            }
        }
        //2.判断冬季
        if (StringUtils.isNotEmpty(winterStartDay)){
            String strWinterStartDay = iYear+""+winterStartDay;
            if (strNowDate.compareTo(strWinterStartDay)>=0){
                isSummer = false;
            }
        }
        return isSummer;
    }
}
