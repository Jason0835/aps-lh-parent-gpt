package com.zlt.aps.factory.scheduling.moulding.single;

import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.enums.ProductionLimitTypeEnum;
import com.zlt.aps.factory.enums.ProductionOrientEnum;
import com.zlt.aps.factory.scheduling.AbstractProductionBusinessService;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.factory.scheduling.moulding.SinglePlanProductionContext;
import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import com.zlt.aps.factory.utils.*;
import com.zlt.aps.monthplan.api.domain.entity.MouldProductionLog;
import com.zlt.aps.monthplan.api.enums.MouldProductionLogType;
import com.zlt.common.utils.PubUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通用单计划模具排产
 * 根据计划的物料配置的可用模具逐一进行排产
 * 对可用模具进行按先续作，再次已排，再次可硫化时间，最后模具编号方式
 * 先用两副，再用两副逐一增模方式按日排产
 *
 * @author
 */
@Slf4j
@Service(value = "generalSinglePlanProductionService")
public class GeneralSinglePlanProductionService extends AbstractProductionBusinessService {

    public GeneralSinglePlanProductionService(ProductionSchedulingDataService dataService) {
        super(dataService);
    }

    /**
     * 单计划通用排产
     * 根据计划的物料编码及对应的可用模具(通过物料与模具关系、模具月可用状态、模具维修返厂配置)按模具按日排产
     * 如果可用模具为1，则进行单模排产-一直按日排产，直到排产计划量 = 0 或是 模具没有产能
     * 如果可用模具>1，则使用逐渐加模生产方式-即先用两副模逐日排产，两模产能分配完后，再取两副模，直到排产计划量 = 0或是1或是
     * 模具没有产能
     *
     * @param context 排产上下文
     * @param userObj 用户数据
     */
    @Override
    public void run(Context context, Object userObj) {
        SinglePlanProductionContext singlePlanProductionContext = (SinglePlanProductionContext) context;
        ProductionContext productionContext = singlePlanProductionContext.getGroupContext().getProductionContext();
        MonthPlanManufacturingRequirementVo productionPlan = singlePlanProductionContext.getProductionPlan();
        if (productionContext.isProductionFinishPlan(productionPlan.getMonthPlanId())) {
            log.warn("排产计划已排产完毕。。。。无需再次排产");
            return;
        }
        BigDecimal singleCuringTime = ProductUtils.getSingleCuringTime(productionPlan, productionContext);
        List<MouldInfoVO> mouldList = singlePlanProductionContext.getEnableMouldList();
        List<MouldInfoVO> enableMouldList = mouldList.stream().filter(mouldInfo -> !PubUtil.isTrue(mouldInfo.getIsFinish())).collect(Collectors.toList());
        int mouldSize = enableMouldList.size();
        //排产流程日志记录
        String logInfoContent = String.format("===分厂%s, 计划年月：%d-%d, 计划版本：%s，生产计划单计划%d 通用计划排产开始：计划排产量%d,可用模具数%d,单条硫化时间%d====", productionContext.getFactoryCode(), productionContext.getYear(), productionContext.getMonth(), productionContext.getProductionVersion(), productionPlan.getMonthPlanId(), productionPlan.getProductionQty(), mouldSize, singleCuringTime.longValue());
        log.info(logInfoContent);
        MouldProductionLog logInfo = ProductionLogUtils.buildProductionLog(productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_GENERAL_LOG, logInfoContent);
        saveProductionLog(productionContext, logInfo);
        //单模具排产
        if (mouldSize == 1) {
            singleMouldGeneralProductionPlan(productionContext, productionPlan, enableMouldList.get(0));
            return;
        }
        //20250829 已经判断完拼模，则无需走单模判断,直接多模具排产
        multiMouldGeneralProductionPlan(productionContext, productionPlan, enableMouldList, singleCuringTime);
        //计划排产结束 [%d]计划多模具排产完成。。。 排产日志打印及保存记录
        ProductionLogUtils.addPlanProductionFinishLog(productionContext, false, productionPlan, MouldProductionLogType.SINGLE_PLAN_GENERAL_LOG, "多");
    }

