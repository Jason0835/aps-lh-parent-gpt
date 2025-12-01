package com.zlt.aps.factory.scheduling;

import com.tlt.aps.enums.ConstructionStageEnum;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.ProductionGroupInfoDto;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.enums.ProductionLimitTypeEnum;
import com.zlt.aps.factory.enums.ProductionOrientEnum;
import com.zlt.aps.factory.utils.MouldBaseUtils;
import com.zlt.aps.factory.utils.MouldUtils;
import com.zlt.aps.factory.utils.ProductionLogUtils;
import com.zlt.aps.factory.utils.ProductionPlanUtils;
import com.zlt.aps.monthplan.api.domain.entity.FactoryNoProduction;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoProductionRecord;
import com.zlt.aps.monthplan.api.domain.entity.MouldProductionLog;
import com.zlt.aps.monthplan.api.enums.MouldProductionLogType;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 排产上下文
 *
 * @author ZLT
 * @date 20250229
 */
@Data
public class ProductionContext extends Context {

    /**
     * 操作批次号
     */
    private String operationWorkNo;
    /**
     * 月份天数
     */
    private Integer monthDays;
    /**
     * 月可排产天数
     */
    private Integer monthWorkDays;
    /**
     * 理论月可排产天明细
     */
    private Set<Integer> wholeMonthWorkDaySet;
    /**
     * 分厂停工日列表<日期>
     */
    private Set<Integer> factoryStopDays;
    /**
     * 分厂参数配置
     */
    private Map<String, Object> factoryParams;
    /**
     * 物料基础信息，Map<物料编码,物料信息>
     */
    private Map<String, ProductBaseInfoVo> productInfoMap;
    /**
     * 拆A率Map，Map<物料编码,拆A率>
     */
    private Map<String, BigDecimal> productDamageMap;
    /**
     * 物料库位的利润等级值Map<物料编码|*|库位编码,利润值>
     */
    private Map<String, Integer> productLocationProfitGradeMap;
    /**
     * 续作规格信息 key ProductCode value 模具列表
     */
    private Map<String, List<MouldProductionProductVo>> continueProductMap;
    /**
     * 物料的施工阶段
     * key 物料编码 value 施工阶段
     */
    private Map<String, ConstructionStageEnum> constructionStageMap;
    /**
     * 基础的施工信息
     * key 生胎代码 value 施工信息--胎体布信息
     */
    private Map<String, BaseConstructionVersionInfoVo> baseConstructionInfoMap;
    /**
     * 物料的施工关系配置
     * key 物料编码 value <硫化规格代号,施工信息></硫化规格代号,施工信息>
     */
    private Map<String, Map<String, ProductConstructionInfoVo>> constructionConfigurationMap;
    /**
     * 开始年月
     */
    private Integer startYearMonth;

    /**
     * 结束年月
     */
    private Integer finallyYearMonth;
    /**
     * 月度模具信息
     * key:模具号-》实例
     */
    private Map<String, MouldInfoVO> mouldInfoMap;

