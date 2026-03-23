package com.zlt.aps.mp.engine.daylimit;

import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxMouldDayProductionHelper;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.mp.engine.scheduling.BaseDataContainer;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.engine.scheduling.cxcapacity.ProductionCapacityParamConfiguration;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 换模信息对象
 *
 * @author ZLT
 * @date 20260317
 */
@Getter
public class ChangeMouldInfo implements Serializable {
    /**
     * 是否需要换模
     */
    private boolean isChangeMould;
    /**
     * 是否需要隔天换模
     */
    private boolean isProductionNextDay;
    /**
     * 换模时：预计排产量
     */
    private Integer preProductionQty;
    /**
     * 换模时：损耗量
     */
    private Integer lossQty;

    /**
     * 构造函数
     *
     * @param isChangeMould       是否需要换模
     * @param isProductionNextDay 是否是隔天换模
     */
    public ChangeMouldInfo(boolean isChangeMould, boolean isProductionNextDay) {
        this.isChangeMould = isChangeMould;
        this.isProductionNextDay = isProductionNextDay;
    }

    /**
     * 根据模具的排产日，获取模具在排产日最后排产的Sku信息
     *
     * @param productionContext 排产上下文
     * @param productionDay     排产日
     * @param selectedMould     模具信息
     * @return
     */
    public static BeforeSkuProductionInfo buildBeforeSkuProductionInfoByMould(TbrProductionContext productionContext, Integer productionDay, List<ProductionMouldInfoVo> selectedMould) {
        if (null == productionDay || CollectionUtils.isEmpty(selectedMould)) {
            return BeforeSkuProductionInfo.buildEmpty(productionDay);
        }
        List<CxMouldDayProductionHelper> lastProductionList = new ArrayList<>();
        Set<Long> skuInfo = new HashSet<>();
        selectedMould.forEach(singleMould -> {
            Map<Integer, List<CxMouldDayProductionHelper>> dayProductionInfo = singleMould.getDayProductionInfo();
            if (CollectionUtils.isEmpty(dayProductionInfo)) {
                return;
            }
            List<CxMouldDayProductionHelper> dayProductionInfoList = dayProductionInfo.get(productionDay);
            if (CollectionUtils.isEmpty(dayProductionInfoList)) {
                return;
            }
            CxMouldDayProductionHelper lastProductionInfo = dayProductionInfoList.get(dayProductionInfoList.size() - BigDecimal.ONE.intValue());
            if (CollectionUtils.isEmpty(skuInfo)) {
                skuInfo.add(lastProductionInfo.getMonthPlanId());
                lastProductionList.add(lastProductionInfo);
                return;
            }
            if (skuInfo.contains(lastProductionInfo.getMonthPlanId())) {
                lastProductionList.add(lastProductionInfo);
            }
        });
        if (CollectionUtils.isEmpty(skuInfo)) {
            return BeforeSkuProductionInfo.buildEmpty(productionDay);
        }
        Long monthPlanId = new ArrayList<>(skuInfo).get(BigDecimal.ZERO.intValue());
        MonthPlanProductionRequirePlanVo fullSkuInfo = productionContext.getAllProductionPlan().get(monthPlanId);
        if (null == fullSkuInfo) {
            return BeforeSkuProductionInfo.buildEmpty(productionDay);
        }
        Integer productionQty = lastProductionList.stream().mapToInt(CxMouldDayProductionHelper::getProductionQty).sum();
        Set<String> usedMouldSet = selectedMould.stream().map(ProductionMouldInfoVo::getMouldCode).collect(Collectors.toSet());
        return BeforeSkuProductionInfo.createByProductionPlan(fullSkuInfo, productionQty, productionDay, usedMouldSet);
    }

    /**
     * 判断是否需要换模
     *
     * @param context       排产上下文
     * @param addSkuInfo    即将要排产的Sku
     * @param beforeSkuInfo 收尾机台反馈的
     * @param mouldSkuInfo  模具前SKu信息
     * @return
     */
    public static ChangeMouldInfo buildChangeMouldInfo(Context context, MonthPlanProductionRequirePlanVo addSkuInfo, BeforeSkuProductionInfo beforeSkuInfo, BeforeSkuProductionInfo mouldSkuInfo) {
        BeforeSkuProductionInfo real = getBeforeSkuProductionInfo(beforeSkuInfo, mouldSkuInfo);
        String materialDesc = real.getMaterialDesc();
        Integer productionQty = real.getProductionQty();
        Integer dayMaxProductionQty = real.getDayMaxQty();
        boolean isChangeMould = isChangeMould(addSkuInfo, materialDesc);
        if (!isChangeMould) {
            //无需换模：同Sku，则是不同优先级的衔接
            return new ChangeMouldInfo(false, false);
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        ProductionCapacityParamConfiguration paramConfiguration = baseDataContainer.getParamConfiguration();
        Integer beforeSkuDayMaxQty = Optional.ofNullable(dayMaxProductionQty).orElse(BigDecimal.ZERO.intValue());
        if (beforeSkuDayMaxQty <= BigDecimal.ZERO.intValue()) {
            return new ChangeMouldInfo(true, false);
        }
        if (StringUtils.isBlank(materialDesc)) {
            return new ChangeMouldInfo(true, false);
        }
        //前Sku的排产量
        Integer beforeSkuProductionQty = Optional.ofNullable(productionQty).orElse(BigDecimal.ZERO.intValue());
        String connectSkuMaterialDesc = addSkuInfo.getMaterialDesc();
        boolean isShareMould = !baseDataContainer.isShareMouldSameGroup(materialDesc, connectSkuMaterialDesc);
        //换活字块
        if (isShareMould) {
            //前Sku排产量与前Sku日硫化量的差值
            Integer beforeSkuDiffValue = Math.abs(beforeSkuDayMaxQty - beforeSkuProductionQty);
            Integer changeTypeBlockQtyDiff = paramConfiguration.getChangeTypeBlockQtyDiff();
            //当天换
            if (beforeSkuDiffValue >= changeTypeBlockQtyDiff) {
                return new ChangeMouldInfo(true, false);
            }
            //隔天换
            return new ChangeMouldInfo(true, true);
        }
        //换模，看一半量
        Integer halfQty = beforeSkuDayMaxQty / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        if (beforeSkuProductionQty < halfQty) {
            //当天换
            return new ChangeMouldInfo(true, false);
        }
        //隔天换
        return new ChangeMouldInfo(true, true);
    }

    /**
     * 当前硫化组是否需要换模
     *
     * @param addSkuInfo
     * @return
     */
    public static boolean isChangeMould(MonthPlanProductionRequirePlanVo addSkuInfo, String materialDesc) {
        if (null == addSkuInfo || StringUtils.isEmpty(addSkuInfo.getMaterialDesc())) {
            return false;
        }
        String connectSkuMaterialDesc = addSkuInfo.getMaterialDesc();
        return !connectSkuMaterialDesc.equals(materialDesc);
    }

    /**
     * 得到前排产的Sku信息
     *
     * @param lhGroup 硫化组推选出来的前Sku信息
     * @param mould   模具推选出来的前Sku信息
     * @return
     */
    private static BeforeSkuProductionInfo getBeforeSkuProductionInfo(BeforeSkuProductionInfo lhGroup, BeforeSkuProductionInfo mould) {
        if (null == mould) {
            return lhGroup;
        }
        if (StringUtils.isNotBlank(mould.getMaterialDesc())) {
            return mould;
        }
        return lhGroup;
    }
}
