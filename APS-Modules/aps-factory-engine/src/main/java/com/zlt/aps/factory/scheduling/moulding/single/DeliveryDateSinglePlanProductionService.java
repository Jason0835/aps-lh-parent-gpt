package com.zlt.aps.factory.scheduling.moulding.single;

import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.MouldRestoreInfoDto;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.enums.ProductionLimitTypeEnum;
import com.zlt.aps.factory.enums.ProductionOrientEnum;
import com.zlt.aps.factory.scheduling.AbstractProductionBusinessService;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.factory.scheduling.moulding.SinglePlanProductionContext;
import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import com.zlt.aps.factory.utils.MouldUtils;
import com.zlt.aps.factory.utils.NoProductionReasonUtils;
import com.zlt.aps.factory.utils.ProductUtils;
import com.zlt.aps.factory.utils.ProductionLogUtils;
import com.zlt.aps.monthplan.api.domain.entity.MouldProductionLog;
import com.zlt.aps.monthplan.api.enums.MouldProductionLogType;
import com.zlt.common.utils.PubUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 交期单计划模具排产
 * 根据计划的物料配置的可用模具集合交期预排排产
 * <p>
 * 对可用模具进行按先续作，再次已排，再次可硫化时间，最后模具编号方式
 * 先用两副，再用两副逐一增模方式按日排产
 *
 * @author
 */
@Slf4j
@Service(value = "deliveryDateSinglePlanProductionService")
public class DeliveryDateSinglePlanProductionService extends AbstractProductionBusinessService {

    public DeliveryDateSinglePlanProductionService(ProductionSchedulingDataService dataService) {
        super(dataService);
    }

    @Override
    public void run(Context context, Object userObj) {
        SinglePlanProductionContext singlePlanProductionContext = (SinglePlanProductionContext) context;
        ProductionContext productionContext = singlePlanProductionContext.getGroupContext().getProductionContext();
        MonthPlanManufacturingRequirementVo productionPlan = singlePlanProductionContext.getProductionPlan();
        if (productionContext.isProductionFinishPlan(productionPlan.getMonthPlanId())) {
            log.warn("排产计划已排产完毕。。。。无需再次排产");
            return;
        }
        //单条硫化时间-单位到秒 增加了每条间隔硫化时间
        BigDecimal singleCuringTime = ProductUtils.getSingleCuringTime(productionPlan, productionContext);
        List<MouldInfoVO> mouldList = singlePlanProductionContext.getEnableMouldList();
        List<MouldInfoVO> enableMouldList = mouldList.stream().filter(mouldInfo -> !PubUtil.isTrue(mouldInfo.getIsFinish())).collect(Collectors.toList());
        int mouldSize = enableMouldList.size();
        //排产流程日志记录 ===分厂%s, 计划年月：%d-%d, 计划版本：%s，生产计划单计划%d 交期计划排产开始：预计交期日期：%s 计划排产量%d,可用模具数%d,单条硫化时间%d====
        ProductionLogUtils.addDeliveryDatePlanStartProductionLog(productionContext, productionPlan, mouldSize, singleCuringTime);
        //单模具排产，则无变化，排到什么日期就是什么日期
        if (mouldSize == 1) {
            MouldInfoVO singleMouldInfo = enableMouldList.get(0);
            //排产流程日志记录
            MouldProductionLog singleMouldLog = ProductionLogUtils.buildProductionLog(productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_DELIVERY_LOG, "计划只有1副模具");
            saveProductionLog(productionContext, singleMouldLog);
            if (null == singleMouldInfo.getProductionOrient()) {
                singleMouldInfo.setGroupValue(BigDecimal.ONE.intValue());
                singleMouldInfo.setProductionOrient(ProductionOrientEnum.FORWARD);
                singleMouldInfo.setBeginDay(BigDecimal.ONE.intValue());
                singleMouldInfo.setEndDay(productionContext.getMonthDays());
            }
            singleMouldProduction(null, singleMouldInfo, productionContext, productionPlan, productionPlan.getProductionQty());
            //计划单模排产结束：[%d]有交期计划单模具排产完成。。。 排产日志打印及保存记录
            ProductionLogUtils.addPlanProductionFinishLog(productionContext, true, productionPlan, MouldProductionLogType.SINGLE_PLAN_DELIVERY_LOG, "单");
            return;
        }
        //多模具排产
        multiMouldDeliveryProductionPlan(productionContext, productionPlan, enableMouldList, singleCuringTime);
        //计划多模排产结束 [%d]有交期计划多模具排产完成。。。排产日志打印及保存记录
        ProductionLogUtils.addPlanProductionFinishLog(productionContext, true, productionPlan, MouldProductionLogType.SINGLE_PLAN_DELIVERY_LOG, "多");
    }