    /***
     *  模具大类分组信息
     *
     *  key:规格+花纹-》模具
     */
    private Map<String, List<MouldInfoVO>> sameMouldMap;
    /**
     * 物料配置的模具列表
     * key：物料编码
     * 值：模具号|*|规格代码
     */
    private Map<String, Set<String>> productRelationMouldMap;
    /**
     * 物料配置的模具列表，
     * 按规格代码再分组
     * key：物料编码
     * 值：{规格代号:模具号}-> key：规格代码 值 模具号
     */
    private Map<String, Map<String, Set<String>>> productRelationSpecCodeMouldMap;
    /**
     * 模具配置的物料列表
     * key：模具号
     * 值：物料编码
     */
    private Map<String, Set<String>> mouldRelationProductMap;
    /**
     * 分厂月计划初始化数据
     */
    private Map<Long, MonthPlanManufacturingRequirementVo> monthPlanInitMap;
    /**
     * 分厂不排产配置--搭配排产时需要再次判断
     */
    private Map<String, FactoryNoProduction> factoryNoProductionMap;
    /**
     * 不排产记录，用于未排计划使用
     */
    private Map<Long, MonthPlanNoProductionRecord> noProductionRecordMap;
    /**
     * 记录排产日志
     */
    private List<MouldProductionLog> productionLogs;
    /**
     * 一键排产时，临时存储使用
     */
    private List<MonthPlanManufacturingRequirementVo> monthPlanInitList;
    /**
     * 一键排产时，临时存储使用
     */
    private List<MonthPlanNoProductionRecord> noProductionRecordList;
    /**
     * 日志存储器
     */
    private StringBuilder logBuilder;
    /**
     * 排产参数配置项
     */
    private ProductionParamConfiguration productionParam;
    /**
     * 排产分组信息对象集合
     * key 分组编号 value 分组排产对象
     */
    private Map<String, ProductionGroupInfoDto> productionGroupInfoMap;
    /**
     * 续作排产分组信息对象集合
     */
    private Map<String, ContinueProductionGroupVo> continueProductionGroupMap;
    /**
     * 每日排产量汇总
     * 20250605 细化到每日按寸口|*|成型法|*|胎体布层级
     */
    private Map<Integer, Map<String, Long>> dayProductionQtyMap;
    /**
     * 每日排产规格数汇总
     */
    private Map<Integer, Set<String>> dayProductCodeMap;
    /**
     * S型排产-第二组双模已经出现超出成型产能的规格信息
     */
    private Map<String, Boolean> exceedCapacityProductMap;
    /**
     * 交期预排使用：--每次预排结束都需要清空
     * 日预排量
     */
    private Map<Integer, Map<String, Long>> preProductionDateQtyMap;
    /**
     * 交期预排使用：--每次预排结束都需要清空
     * 模具日预排量
     */
    private Map<String, Map<Integer, Long>> mouldPreProductionDateQtyMap;
    /**
     * 已排产计划ID及真实排产顺序
     */
    private Map<Long, Integer> productionSchedulePlanMap;
    /**
     * 续作模具需满月排产的规格，需开启SYS038，且月平均销量需大于SYS042
     */
    private Set<String> continueFullMonthProductionSet;
    /**
     * 排产周期--排产开始日
     */
    private Date productionStartDate;
    /**
     * 排产周期--排产结束日
     */
    private Date productionEndDate;
    /**
     * 寸口|*|工装类型|*|成型法|*|胎体布层级，月产能控制
     * key proSize|*|成型法|*|胎体布层级 value 月总产能
     */
    private Map<String, Long> sizeMonthCapacityMap;
    /**
     * 寸口|*|工装类型|*|成形法|*|胎体布层级，天产能控制
     * key day : value proSize|*|胎体布层级成型法|*|胎体布层级, 产能
     */
    private Map<Integer, Map<String, Long>> daySizeCapacityMap;
    /**
     * 寸口|*|工装类型|*|成形法|*|胎体布层级，天最大模具数--产能对等
     * key day : value proSize|*|工装类型|*|胎体布层级成型法|*|胎体布层级, 最大模具数
     * 20251010 ZLT 成型产能对等-使用模具数控制
     */
    private Map<Integer, Map<String, Integer>> dayMaxMouldQtyMap;
    /**
     * 寸口|*|工装类型|*|成形法|*|胎体布层级，天排产模具数--产能对等
     * key day : value proSize|*|工装类型|*|胎体布层级成型法|*|胎体布层级, 排产模具数
     * 20251011 ZLT 成型产能对等-使用模具数控制
     */
    private Map<Integer, Map<String, Integer>> dayProductionMouldQtyMap;
    /**
     * 20251014 ZLT
     * 成型产能对等使用模具数控制使用，计算日模具排产数使用，防止一个模具多次计算天排产模具数
     */
    private Map<Integer, Set<String>> dayProductionFinishMouldMap;
    /**
     * 每天的最大产能控制量
     */
    private Map<Integer, Long> dayMaxCapacityMap;
    /**
     * 轮胎类型 + 寸口 月产能控制
     * key 轮胎类型|*|proSize value 月总产能
     */
    private Map<String, Long> tireCapacityMap;
    /**
     * 最小批量
     */
    private Map<String, Long> minimumLotSizeMap;
    /**
     * 每日新增规格数限制量
     */
    private Map<Integer, Integer> dayAddedProductLimitMap;
    /**
     * 当前每日新增的规格数
     */
    private Map<Integer, Set<String>> dayAddProductMap;
    /**
     * 排产起始日--在排产周期的第几天
     */
    private Integer firstProductionDay;
    /**
     * 是否拼模排产--拼模排产使用
     */
    private boolean assemblingMouldProduction;
    /**
     * 拼模排产起始日--拼模排产使用
     */
    private Integer assemblingMouldStartDay;
    /**
     * 拼模排产下一个规格--拼模排产使用
     */
    private boolean assemblingMouldNextProductCode;
    /**
     * 已经不可拼的规格--拼模排产使用
     */
    private Set<String> noAssemblingMouldProductSet;

    /**
     * 判断排产日是否为周期第一个可排产日
     *
     * @param productionDate
     * @return
     */
    public boolean isProductionFirstDay(Integer productionDate) {
        if (null != firstProductionDay) {
            return firstProductionDay.equals(productionDate);
        }
        if (null == productionDate) {
            return false;
        }
        if (productionDate < ProductionConstant.MONTH_START_DAY) {
            return false;
        }
        if (productionDate > monthDays) {
            return false;
        }
        for (Integer day = ProductionConstant.MONTH_START_DAY; day <= monthDays; day++) {
            if (factoryStopDays.contains(day)) {
                continue;
            }
            if (null == firstProductionDay) {
                firstProductionDay = day;
                break;
            }
        }
        return productionDate.equals(firstProductionDay);
    }