    /**
     * 单模排产
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划
     * @param singleMouldInfo   模具
     */
    private void singleMouldGeneralProductionPlan(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, MouldInfoVO singleMouldInfo) {
        //排产流程日志记录
        MouldProductionLog singleMouldLog = ProductionLogUtils.buildProductionLog(productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_GENERAL_LOG, "计划只用1副模具");
        saveProductionLog(productionContext, singleMouldLog);
        if (null == singleMouldInfo.getProductionOrient()) {
            singleMouldInfo.setGroupValue(1);
            singleMouldInfo.setProductionOrient(ProductionOrientEnum.FORWARD);
            singleMouldInfo.setBeginDay(BigDecimal.ONE.intValue());
            singleMouldInfo.setEndDay(productionContext.getMonthDays());
        }
        singleMouldProduction(null, singleMouldInfo, productionContext, productionPlan, productionPlan.getProductionQty());
        //增加计划排产结束日志记录--[%d]计划单模具排产完成。。。
        ProductionLogUtils.addPlanProductionFinishLog(productionContext, false, productionPlan, MouldProductionLogType.SINGLE_PLAN_GENERAL_LOG, "单");
    }

    /**
     * 是否可进行单模排产，可则直接单模排产
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划
     * @param enableMouldList   模具列表
     * @param singleCuringTime  单条硫化时间(包含间隔)
     * @return
     */
    private boolean isSingleProduction(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, List<MouldInfoVO> enableMouldList, BigDecimal singleCuringTime) {
        Long singleMouldCapacity = MouldUtils.getSingleMouldCapacity(productionContext, singleCuringTime);
        Long needProductionQty = productionPlan.getProductionQty();
        if (needProductionQty > singleMouldCapacity) {
            return false;
        }
        List<MouldInfoVO> productionMouldList = MouldUtils.getOptimalMouldList(productionPlan, enableMouldList, singleCuringTime);
        if (CollectionUtils.isEmpty(productionMouldList)) {
            return false;
        }
        //判断前面是否多模排产
        List<MouldInfoVO> productionProductCodeMouldList = MouldUtils.getProductionProductCodeMould(enableMouldList, productionPlan);
        if (!CollectionUtils.isEmpty(productionProductCodeMouldList) && productionProductCodeMouldList.size() > BigDecimal.ONE.intValue()) {
            return false;
        }
        MouldInfoVO singleMould = productionMouldList.get(0);
        Map<Integer, List<MouldDayProductionVo>> dayProductionMap = singleMould.getDayProductionMap();
        if (CollectionUtils.isEmpty(dayProductionMap)) {
            singleMouldGeneralProductionPlan(productionContext, productionPlan, singleMould);
            return true;
        }
        return false;
    }