    /**
     * 有交期-多模具排产计划
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划
     * @param enableMouldList   可用模具列表
     * @param singleCuringTime  单条硫化时间 单位秒(包含了间隔增加时间)
     */
    private void multiMouldDeliveryProductionPlan(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, List<MouldInfoVO> enableMouldList, BigDecimal singleCuringTime) {
        Long monthPlanId = productionPlan.getMonthPlanId();
        //获取交期
        Date deliveryDate = productionPlan.getDeliveryDateDue();
        if (null == deliveryDate) {
            //排产流程日志打印及保存记录
            String noHasDeliveryDateLogContent = String.format("%d:计划数据出错，有交期计划发现没有交期日期", monthPlanId);
            log.warn(noHasDeliveryDateLogContent);
            MouldProductionLog noHasDeliveryDateLog = ProductionLogUtils.buildProductionLog(productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_DELIVERY_LOG, noHasDeliveryDateLogContent);
            saveProductionLog(productionContext, noHasDeliveryDateLog);
            return;
        }
        String productCode = productionPlan.getProductCode();
        List<MouldInfoVO> maxMouldInfoList = MouldUtils.getMaxEnableMouldList(productionContext, productCode);
        //按续作优先 > 已排优先 > 物料关联数 > 分组值 > 剩余硫化时间多 > 模具编号
        enableMouldList.sort(MouldUtils.buildMouldSortComparator());
        //记录模具初始状态数据--主要为排产方向、分组值、起始日、截止日
        Map<String, MouldRestoreInfoDto> restoreInfoMap = buildRestoreInfo(enableMouldList);
        //进行预排-用来确认交期，按先交期日，进行模具排产，交期日可满足，则到交期日，交期日不满足，则往后延一天，直到可满足或是模具产能不足
        Integer deliveryDay = preConfirmRealDeliveryDate(productionContext, productionPlan, enableMouldList, maxMouldInfoList, restoreInfoMap, singleCuringTime);
        //确认交期正式排产
        formalProduction(deliveryDay, productionPlan, enableMouldList, maxMouldInfoList, productionContext, singleCuringTime);
        //计划排产结束
        String endPlanProductionLogContent = String.format("[%d]有交期计划多模具排产完成。。。", productionPlan.getMonthPlanId());
        log.info(endPlanProductionLogContent);
        MouldProductionLog endPlanProductionLog = ProductionLogUtils.buildProductionLog(productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_DELIVERY_LOG, endPlanProductionLogContent);
        saveProductionLog(productionContext, endPlanProductionLog);
    }

    /**
     * 记录模具预排前的初始信息
     * 主要为预排关注的信息，排产方向，分组值，当前排产日，排产截止日
     *
     * @param enableMouldList
     */
    private Map<String, MouldRestoreInfoDto> buildRestoreInfo(List<MouldInfoVO> enableMouldList) {
        Map<String, MouldRestoreInfoDto> restoreInfoMap = new HashMap<>();
        enableMouldList.stream().forEach(mouldInfo -> {
            String mouldCode = mouldInfo.getMouldCode();
            if (restoreInfoMap.containsKey(mouldCode)) {
                return;
            }
            MouldRestoreInfoDto restoreInfo = new MouldRestoreInfoDto();
            restoreInfo.setMouldCode(mouldCode);
            restoreInfo.setProductionOrientEnum(mouldInfo.getProductionOrient());
            restoreInfo.setGroupValue(mouldInfo.getGroupValue());
            restoreInfo.setBeginDay(mouldInfo.getBeginDay());
            restoreInfo.setEndDay(mouldInfo.getEndDay());
            restoreInfoMap.put(mouldCode, restoreInfo);
        });
        return restoreInfoMap;
    }

    /**
     * 进行预排-用来确认交期
     * 1、按先交期日，进行模具排产，交期日可满足，则到交期日
     * 2、交期日不满足，则往后延一天，按新的交期日重新预排直到可满足或是模具产能不足
     *
     * @param productionContext 排产上下文
     * @param productionPlan    预排排产计划
     * @param enableMouldList   可用模具列表
     * @param maxMouldInfoList  最大可用模具
     * @param restoreInfoMap    模具初始信息
     * @param singleCuringTime  单条硫化时间(包含间隔增加硫化时间)
     * @return
     */
    private Integer preConfirmRealDeliveryDate(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, List<MouldInfoVO> enableMouldList, List<MouldInfoVO> maxMouldInfoList, Map<String, MouldRestoreInfoDto> restoreInfoMap, BigDecimal singleCuringTime) {
        String format = "=====[%d]:交期计划进行交期日到 【%d】日的预排，使用模具数[%d]，可排产量[%d]=====";
        String resultFormat = "=====[%d]:交期计划使用[%d]副模具进行交期日到 【%d】日的预排结果[%b]";
        Date deliveryDate = productionPlan.getDeliveryDateDue();
        //初始交期日
        Integer deliveryDay = com.zlt.aps.factory.utils.DateUtils.getDaysByMonth(deliveryDate);
        Integer mouldSize = enableMouldList.size();
        Long monthPlanId = productionPlan.getMonthPlanId();
        Integer monthDay = productionContext.getMonthDays();
        for (; deliveryDay <= monthDay; ) {
            Long needProductionQty = productionPlan.getProductionQty();
            //交期预排-日排产开始：日志打印及保存记录
            String preProductionDateLogContent = String.format(format, monthPlanId, deliveryDay, mouldSize, needProductionQty);
            log.info(preProductionDateLogContent);
            MouldProductionLog preProductionDateLog = ProductionLogUtils.buildProductionLog(productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_DELIVERY_LOG, preProductionDateLogContent);
            saveProductionLog(productionContext, preProductionDateLog);
            //20250414 交期预排-预排日产能存储-临时存储
            productionContext.setPreProductionDateQtyMap(new HashMap<>());
            productionContext.setMouldPreProductionDateQtyMap(new HashMap<>());
            //预排结果
            boolean preResult = preProduction(productionPlan, deliveryDay, needProductionQty, enableMouldList, maxMouldInfoList, productionContext, singleCuringTime);
            //交期预排-日排产结束：日志打印及保存记录
            String preProductionDateResultLogContent = String.format(resultFormat, monthPlanId, mouldSize, deliveryDay, preResult);
            log.info(preProductionDateResultLogContent);
            MouldProductionLog preProductionDateResultLog = ProductionLogUtils.buildProductionLog(productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_DELIVERY_LOG, preProductionDateResultLogContent);
            saveProductionLog(productionContext, preProductionDateResultLog);
            //每次预排完还原模具信息
            enableMouldList.stream().forEach(enableMould -> {
                String mouldCode = enableMould.getMouldCode();
                MouldRestoreInfoDto restoreInfo = restoreInfoMap.get(mouldCode);
                if (null == restoreInfo) {
                    return;
                }
                enableMould.setProductionOrient(restoreInfo.getProductionOrientEnum());
                enableMould.setGroupValue(restoreInfo.getGroupValue());
                enableMould.setBeginDay(restoreInfo.getBeginDay());
                enableMould.setEndDay(restoreInfo.getEndDay());
            });
            //20250414 交期预排-预排日产能存储-清空
            productionContext.setPreProductionDateQtyMap(null);
            productionContext.setMouldPreProductionDateQtyMap(null);
            if (preResult || deliveryDay.equals(monthDay)) {
                break;
            }
            deliveryDay = deliveryDay + 1;
        }
        return deliveryDay;
    }

