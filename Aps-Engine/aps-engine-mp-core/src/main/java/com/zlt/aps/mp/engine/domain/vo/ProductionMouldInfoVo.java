package com.zlt.aps.mp.engine.domain.vo;

import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxMouldDayProductionHelper;
import com.zlt.aps.mp.engine.enums.MouldRelationTypeEnum;
import com.zlt.aps.mp.engine.utils.DateUtils;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

/**
 * 排产模具信息对象
 * 根据SKU与模具关系转化，以模具编号为唯一关系，记录模具信息
 * 包含其共用的物料
 *
 * @author ZLT
 * @date 20251218
 */
@Data
public class ProductionMouldInfoVo implements Serializable {

    /**
     * 型腔模号-唯一性
     */
    private String mouldCode;
    /**
     * 模具状态
     */
    private String mouldStatus;
    /**
     * 模壳标准--即模壳型号
     */
    private String mouldSetCode;
    /**
     * 关系类型 01 sku与模具关系 02 新模具到货计划
     */
    private MouldRelationTypeEnum relationType;
    /**
     * 关联的物料集合
     */
    private Set<String> associationMaterialSet;
    /**
     * 可排产日集合信息
     */
    private Set<Integer> productionDaySet;
    /**
     * 排产完毕日集合信息
     */
    private Set<Integer> finishDaySet;
    /**
     * 日排产信息
     */
    private Map<Integer, List<CxMouldDayProductionHelper>> dayProductionInfo;

    /**
     * 创建空的排产模具信息
     * 只包含型腔模号及relationType类型
     *
     * @param relationInfo 某个单一模具关系
     * @return
     */
    public static ProductionMouldInfoVo createEmptyProductionMouldInfo(MonthPlanProductMouldInfoVo relationInfo) {
        if (null == relationInfo) {
            return null;
        }
        String mouldCode = relationInfo.getMouldCode();
        String mouldStatus = relationInfo.getMouldStatus();
        MouldRelationTypeEnum relationType = MouldRelationTypeEnum.getInstance(relationInfo.getRelationType());
        if (StringUtils.isBlank(mouldCode)) {
            return null;
        }
        ProductionMouldInfoVo productionMouldInfo = new ProductionMouldInfoVo();
        productionMouldInfo.setMouldCode(mouldCode);
        productionMouldInfo.setMouldStatus(mouldStatus);
        productionMouldInfo.setMouldSetCode(relationInfo.getShellStandard());
        if (null == relationType) {
            productionMouldInfo.setRelationType(MouldRelationTypeEnum.SKU_RELATION_CONFIGURATION);
        } else {
            productionMouldInfo.setRelationType(relationType);
        }
        //可排产日信息
        productionMouldInfo.setProductionDaySet(new HashSet<>(64));
        //关联SKU
        productionMouldInfo.setAssociationMaterialSet(new HashSet<>(32));
        //排产完毕日
        productionMouldInfo.setFinishDaySet(new HashSet<>(64));
        return productionMouldInfo;
    }

    /**
     * 是否还有产能
     *
     * @return
     */
    public boolean hasCapacity() {
        if (CollectionUtils.isEmpty(productionDaySet)) {
            return false;
        }
        if (CollectionUtils.isEmpty(finishDaySet)) {
            return true;
        }
        Set<Integer> hasLeftOverDaySet = new HashSet<>();
        productionDaySet.forEach(productionDay -> {
            if (finishDaySet.contains(productionDay)) {
                return;
            }
            hasLeftOverDaySet.add(productionDay);
        });
        return !CollectionUtils.isEmpty(hasLeftOverDaySet);
    }

