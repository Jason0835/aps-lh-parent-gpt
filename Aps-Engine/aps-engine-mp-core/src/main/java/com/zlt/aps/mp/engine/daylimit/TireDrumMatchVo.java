package com.zlt.aps.mp.engine.daylimit;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * 轮胎成型鼓匹配条件对象
 *
 * @author ZLT
 * @date 20260314
 */
@Getter
public class TireDrumMatchVo implements Serializable {
    /**
     * 成型机台编号
     */
    private String cxMachineCode;
    /**
     * 成型机台类型 赛象三股/软控三股
     */
    private String cxMachineTypeCode;
    /**
     * 英寸
     */
    private String proSize;

    /**
     * 创建空值对象
     *
     * @return
     */
    public static TireDrumMatchVo createEmpty() {
        return new TireDrumMatchVo("", "", "");
    }

    /**
     * 构建参数对象
     *
     * @param cxMachineCode     成型机台编号
     * @param cxMachineTypeCode 成型机台类型
     * @param proSize           英寸
     */
    public TireDrumMatchVo(String cxMachineCode, String cxMachineTypeCode, String proSize) {
        this.cxMachineCode = cxMachineCode;
        this.cxMachineTypeCode = cxMachineTypeCode;
        this.proSize = proSize;
    }

    /**
     * 是否为空值对象
     *
     * @return
     */
    public boolean isEmptyValue() {
        if (StringUtils.isBlank(cxMachineCode) || StringUtils.isBlank(cxMachineTypeCode)) {
            return true;
        }
        return StringUtils.isBlank(proSize);
    }
}