    /**
     * 按交期日进行预排
     *
     * @param productionPlan    排产计划
     * @param deliveryDay       计算到的排产交期日
     * @param needProductionQty 需要排产的数量
     * @param enableMouldList   可排产模具
     * @param maxMouldInfoList  最大可用模具
     * @param productionContext 排产上下文
     * @param singleCuringTime  单条硫化时间-秒
     * @return
     */
    private boolean preProduction(MonthPlanManufacturingRequirementVo productionPlan, Integer deliveryDay, Long needProductionQty, List<MouldInfoVO> enableMouldList, List<MouldInfoVO> maxMouldInfoList, ProductionContext productionContext, BigDecimal singleCuringTime) {
        int mouldSize = enableMouldList.size();
        int groupCount = mouldSize / 2;
        //两副两副
        for (int index = 0; index < groupCount; index++) {
            int startIndex = index * 2;
            int endIndex = (index + 1) * 2;
            List<MouldInfoVO> productionMouldList = enableMouldList.subList(startIndex, endIndex);
            needProductionQty = preDoubleMouldProduction(productionPlan, productionMouldList, needProductionQty, deliveryDay, maxMouldInfoList, productionContext, singleCuringTime);
            //剩余一个直接退出双模
            if (needProductionQty <= 1) {
                break;
            }
        }
        //双模模具-余1则表示可结束：当前日期可排完
        if (needProductionQty <= 1) {
            return true;
        }
        //最后一副模具排
        MouldInfoVO lastMouldInfo = enableMouldList.get(mouldSize - 1);
        MouldUtils.setLastMouldInfo(lastMouldInfo, maxMouldInfoList, deliveryDay, true);
        needProductionQty = preSingleMouldProduction(productionContext, productionPlan, lastMouldInfo, needProductionQty, singleCuringTime);
        if (needProductionQty <= 0) {
            return true;
        }
        return false;
    }