    /**
     * 模具增加排产信息
     *
     * @param day                排产日
     * @param productionPlanInfo 排产计划
     * @param isFinishDay        天是否排产完毕(包含正常排产完成，因换模或是换活字块导致的完成)
     * @param productionQty      双模实际排产量
     * @param cxMachineCodeInfo  成型机台
     */
    public void addProductionInfo(Integer day, MonthPlanProductionRequirePlanVo productionPlanInfo, boolean isFinishDay, Integer productionQty, Set<String> cxMachineCodeInfo) {
        //加入已经排产完毕
        if (isFinishDay) {
            finishDaySet.add(day);
        }
        CxMouldDayProductionHelper mouldProductionHelper = CxMouldDayProductionHelper.createCxMouldDayProductionInfo(productionPlanInfo, cxMachineCodeInfo, day, productionQty);
        mouldProductionHelper.setMouldCode(mouldCode);
        addDayProductionInfo(day, mouldProductionHelper);
    }

    /**
     * 增加模具排产信息
     *
     * @param day                   排产天
     * @param mouldProductionHelper 排产信息对象
     */
    private void addDayProductionInfo(Integer day, CxMouldDayProductionHelper mouldProductionHelper) {
        if (null == day || null == mouldProductionHelper) {
            return;
        }
        if (null == dayProductionInfo) {
            dayProductionInfo = new HashMap<>();
        }
        List<CxMouldDayProductionHelper> dayProductionList = dayProductionInfo.get(day);
        if (null == dayProductionList) {
            dayProductionList = new ArrayList<>();
            dayProductionInfo.put(day, dayProductionList);
        }
        dayProductionList.add(mouldProductionHelper);
    }

    /**
     * 设置模具的可排产日信息
     * 首次可用日期 = 上机时间 + 1
     *
     * @param context      排产上下文
     * @param boardingDate 上机时间
     */
    public void setProductionDayInfo(Context context, Date boardingDate) {
        //模具关系
        if (relationType == MouldRelationTypeEnum.SKU_RELATION_CONFIGURATION) {
            productionDaySet.addAll(context.getProductionDay());
            return;
        }
        if (null == boardingDate) {
            productionDaySet.addAll(context.getProductionDay());
            return;
        }
        //可用时间 = 上机日期 + 1
        Integer startDay = DateUtils.getIntervalDays(context.getProductionStartDate(), boardingDate) + BigDecimal.ONE.intValue();
        Integer monthDays = context.getMonthDays();
        Set<Integer> monthStopDaySet = context.getStopDays();
        for (int day = startDay; day <= monthDays; day++) {
            if (null != monthStopDaySet && monthStopDaySet.contains(day)) {
                continue;
            }
            productionDaySet.add(day);
        }
    }

    /**
     * 模具在startDay~endDay范围内是否可一直排产
     * 即在startDay~endDay可排且没有已经排产完毕的天数
     * true表示可排产，false表示不可排产
     *
     * @param startDay 开始排产日
     * @param endDay   结束排产日
     * @return
     */
    public boolean isProduction(Integer startDay, Integer endDay) {
        if (startDay > endDay) {
            return false;
        }
        for (int day = startDay; day <= endDay; day++) {
            if (finishDaySet.contains(day)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 模具共用性
     * 数字越小，共用性越低
     *
     * @return
     */
    public Integer getCommonalityValue() {
        if (CollectionUtils.isEmpty(associationMaterialSet)) {
            return BigDecimal.ZERO.intValue();
        }
        return associationMaterialSet.size();
    }

    /**
     * 获取模具剩余产能
     * 排产天数 - 已排产日
     *
     * @return
     */
    public Integer getLeftOverCapacity() {
        if (CollectionUtils.isEmpty(productionDaySet)) {
            return BigDecimal.ZERO.intValue();
        }
        if (CollectionUtils.isEmpty(dayProductionInfo)) {
            return productionDaySet.size();
        }
        Set<Integer> leftOverSet = new HashSet<>();
        productionDaySet.forEach(needProductionDay -> {
            if (dayProductionInfo.containsKey(needProductionDay)) {
                return;
            }
            leftOverSet.add(needProductionDay);
        });
        return leftOverSet.size();
    }
}
