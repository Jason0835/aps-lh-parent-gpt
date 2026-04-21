package com.zlt.aps.mp.engine.handler;

import com.google.common.collect.Sets;
import com.zlt.aps.mp.engine.domain.dto.SkuDayProductionInfoHelper;
import lombok.Getter;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Set;

/**
 * Sku日排产模具信息对象
 * 用以辅助判断续作Sku模具分配比例调整
 *
 * @author ZLT
 * @date 20260420
 */
@Getter
public class SkuDayUsedMoldInfoHelper implements Serializable {
    /**
     * 排产日
     */
    private Integer productionDay;
    /**
     * 分组名
     * TBR 结构名
     * PCR 英寸
     */
    private String groupName;
    /**
     * 物料描述
     */
    private String materialDesc;
    /**
     * 物料编码
     */
    private String materialCode;
    /**
     * 生胎代码
     */
    private String embryoCode;

    /**
     * 主胎胚描述
     */
    private String mainMaterialDesc;

    /**
     * 主花纹
     */
    private String mainPattern;

    /**
     * 品牌
     */
    private String brand;

    private Set<String> usedMouldSet;

    /**
     * 创建SkuDayUsedMoldInfoHelper对象
     *
     * @param dayProductionInfo
     * @return
     */
    public static SkuDayUsedMoldInfoHelper build(SkuDayProductionInfoHelper dayProductionInfo) {
        SkuDayUsedMoldInfoHelper result = new SkuDayUsedMoldInfoHelper();
        result.productionDay = dayProductionInfo.getProductionDay();
        result.groupName = dayProductionInfo.getGroupName();
        result.materialDesc = dayProductionInfo.getMaterialDesc();
        result.materialCode = dayProductionInfo.getMaterialCode();
        result.mainPattern = dayProductionInfo.getMainPattern();
        result.embryoCode = dayProductionInfo.getEmbryoCode();
        result.mainMaterialDesc = dayProductionInfo.getMainMaterialDesc();
        result.brand = dayProductionInfo.getBrand();
        result.usedMouldSet = Sets.newHashSet();
        return result;
    }

    /**
     * 增加模具使用量
     *
     * @param dayProductionInfo
     */
    public void addUsedMoldNumber(SkuDayProductionInfoHelper dayProductionInfo) {
        if (null == dayProductionInfo) {
            return;
        }
        if (!(productionDay.equals(dayProductionInfo.getProductionDay()) && materialDesc.equals(dayProductionInfo.getMaterialDesc()))) {
            return;
        }
        Set<String> usedMouldSet = dayProductionInfo.getUsedMouldSet();
        if (CollectionUtils.isEmpty(usedMouldSet)) {
            return;
        }
        this.usedMouldSet.addAll(usedMouldSet);
    }

    /**
     * 模具使用量
     *
     * @return
     */
    public Integer getUsedMoldNumber() {
        if (CollectionUtils.isEmpty(usedMouldSet)) {
            return BigDecimal.ZERO.intValue();
        }
        return usedMouldSet.size();
    }

    /**
     * 获取分组+主花纹Key
     * 分组名|*|主花纹
     *
     * @return
     */
    public String getGroupMainPatternKey() {
        String keyFormat = "%s|*|%s";
        return String.format(keyFormat, groupName, mainPattern);
    }
}