    /**
     * 正式排产信息
     *
     * @param deliveryDay       确定交期日
     * @param productionPlan    排产计划
     * @param enableMouldList   可用模具列表
     * @param maxMouldInfoList  最大可用模具列表
     * @param productionContext 排产上下文
     * @param singleCuringTime  单条硫化时间-秒
     */
    private void formalProduction(Integer deliveryDay, MonthPlanManufacturingRequirementVo productionPlan, List<MouldInfoVO> enableMouldList, List<MouldInfoVO> maxMouldInfoList, ProductionContext productionContext, BigDecimal singleCuringTime) {
        int mouldSize = enableMouldList.size();
        int groupCount = mouldSize / 2;
        int remainder = mouldSize % 2;
        Long needProductionQty = productionPlan.getProductionQty();
        Long monthPlanId = productionPlan.getMonthPlanId();
        //交期正式排产开始：排产流程日志打印及保存记录
        String format = "=====%d :交期计划进行交期日到 【%d】日的正式排产，可排产量[%d]，可用模具数[%d]=====";
        String formalProductionLogContent = String.format(format, monthPlanId, deliveryDay, needProductionQty, mouldSize);
        log.info(formalProductionLogContent);
        MouldProductionLog formalProductionLog = ProductionLogUtils.buildProductionLog(productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_DELIVERY_LOG, formalProductionLogContent);
        saveProductionLog(productionContext, formalProductionLog);
        //开始排产
        MonthPlanManufacturingRequirementVo originalPlan = productionContext.getMonthPlanInitMap().get(monthPlanId);
        for (int index = 0; index < groupCount; index++) {
            int startIndex = index * 2;
            int endIndex = (index + 1) * 2;
            List<MouldInfoVO> productionMouldList = enableMouldList.subList(startIndex, endIndex);
            needProductionQty = formalDoubleMouldProduction(productionPlan, productionMouldList, needProductionQty, deliveryDay, maxMouldInfoList, productionContext, singleCuringTime);
            //剩余一个直接退出双模
            if (needProductionQty <= 1) {
                break;
            }
        }
        if (needProductionQty <= 0) {
            //标记不可排产了
            originalPlan.setIsProduction(YesOrNoEnum.NO.getValue());
            return;
        }
        //先双模模具，剩余1则不排了 双模排产不排单
        if (needProductionQty == 1) {
            //标记不可排产了
            originalPlan.setIsProduction(YesOrNoEnum.NO.getValue());
            //双模排产不排单
            String noProductionReason = NoProductionReasonUtils.getDoubleNoSingle();
            originalPlan.addNoProductionReasonAndQty(noProductionReason, needProductionQty);
            //交期正式排产结果：排产流程日志打印及保存记录
            ProductionLogUtils.addDoubleMouldNoProductionSingle(noProductionReason, productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_DELIVERY_LOG);
            return;
        }
        if (remainder == 0) {
            //标记不可排产了
            originalPlan.setIsProduction(YesOrNoEnum.NO.getValue());
            //模具产能不足
            String noProductionReason = NoProductionReasonUtils.getMouldNotEnough();
            if (Boolean.TRUE.equals(originalPlan.getIsCapacityLimit())) {
                noProductionReason = NoProductionReasonUtils.getDayLimit();
            }
            originalPlan.addNoProductionReasonAndQty(noProductionReason, needProductionQty);
            //交期正式排产结果：排产流程日志打印及保存记录
            ProductionLogUtils.addDoubleMouldProductionResult(noProductionReason, productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_DELIVERY_LOG);
            return;
        }
        MouldInfoVO lastMouldInfo = enableMouldList.get(mouldSize - 1);
        MouldUtils.setLastMouldInfo(lastMouldInfo, maxMouldInfoList, deliveryDay, true);
        singleMouldProduction(null, lastMouldInfo, productionContext, productionPlan, needProductionQty);
    }

