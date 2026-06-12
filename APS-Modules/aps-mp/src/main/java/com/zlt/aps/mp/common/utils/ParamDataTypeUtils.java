package com.zlt.aps.mp.common.utils;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.enums.SysParamDataTypeEnum;
import com.zlt.aps.exception.BusinessException;
import com.zlt.common.utils.PubUtil;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.text.ParseException;

/**
 * 参数配置业务：数据类型校验工具类
 *
 * @author ZLT
 * @date 20260611
 */
@Slf4j
public class ParamDataTypeUtils {
    /**
     * 校验参数-值数据类型
     *
     * @param paramCode  参数编码
     * @param paramName  参数名称
     * @param dataType   数据类型
     * @param paramValue 值
     */
    public static void checkValidParams(String paramCode, String paramName, Integer dataType, String paramValue) {
        //获取数据类型对应的枚举类
        SysParamDataTypeEnum sysParamDataTypeEnum = SysParamDataTypeEnum.getEnumByValue(dataType);
        if (SysParamDataTypeEnum.NUMBER.equals(sysParamDataTypeEnum)) {
            try {
                new BigDecimal(paramValue);
            } catch (NumberFormatException e) {
                throw new BusinessException(String.format("系统参数【1$s %2$s】解析参数值错误.", paramValue, paramName));
            }
        } else if (SysParamDataTypeEnum.INTEGER.equals(sysParamDataTypeEnum)) {

            try {
                Integer.parseInt(paramValue);
            } catch (NumberFormatException e) {
                throw new BusinessException(String.format("系统参数【1$s %2$s】解析参数值错误.", paramValue, paramName));
            }
        } else if (SysParamDataTypeEnum.BOOLEAN.equals(sysParamDataTypeEnum)) {
            try {
                PubUtil.isTrue(paramValue);
            } catch (NumberFormatException e) {
                throw new BusinessException(String.format("系统参数【1$s %2$s】解析参数值错误.", paramValue, paramName));
            }
        } else if (SysParamDataTypeEnum.DATE.equals(sysParamDataTypeEnum)) {
            try {
                DateUtils.parseDate(paramValue, DateUtils.YYYY_MM_DD);
            } catch (NumberFormatException | ParseException e) {
                throw new BusinessException(String.format("系统参数【1$s %2$s】解析参数值错误.", paramValue, paramName));
            }
        } else if (SysParamDataTypeEnum.CUSTOM.equals(sysParamDataTypeEnum)) {
            if (!paramValue.matches("^\\w+:\\w+$")) {
                throw new BusinessException(String.format("系统参数【%s %s】格式应为x:y.", paramValue, paramName));
            }
        } else if (SysParamDataTypeEnum.STRING.equals(sysParamDataTypeEnum)) {
            //20251021 ZLT 字符类型允许为空
            return;
        } else {
            throw new BusinessException(String.format("系统参数【%1$s %2$s】数据类型不对.", paramCode, paramName));
        }
    }

    private ParamDataTypeUtils() {

    }
}