    /**
     * 获取交期预排日--剩余可排产量
     * 因为预排时，不能直接对排产日的排产量增加，故而需要临时存储
     *
     * @param productionDate 预排日
     * @return
     */
    public Long getDayPreLeftOverQty(Integer productionDate, String sizeCapacityGroupKey) {
        Long currentLeftOverQty = getDayLeftOverQty(productionDate, sizeCapacityGroupKey);
        if (CollectionUtils.isEmpty(preProductionDateQtyMap)) {
            return currentLeftOverQty;
        }
        Map<String, Long> preProductionSizeCapacityQtyMap = preProductionDateQtyMap.get(productionDate);
        if (CollectionUtils.isEmpty(preProductionSizeCapacityQtyMap)) {
            return currentLeftOverQty;
        }
        Long preQty = preProductionSizeCapacityQtyMap.get(sizeCapacityGroupKey);
        if (null == preQty) {
            return currentLeftOverQty;
        }
        return currentLeftOverQty - preQty;
    }

    /**
     * 是否采用自然月进行排产
     *
     * @return
     */
    public boolean isNaturalMonth() {
        ProductionParamConfiguration productionParam = getProductionParam();
        if (null == productionParam) {
            return true;
        }
        Integer startDay = productionParam.getMonthCycleStartDay();
        if (null == startDay) {
            return true;
        }
        if (startDay > ProductionConstant.NO_NATURAL_MONTH_MAX_VALUE) {
            return true;
        }
        if (startDay <= ProductionConstant.MONTH_START_DAY) {
            return true;
        }
        return false;
    }

    /**
     * 当前排产月前一个月时间
     *
     * @return
     */
    public LocalDate getPreviousMonth() {
        LocalDate currentProductionMonth = LocalDate.of(getYear(), getMonth(), ProductionConstant.MONTH_START_DAY);
        LocalDate previousMonth = currentProductionMonth.minusMonths(BigDecimal.ONE.intValue());
        return previousMonth;
    }

    /**
     * 根据物料编码，获取续作规格
     *
     * @param productCode 物料编码
     * @param mouldCode   模具编号
     * @return
     */
    public String getContinueSpecCode(String productCode, String mouldCode) {
        if (StringUtils.isBlank(productCode) || StringUtils.isBlank(mouldCode)) {
            return null;
        }
        if (CollectionUtils.isEmpty(continueProductMap)) {
            return null;
        }
        List<MouldProductionProductVo> mouldProductionList = continueProductMap.get(productCode);
        if (CollectionUtils.isEmpty(mouldProductionList)) {
            return null;
        }
        for (MouldProductionProductVo mouldProduction : mouldProductionList) {
            String continueMouldCode = mouldProduction.getMouldCode();
            if (mouldCode.equals(continueMouldCode)) {
                return mouldProduction.getSpecCode();
            }
        }
        return null;
    }

    /**
     * 获取排产日剩余寸口|*|成形法的可排产量
     *
     * @param productionDate       排产日
     * @param sizeCapacityGroupKey 寸口|*|成型法|*|胎体布层级
     * @return
     */
    public Long getDayLeftOverQty(Integer productionDate, String sizeCapacityGroupKey) {
        Long currentProductionQty = getDayProductionQty(productionDate, sizeCapacityGroupKey);
        if (null == currentProductionQty) {
            currentProductionQty = BigDecimal.ZERO.longValue();
        }
        Long dayMaxProductionQty = getDayMaxProductionQty(productionDate, sizeCapacityGroupKey);
        if (null == dayMaxProductionQty) {
            return BigDecimal.ZERO.longValue();
        }
        return dayMaxProductionQty - currentProductionQty;
    }

    /**
     * 获取排产日剩余可排产量，无关寸口
     *
     * @param productionDate 排产日
     * @return
     */
    public Long getDayLeftOverQty(Integer productionDate) {
        Long currentProductionQty = getDayProductionQty(productionDate);
        if (null == currentProductionQty) {
            currentProductionQty = BigDecimal.ZERO.longValue();
        }
        Long dayMaxLimitQty = dayMaxCapacityMap.get(productionDate);
        if (null == dayMaxLimitQty) {
            return BigDecimal.ZERO.longValue();
        }
        return dayMaxLimitQty - currentProductionQty;
    }

