package com.zlt.aps.factory.domain.dto;

import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 在机结构续作胎胚
 * 使用的模具数以及对应的计划量
 *
 * @author zlt
 * @date 20251224
 */
@Data
public class CxContinueEmbryoInfoHelper implements Serializable {

    /**
     * 胎胚号
     */
    private String embryoCode;

    /**
     * 分组信息--TBR结构名
     */
    private String groupName;

    /**
     * 英寸
     */
    private String proSize;
    /**
     * 模具数
     */
    private Integer mouldNumber;
    /**
     * 计划需求量--高优先级或是总排产量？
     */
    private Integer planDemandQty;

    /**
     * 计划量/与模具数的比值
     *
     * @return
     */
    public Integer getDemandMouldRatioInfo() {
        if (mouldNumber <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        return BigDecimal.valueOf(planDemandQty).divide(BigDecimal.valueOf(mouldNumber), 0, RoundingMode.UP).intValue();
    }

    /**
     * 创建空的续作胎胚信息
     * 续作计划量为零，模具数为零
     *
     * @param continueSkuInfo 续作Sku
     * @return
     */
    public static CxContinueEmbryoInfoHelper buildEmpty(CxContinueSkuInfoHelper continueSkuInfo) {
        CxContinueEmbryoInfoHelper continueEmbryoInfo = new CxContinueEmbryoInfoHelper();
        BeanUtils.copyProperties(continueSkuInfo, continueEmbryoInfo);
        continueEmbryoInfo.setPlanDemandQty(BigDecimal.ZERO.intValue());
        continueEmbryoInfo.setMouldNumber(BigDecimal.ZERO.intValue());
        return continueEmbryoInfo;
    }
}