    /**
     * 对排产计划进行双模具预排
     *
     * @param productionPlan      排产计划
     * @param productionMouldList 预排模具
     * @param needProductionQty   需排产量
     * @param deliveryDay         交期日
     * @param maxMouldInfoList    最大可用模具列表
     * @param productionContext   排产上下文
     * @param singleCuringTime    单条硫化时间
     * @return
     */
    private Long preDoubleMouldProduction(MonthPlanManufacturingRequirementVo productionPlan, List<MouldInfoVO> productionMouldList, Long needProductionQty, Integer deliveryDay, List<MouldInfoVO> maxMouldInfoList, ProductionContext productionContext, BigDecimal singleCuringTime) {
        String productCode = productionPlan.getProductCode();
        //分组值为空，则表示新挑选上来的模具，则此时需要赋值分组值和排产方向
        MouldUtils.setGroupValueAndProductionOrient(productionMouldList, maxMouldInfoList, deliveryDay, true);
        MouldInfoVO first = productionMouldList.get(0);
        MouldInfoVO second = productionMouldList.get(1);
        String mouldCodeInfo = String.format("[%s]、[%s]", first.getMouldCode(), second.getMouldCode());
        //排产方向
        ProductionOrientEnum productionOrient = first.getProductionOrient();
        //交期双模预排开始：排产流程日志打印及保存记录
        String preProductionFormat = "[%s]计划物料预排：模具号%s，预排到[%d]日，排产方向：[%s]";
        String mouldPreProductionLogContent = String.format(preProductionFormat, productCode, mouldCodeInfo, deliveryDay, productionOrient.getDesc());
        log.info(mouldPreProductionLogContent);
        MouldProductionLog mouldPreProductionLog = ProductionLogUtils.buildProductionLog(productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_DELIVERY_LOG, mouldPreProductionLogContent);
        saveProductionLog(productionContext, mouldPreProductionLog);
        //排产起始日
        Integer startProductionDate = MouldUtils.getStartProductionDate(productionMouldList);
        String sizeCapacityKey = productionPlan.getSizeCapacityGroupKey();
        //排产截止日
        Integer endProductionDate = MouldUtils.getEndProductionDate(productionMouldList);
        for (; MouldUtils.isDateProduction(startProductionDate, endProductionDate, productionOrient); ) {
            if (needProductionQty <= 1) {
                break;
            }
            //获取下一个排产日
            Integer nextProductionDate = MouldUtils.getNextProductionDate(productionContext, startProductionDate, productionOrient);
            //20251013 ZLT 校验成型硫化配比控制
            ProductionLimitTypeEnum limitType = productionContext.isReachTheLimit(true, productionOrient, startProductionDate, productionMouldList.size(), productionPlan);
            if (ProductionLimitTypeEnum.DAY_MOULD_QTY_LIMIT == limitType) {
                ProductionLogUtils.addProductionMouldQtyLimitLog(productionContext, MouldProductionLogType.SINGLE_PLAN_DELIVERY_LOG, productionPlan, startProductionDate);
                startProductionDate = nextProductionDate;
                continue;
            }
            //校验是否达到规格数限制
            if (!productionContext.isAddProduct(true, productionOrient, startProductionDate, productCode, productionPlan)) {
                if (!productionContext.getFactoryStopDays().contains(startProductionDate)) {
                    ProductionLogUtils.addProductionProductNumberLimitLog(productionContext, MouldProductionLogType.SINGLE_PLAN_DELIVERY_LOG, productionPlan, startProductionDate);
                }
                //排产日迭代
                startProductionDate = nextProductionDate;
                continue;
            }
            //双模排产量
            Map<String, ProductionInfoVo> doubleProductionMap = getPreDoubleProductionInfo(productionContext, productionMouldList, productionPlan.getMonthPlanId(), productCode, sizeCapacityKey, startProductionDate, needProductionQty, singleCuringTime);
            ProductionInfoVo firstProductionInfo = doubleProductionMap.get(first.getMouldCode());
            ProductionInfoVo secondProductionInfo = doubleProductionMap.get(second.getMouldCode());
            //20250325 赋值是否需要跨天扣减产能
            if (firstProductionInfo.hasCrossDaySubtractCapacity()) {
                first.setNextDaySubtractTime(firstProductionInfo.getNextDaySubtractTime());
            } else {
                first.setNextDaySubtractTime(null);
            }
            if (secondProductionInfo.hasCrossDaySubtractCapacity()) {
                second.setNextDaySubtractTime(secondProductionInfo.getNextDaySubtractTime());
            } else {
                second.setNextDaySubtractTime(null);
            }
            Long productionQty = firstProductionInfo.getProductionQty() + secondProductionInfo.getProductionQty();
            //20250620 排产量为零，直接跳过
            if (productionQty <= BigDecimal.ZERO.longValue()) {
                startProductionDate = nextProductionDate;
                continue;
            }
            //剩余还需排产量
            Long leftOverNeedProductionQty = needProductionQty - productionQty;
            String firstMouldCode = first.getMouldCode();
            String secondMouldCode = second.getMouldCode();
            //20250414 交期预排-预排日产能存储-临时存储
            productionContext.addDayPreProductionQty(startProductionDate, sizeCapacityKey, firstProductionInfo.getProductionQty(), firstMouldCode);
            productionContext.addDayPreProductionQty(startProductionDate, sizeCapacityKey, secondProductionInfo.getProductionQty(), secondMouldCode);
            //预排双模结果：排产流程日志打印及保存记录
            String doubleMouldPreProductionDateResultLogContent = String.format("[%d]计划，使用模具%s在[%d]日预排量[%d]", productionPlan.getMonthPlanId(), mouldCodeInfo, startProductionDate, productionQty);
            log.info(doubleMouldPreProductionDateResultLogContent);
            MouldProductionLog doubleMouldPreProductionDateResultLog = ProductionLogUtils.buildProductionLog(productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_DELIVERY_LOG, doubleMouldPreProductionDateResultLogContent);
            saveProductionLog(productionContext, doubleMouldPreProductionDateResultLog);
            //需排产量减少
            needProductionQty = leftOverNeedProductionQty;
            //排产日迭代
            startProductionDate = nextProductionDate;
        }
        return needProductionQty;
    }