    /**
     * 判断计划是否排产完毕
     *
     * @param monthPlanId 排产计划ID
     * @return
     */
    public boolean isProductionFinishPlan(Long monthPlanId) {
        if (null == monthPlanId) {
            return true;
        }
        if (null == productionSchedulePlanMap) {
            return false;
        }
        return productionSchedulePlanMap.containsKey(monthPlanId);
    }

    /**
     * 加入已排产完毕计划，并记录其顺序
     *
     * @param monthPlanId 排产计划ID
     */
    public void addProductionFinishPlan(Long monthPlanId) {
        if (null == monthPlanId) {
            return;
        }
        if (null == productionSchedulePlanMap) {
            return;
        }
        if (CollectionUtils.isEmpty(productionSchedulePlanMap)) {
            productionSchedulePlanMap.put(monthPlanId, BigDecimal.ONE.intValue());
            return;
        }
        Set<Long> productionFinishedSet = productionSchedulePlanMap.keySet();
        if (productionFinishedSet.contains(monthPlanId)) {
            return;
        }
        Integer currentSeq = productionFinishedSet.size();
        Integer nextSeq = currentSeq + 1;
        productionSchedulePlanMap.put(monthPlanId, nextSeq);
    }

    /**
     * 汇总每日的排产量
     *
     * @param productionDate 排产日
     * @param productionQty  排产量
     */
    public void addDayProductionQty(Integer productionDate, String sizeCapacityGroupKey, Long productionQty) {
        if (null == productionDate || null == productionQty || StringUtils.isBlank(sizeCapacityGroupKey)) {
            return;
        }
        if (productionQty <= BigDecimal.ZERO.longValue()) {
            return;
        }
        Map<String, Long> sizeCapacityMap = dayProductionQtyMap.get(productionDate);
        if (null == sizeCapacityMap) {
            sizeCapacityMap = new HashMap<>();
            sizeCapacityMap.put(sizeCapacityGroupKey, productionQty);
            dayProductionQtyMap.put(productionDate, sizeCapacityMap);
            //增加日志记录
            ProductionLogUtils.addPreemptionConsumeQty(this, productionDate, sizeCapacityGroupKey, productionQty);
            return;
        }
        Long currentProductionQty = sizeCapacityMap.get(sizeCapacityGroupKey);
        if (null == currentProductionQty) {
            currentProductionQty = BigDecimal.ZERO.longValue();
        }
        Long sumQty = currentProductionQty + productionQty;
        sizeCapacityMap.put(sizeCapacityGroupKey, sumQty);
        dayProductionQtyMap.put(productionDate, sizeCapacityMap);
        Long maxQty = getDayMaxProductionQty(productionDate, sizeCapacityGroupKey);
        //增加日志记录
        ProductionLogUtils.addPreemptionConsumeQtyAndCurrent(this, productionDate, sizeCapacityGroupKey, productionQty, currentProductionQty, maxQty);
    }

    /**
     * 移动时，需要减对应的量
     *
     * @param productionDate       排产日
     * @param sizeCapacityGroupKey 寸口|*|成型法
     * @param productionQty        排产量
     */
    public void moveDayProductionQty(Integer productionDate, String sizeCapacityGroupKey, Long productionQty) {
        if (null == productionDate || null == productionQty || StringUtils.isBlank(sizeCapacityGroupKey)) {
            return;
        }
        if (productionQty <= BigDecimal.ZERO.longValue()) {
            return;
        }
        Map<String, Long> daySizeCapacityMap = dayProductionQtyMap.get(productionDate);
        if (CollectionUtils.isEmpty(daySizeCapacityMap)) {
            return;
        }
        Long currentProductionQty = daySizeCapacityMap.get(sizeCapacityGroupKey);
        if (null == currentProductionQty) {
            return;
        }
        Long sumQty = currentProductionQty - productionQty;
        daySizeCapacityMap.put(sizeCapacityGroupKey, sumQty);
    }

    /**
     * 预排时，增加日预排量
     *
     * @param preProductionDate    预排日
     * @param sizeCapacityGroupKey 寸口|*|成型法
     * @param preProductionQty     预排量
     * @param mouldCode            模具编号
     */
    public void addDayPreProductionQty(Integer preProductionDate, String sizeCapacityGroupKey, Long preProductionQty, String mouldCode) {
        //增加模具预排量
        addDayPreProductionQtyByMould(preProductionDate, preProductionQty, mouldCode);
        //总量
        if (null == preProductionDateQtyMap) {
            return;
        }
        Map<String, Long> sizeCapacityMap = preProductionDateQtyMap.get(productionEndDate);
        if (null == sizeCapacityMap) {
            sizeCapacityMap = new HashMap<>();
            preProductionDateQtyMap.put(preProductionDate, sizeCapacityMap);
        }
        Long preQty = sizeCapacityMap.get(sizeCapacityGroupKey);
        if (null == preQty) {
            preQty = BigDecimal.ZERO.longValue();
        }
        sizeCapacityMap.put(sizeCapacityGroupKey, preProductionQty + preQty);
    }

