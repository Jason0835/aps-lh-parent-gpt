package com.zlt.aps.mp.engine.domain.vo;

import com.zlt.aps.mp.api.domain.entity.MpSkuMoldCapacityAllocateLog;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 各Sku模具产能信息对象
 *
 * @author ZLT
 * @date 20260515
 */
@Slf4j
@Data
public class SkuMoldCapacityInfoVo implements Serializable {
    /**
     * 分组名
     */
    private String groupName;
    /**
     * 物料编码
     */
    private String materialCode;
    /**
     * 物料描述
     */
    private String materialDesc;
    /**
     * 主花纹
     */
    private String mainPattern;
    /**
     * 单模日硫化量
     */
    private Integer dayVulcanizationQty;
    /**
     * 高优先级
     */
    private Integer heightProductionQty;
    /**
     * 排产净需求(含损耗，排除高优先级损耗)
     */
    private Integer productionQty;
    /**
     * 同主花纹下-所有高优级需求量
     */
    private Integer sumHeightProductionQty;
    /**
     * 同主花纹下-所有净需求量
     */
    private Integer sumProductionQty;
    /**
     * 同主花纹下-最大模具产能
     */
    private Integer maxMoldCapacity;

    /**
     * 分摊高优先级需求量
     */
    private Integer allocateHeightQty;
    /**
     * 分摊净需求需求量
     */
    private Integer allocateNetQty;

    /**
     * 构建基础的信息
     *
     * @param skuRequireInfo sku计划
     * @return
     */
    public static SkuMoldCapacityInfoVo buildByBaseInfo(MonthPlanProductionRequirePlanVo skuRequireInfo) {
        SkuMoldCapacityInfoVo skuCapacityInfo = new SkuMoldCapacityInfoVo();
        skuCapacityInfo.setGroupName(skuRequireInfo.getStructureName());
        skuCapacityInfo.setMaterialCode(skuRequireInfo.getMaterialCode());
        skuCapacityInfo.setMaterialDesc(skuRequireInfo.getMaterialDesc());
        skuCapacityInfo.setMainPattern(skuRequireInfo.getMainPattern());
        skuCapacityInfo.setDayVulcanizationQty(skuRequireInfo.getDayVulcanizationQty());
        return skuCapacityInfo;
    }

    /**
     * 创建存储对象
     *
     * @return
     */
    public MpSkuMoldCapacityAllocateLog buildLog() {
        MpSkuMoldCapacityAllocateLog log = new MpSkuMoldCapacityAllocateLog();
        BeanUtils.copyProperties(this, log);
        log.setStructureName(groupName);
        log.setMaxHeightQty(sumHeightProductionQty);
        log.setMaxNetQty(sumProductionQty);
        log.setHeightQty(allocateHeightQty);
        log.setNetQty(allocateNetQty);
        return log;
    }

    /**
     * 1、如果总需求 <= 总产能，则不用分摊，为各自的值
     * 2、如果总需求 > 总产能，则看高优先级需求量与总产能比较
     * 2.1、如果高优先级需求量 <= 总产能，则高优先级不用分摊
     * 再看除高外的其它需求，进行比例分摊
     * 2.2、如果高优先级需求量 > 总产能，则高优先级分摊，其它需求不用分摊
     */
    public void allocateHandler() {
        //总需求量 小于模具产能
        if (sumProductionQty <= maxMoldCapacity) {
            allocateHeightQty = heightProductionQty;
            allocateNetQty = productionQty;
            return;
        }
        //高优先级需求量 > 模具产能
        if (sumHeightProductionQty > maxMoldCapacity) {
            //等比例分摊
            allocateHeightQty = BigDecimal.valueOf(heightProductionQty).multiply(BigDecimal.valueOf(maxMoldCapacity)).divide(BigDecimal.valueOf(sumHeightProductionQty), BigDecimal.ZERO.intValue(), RoundingMode.UP).intValue();
            allocateNetQty = allocateHeightQty;
            return;
        }
        //高优先级需求量 == 模具产能
        if (sumHeightProductionQty.equals(maxMoldCapacity)) {
            allocateHeightQty = heightProductionQty;
            allocateNetQty = allocateHeightQty;
            return;
        }
        //高优先级需求量 < 模具产能，总需求量 > 模具产能
        allocateHeightQty = heightProductionQty;
        //计算其它净需求占比
        Integer sumOtherQty = sumProductionQty - sumHeightProductionQty;
        Integer skuOtherQty = productionQty - heightProductionQty;
        Integer moldSurplusCapacity = maxMoldCapacity - sumHeightProductionQty;
        Integer otherAllocateQty = BigDecimal.valueOf(skuOtherQty).multiply(BigDecimal.valueOf(moldSurplusCapacity)).divide(BigDecimal.valueOf(sumOtherQty), BigDecimal.ZERO.intValue(), RoundingMode.UP).intValue();
        allocateNetQty = allocateHeightQty + otherAllocateQty;
    }

}