    /**
     * 对排产计划安排两副模具排产
     *
     * @param productionPlan      排产计划
     * @param productionMouldList 模具
     * @param needProductionQty   需排产量
     * @param deliveryDay         交期日
     * @param maxMouldInfoList    最大模具列表
     * @param productionContext   排产上下文
     * @param singleCuringTime    单条硫化时间
     * @return
     */
    private Long formalDoubleMouldProduction(MonthPlanManufacturingRequirementVo productionPlan, List<MouldInfoVO> productionMouldList, Long needProductionQty, Integer deliveryDay, List<MouldInfoVO> maxMouldInfoList, ProductionContext productionContext, BigDecimal singleCuringTime) {
        String productCode = productionPlan.getProductCode();
        String sizeCapacityKey = productionPlan.getSizeCapacityGroupKey();
        //分组值为空，则表示新挑选上来的模具，则此时需要赋值分组值和排产方向
        MouldUtils.setGroupValueAndProductionOrient(productionMouldList, maxMouldInfoList, deliveryDay, true);
        MouldInfoVO first = productionMouldList.get(0);
        MouldInfoVO second = productionMouldList.get(1);
        String mouldCodeInfo = String.format("[%s]、[%s]", first.getMouldCode(), second.getMouldCode());
        //排产方向
        ProductionOrientEnum productionOrient = first.getProductionOrient();
        //排产流程日志记录
        String productionStartFormat = "[%s]计划物料正式排产：模具号%s，排产到[%d]日，排产方向：[%s]";
        String doubleMouldProductionLogContent = String.format(productionStartFormat, productCode, mouldCodeInfo, deliveryDay, productionOrient.getDesc());
        log.info(doubleMouldProductionLogContent);
        MouldProductionLog doubleMouldProductionLog = ProductionLogUtils.buildProductionLog(productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_DELIVERY_LOG, doubleMouldProductionLogContent);
        saveProductionLog(productionContext, doubleMouldProductionLog);
        //排产起始日
        Integer startProductionDate = MouldUtils.getStartProductionDate(productionMouldList);
        //排产截止日
        Integer endProductionDate = MouldUtils.getEndProductionDate(productionMouldList);
        ProductionInfoVo finalFirstProductionInfo = null;
        ProductionInfoVo finalSecondProductionInfo = null;
        String productionResultFormat = "[%s]计划物料排产日[%d]：模具号%s，排产量：[%d]，排产方向：[%s]";
        for (; MouldUtils.isDateProduction(startProductionDate, endProductionDate, productionOrient); ) {
            if (needProductionQty <= 1) {
                break;
            }
            //得到下一个排产日
            Integer nextProductionDate = MouldUtils.getNextProductionDate(productionContext, startProductionDate, productionOrient);
            //20251013 ZLT 校验成型硫化配比控制
            ProductionLimitTypeEnum limitType = productionContext.isReachTheLimit(false, productionOrient, startProductionDate, productionMouldList.size(), productionPlan);
            if (ProductionLimitTypeEnum.DAY_MOULD_QTY_LIMIT == limitType) {
                ProductionLogUtils.addProductionMouldQtyLimitLog(productionContext, MouldProductionLogType.SINGLE_PLAN_DELIVERY_LOG, productionPlan, startProductionDate);
                startProductionDate = nextProductionDate;
                continue;
            }
            //校验是否达到规格数限制
            if (!productionContext.isAddProduct(false, productionOrient, startProductionDate, productCode, productionPlan)) {
                if (!productionContext.getFactoryStopDays().contains(startProductionDate)) {
                    ProductionLogUtils.addProductionProductNumberLimitLog(productionContext, MouldProductionLogType.SINGLE_PLAN_DELIVERY_LOG, productionPlan, startProductionDate);
                }
                startProductionDate = nextProductionDate;
                continue;
            }
            //双模排产量 20250903 续作排产标识-false
            DayProductionPlanInfoVo dayProductionPlanInfo = new DayProductionPlanInfoVo(productionPlan.getMonthPlanId(), productCode, sizeCapacityKey, startProductionDate, needProductionQty, singleCuringTime, false);
            Map<String, ProductionInfoVo> productionInfoMap = MouldUtils.calculateProductionQty(productionMouldList, dayProductionPlanInfo, productionContext, false);
            ProductionInfoVo firstProductionInfo = productionInfoMap.get(first.getMouldCode());
            ProductionInfoVo secondProductionInfo = productionInfoMap.get(second.getMouldCode());
            Long productionQty = firstProductionInfo.getProductionQty() + secondProductionInfo.getProductionQty();
            //交期双模正式排产结果：排产流程日志打印及保存记录
            String doubleMouldProductionDateResultLogContent = String.format(productionResultFormat, productionPlan.getProductCode(), firstProductionInfo.getProductionDate(), mouldCodeInfo, productionQty, productionOrient.getDesc());
            log.info(doubleMouldProductionDateResultLogContent);
            MouldProductionLog doubleMouldProductionDateResultLog = ProductionLogUtils.buildProductionLog(productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_DELIVERY_LOG, doubleMouldProductionDateResultLogContent);
            saveProductionLog(productionContext, doubleMouldProductionDateResultLog);
            finalFirstProductionInfo = firstProductionInfo;
            finalSecondProductionInfo = secondProductionInfo;
            //更新模具日排产信息
            updateMouldDayProductionInfo(null, productionMouldList.get(0), productionPlan, firstProductionInfo, productionContext, false);
            updateMouldDayProductionInfo(null, productionMouldList.get(1), productionPlan, secondProductionInfo, productionContext, false);
            //需排产量减少
            needProductionQty = needProductionQty - productionQty;
            //20251011 ZLT 增加天排产模具数信息
            handlerProductionMouldQty(productionContext, productionOrient, productionPlan, startProductionDate, needProductionQty, productionMouldList, false);
            //排产日迭代
            startProductionDate = nextProductionDate;
        }
        //更新模具的当前排产信息
        if (null != finalFirstProductionInfo) {
            MouldUtils.setMouldCurrentProductionInfo(first, finalFirstProductionInfo, productCode, productionContext);
            MouldUtils.setMouldCurrentProductionInfo(second, finalSecondProductionInfo, productCode, productionContext);
        }
        return needProductionQty;
    }