    /**
     * 判断productCode在productionDate是否可加入排产
     * 根据每日最大排产规格数，及每日新增规格数限制进行判定
     * 如果没有达到规格数限定，则需要判断日排产情况
     *
     * @param isPreFlag        是否预排
     * @param productionOrient 排产方向
     * @param productionDate   排产日
     * @param productCode      排产规格
     * @param productionPlan   排产计划
     * @return
     */
    public boolean isAddProduct(boolean isPreFlag, ProductionOrientEnum productionOrient, Integer productionDate, String productCode, MonthPlanManufacturingRequirementVo productionPlan) {
        //20250624 拼模排产后一个排产规格，则直接不校验
        if (isAssemblingMouldNextProductCode()) {
            return true;
        }
        //先看规格总数是否达到限制
        boolean isAddDaySumProductLimit = isAddDaySumProductLimit(productionDate, productCode, productionPlan);
        if (!isAddDaySumProductLimit) {
            return false;
        }
        if (ProductionOrientEnum.REVERSE == productionOrient) {
            return true;
        }
        //是否是新增规格
        boolean isAddProductionProduct = ProductionPlanUtils.isAddProductByDay(this, isPreFlag, productionOrient, productionDate, productionPlan);
        //非新增规格
        if (!isAddProductionProduct) {
            return true;
        }
        //新增规格，判断是否共生胎，则需要看是否排产了共生胎，如果排产了则可以排产
        Integer previousProductionDate = MouldUtils.getPreviousProductionDate(this, productionDate, productionOrient);
        boolean isProduction = MouldBaseUtils.isProduction(previousProductionDate, this, productCode, isPreFlag, productionPlan.getEmbryoCode());
        if (isProduction) {
            return true;
        }
        //新增规格
        Set<String> addedProductCodeSet = dayAddProductMap.get(productionDate);
        if (addedProductCodeSet.contains(productCode)) {
            return true;
        }
        Integer dayAddedMaxLimitQty = dayAddedProductLimitMap.get(productionDate);
        if (addedProductCodeSet.size() >= dayAddedMaxLimitQty) {
            productionPlan.setIsAddedProductLimit(true);
            return false;
        }
        return true;
    }

    /**
     * 是否达到排产限制-成型硫化配比限制
     * 根据寸口|*|工装类别|*|成型法|*|胎体布层级的分组维度，
     * 获取天分配的最大模具数，如果已经排产模具数+mouldSize大于最大模具数，则表示不可排产
     * 否则表示可排产
     *
     * @param isPreFlag        是否预排
     * @param productionOrient 排产方向
     * @param productionDate   排产日期
     * @param mouldSize        排产模具数
     * @param productionPlan   当前排产计划
     * @return
     */
    public ProductionLimitTypeEnum isReachTheLimit(boolean isPreFlag, ProductionOrientEnum productionOrient, Integer productionDate, int mouldSize, MonthPlanManufacturingRequirementVo productionPlan) {
        if (null == productionDate || mouldSize <= BigDecimal.ZERO.intValue() || null == productionPlan) {
            return ProductionLimitTypeEnum.NO_LIMIT;
        }
        //停产日跳过
        if (factoryStopDays.contains(productionDate)) {
            return ProductionLimitTypeEnum.NO_LIMIT;
        }
        //拼模排产后一个排产规格，则直接不校验
        if (isAssemblingMouldNextProductCode()) {
            return ProductionLimitTypeEnum.NO_LIMIT;
        }
        if (CollectionUtils.isEmpty(dayProductionQtyMap)) {
            return ProductionLimitTypeEnum.NO_LIMIT;
        }
        //输出打印当前的排产模具数
        String groupKey = productionPlan.getSizeCapacityGroupKey();
        Map<String, Map<Integer, Integer>> currentProductionMouldQtyMap = getGroupDayProductionMouldQtyInfo();
        if (!CollectionUtils.isEmpty(currentProductionMouldQtyMap)) {
            ProductionLogUtils.addCurrentGroupDayProductionMouldQtyLog(this, groupKey, productionDate, MouldProductionLogType.PRODUCTION_MOULD_QTY_LIMIT_LOG, currentProductionMouldQtyMap.get(groupKey));
        }
        Map<String, Integer> groupLimitMap = dayMaxMouldQtyMap.get(productionDate);
        if (CollectionUtils.isEmpty(groupLimitMap)) {
            return ProductionLimitTypeEnum.DAY_MOULD_QTY_LIMIT;
        }
        Integer dayMaxMouldQty = groupLimitMap.get(groupKey);
        if (null == dayMaxMouldQty) {
            return ProductionLimitTypeEnum.DAY_MOULD_QTY_LIMIT;
        }
        if (CollectionUtils.isEmpty(dayProductionMouldQtyMap)) {
            return ProductionLimitTypeEnum.NO_LIMIT;
        }
        Map<String, Integer> groupProductionMap = dayProductionMouldQtyMap.get(productionDate);
        if (CollectionUtils.isEmpty(groupProductionMap)) {
            return ProductionLimitTypeEnum.NO_LIMIT;
        }
        Integer productionMouldQty = groupProductionMap.get(groupKey);
        if (null == productionMouldQty) {
            productionMouldQty = BigDecimal.ZERO.intValue();
        }
        if (productionMouldQty + mouldSize <= dayMaxMouldQty) {
            return ProductionLimitTypeEnum.NO_LIMIT;
        }
        //反向排产，不可排
        if (ProductionOrientEnum.REVERSE == productionOrient) {
            return ProductionLimitTypeEnum.DAY_MOULD_QTY_LIMIT;
        }
        //正向排产，判断前一日,忽略生胎
        Integer previousProductionDate = MouldUtils.getPreviousProductionDate(this, productionDate, productionOrient);
        boolean isProduction = MouldBaseUtils.isProduction(previousProductionDate, this, productionPlan.getProductCode(), isPreFlag, productionPlan.getEmbryoCode() + ProductionConstant.PRODUCT_SPLIT);
        if (isProduction) {
            return ProductionLimitTypeEnum.NO_LIMIT;
        }
        return ProductionLimitTypeEnum.DAY_MOULD_QTY_LIMIT;
    }