    /**
     * 多模具排产计划
     *
     * @param productionContext
     * @param productionPlan
     * @param enableMouldList
     * @param singleCuringTime
     */
    private void multiMouldGeneralProductionPlan(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, List<MouldInfoVO> enableMouldList, BigDecimal singleCuringTime) {
        //排产处理
        Long needProductionQty = multiMouldHandler(productionContext, productionPlan, enableMouldList, singleCuringTime);
        int mouldSize = enableMouldList.size();
        String productCode = productionPlan.getProductCode();
        Long monthPlanId = productionPlan.getMonthPlanId();
        MonthPlanManufacturingRequirementVo originalPlan = productionContext.getMonthPlanInitMap().get(monthPlanId);
        List<MouldInfoVO> maxMouldInfoList = MouldUtils.getMaxEnableMouldList(productionContext, productCode);
//        Long needProductionQty = productionPlan.getProductionQty();
        int groupCount = mouldSize / 2;
        int remainder = mouldSize % 2;
//        //1、优先获取硫化时间满足的模具,pancd+ 2025.03.26
//        List<MouldInfoVO> productionMouldList = MouldUtils.getOptimalMouldList(productionPlan, enableMouldList, singleCuringTime);
//        if (PubUtil.isNotEmpty(productionMouldList)) {
//            if (ProductionPlanUtils.isExceedCapacity(productionContext, productionPlan, 0)) {
//                //20250903 ZLT 排产流程日志记录
//                ProductionLogUtils.addSkipProductionByExceedCapacity(productionContext, productionPlan);
//            } else {
//                //双模排产
//                needProductionQty = doubleMouldProduction(productionContext, productionPlan, productionMouldList, maxMouldInfoList, needProductionQty, singleCuringTime);
//            }
//        } else {
//            //2、双模没有直接满足的，则按续作优先 > 已排优先 > 物料关联数 > 分组值 > 剩余硫化时间多 > 模具编号
//            enableMouldList.sort(MouldUtils.buildMouldSortComparator());
//            //两副两副
//            for (int index = 0; index < groupCount; index++) {
//                if (ProductionPlanUtils.isExceedCapacity(productionContext, productionPlan, index)) {
//                    //20250903 ZLT 排产流程日志记录
//                    ProductionLogUtils.addSkipProductionByExceedCapacity(productionContext, productionPlan);
//                    break;
//                }
//                int startIndex = index * 2;
//                int endIndex = (index + 1) * 2;
//                productionMouldList = enableMouldList.subList(startIndex, endIndex);
//                //双模排产
//                needProductionQty = doubleMouldProduction(productionContext, productionPlan, productionMouldList, maxMouldInfoList, needProductionQty, singleCuringTime);
//                //剩余一个直接退出双模
//                if (needProductionQty <= 1) {
//                    break;
//                }
//            }
//        }
        //已排产完毕
        if (needProductionQty <= 0) {
            //标记不可排产了
            originalPlan.setIsProduction(YesOrNoEnum.NO.getValue());
            return;
        }
        //先双模排产，出现剩余1，则直接不排了
        if (needProductionQty == 1) {
            originalPlan.setIsProduction(YesOrNoEnum.NO.getValue());
            //双模排产不排单
            String noProductionReason = NoProductionReasonUtils.getDoubleNoSingle();
            originalPlan.addNoProductionReasonAndQty(noProductionReason, needProductionQty);
            //排产流程日志记录
            ProductionLogUtils.addDoubleMouldNoProductionSingle(noProductionReason, productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_GENERAL_LOG);
            return;
        }
        //双模模具，模具产能不足
        if (remainder == 0) {
            originalPlan.setIsProduction(YesOrNoEnum.NO.getValue());
            //模具产能不足
            String noProductionReason = NoProductionReasonUtils.getMouldNotEnough();
            if (Boolean.TRUE.equals(originalPlan.getIsCapacityLimit())) {
                noProductionReason = NoProductionReasonUtils.getDayLimit();
            }
            originalPlan.addNoProductionReasonAndQty(noProductionReason, needProductionQty);
            //排产流程日志记录
            ProductionLogUtils.addDoubleMouldProductionResult(noProductionReason, productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_GENERAL_LOG);
            return;
        }
        //20250903 ZLT 超出成型配置产能导致不能排产
        if (ProductionPlanUtils.isExceedCapacity(productionContext, productionPlan, groupCount + BigDecimal.ZERO.intValue())) {
            originalPlan.setIsProduction(YesOrNoEnum.NO.getValue());
            //超出成型配比产能
            String noProductionReason = NoProductionReasonUtils.getExceedRatioCapacity();
            originalPlan.addNoProductionReasonAndQty(noProductionReason, needProductionQty);
            return;
        }
        //剩余最后一副模排剩余量
        MouldInfoVO lastMouldInfo = enableMouldList.get(mouldSize - 1);
        MouldUtils.setLastMouldInfo(lastMouldInfo, maxMouldInfoList, productionContext.getMonthDays(), false);
        singleMouldProduction(null, lastMouldInfo, productionContext, productionPlan, needProductionQty);
    }

