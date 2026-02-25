package com.zlt.aps.mp.engine.domain.vo;

import lombok.Data;

/**
 * 搭配排产调整排程对象
 *
 * @author ZLT
 * @date 20260225
 */
@Data
public class MatchingProductionAdjuestVo {
    /**
     * 是否首日
     */
    private Boolean isFirstDay;
    /**
     * 物料编码
     */
    private String materialCode;
    /**
     * 物料描述
     */
    private String materialDesc;
    /**
     * 排产日 1~31
     */
    private Integer productionDate;
    /**
     * 排产数量
     */
    private Integer productionQty;
}
