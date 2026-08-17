package com.zlt.aps.mp.engine.handler.appoint;

import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.domain.dto.SkuDayProductionInfoHelper;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

/**
 * 在机结构续作Sku在强制下机日使用信息
 * 指定机台时间段业务辅助类
 *
 * @author ZLT
 * @date 20260810
 */
@Getter
public class ContinueSkuDayUsedInfo {
    /**
     * 排产日
     */
    private Integer productionDay;
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
     * 使用硫化机台数
     */
    private Integer usedLhMachine;
    /**
     * 使用模具数
     */
    private Integer usedMoldNumber;
    /**
     * 是否强制下机
     */
    private Boolean isForceOffline;
    /**
     * 需要降膜的数量
     */
    private Integer reduceMoldNumber;

    /**
     * 根据排产信息创建
     *
     * @param productionInfo
     * @return
     */
    public static ContinueSkuDayUsedInfo creatInitByProductionInfo(SkuDayProductionInfoHelper productionInfo) {
        ContinueSkuDayUsedInfo usedInfo = new ContinueSkuDayUsedInfo();
        usedInfo.productionDay = productionInfo.getProductionDay();
        usedInfo.materialCode = productionInfo.getMaterialCode();
        usedInfo.materialDesc = productionInfo.getMaterialDesc();
        usedInfo.embryoCode = productionInfo.getEmbryoCode();
        usedInfo.usedMoldNumber = BigDecimal.ZERO.intValue();
        usedInfo.usedLhMachine = BigDecimal.ZERO.intValue();
        usedInfo.isForceOffline = false;
        usedInfo.reduceMoldNumber = BigDecimal.ZERO.intValue();
        return usedInfo;
    }

    /**
     * 更新使用硫化机台数
     *
     * @param usedMoldNumber
     */
    public void updateUsedLhMachine(Integer usedMoldNumber) {
        if (null == usedMoldNumber) {
            return;
        }
        if (usedMoldNumber <= BigDecimal.ONE.intValue()) {
            return;
        }
        this.usedMoldNumber = usedMoldNumber;
        Integer usedLhMachines = this.usedMoldNumber / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        this.usedLhMachine = usedLhMachines;
    }

    /**
     * 获取剩余使用的硫化机台数
     *
     * @return
     */
    public Integer getLeftOverUsedLhMachine() {
        if (null == usedMoldNumber || usedMoldNumber <= BigDecimal.ONE.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        if (null == reduceMoldNumber) {
            reduceMoldNumber = BigDecimal.ZERO.intValue();
        }
        Integer leftOver = usedMoldNumber - reduceMoldNumber;
        if (leftOver <= BigDecimal.ONE.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        return leftOver / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
    }

    /**
     * 设置Sku在productionDay强制下机
     */
    public void setSkuForceOffline() {
        this.isForceOffline = true;
    }

    /**
     * 增加需要降膜的数量
     */
    public void addReduceMoldNumber() {
        if (null == reduceMoldNumber) {
            reduceMoldNumber = BigDecimal.ZERO.intValue();
        }
        reduceMoldNumber = reduceMoldNumber + ProductionConstant.DOUBLE_MOULD_PRODUCTION;
    }

    /**
     * 是否为Sku强制下机调整日
     *
     * @param materialDesc
     */
    public boolean isForceOfflineSku(String materialDesc) {
        if (!isBaseMatch(materialDesc)) {
            return false;
        }
        return Boolean.TRUE.equals(isForceOffline);
    }

    /**
     * 是否为Sku降膜调整日
     *
     * @param materialDesc
     * @return
     */
    public boolean isReduceMoldAdjust(String materialDesc) {
        if (!isBaseMatch(materialDesc)) {
            return false;
        }
        if (Boolean.TRUE.equals(isForceOffline)) {
            return false;
        }
        if (null == reduceMoldNumber) {
            return false;
        }
        return reduceMoldNumber > BigDecimal.ZERO.intValue();
    }

    /**
     * 是否需要调整
     * 1、强制下机
     * 2、需要降膜
     * true 需要 false 不需要
     *
     * @return
     */
    public boolean isNeedAdjust() {
        if (null == productionDay) {
            return false;
        }
        if (Boolean.TRUE.equals(isForceOffline)) {
            return true;
        }
        if (null == reduceMoldNumber) {
            return false;
        }
        return reduceMoldNumber > BigDecimal.ZERO.intValue();
    }

    /**
     * 标记是否同日同Sku信息
     *
     * @param comparator
     * @return
     */
    public boolean isSameDaySameSku(ContinueSkuDayUsedInfo comparator) {
        if (null == comparator) {
            return false;
        }
        if (null == comparator.getProductionDay()) {
            return false;
        }
        if (!comparator.getProductionDay().equals(productionDay)) {
            return false;
        }
        return comparator.getMaterialDesc().equals(materialDesc);
    }

    /**
     * 基础匹配
     * 物料匹配，且排产日不可为空
     *
     * @param materialDesc
     * @return
     */
    private boolean isBaseMatch(String materialDesc) {
        if (StringUtils.isBlank(materialDesc)) {
            return false;
        }
        if (!materialDesc.equals(this.materialDesc)) {
            return false;
        }
        if (null == productionDay) {
            return false;
        }
        return true;
    }
}