    /**
     * 多模具排产处理，如果只有双模，则直接双模
     * 如果多于双模，则挑选最合适的双模看能否满足，否则多模排产
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划
     * @param enableMouldList   可用模具
     * @param singleCuringTime  单条硫化时间(含间隔)
     * @return
     */
    private Long multiMouldHandler(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, List<MouldInfoVO> enableMouldList, BigDecimal singleCuringTime) {
        int mouldSize = enableMouldList.size();
        String productCode = productionPlan.getProductCode();
        List<MouldInfoVO> maxMouldInfoList = MouldUtils.getMaxEnableMouldList(productionContext, productCode);
        Long needProductionQty = productionPlan.getProductionQty();
        //1、只有双模
        if (mouldSize == ProductionConstant.DOUBLE_MOULD_PRODUCTION) {
            return doubleMouldProduction(productionContext, productionPlan, enableMouldList, maxMouldInfoList, needProductionQty, singleCuringTime);
        }
        //2、多于双模，优先获取硫化时间满足的模具,pancd+ 2025.03.26
        List<MouldInfoVO> productionMouldList = MouldUtils.getOptimalMouldList(productionPlan, enableMouldList, singleCuringTime);
        if (PubUtil.isNotEmpty(productionMouldList)) {
            if (ProductionPlanUtils.isExceedCapacity(productionContext, productionPlan, 0)) {
                //20250903 ZLT 排产流程日志记录
                ProductionLogUtils.addSkipProductionByExceedCapacity(productionContext, productionPlan);
                return needProductionQty;
            }
            //双模排产
            return doubleMouldProduction(productionContext, productionPlan, productionMouldList, maxMouldInfoList, needProductionQty, singleCuringTime);
        }
        int groupCount = mouldSize / 2;
        //2、双模没有直接满足的，则按续作优先 > 已排优先 > 物料关联数 > 分组值 > 剩余硫化时间多 > 模具编号
        enableMouldList.sort(MouldUtils.buildMouldSortComparator());
        //两副两副
        for (int index = 0; index < groupCount; index++) {
            if (ProductionPlanUtils.isExceedCapacity(productionContext, productionPlan, index)) {
                //20250903 ZLT 排产流程日志记录
                ProductionLogUtils.addSkipProductionByExceedCapacity(productionContext, productionPlan);
                break;
            }
            int startIndex = index * 2;
            int endIndex = (index + 1) * 2;
            productionMouldList = enableMouldList.subList(startIndex, endIndex);
            //双模排产
            needProductionQty = doubleMouldProduction(productionContext, productionPlan, productionMouldList, maxMouldInfoList, needProductionQty, singleCuringTime);
            //剩余一个直接退出双模
            if (needProductionQty <= 1) {
                break;
            }
        }
        return needProductionQty;
    }

