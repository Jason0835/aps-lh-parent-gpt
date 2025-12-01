package com.zlt.aps.monthplan.api.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author Chen
 * @date 2025/4/1
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TireTypeClassificationAllVo extends TireTypeClassificationVo {

    /**
     * 版本
     */
    private String productionVersion;

    /**
     * 物料号
     */
    private String productCode;

    /**
     * 库位
     */
    private String locationType;

    /**
     * 品牌
     */
    private String brand;

    /**
     * 渠道
     */
    private String channel;

    /**
     * 期望交期
     */
    private Date deliveryDateDue;

    /**
     * 是否导入
     */
    private Integer isImport;

    /**
     * 规格代号
     */
    private String specCode;

    /**
     * 是否有交期
     *
     * @return 结果
     */
    public Integer getIsDeliveryDate() {
        if (null == deliveryDateDue) {
            return BigDecimal.ZERO.intValue();
        }
        return BigDecimal.ONE.intValue();
    }

    /**
     * 获取合并汇总值
     * 生产版本号、物料编码
     * 库位类别、品牌、渠道、
     * 是否有交期，是否导入
     * 规格代号
     *
     * @return 汇总的key
     */
    public String getSummaryValue() {
        String summaryFormat = "%s|*|%s|*|%s|*|%s|*|%s|*|%d|*|%s";
        return String.format(summaryFormat, getProductionVersion(), getProductCode(),
                getLocationType(), getBrand(), getChannel(), getIsDeliveryDate(), getSpecCode());
    }
}