    /**
     * 是否需要满月排产规格
     *
     * @param productCode 物料编码
     * @return
     */
    public boolean isFullMonthProduction(String productCode) {
        if (StringUtils.isBlank(productCode)) {
            return false;
        }
        if (CollectionUtils.isEmpty(continueFullMonthProductionSet)) {
            return false;
        }
        return continueFullMonthProductionSet.contains(productCode);
    }

    /**
     * 判断当前是否为夏季月份
     * <p>
     * summerMonth <= month < winterMonth
     *
     * @return
     */
    public boolean isSummerMonth() {
        Integer currentMonth = getMonth();
        if (null == currentMonth) {
            return false;
        }
        if (null == productionParam) {
            return false;
        }
        Integer summerMonth = productionParam.getSummerMonth();
        Integer winterMonth = productionParam.getWinterMonth();
        if (null == summerMonth || null == winterMonth) {
            return false;
        }
        return currentMonth >= summerMonth && currentMonth < winterMonth;
    }

    /**
     * 增加每日排产规格数
     *
     * @param productionOrient     排产方向
     * @param productionDate       排产日
     * @param productCode          排产SAP
     * @param productionPlan       排产计划
     * @param isContinueProduction 是否续作排产
     * @pa
     */
    public void addDayProductNumber(ProductionOrientEnum productionOrient, Integer productionDate, String productCode, MonthPlanManufacturingRequirementVo productionPlan, boolean isContinueProduction) {
        //续作排产模式，不算新增规格数
        if (isContinueProduction) {
            return;
        }
        boolean isAddProduct = isAddDaySumProductLimit(productionDate, productCode, productionPlan);
        if (!isAddProduct) {
            return;
        }
        Set<String> productCodeSet = dayProductCodeMap.get(productionDate);
        if (null == productCodeSet) {
            productCodeSet = new HashSet<>();
        }
        if (!productCodeSet.contains(productCode)) {
            productCodeSet.add(productCode);
            dayProductCodeMap.put(productionDate, productCodeSet);
        }
        boolean isAddProductionProduct = ProductionPlanUtils.isAddProductByDay(this, false, productionOrient, productionDate, productionPlan);
        if (!isAddProductionProduct) {
            return;
        }
        Set<String> addedProductCodeSet = dayAddProductMap.get(productionDate);
        if (null == addedProductCodeSet) {
            addedProductCodeSet = new HashSet<>();
        }
        if (addedProductCodeSet.contains(productCode)) {
            return;
        }
        addedProductCodeSet.add(productCode);
        dayAddProductMap.put(productionDate, addedProductCodeSet);
    }

