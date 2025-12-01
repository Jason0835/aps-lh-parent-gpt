package com.zlt.aps.factory.domain.vo;

import com.zlt.aps.factory.constant.ProductionConstant;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

/**
 * 需搭配排产的需求信息
 *
 * @author ZLT
 * @date 20250829
 */
@Getter
public class MatchingProductionRequireVo implements Serializable {
    /**
     * 成型法
     */
    private String mouldMethod;
    /**
     * 寸口
     */
    private BigDecimal proSize;
    /**
     * 胎体布层级数 1 表示单层 2 表示多层(即2,3等)单层可使用多层，多层不能使用单层 即1可变成2,2不能变成1
     */
    private Integer tireFabricNumber;
    /**
     * 日剩余产能--即产能不对等信息
     */
    private Map<Integer, Long> daySurplusInfo;
    /**
     * 必要属性个数
     */
    private static final int KEY_COUNT = 3;

    /**
     * 构造函数-构建信息
     *
     * @param sizeCapacityGroupKey
     * @param daySurplusInfo
     */
    public MatchingProductionRequireVo(String sizeCapacityGroupKey, Map<Integer, Long> daySurplusInfo) {
        if (StringUtils.isBlank(sizeCapacityGroupKey)) {
            return;
        }
        String[] configurationInfo = sizeCapacityGroupKey.split(ProductionConstant.PRODUCT_SPLIT);
        if (configurationInfo.length != KEY_COUNT) {
            return;
        }
        this.proSize = new BigDecimal(configurationInfo[0]);
        this.mouldMethod = configurationInfo[1];
        this.tireFabricNumber = Integer.valueOf(configurationInfo[2]);
        if (CollectionUtils.isEmpty(daySurplusInfo)) {
            this.daySurplusInfo = Collections.emptyMap();
        } else {
            this.daySurplusInfo = daySurplusInfo;
        }
    }

    /**
     * 搭配排产需求分组值
     * 寸口|*|成型法|*|胎体布层级
     *
     * @return
     */
    public String getGroupKey() {
        String groupKey = "%s|*|%s|*|%s";
        return String.format(groupKey, proSize, mouldMethod, tireFabricNumber);
    }
}
