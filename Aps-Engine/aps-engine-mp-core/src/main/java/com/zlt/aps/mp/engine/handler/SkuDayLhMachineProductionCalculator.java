package com.zlt.aps.mp.engine.handler;

import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxLhProductionHelper;
import com.zlt.aps.mp.engine.domain.dto.LhProductionQtyHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.engine.scheduling.cxcapacity.ProductionCapacityParamConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

/**
 * Sku单台硫化机日排产量计算器
 *
 * @author ZLT
 * @date 20251221
 */
@Slf4j
public class SkuDayLhMachineProductionCalculator {


    /**
     * 计算单硫化组的天硫化量
     * 此时不考虑与计划的余量
     * 1、非首日排产，则表示续作，
     * 排产量 = 日硫化量
     * 损耗量 = 0
     * 2、首日排产即为结构收尾日
     * 排产量 = 首日排产量参数
     * 损耗量 = 日硫化量 - 排产量
     * 3、首日排产，前Sku=后Sku，则为同Sku不同优先级的衔接排产
     * 排产量 = 日硫化量 - 前Sku排产量
     * 损耗量 = 0
     * 4、首日排产，前Sku！=后Sku，需要判断是否为同生胎共模具
     * 4.1、如果不是同生胎共模具，则为换模
     * 参见buildByChangeMould的说明
     * 4.2、否则为换活字块，需计算前Sku排产量与前Sku日硫化量的差值
     * 4.2.1、如果差值 <= 参数值，
     * 排产量 = 小于差值的排产量参数
     * 损耗量 = 后Sku日硫化量 - 前Sku排产量 - 排产量
     * 4.2.2、如果差值 > 参数值
     * 排产量 = 大于差值的排产量参数
     * 损耗量 = 后Sku日硫化量 - 前Sku排产量 - 排产量
     *
     * @param context               排产上下文
     * @param lhProductionQtyHelper 硫化排产信息(前后Sku信息)
     * @param productionDay         排产日
     * @param firstDay              排产首日
     * @param conclusionDay         收尾日
     * @return
     */
    public static DayProductionQtyHelper calculateSingleLhGroupQty(Context context, LhProductionQtyHelper lhProductionQtyHelper, Integer productionDay, Integer firstDay, Integer conclusionDay, MonthPlanProductionRequirePlanVo productionSkuInfo) {
        //不是首日即为排产Sku续作日，则直接=日硫化量
        if (!firstDay.equals(productionDay)) {
            return new DayProductionQtyHelper(productionDay, false, lhProductionQtyHelper.getDayMaxProductionQty(), BigDecimal.ZERO.intValue(), BigDecimal.ZERO.intValue(), false);
        }
        //上模首日(即换模日：新增换模或是换活字块)
        TbrProductionContext productionContext = (TbrProductionContext) context;
        ProductionPlanGroupInfo productionPlanInfo = lhProductionQtyHelper.getProductionPlanInfo();
        //首日排产量参数
        ProductionCapacityParamConfiguration paramConfiguration = productionContext.getBaseDataContainer().getParamConfiguration();
        Integer firstQty = paramConfiguration.getChangeMouldFirstQty();
        //首日非前Sku收尾日
        if (!firstDay.equals(conclusionDay)) {
            //20260425+ 前日胎胚有排产，首日32
            boolean isProductionEmbryo = productionPlanInfo.hasProductionEmbryo(productionDay - BigDecimal.ONE.intValue(), productionSkuInfo);
            if (isProductionEmbryo) {
                firstQty = paramConfiguration.getChangeTypeBlockQty();
            }
            Integer lossQty = lhProductionQtyHelper.getDayMaxProductionQty() - firstQty;
            return new DayProductionQtyHelper(productionDay, false, firstQty, lossQty, BigDecimal.ZERO.intValue(), true);
        }
        CxLhProductionHelper cxLhGroup = lhProductionQtyHelper.getCxLhGroup();
        String beforeSku = cxLhGroup.getBeforeSku().getMaterialDesc();
        String needProductionSku = productionSkuInfo.getMaterialDesc();
        Integer beforeSkuDayMaxQty = cxLhGroup.getBeforeSku().getDayMaxQty();
        //前Sku的排产量
        Integer beforeSkuProductionQty = cxLhGroup.getBeforeSku().getProductionQty();
        //同Sku，则是不同优先级的衔接(余量不为零)
        if (needProductionSku.equals(beforeSku) && null != beforeSkuProductionQty) {
            Integer needProductionQty = beforeSkuDayMaxQty - beforeSkuProductionQty;
            return new DayProductionQtyHelper(productionDay, false, needProductionQty, BigDecimal.ZERO.intValue(), BigDecimal.ZERO.intValue(), false);
        }
        boolean isChangeMould = !productionContext.getBaseDataContainer().isShareMouldSameGroup(beforeSku, needProductionSku);
        if (isChangeMould) {
            //换模
            return buildByChangeMould(productionDay, lhProductionQtyHelper, paramConfiguration, productionSkuInfo);
        }
        if (null == beforeSkuProductionQty) {
            //判断提前：当天没有，表示前天有量但是在隔天换模，因此为空值
            beforeSkuProductionQty = BigDecimal.ZERO.intValue();
        }
        Integer changeTypeBlockQtyDiff = paramConfiguration.getChangeTypeBlockQtyDiff();
        //当天损耗量
        Integer lossQty = lhProductionQtyHelper.getDayMaxProductionQty() - beforeSkuProductionQty;
        //前Sku排产量与前Sku日硫化量的差值
        Integer beforeSkuDiffValue = Math.abs(beforeSkuDayMaxQty - beforeSkuProductionQty);
        //差值 >= 参数值，表示可以当天换活字块，排量 = changeTypeBlockMaxQty
        if (beforeSkuDiffValue >= changeTypeBlockQtyDiff) {
            Integer afterSkuProductionQty = paramConfiguration.getChangeTypeBlockMaxQty();
            //损耗量 = 日硫化量 - 前Sku排产量 - 自己排产量
            lossQty = lossQty - afterSkuProductionQty;
            if (lossQty < BigDecimal.ZERO.intValue()) {
                lossQty = BigDecimal.ZERO.intValue();
            }
            return new DayProductionQtyHelper(productionDay, false, afterSkuProductionQty, lossQty, BigDecimal.ZERO.intValue(), true);
        }
        //20260317 隔天换模已经提前，理论不会走到此处 差值 < 参数值，表示隔天换活字块，排量 = changeTypeBlockQty
        if (lossQty < BigDecimal.ZERO.intValue()) {
            lossQty = BigDecimal.ZERO.intValue();
        }
        //隔天换活字块，则隔天损耗量 = 日硫化量 - 排产量
        Integer afterSkuProductionQty = paramConfiguration.getChangeTypeBlockQty();
        Integer nextDayLossQty = lhProductionQtyHelper.getDayMaxProductionQty() - afterSkuProductionQty;
        if (lossQty < BigDecimal.ZERO.intValue()) {
            lossQty = BigDecimal.ZERO.intValue();
        }
        return new DayProductionQtyHelper(productionDay, true, afterSkuProductionQty, lossQty, nextDayLossQty, true);
    }

