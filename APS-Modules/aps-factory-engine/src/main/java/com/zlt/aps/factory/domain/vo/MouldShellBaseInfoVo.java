package com.zlt.aps.factory.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 工厂模壳台账信息
 *
 * @author ZLT
 * @date 20251217
 */
@Data
public class MouldShellBaseInfoVo implements Serializable {

    /**
     * 工厂编码
     */
    private String factoryCode;

    /**
     * 模套型号-模壳
     */
    private String mouldSetCode;

    /**
     * 在机数量
     */
    private Integer machineQty;

    /**
     * 在库数量
     */
    private Integer onHandQty;

    /**
     * 创建无限制的模壳实例
     *
     * @param mouldSetCode
     * @return
     */
    public static MouldShellBaseInfoVo createNoLimit(String mouldSetCode) {
        MouldShellBaseInfoVo noLimit = new MouldShellBaseInfoVo();
        noLimit.setMouldSetCode(mouldSetCode);
        noLimit.setMachineQty(BigDecimal.ZERO.intValue());
        noLimit.setOnHandQty(Integer.MAX_VALUE);
        return noLimit;
    }
}