    /**
     * 双模排产计划
     *
     * @param productionContext   排产上下文
     * @param productionPlan      排产计划
     * @param productionMouldList 双模模具
     * @param maxMouldInfoList    最大可用模具
     * @param needProductionQty   需排产量
     * @param singleCuringTime    单条硫化时间
     * @return
     */
    private Long doubleMouldProduction(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, List<MouldInfoVO> productionMouldList, List<MouldInfoVO> maxMouldInfoList, Long needProductionQty, BigDecimal singleCuringTime) {
        String productCode = productionPlan.getProductCode();
        String sizeCapacityKey = productionPlan.getSizeCapacityGroupKey();
        //分组值为空，则表示新挑选上来的模具，则此时需要赋值分组值和排产方向
        MouldUtils.setGroupValueAndProductionOrient(productionMouldList, maxMouldInfoList, productionContext.getMonthDays(), false);
        MouldInfoVO first = productionMouldList.get(0);
        MouldInfoVO second = productionMouldList.get(1);
        //移动规格位置，pancd+ 2025.03.28
        moveProductPosition(productionContext, first, second);
//        moveProductPosition(first, productionContext);
//        moveProductPosition(second, productionContext);
        //排产方向
        ProductionOrientEnum productionOrient = first.getProductionOrient();
        //排产起始日
        Integer startProductionDate = MouldUtils.getStartProductionDate(productionMouldList);
        //排产截止日
        Integer endProductionDate = MouldUtils.getEndProductionDate(productionMouldList);
        //双模排产开始： [%d]计划使用[%s]、[%s]模具[%s]排产,从[%d]-[%d]日进行排产，需排产量[%d] 流程日志打印及保存记录
        String mouldCodeInfo = String.format("[%s]、[%s]", first.getMouldCode(), second.getMouldCode());
        String dateRange = String.format("[%d]-[%d]", startProductionDate, endProductionDate);
        ProductionLogUtils.addProductionPlanCycleMouldStartLog(productionContext, MouldProductionLogType.SINGLE_PLAN_GENERAL_LOG, productionPlan, mouldCodeInfo, productionOrient, dateRange, needProductionQty);
        //开始计算
        ProductionInfoVo finalFirstProductionInfo = null;
        ProductionInfoVo finalSecondProductionInfo = null;
        for (; MouldUtils.isDateProduction(startProductionDate, endProductionDate, productionOrient); ) {
            if (needProductionQty <= 1) {
                break;
            }
            //得到下一个排产日
            Integer nextProductionDate = MouldUtils.getNextProductionDate(productionContext, startProductionDate, productionOrient);
            //20251013 ZLT 校验成型硫化配比控制
            ProductionLimitTypeEnum limitType = productionContext.isReachTheLimit(false, productionOrient, startProductionDate, productionMouldList.size(), productionPlan);
            if (ProductionLimitTypeEnum.DAY_MOULD_QTY_LIMIT == limitType) {
                ProductionLogUtils.addProductionMouldQtyLimitLog(productionContext, MouldProductionLogType.SINGLE_PLAN_GENERAL_LOG, productionPlan, startProductionDate);
                startProductionDate = nextProductionDate;
                continue;
            }
            //校验是否达到规格数限制
            if (!productionContext.isAddProduct(false, productionOrient, startProductionDate, productCode, productionPlan)) {
                if (!productionContext.getFactoryStopDays().contains(startProductionDate)) {
                    ProductionLogUtils.addProductionProductNumberLimitLog(productionContext, MouldProductionLogType.SINGLE_PLAN_GENERAL_LOG, productionPlan, startProductionDate);
                }
                startProductionDate = nextProductionDate;
                continue;
            }
            //双模排产量 20250903 续作排产标识-false
            DayProductionPlanInfoVo dayProductionPlanInfo = new DayProductionPlanInfoVo(productionPlan.getMonthPlanId(), productCode, sizeCapacityKey, startProductionDate, needProductionQty, singleCuringTime, false);
            Map<String, ProductionInfoVo> productionInfoMap = MouldUtils.calculateProductionQty(productionMouldList, dayProductionPlanInfo, productionContext, false);
            ProductionInfoVo firstProductionInfo = productionInfoMap.get(first.getMouldCode());
            ProductionInfoVo secondProductionInfo = productionInfoMap.get(second.getMouldCode());
            //双模单日总排产量
            Long productionQty = firstProductionInfo.getProductionQty() + secondProductionInfo.getProductionQty();
            //双模单日排产结果：[%d]计划使用[%s]、[%s]模具[%s]排产,在[%d]日排产量[%d] 日志打印及保存记录
            ProductionLogUtils.addProductionDateResultMouldProductionLog(productionContext, MouldProductionLogType.SINGLE_PLAN_GENERAL_LOG, productionPlan, mouldCodeInfo, productionOrient, startProductionDate, productionQty);
            //20250620 排产量为零，直接跳过
            if (productionQty < BigDecimal.ZERO.longValue()) {
                startProductionDate = nextProductionDate;
                continue;
            }
            //剩余还需排产量
            Long leftOverNeedProductionQty = needProductionQty - productionQty;
            finalFirstProductionInfo = firstProductionInfo;
            finalSecondProductionInfo = secondProductionInfo;
            //更新模具日排产信息
            updateMouldDayProductionInfo(null, first, productionPlan, firstProductionInfo, productionContext, false);
            updateMouldDayProductionInfo(null, second, productionPlan, secondProductionInfo, productionContext, false);
            //20251011 ZLT 增加天排产模具数信息
            handlerProductionMouldQty(productionContext, productionOrient, productionPlan, startProductionDate, leftOverNeedProductionQty, productionMouldList, false);
            //更新需排产量
            needProductionQty = leftOverNeedProductionQty;
            //迭代排产日
            startProductionDate = nextProductionDate;
        }
        //双模排产结果：[%d]计划使用[%s]、[%s]模具[%s]排产,从[%d]-[%d]日进行排产，还需要排产量[%d] 日志打印及保存记录
        ProductionLogUtils.addProductionCycleResultMouldProductionLog(productionContext, MouldProductionLogType.SINGLE_PLAN_GENERAL_LOG, productionPlan, mouldCodeInfo, productionOrient, dateRange, needProductionQty);
        //更新模具的当前排产信息
        if (null != finalFirstProductionInfo) {
            MouldUtils.setMouldCurrentProductionInfo(productionMouldList.get(0), finalFirstProductionInfo, productCode, productionContext);
            MouldUtils.setMouldCurrentProductionInfo(productionMouldList.get(1), finalSecondProductionInfo, productCode, productionContext);
        }
        return needProductionQty;
    }

}