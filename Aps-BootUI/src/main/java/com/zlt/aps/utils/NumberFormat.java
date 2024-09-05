package com.zlt.aps.utils;

import com.ruoyi.common.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service("nf")
public class NumberFormat {

    /**
     * 如果浮点是个整数，那么去掉浮点后面的0（例如一个浮点为12.0，那么页面展示需要把。0去掉，直接展示12）
     * @param value
     * @return
     */
    public String stripZeros(Object value) {
        if(value == null) {
            return "0";
        }
        return new BigDecimal(String.valueOf(value)).stripTrailingZeros().toPlainString();
    }

    /**
     * 如果浮点是个整数，那么去掉浮点后面的0（例如一个浮点为12.0，那么页面展示需要把。0去掉，直接展示12）
     * @param value
     * @return
     */
    public String stripZerosThenNull(Object value) {
        if(value == null) {
            return null;
        }
        return new BigDecimal(String.valueOf(value)).stripTrailingZeros().toPlainString();
    }
}
