package com.zlt.aps.factory.domain.dto;

import com.tlt.aps.constant.StringConstant;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 排产计划-续作信息
 *
 * @author ZLT
 * @date 20251215
 */
@Data
public class ContinueProductInfo implements Serializable {

    /**
     * 分组信息--TBR结构名
     */
    private String groupName;

    /**
     * 成型机台编号
     */
    private String cxMachineCode;

    /**
     * 物料编码
     */
    private String materialCode;

    /**
     * 物料描述
     */
    private String materialDesc;

    /**
     * 英寸
     */
    private String proSize;

    /**
     * 模具使用数量变化信息
     */
    private String mouldChangeInfo;

    /**
     * 获取对应的硫化机台数
     *
     * @return
     */
    public Integer getLhMachineCount() {
        if (StringUtils.isBlank(mouldChangeInfo)) {
            return BigDecimal.ZERO.intValue();
        }
        String[] changeArray = mouldChangeInfo.split(StringConstant.DASH);
        if (changeArray.length <= BigDecimal.ONE.intValue()) {
            return Integer.parseInt(changeArray[0]);
        }
        return Integer.parseInt(changeArray[changeArray.length - 1]);
    }

}