    /**
     * 增加天的模具排产数
     *
     * @param productionOrient     排产方向
     * @param productionDate       排产日
     * @param productionPlan       排产计划
     * @param isContinueProduction 是否续作排产模式
     * @param productionMouldQty   当前排产模具数
     */
    public void addDayProductionMouldQty(ProductionOrientEnum productionOrient, Integer productionDate, MonthPlanManufacturingRequirementVo productionPlan, boolean isContinueProduction, Integer productionMouldQty) {
        if (null == productionDate || null == productionPlan || null == productionMouldQty) {
            return;
        }
        String groupKey = productionPlan.getSizeCapacityGroupKey();
        if (StringUtils.isBlank(groupKey) || productionMouldQty <= BigDecimal.ZERO.intValue()) {
            return;
        }
        Map<String, Integer> dayProductionMouldMap = dayProductionMouldQtyMap.get(productionDate);
        if (null == dayProductionMouldMap) {
            dayProductionMouldMap = new HashMap<>();
            dayProductionMouldQtyMap.put(productionDate, dayProductionMouldMap);
        }
        Integer sumProductionMouldQty = dayProductionMouldMap.get(groupKey);
        if (null == sumProductionMouldQty) {
            sumProductionMouldQty = BigDecimal.ZERO.intValue();
        }
        sumProductionMouldQty = sumProductionMouldQty + productionMouldQty;
        dayProductionMouldMap.put(groupKey, sumProductionMouldQty);
    }

    /**
     * 获取分组的日排产量信息
     * 20251011 ZLT
     * 寸口|*|工装类型|*|成型法|*|胎体布层级
     *
     * @return
     */
    public final Map<String, Map<Integer, Long>> getGroupDayProductionQtyInfo() {
        if (CollectionUtils.isEmpty(dayProductionQtyMap)) {
            return Collections.emptyMap();
        }
        Map<String, Map<Integer, Long>> groupDayProductionQty = new HashMap<>();
        dayProductionQtyMap.entrySet().forEach(entry -> {
            Integer day = entry.getKey();
            Map<String, Long> groupProductionMap = entry.getValue();
            if (CollectionUtils.isEmpty(groupProductionMap)) {
                return;
            }
            groupProductionMap.entrySet().forEach(groupEntry -> {
                String groupKey = groupEntry.getKey();
                Map<Integer, Long> groupDayProductionMap = groupDayProductionQty.get(groupKey);
                if (null == groupDayProductionMap) {
                    groupDayProductionMap = new HashMap<>();
                    groupDayProductionQty.put(groupKey, groupDayProductionMap);
                }
                groupDayProductionMap.put(day, groupEntry.getValue());
            });
        });
        return groupDayProductionQty;
    }

    /**
     * 获取分组的日排产最大模具数信息
     * 20251011 ZLT
     * 寸口|*|工装类型|*|成型法|*|胎体布层级
     *
     * @return
     */
    public final Map<String, Map<Integer, Integer>> getGroupDayMaxMouldQtyInfo() {
        if (CollectionUtils.isEmpty(dayMaxMouldQtyMap)) {
            return Collections.emptyMap();
        }
        Map<String, Map<Integer, Integer>> dayMaxMouldQty = new HashMap<>();
        dayMaxMouldQtyMap.entrySet().forEach(entry -> {
            Integer day = entry.getKey();
            Map<String, Integer> groupMaxMap = entry.getValue();
            if (CollectionUtils.isEmpty(groupMaxMap)) {
                return;
            }
            groupMaxMap.entrySet().forEach(groupEntry -> {
                String groupKey = groupEntry.getKey();
                Map<Integer, Integer> groupDayMaxMap = dayMaxMouldQty.get(groupKey);
                if (null == groupDayMaxMap) {
                    groupDayMaxMap = new HashMap<>();
                    dayMaxMouldQty.put(groupKey, groupDayMaxMap);
                }
                groupDayMaxMap.put(day, groupEntry.getValue());
            });
        });
        return dayMaxMouldQty;
    }

    /**
     * 获取分组的日排产模具数信息
     * 20251011 ZLT
     * 寸口|*|工装类型|*|成型法|*|胎体布层级
     *
     * @return
     */
    public final Map<String, Map<Integer, Integer>> getGroupDayProductionMouldQtyInfo() {
        if (CollectionUtils.isEmpty(dayProductionMouldQtyMap)) {
            return Collections.emptyMap();
        }
        Map<String, Map<Integer, Integer>> dayProductionMouldQty = new HashMap<>();
        dayProductionMouldQtyMap.entrySet().forEach(entry -> {
            Integer day = entry.getKey();
            Map<String, Integer> groupProductionMap = entry.getValue();
            if (CollectionUtils.isEmpty(groupProductionMap)) {
                return;
            }
            groupProductionMap.entrySet().forEach(groupEntry -> {
                String groupKey = groupEntry.getKey();
                Map<Integer, Integer> groupDayProductionMap = dayProductionMouldQty.get(groupKey);
                if (null == groupDayProductionMap) {
                    groupDayProductionMap = new HashMap<>();
                    dayProductionMouldQty.put(groupKey, groupDayProductionMap);
                }
                groupDayProductionMap.put(day, groupEntry.getValue());
            });
        });
        return dayProductionMouldQty;
    }

