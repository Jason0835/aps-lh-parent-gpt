package com.zlt.aps.factory.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 胎胚特殊材料配置对象
 *
 * @author ZLT
 * @date 20251222
 */
@Data
public class EmbryoSpecialMaterialInfoVo implements Serializable {
    /**
     * 胎胚号
     */
    private String embryoCode;
    /**
     * 原材料编码
     */
    private String childMaterialCode;
    /**
     * 原材料名称
     */
    private String childMaterialName;
    /**
     * 一条胎胚消耗的数量
     */
    private BigDecimal dosage;
}
