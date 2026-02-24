package com.zlt.aps.mdm.utils;

import com.ruoyi.common.core.utils.DateUtils;
import com.tlt.aps.enums.SysParamDataTypeEnum;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.mdm.api.domain.entity.FactoryParam;
import com.zlt.common.utils.PubUtil;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 分厂系统控制参数工具类
 *
 * @author ZLT
 * @date 20250220
 */
@Slf4j
public class FactoryParamUtils {

    /**
     * 对分厂系统控制参数值进行转换成对应类型数据
     *
     * @param factoryParam
     * @return
     */
    public static Object getParamValue(FactoryParam factoryParam) {
        SysParamDataTypeEnum sysParamDataTypeEnum = SysParamDataTypeEnum.getEnumByValue(factoryParam.getDataType().intValue());
        Object paramValue;
        try {
            switch (sysParamDataTypeEnum) {
                case DATETIME: {
                    paramValue = DateUtils.parseDate(factoryParam.getParamValue(), DateUtils.YYYY_MM_DD_HH_MM_SS);
                    break;
                }
                case DATE: {
                    paramValue = DateUtils.parseDate(factoryParam.getParamValue(), DateUtils.YYYY_MM_DD);
                    break;
                }
                case TIME: {
                    paramValue = DateUtils.parseDate(factoryParam.getParamValue(), "HH:mm:ss");
                    break;
                }
                case NUMBER: {
                    paramValue = new BigDecimal(factoryParam.getParamValue());
                    break;
                }
                case BOOLEAN: {
                    paramValue = PubUtil.isTrue(factoryParam.getParamValue());
                    break;
                }
                case INTEGER: {
                    paramValue = Integer.parseInt(factoryParam.getParamValue());
                    break;
                }
                default: {
                    //字符
                    paramValue = factoryParam.getParamValue();
                }
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            throw new BusinessException(String.format("系统参数【%s %s】数据类型不对.", factoryParam.getParamCode(), factoryParam.getParamName()));
        }
        return paramValue;
    }

    private FactoryParamUtils() {

    }
}