    /**
     * 获取排产日当前排产汇总量
     *
     * @param productionDate       排产日
     * @param sizeCapacityGroupKey 寸口|*|成型法|*|胎体布层级
     * @return
     */
    private Long getDayProductionQty(Integer productionDate, String sizeCapacityGroupKey) {
        if (null == productionDate || StringUtils.isBlank(sizeCapacityGroupKey)) {
            return null;
        }
        Map<String, Long> sizeCapacityMap = dayProductionQtyMap.get(productionDate);
        if (CollectionUtils.isEmpty(sizeCapacityMap)) {
            return null;
        }
        Long currentProductionQty = sizeCapacityMap.get(sizeCapacityGroupKey);
        if (null == currentProductionQty) {
            return null;
        }
        return currentProductionQty;
    }

    /**
     * 获取每日汇总排产数量，各寸口汇总量
     *
     * @param productionDate 排产日
     * @return
     */
    private Long getDayProductionQty(Integer productionDate) {
        if (null == productionDate) {
            return null;
        }
        Map<String, Long> sizeCapacityMap = dayProductionQtyMap.get(productionDate);
        if (CollectionUtils.isEmpty(sizeCapacityMap)) {
            return null;
        }
        List<Long> sizeCapacityList = sizeCapacityMap.values().stream().collect(Collectors.toList());
        return sizeCapacityList.stream().mapToLong(Long::longValue).sum();
    }

    /**
     * 获取排产日-寸口|*|成型法|*|胎体布层级最大可排产量
     *
     * @param productionDate       排产日
     * @param sizeCapacityGroupKey 寸口|*|成型法|*|胎体布层级
     * @return
     */
    private Long getDayMaxProductionQty(Integer productionDate, String sizeCapacityGroupKey) {
        if (null == productionDate || StringUtils.isBlank(sizeCapacityGroupKey)) {
            return null;
        }
        Map<String, Long> sizeCapacityMap = daySizeCapacityMap.get(productionDate);
        if (CollectionUtils.isEmpty(sizeCapacityMap)) {
            return null;
        }
        Long maxProductionQty = sizeCapacityMap.get(sizeCapacityGroupKey);
        if (null == maxProductionQty) {
            return null;
        }
        return maxProductionQty;
    }

    /**
     * 预排时，增加模具日排产量
     *
     * @param preProductionDate 预排日期
     * @param preProductionQty  预排量
     * @param mouldCode         预排模具
     */
    private void addDayPreProductionQtyByMould(Integer preProductionDate, Long preProductionQty, String mouldCode) {
        if (null == mouldPreProductionDateQtyMap) {
            return;
        }
        if (StringUtils.isBlank(mouldCode)) {
            return;
        }
        Map<Integer, Long> mouldPreProductionDateQtyInfo = mouldPreProductionDateQtyMap.get(mouldCode);
        if (null == mouldPreProductionDateQtyInfo) {
            mouldPreProductionDateQtyInfo = new HashMap<>();
        }
        Long preProductionDateQty = mouldPreProductionDateQtyInfo.get(preProductionDate);
        if (null == preProductionDateQty) {
            preProductionDateQty = BigDecimal.ZERO.longValue();
        }
        preProductionDateQty = preProductionDateQty + preProductionQty;
        mouldPreProductionDateQtyInfo.put(preProductionDate, preProductionDateQty);
        mouldPreProductionDateQtyMap.put(mouldCode, mouldPreProductionDateQtyInfo);
    }

    /**
     * 是否达到每日规格数的总数限制
     *
     * @param productionDate 排产日
     * @param productCode    排产规格
     * @param productionPlan 排产计划
     */
    private boolean isAddDaySumProductLimit(Integer productionDate, String productCode, MonthPlanManufacturingRequirementVo productionPlan) {
        if (null == productionDate || StringUtils.isBlank(productCode)) {
            return false;
        }
        if (productionDate < ProductionConstant.MONTH_START_DAY || productionDate > monthDays) {
            return true;
        }
        if (factoryStopDays.contains(productionDate)) {
            return false;
        }
        if (CollectionUtils.isEmpty(dayProductCodeMap)) {
            return true;
        }
        Set<String> productCodeSet = dayProductCodeMap.get(productionDate);
        if (CollectionUtils.isEmpty(productCodeSet)) {
            return true;
        }
        if (productCodeSet.contains(productCode)) {
            return true;
        }
        Integer limit = productionParam.getDayMaxProductCount();
        if (null == limit || limit <= 0) {
            return true;
        }
        if (productCodeSet.size() + 1 <= limit) {
            return true;
        }
        productionPlan.setIsCapacityLimit(true);
        return false;
    }

}