    /**
     * 单模具预排
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划
     * @param mouldInfo         排产模具
     * @param needProductionQty 需排产量
     * @param singleCuringTime  单条硫化时间
     */
    private Long preSingleMouldProduction(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, MouldInfoVO mouldInfo, Long needProductionQty, BigDecimal singleCuringTime) {
        String productCode = productionPlan.getProductCode();
        //获取模具当前排产日
        Integer startProductionDate = mouldInfo.getBeginDay();
        String sizeCapacityKey = productionPlan.getSizeCapacityGroupKey();
        //获取模具排产截止日
        Integer endDate = mouldInfo.getEndDay();
        String mouldCode = mouldInfo.getMouldCode();
        //模具排产方向
        ProductionOrientEnum productionOrient = mouldInfo.getProductionOrient();
        String productionResultFormat = "[%s]计划物料在排产日[%d]预排：模具号[%s]，排产量：[%d]，排产方向：[%s]";
        for (; MouldUtils.isDateProduction(startProductionDate, endDate, productionOrient); ) {
            if (needProductionQty <= 0) {
                break;
            }
            //获取下一个排产日
            Integer nextProductionDate = MouldUtils.getNextProductionDate(productionContext, startProductionDate, productionOrient);
            //校验是否达到规格数限制
            if (!productionContext.isAddProduct(true, productionOrient, startProductionDate, productCode, productionPlan)) {
                ProductionLogUtils.addProductionProductNumberLimitLog(productionContext, MouldProductionLogType.SINGLE_PLAN_DELIVERY_LOG, productionPlan, startProductionDate);
                //排产日迭代
                startProductionDate = nextProductionDate;
                continue;
            }
            //20250903 续作排产标识-false
            DayProductionPlanInfoVo dayProductionPlanInfo = new DayProductionPlanInfoVo(productionPlan.getMonthPlanId(), productCode, sizeCapacityKey, startProductionDate, needProductionQty, singleCuringTime, false);
            //20250411 洗模后，不再是全天无产能，按扣减产能计算启用剩余产能，故而用标记方式处理
            boolean isCleaningDay = hasCleaningDay(productionContext, mouldInfo, productCode, startProductionDate);
            if (isCleaningDay) {
                mouldInfo.setIsClearMould(true);
            } else {
                mouldInfo.setIsClearMould(false);
            }
            ProductionInfoVo productionInfo = MouldUtils.calculateProductionQty(mouldInfo, dayProductionPlanInfo, productionContext, true);
            Long productionQty = productionInfo.getProductionQty();
            //剩余还需排产量
            Long leftOverNeedProductionQty = needProductionQty - productionQty;
            //20250414 交期预排-预排日产能存储-临时存储
            productionContext.addDayPreProductionQty(startProductionDate, sizeCapacityKey, productionQty, mouldCode);
            //交期单模日预排结束：排产流程日志打印及保存记录
            String lastMouldProductionDateResultLogContent = String.format(productionResultFormat, productCode, startProductionDate, mouldCode, productionQty, productionOrient.getDesc());
            log.info(lastMouldProductionDateResultLogContent);
            MouldProductionLog lastMouldProductionDateResultLog = ProductionLogUtils.buildProductionLog(productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_DELIVERY_LOG, lastMouldProductionDateResultLogContent);
            saveProductionLog(productionContext, lastMouldProductionDateResultLog);
            //更新需排产量
            needProductionQty = leftOverNeedProductionQty;
            //排产日迭代
            startProductionDate = nextProductionDate;
        }
        return needProductionQty;
    }

    /**
     * 得到双模预排结果信息
     *
     * @param productionContext   排产上下文
     * @param productionMouldList 排产模具
     * @param monthPlanId         排产计划ID
     * @param productCode         排产规格
     * @param sizeCapacityKey     寸口|*|成型法
     * @param startProductionDate 排产日期
     * @param needProductionQty   排产量
     * @param singleCuringTime    单条硫化时间
     * @return
     */
    private Map<String, ProductionInfoVo> getPreDoubleProductionInfo(ProductionContext productionContext,
                                                                     List<MouldInfoVO> productionMouldList,
                                                                     Long monthPlanId,
                                                                     String productCode,
                                                                     String sizeCapacityKey,
                                                                     Integer startProductionDate,
                                                                     Long needProductionQty,
                                                                     BigDecimal singleCuringTime) {
        MouldInfoVO first = productionMouldList.get(0);
        MouldInfoVO second = productionMouldList.get(1);
        boolean firstIsCleaningDay = hasCleaningDay(productionContext, first, productCode, startProductionDate);
        boolean secondIsCleaningDay = hasCleaningDay(productionContext, second, productCode, startProductionDate);
        //20250411 洗模后，不再是全天无产能，按扣减产能计算启用剩余产能，故而用标记方式处理
        if (firstIsCleaningDay) {
            first.setIsClearMould(true);
        } else {
            first.setIsClearMould(false);
        }
        if (secondIsCleaningDay) {
            second.setIsClearMould(true);
        } else {
            second.setIsClearMould(false);
        }
        //20250903 续作排产标识-false
        DayProductionPlanInfoVo dayProductionPlanInfo = new DayProductionPlanInfoVo(monthPlanId, productCode, sizeCapacityKey, startProductionDate, needProductionQty, singleCuringTime, false);
        return MouldUtils.calculateProductionQty(productionMouldList, dayProductionPlanInfo, productionContext, true);
    }