    /**
     * 构建日排产信息-换模场景
     * 1、结构上机首日
     * 2、衔接前后规格-换模
     * 排产量及损耗量计算
     * 1、结构上机首日，则直接当天换模
     * 排产量 = 首日排产量参数
     * 损耗量 = 日硫化量 - 首日排产量
     * 2、衔接前后规格
     * 2.1、判断前Sku排产量与前Sku日硫化量/2的大小
     * 2.1.1、如果排产量小于1/2的日硫化量，则当天换模
     * 排产量 = 首日排产量量
     * 损耗量 = 日硫化量 - 前Sku排产量 - 排产量
     * 2.1.2、如果排产量大于1/2的日硫化量，则隔天换模
     * 当天排产量 = 0
     * 当天损耗量 = 日硫化量 - 前Sku排产量
     * 隔天排产量 = 首日排产量
     * 隔天损耗量 = 日硫化量 - 隔天排产量
     * 换模场景而外处理：
     * 如果前日有排产后Sku的胎胚，则后Sku首日也排产32，不再是8
     *
     * @param productionDay         排产日
     * @param lhProductionQtyHelper 排产信息
     * @param paramConfiguration    排产参数
     * @param productionSkuInfo     排产Sku
     * @return
     */
    private static DayProductionQtyHelper buildByChangeMould(Integer productionDay, LhProductionQtyHelper lhProductionQtyHelper, ProductionCapacityParamConfiguration paramConfiguration, MonthPlanProductionRequirePlanVo productionSkuInfo) {
        CxLhProductionHelper cxLhGroup = lhProductionQtyHelper.getCxLhGroup();
        String beforeSku = cxLhGroup.getBeforeSku().getMaterialDesc();
        Integer firstQty = paramConfiguration.getChangeMouldFirstQty();
        ProductionPlanGroupInfo productionPlanInfo = lhProductionQtyHelper.getProductionPlanInfo();
        //没有前规格，通常为结构上机首日
        if (StringUtils.isBlank(beforeSku)) {
            //20260425+ 前日胎胚有排产，首日32
            boolean isProductionEmbryo = productionPlanInfo.hasProductionEmbryo(productionDay - BigDecimal.ONE.intValue(), productionSkuInfo);
            if (isProductionEmbryo) {
                firstQty = paramConfiguration.getChangeTypeBlockQty();
            }
            Integer lossQty = lhProductionQtyHelper.getDayMaxProductionQty() - firstQty;
            return new DayProductionQtyHelper(productionDay, false, firstQty, lossQty, BigDecimal.ZERO.intValue(), true);
        }
        //衔接
        Integer beforeSkuProductionQty = cxLhGroup.getBeforeSku().getProductionQty();
        if (null == beforeSkuProductionQty) {
            beforeSkuProductionQty = BigDecimal.ZERO.intValue();
        }
        Integer beforeSkuDayMaxQty = cxLhGroup.getBeforeSku().getDayMaxQty();
        //当天损耗量 = 日硫化量 - 前Sku排产量
        Integer lossQty = lhProductionQtyHelper.getDayMaxProductionQty() - beforeSkuProductionQty;
        Integer halfQty = beforeSkuDayMaxQty / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        if (beforeSkuProductionQty < halfQty) {
            //20260425+ 前日胎胚有排产，首日32
            boolean isProductionEmbryo = productionPlanInfo.hasProductionEmbryo(productionDay - BigDecimal.ONE.intValue(), productionSkuInfo);
            if (isProductionEmbryo) {
                firstQty = paramConfiguration.getChangeTypeBlockQty();
            }
            //当天换模 损耗量 = 当天损耗量 - 首日排产量
            lossQty = lossQty - firstQty;
            if (lossQty < BigDecimal.ZERO.intValue()) {
                lossQty = BigDecimal.ZERO.intValue();
            }
            return new DayProductionQtyHelper(productionDay, false, firstQty, lossQty, BigDecimal.ZERO.intValue(), true);
        }
        //20260425+ 前日胎胚有排产，首日32
        boolean isProductionEmbryo = productionPlanInfo.hasProductionEmbryo(productionDay, productionSkuInfo);
        if (isProductionEmbryo) {
            firstQty = paramConfiguration.getChangeTypeBlockQty();
        }
        //隔天换模
        if (lossQty < BigDecimal.ZERO.intValue()) {
            lossQty = BigDecimal.ZERO.intValue();
        }
        //隔天换模，则隔天损耗量 = 日硫化量 - 首日排产量
        Integer nextDayLossQty = lhProductionQtyHelper.getDayMaxProductionQty() - firstQty;
        return new DayProductionQtyHelper(productionDay, true, firstQty, lossQty, nextDayLossQty, true);
    }

}
