package com.zlt.aps.factory.domain.vo;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * 工厂结构硫化配比对象
 *
 * @author ZLT
 * @date 20251212
 */
@Data
public class MonthPlanStructureLhRatioVo implements Serializable {
    /**
     * 产品结构
     */
    private String structureName;
    /**
     * 成型类型-品牌
     */
    private String cxMachineBrandCode;
    /**
     * 硫化机台配比值
     */
    private Integer lhMachineMaxQty;
    /**
     * 最大胎胚数
     */
    private Integer maxEmbryoQty;
    /**
     * 硫化机台最低配比值
     */
    private Integer lhMachineMinQty;

    /**
     * 结构成型硫化是否匹配
     *
     * @param structureName      结构名
     * @param cxMachineBrandCode 成型类型
     * @return
     */
    public boolean isMatch(String structureName, String cxMachineBrandCode) {
        if (StringUtils.isBlank(structureName) || StringUtils.isBlank(cxMachineBrandCode)) {
            return false;
        }
        if (!structureName.equals(this.structureName)) {
            return false;
        }
        return cxMachineBrandCode.equals(this.cxMachineBrandCode);
    }
}