    /**
     * 预排判断是否洗模日的判断
     * 1、根据模具已排产日列表信息--如果是空模具排产
     * 1.1、正向排产，排产日-起始日的间隔时间，如果为(连续天数+1)的倍数，则为洗模日
     * 1.2、反向排产，则起始日-排产日的间隔时间，如果为(连续天数+1)的倍数，则为洗模日
     * 2、如果有排产
     * 2.1、正向排产，获取最大排产日，往前查找每日最后一个规格，如果与当前规格相等，则连续排产天数 + 1，直到不是连续(最后一个规格不一致、停工日、维修日、洗模日)
     * 根据【连续天数 + （排产日 - 最大排产日）】% (连续天数+1) == 0 则为洗模日
     * 2.2、反向排产，获取最大排产日，往前查找每日最后一个规格，如果与当前规格相等，则连续排产天数 + 1，直到不是连续(最后一个规格不一致、停工日、维修日、洗模日)
     * 根据【连续天数 + （排产日 - 最大排产日）】% (连续天数+1) == 0 则为洗模日
     *
     * @param productionContext 排产上下文
     * @param mouldInfo         模具信息
     * @param productCode       当前排产规格
     * @param productionDate    排产日期
     * @return
     */
    private boolean hasCleaningDay(ProductionContext productionContext, MouldInfoVO mouldInfo, String productCode, Integer productionDate) {
        Integer continueCleanDay = (Integer) productionContext.getFactoryParams().get(FactoryConstant.SYS_PARAM_CONNECTION_SCHEDULING_DAYS);
        if (null == continueCleanDay || continueCleanDay <= 0) {
            return false;
        }
        //排产方向
        ProductionOrientEnum productionOrient = mouldInfo.getProductionOrient();
        //已排产信息
        Map<Integer, List<MouldDayProductionVo>> dayProductionMap = mouldInfo.getDayProductionMap();
        if (CollectionUtils.isEmpty(dayProductionMap)) {
            int continueDay;
            if (ProductionOrientEnum.FORWARD == productionOrient) {
                continueDay = productionDate - mouldInfo.getBeginDay() + 1;
            } else {
                continueDay = mouldInfo.getBeginDay() - productionDate + 1;
            }
            if (continueDay == 1) {
                return false;
            }
            return (continueDay - 1) % continueCleanDay == 0;
        }
        //正向排产
        if (ProductionOrientEnum.FORWARD == productionOrient) {
            return isCleaningDayByForward(mouldInfo, productCode, productionDate, continueCleanDay);
        }
        Set<Integer> daySet = dayProductionMap.keySet();
        Optional<Integer> max = daySet.stream().max(Comparator.comparing(Function.identity()));
        //当前排产最大天数
        Integer maxDay = max.get();
        //间隔天数
        int intervalDay = productionDate - maxDay;
        int continuedDay = continuousCalculation(productCode, maxDay, mouldInfo);
        int preContinueDay = intervalDay + continuedDay;
        if (preContinueDay <= 1) {
            return false;
        }
        return (preContinueDay - 1) % continueCleanDay == 0;
    }

    /**
     * 预排正向排产-是否为洗模日
     * 1、得到已排产中的最大日到当前排产日的间隔天数
     * 2.已排产日的总数 + 间隔日数 < 连续排产 + 1 则一定不为洗模日
     * 3、从最大排产日往前推，排产的最后一个规格是否为当前规格，如果是则已连续排产天数+1
     * 3.1、如果中间出现停工日、维修日、洗模日则不再循环
     * 判断 已连续排产天数 + 间隔天数是否为(连续排产需洗模参数+1)的倍数，是
     * 则需要洗模
     *
     * @param mouldInfo        模具信息
     * @param productCode      当前需要排产的规格
     * @param productionDate   当前排产日
     * @param continueCleanDay 连续排产需洗模参数
     * @return
     */
    private boolean isCleaningDayByForward(MouldInfoVO mouldInfo, String productCode, Integer productionDate, int continueCleanDay) {
        Map<Integer, List<MouldDayProductionVo>> dayProductionMap = mouldInfo.getDayProductionMap();
        Set<Integer> daySet = dayProductionMap.keySet();
        Optional<Integer> max = daySet.stream().max(Comparator.comparing(Function.identity()));
        //当前排产最大天数
        Integer maxDay = max.get();
        //间隔天数
        int intervalDay = productionDate - maxDay;
        //间隔天数 + 已排天数 < 洗模倍数
        if ((daySet.size() + intervalDay) < (continueCleanDay + 1)) {
            return false;
        }
        int continuedDay = continuousCalculation(productCode, maxDay, mouldInfo);
        int preContinueDay = intervalDay + continuedDay;
        if (preContinueDay <= 1) {
            return false;
        }
        return (preContinueDay - 1) % continueCleanDay == 0;
    }

    /**
     * 计算连续排产天数
     *
     * @param productCode 当前排产规格
     * @param maxDay      最大排产日
     * @param mouldInfo   模具信息
     * @return
     */
    private int continuousCalculation(String productCode, Integer maxDay, MouldInfoVO mouldInfo) {
        Map<Integer, List<MouldDayProductionVo>> dayProductionMap = mouldInfo.getDayProductionMap();
        //已连续排产天数
        int continuedDay = 0;
        for (int index = maxDay; index >= 1; index--) {
            //停工日、维修日、洗模日
            if (!dayProductionMap.containsKey(index)) {
                break;
            }
            //没有排产列表
            List<MouldDayProductionVo> dayProductionList = dayProductionMap.get(index);
            if (CollectionUtils.isEmpty(dayProductionList)) {
                break;
            }
            String last = dayProductionList.get(dayProductionList.size() - 1).getProductCode();
            //不连续
            if (!productCode.equals(last)) {
                break;
            }
            continuedDay = continuedDay + 1;
            if (mouldInfo.getCleanDayList().containsKey(index)) {
                break;
            }
        }
        return continuedDay;
    }
}