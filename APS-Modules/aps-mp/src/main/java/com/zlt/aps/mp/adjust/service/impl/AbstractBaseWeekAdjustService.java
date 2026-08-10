package com.zlt.aps.mp.adjust.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.common.collect.Maps;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.constant.BusiConstant;
import com.zlt.aps.common.core.enums.DataSourceEnum;
import com.zlt.aps.constant.Constant;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.constant.IncrementConstant;
import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.enums.ConstructionStageEnum;
import com.zlt.aps.enums.ProductTypeEnum;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.enums.MsgTemplateEnums;
import com.zlt.aps.maindata.mapper.MdmMonthSurplusEntityMapper;
import com.zlt.aps.maindata.mapper.MpMonthPlanMonitorEntityMapper;
import com.zlt.aps.maindata.service.IMdmSkuScheduleCategoryService;
import com.zlt.aps.maindata.utils.MessageServiceUtils;
import com.zlt.aps.mp.adjust.mapper.MpAdjustResultEntityMapper;
import com.zlt.aps.mp.adjust.mapper.MpAdjustStructureInEntityMapper;
import com.zlt.aps.mp.adjust.mapper.MpAdjustStructureOutEntityMapper;
import com.zlt.aps.mp.adjust.service.*;
import com.zlt.aps.mp.api.domain.capacity.MpDailyCapacityLimitVo;
import com.zlt.aps.mp.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.mp.api.domain.entity.*;
import com.zlt.aps.mp.api.domain.vo.*;
import com.zlt.aps.mp.api.enums.AdjustItemSourceEnum;
import com.zlt.aps.mp.common.utils.DistributedVersionGenerator;
import com.zlt.aps.mp.common.utils.StringUtil;
import com.zlt.aps.mp.demand.mapper.DpDemandPlanEntityMapper;
import com.zlt.aps.mp.demand.mapper.SalesOrderPoolRecordEntityMapper;
import com.zlt.aps.mp.demand.service.IDpDemandPlanService;
import com.zlt.aps.mp.demand.service.ISalesOrderPoolService;
import com.zlt.aps.mp.engine.adjust.MpWeekRollAdjustEngine;
import com.zlt.aps.mp.engine.capacity.MpAdjustDailyCapacityLimit;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.factory.dto.MpSkuAdjustInfoVo;
import com.zlt.aps.mp.factory.mapper.FactoryMonthPlanProductionFinalResultEntityMapper;
import com.zlt.aps.mp.factory.mapper.MpStructureAllocationEntityMapper;
import com.zlt.aps.mp.factory.service.IBatchMpMonthPlanStatisticsService;
import com.zlt.aps.mp.factory.service.IFactoryMonthPlanProductionFinalResultService;
import com.zlt.aps.mp.factory.service.IMpMonthPlanStatisticsService;
import com.zlt.aps.mp.factory.service.IMpStructureAllocationService;
import com.zlt.aps.mp.factory.service.MpSkuAdjustInfoService;
import com.zlt.aps.mp.factory.service.impl.MoldCavityInsertMaxValueCalculatorImpl;
import com.zlt.aps.mp.mdm.dto.DataDTO;
import com.zlt.aps.mp.mdm.handler.DataManager;
import com.zlt.aps.utils.AppUtils;
import com.zlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.utils.IncrementService;
import com.zlt.aps.utils.ThreadPoolUtil;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.dao.basedao.BaseDao;
import com.zlt.msg.message.domain.vo.MessageContext;
import com.zlt.msg.message.enums.MsgTypeEnums;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 周程滚动调整通用抽象类
 *
 * @author wengpc
 */
@Slf4j
public abstract class AbstractBaseWeekAdjustService implements IMpWeekAdjustService {

    @Autowired
    protected FactoryMonthPlanProductionFinalResultEntityMapper factoryMonthPlanProdFinalMapper;
    @Autowired
    protected IBatchMpProductionFinalResultService batchMpProductionFinalResultService;
    @Autowired
    protected MdmMonthSurplusEntityMapper mdmMonthSurplusEntityMapper;
    @Autowired
    protected MpMonthPlanMonitorEntityMapper mpMonthPlanMonitorEntityMapper;

    @Autowired
    protected IMesItfService mesItfService;

    @Autowired
    protected DistributedVersionGenerator versionGenerator;

    @Autowired
    protected MpAdjustResultEntityMapper mpAdjustResultEntityMapper;

    @Autowired
    protected MpAdjustStructureInEntityMapper mpAdjustStructureInEntityMapper;

    @Autowired
    protected MpAdjustStructureOutEntityMapper mpAdjustStructureOutEntityMapper;

    @Autowired
    protected MpStructureAllocationEntityMapper mpStructureAllocationEntityMapper;

    @Autowired
    protected IDpDemandPlanService dpDemandPlanService;

    @Autowired
    protected IBatchMpAdjustResultService batchMpAdjustResultService;

    @Autowired
    protected IMpAdjustMaterialLogService mpAdjustMaterialLogService;

    @Autowired
    protected IBatchMpAdjustMaterialLogService batchMpAdjustMaterialLogService;

    @Autowired
    protected IMpAdjustStructureLogService mpAdjustLogService;

    @Autowired
    protected IMpAdjustStructureInService mpAdjustStructureInService;

    @Autowired
    protected IMpStructureAllocationService mpStructureAllocationService;

    @Autowired
    protected MoldCavityInsertMaxValueCalculatorImpl moldCavityInsertMaxValueCalculator;

    @Autowired
    protected IMpMonthPlanStatisticsService mpMonthPlanStatisticsService;

    @Autowired
    protected IBatchMpMonthPlanStatisticsService batchMpMonthPlanStatisticsService;

    @Autowired
    protected MpSkuAdjustInfoService mpSkuAdjustInfoService;

    @Autowired
    protected BaseDao baseDao;

    @Autowired
    protected IncrementService incrementService;

    @Autowired
    private MessageServiceUtils messageServiceAdapter;

    @Autowired
    protected DataManager dataManager;

    @Autowired
    protected DpDemandPlanEntityMapper demandPlanEntityMapper;

    @Autowired
    protected IMdmSkuScheduleCategoryService mdmSkuScheduleCategoryService;

    @Autowired
    private IFactoryMonthPlanProductionFinalResultService finalResultService;

    @Autowired
    private ISalesOrderPoolService iSalesOrderPoolService;
    @Autowired
    private SalesOrderPoolRecordEntityMapper salesOrderPoolRecordEntityMapper;

    @Override
    public void generateAdjust(MpRollAdjustContextDTO contextDTO) throws BusinessException {
        // 前置处理
        preProcess(contextDTO);
        // 生成调整明细
        doGenerateAdjust(contextDTO);
        // 后置处理
        postProcess(contextDTO);
    }

    /**
     * 前置处理
     */
    private void preProcess(MpRollAdjustContextDTO contextDTO) {
        // 校验
        check(contextDTO);
        // 并行初始化
        initParallel(contextDTO);
    }

    /**
     * 从供应链抓取最新的销售订单池数据<br/>
     * 抓取年月判断最新抓取记录是否晚于本月，是则已最新抓取记录月份为准，否则已本月为准
     *
     * @param contextDTO
     */
    public void syncSalesOrderPool(MpRollAdjustContextDTO contextDTO) {
        String isPreScmGrape = (String) contextDTO.getParamMap().get(MonthPlanEnums.ADJUST_GET_ORDER_PRE_SCM_GRAPE.getCode());
        if (!FactoryConstant.YES_VALUE.equals(isPreScmGrape)) {
            return;
        }
        // 先查询最新的同步记录年月
        // 年月默认当前时间年月
        Date currentDate = DateUtils.getNowDate();
        Integer year = DateUtils.getYear(currentDate);
        Integer month = DateUtils.getMonth(currentDate);
        LambdaQueryWrapper<SalesOrderPoolRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(SalesOrderPoolRecord::getYear, SalesOrderPoolRecord::getMonth);
        queryWrapper.groupBy(Arrays.asList(SalesOrderPoolRecord::getYear, SalesOrderPoolRecord::getMonth));
        queryWrapper.ge(SalesOrderPoolRecord::getYear, year); // 今年之后的记录，支持跨年的场景
        queryWrapper.isNotNull(SalesOrderPoolRecord::getMonth);
        SalesOrderPoolRecord yearMonth = salesOrderPoolRecordEntityMapper.selectList(queryWrapper).stream()
                .max((r1, r2) -> { // 取最新的同步年月
                    Integer yearMonth1 = r1.getYear() * 100 + r1.getMonth();
                    Integer yearMonth2 = r2.getYear() * 100 + r2.getMonth();
                    return yearMonth1.compareTo(yearMonth2);
                }).orElseGet(null);
        if (yearMonth != null) { // 如果有更新的同步记录，则已同步记录的年月为准
            year = yearMonth.getYear();
            month = yearMonth.getMonth();
        }
        // 调用SCM接口同步数据
        SalesOrderPool salesOrderPool = new SalesOrderPool();
        salesOrderPool.setFactoryCode(contextDTO.getFactoryCode());
        salesOrderPool.setYear(year);
        salesOrderPool.setMonth(month);
        AjaxResult result = iSalesOrderPoolService.getSCMData(salesOrderPool);
        if (!AppUtils.checkAjaxSuccess(result)) {
            throw new BusinessException(String.valueOf(result.get(AjaxResult.MSG_TAG)));
        }
    }

    /**
     * 后置处理
     */
    private void postProcess(MpRollAdjustContextDTO contextDTO) {
        // 后置检查
        postCheck(contextDTO);
        // 后置处理
        doPostProcess(contextDTO);
        // 排序调整明细
        sortAdjustDetailList(contextDTO);
        // 保存调整明细
        saveAdjustDetailList(contextDTO);
    }


    protected void doPostProcess(MpRollAdjustContextDTO contextDTO) {
        List<MpAdjustDetailVo> adjustDetailList = contextDTO.getAdjustDetailList();
        // 将集合中指定字段的null值替换为0
        setNullFieldsToZero(adjustDetailList);
        // 将集合中指定字段的0值替换为null
        setZeroFieldsToNull(adjustDetailList);
        // 发送消息
        Map<String, List<String>> messageMap = Optional.ofNullable(contextDTO.getMessageMap())
                .orElseGet(HashMap::new);
        if (PubUtil.isNotEmpty(messageMap.get(ApsConstant.APS_STRING_0))) {
            List<String> msgList = messageMap.get(ApsConstant.APS_STRING_0);
            String msg = Optional.ofNullable(msgList)
                    .orElse(Collections.emptyList())
                    .stream()
                    .distinct()
                    .collect(Collectors.joining(BusiConstant.WeekRollAdjust.SPLIT_SEMICOLON));
            sendMessage(MsgTemplateEnums.MP_SKU_TYPE_PRODUCT_STATUS_NO_SAME.getCode(),
                    MsgTypeEnums.NOTICE.getCode(), msg);
        }
    }


    /**
     * 将集合中指定字段的0值替换为null
     *
     * @param adjustDetailList
     */
    protected void setZeroFieldsToNull(List<MpAdjustDetailVo> adjustDetailList) {
        if (PubUtil.isEmpty(adjustDetailList)) {
            return;
        }
        for (MpAdjustDetailVo vo : adjustDetailList) {
            vo.setActualAdjustQty(Convert.toInt(vo.getActualAdjustQty(), 0).equals(0) ? null : vo.getActualAdjustQty());
        }
    }

    /**
     * 将集合中指定字段的null值替换为0
     *
     * @param adjustDetailList
     */
    protected void setNullFieldsToZero(List<MpAdjustDetailVo> adjustDetailList) {
        if (PubUtil.isEmpty(adjustDetailList)) {
            return;
        }
        for (MpAdjustDetailVo vo : adjustDetailList) {
            vo.setHeightQty(Objects.nonNull(vo.getHeightQty()) ? vo.getHeightQty() : 0);
            vo.setMidQty(Objects.nonNull(vo.getMidQty()) ? vo.getMidQty() : 0);
            vo.setPostponeQty(Objects.nonNull(vo.getPostponeQty()) ? vo.getPostponeQty() : 0);
            vo.setCycleReserveQty(Objects.nonNull(vo.getCycleReserveQty()) ? vo.getCycleReserveQty() : 0);
            vo.setConventionReserveQty(Objects.nonNull(vo.getConventionReserveQty()) ? vo.getConventionReserveQty() : 0);
        }
    }


    /**
     * 后置检查
     *
     * @param contextDTO
     */
    protected void postCheck(MpRollAdjustContextDTO contextDTO) {
        // 错误信息列表
        List<String> errorMsgList = new ArrayList<>();
        // 检查调整明细列表中的必填字段是否为空
        errorMsgList.addAll(checkEmptyFields(contextDTO.getAdjustDetailList()));
        // 检查sku与施工示方书关系是否有数据
        errorMsgList.addAll(checkExistSkuConstructionRef(contextDTO));
        // 格式化错误信息（换行）
//        String errorMsg = Optional.ofNullable(errorMsgList)
//                .orElse(Collections.emptyList())
//                .stream()
//                .collect(Collectors.joining(BusiConstant.WeekRollAdjust.SPLIT_FRONT_NEW_LINE));
//        Assert.isFalse(PubUtil.isNotEmpty(errorMsgList), () -> {
//            return new BusinessException(errorMsg);
//        });
    }

    /**
     * 检查sku与施工示方书关系是否有数据
     *
     * @param contextDTO
     * @return
     */
    protected List<String> checkExistSkuConstructionRef(MpRollAdjustContextDTO contextDTO) {
        // 调整明细列表
        List<MpAdjustDetailVo> adjustDetailList = contextDTO.getAdjustDetailList();
        // SKU与施工（示方书）关系列表
        List<MdmSkuConstructionRef> skuConstructionRefList = contextDTO.getMdmSkuConstructionRefList();
        if (PubUtil.isEmpty(adjustDetailList)) {
            return Collections.emptyList();
        }
        // 按照物料编码分组
        Map<String, List<String>> skuConstructionRefMap = skuConstructionRefList.stream()
                .filter(obj -> StringUtils.isNotEmpty(obj.getMaterialCode()) && StringUtils.isNotEmpty(obj.getEmbryoNo()))
                .collect(Collectors.groupingBy(
                        MdmSkuConstructionRef::getMaterialCode,
                        Collectors.mapping(
                                MdmSkuConstructionRef::getEmbryoNo,
                                Collectors.toList()
                        )
                ));
        // 错误信息列表
        List<String> notExistMsgList = new ArrayList<>();
        // 循环检查sku与施工示方书关系是否有数据
        for (MpAdjustDetailVo adjustDetailVo : adjustDetailList) {
            String materialCode = adjustDetailVo.getMaterialCode();
            String isTrial = adjustDetailVo.getIsTrial();
            List<String> embryoNoList = skuConstructionRefMap.getOrDefault(materialCode, new ArrayList<>());
            // 构建错误信息
            String errorMsg = StrUtil.format(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.checkNotExistConstructionRef"), materialCode);
            // 无论是否试制量试，列表为空则直接添加错误信息
            if (PubUtil.isEmpty(embryoNoList)) {
                notExistMsgList.add(errorMsg);
                continue;
            }
            // 试制量试需要额外校验是否在列表中
            if (ApsConstant.TRUE.equals(isTrial)) {
                String embryoNo = adjustDetailVo.getEmbryoNo();
                // 非空时，校验是否存在于列表中
                if (StringUtils.isNotEmpty(embryoNo) && !embryoNoList.contains(embryoNo)) {
                    notExistMsgList.add(errorMsg);
                }
            }
        }
        return notExistMsgList;
    }


    /**
     * 检查调整明细列表中的必填字段是否为空
     *
     * @param adjustDetailList 调整明细列表
     * @return 错误信息列表
     */
    protected List<String> checkEmptyFields(List<MpAdjustDetailVo> adjustDetailList) {
        // 获取检查为空的字段
        Map<String, String> checkFieldMap = getCheckEmptyFieldMap();
        // 错误信息列表
        List<String> errorMsgList = new ArrayList<>();
        if (PubUtil.isEmpty(adjustDetailList) || PubUtil.isEmpty(checkFieldMap)) {
            return errorMsgList;
        }
        for (MpAdjustDetailVo detail : adjustDetailList) {
            String materialCode = detail.getMaterialCode();
            if (StringUtils.isEmpty(materialCode)) {
                continue;
            }
            // 遍历需要检查的字段
            for (Map.Entry<String, String> entry : checkFieldMap.entrySet()) {
                // 字段英文名称
                String fieldEnName = entry.getKey();
                // 字段中文名称
                String fieldCnName = entry.getValue();
                try {
                    Object fieldValue = detail.getFieldValueByFieldName(fieldEnName);
                    // 判断字段值是否为空
                    boolean isEmpty = false;
                    if (fieldValue == null) {
                        isEmpty = true;
                    } else if (fieldValue instanceof String) {
                        String strValue = (String) fieldValue;
                        isEmpty = StringUtils.isBlank(strValue);
                    }
                    // 字段为空添加错误信息
                    if (isEmpty) {
                        String errorMsg = StrUtil.format(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.checkEmptyFields"),
                                materialCode, fieldCnName);
                        errorMsgList.add(errorMsg);
                    }
                } catch (Exception e) {
                    log.error("物料编码: {} 检查字段【{}】失败：{}", materialCode, fieldCnName, e.getMessage());
                    continue;
                }
            }
        }
        return errorMsgList;
    }

    /**
     * 获取检查为空的字段
     *
     * @return
     */
    protected Map<String, String> getCheckEmptyFieldMap() {
        Map<String, String> checkFieldMap = new HashMap<>();
//        checkFieldMap.put("structureName", "结构名称");
//        checkFieldMap.put("constructionStage", "施工阶段");
//        checkFieldMap.put("productTypeCode", "产品品类");
//        checkFieldMap.put("mainMaterialDesc", "主物料胎胚号");
//        checkFieldMap.put("mainPattern", "主花纹");
//        checkFieldMap.put("curingTime", "硫化时间");
////        checkFieldMap.put("mouldCavityQty", "型腔数量");
////        checkFieldMap.put("typeBlockQty", "活块数量");
//        checkFieldMap.put("dayVulcanizationQty", "日硫化量单模");
        return Collections.unmodifiableMap(checkFieldMap);
    }

    /**
     * 保存调整明细
     *
     * @param contextDTO
     */
    public abstract void saveAdjustDetailList(MpRollAdjustContextDTO contextDTO);

    /**
     * 排序：按英寸->结构->最大型腔数->主花纹->活块数->物料描述
     *
     * @param contextDTO
     */
    protected void sortAdjustDetailList(MpRollAdjustContextDTO contextDTO) {
        List<MpAdjustDetailVo> mpAdjustDetailList = contextDTO.getAdjustDetailList();
        if (PubUtil.isEmpty(mpAdjustDetailList)) {
            return;
        }
        // 主花纹的最大型腔数
        Map<String, Integer> maxMouldCavityQtyMap = new HashMap<>();
        for (MpAdjustDetailVo adjustDetail : mpAdjustDetailList) {
            //记录主花纹的最大型腔数
            Integer maxMouldCavityQty = maxMouldCavityQtyMap.getOrDefault(adjustDetail.getMainPattern(), 0);
            maxMouldCavityQtyMap.put(adjustDetail.getMainPattern(), Math.max(maxMouldCavityQty, adjustDetail.getMouldCavityQty()));
        }
        mpAdjustDetailList.stream().forEach(s -> { // 设置对应的最大型腔数和最大活块数
            s.setMaxMouldCavityQty(maxMouldCavityQtyMap.getOrDefault(s.getMainPattern(), 0));
        });

        Collections.sort(mpAdjustDetailList, getAdjustDetailSortComparator());
    }

    /**
     * 排序器：按英寸->结构->最大型腔数->主花纹->活块数->花纹->物料描述
     *
     * @return
     */
    protected Comparator<MpAdjustDetailVo> getAdjustDetailSortComparator() {
        // 一级排序：结构名称升序，空值排最后
        return Comparator.comparing(MpAdjustDetailVo::getTbrProSize, Comparator.nullsLast(String::compareTo))
                .thenComparing(MpAdjustDetailVo::getStructureName, Comparator.nullsLast(String::compareTo))
                // 最大型腔数
                .thenComparing(MpAdjustDetailVo::getMaxMouldCavityQty, Comparator.nullsLast(Comparator.reverseOrder()))
                // 主花纹
                .thenComparing(MpAdjustDetailVo::getMainPattern, Comparator.nullsLast(String::compareTo))
                // 活块数
                .thenComparing(MpAdjustDetailVo::getTypeBlockQty, Comparator.nullsLast(Comparator.reverseOrder()))
                // 花纹
                .thenComparing(MpAdjustDetailVo::getPattern, Comparator.nullsLast(String::compareTo))
                // 物料描述
                .thenComparing(MpAdjustDetailVo::getMaterialDesc, Comparator.nullsLast(String::compareTo));
    }

    @Override
    public void productAlign(MpRollAdjustContextDTO contextDTO) {
        if (StringUtil.isEmptyWithTrim(contextDTO.getVersion())) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.versionEmpty"));
        }
        //1、查询月计划定稿数据
        FactoryMonthPlanProductionFinalResult params = new FactoryMonthPlanProductionFinalResult();
        params.setFactoryCode(contextDTO.getFactoryCode());
        params.setYear(contextDTO.getMpYear());
        params.setMonth(contextDTO.getMpMonth());
        params.setVersion(contextDTO.getVersion());
        params.setStructureName(contextDTO.getStructureName());
        List<FactoryMonthPlanFinalAdjustVo> adjustVos = finalResultService.list4Adjust(params);
        contextDTO.setFactoryMonthPlanProdFinalList(adjustVos);
        //2、计算本次超欠产 = 累计已排产量 - 已生产量, 以及 待调整量
        setCurrentOverdueQtyAndPendingQty(contextDTO);
        //3、针对待调整量，尝试自动生产对齐
        doProductAlign(contextDTO);
        //4、设置0日生产超欠产
        setZeroProductOverdueQty(contextDTO);
        //5、保存调整结果
        saveMpAdjustResult(contextDTO);
        //6、保存调整过程日志
        saveMpAdjustProcLog(contextDTO);
        //7、保存月计划统计结果
        saveMonthPlanStatisticsResult(contextDTO, YesOrNoEnum.YES.getCode());
    }

    /**
     * 计算本次超欠产、待调整量
     * @param contextDTO
     */
    private void setCurrentOverdueQtyAndPendingQty(MpRollAdjustContextDTO contextDTO) {
        List<MpMonthPlanMonitor> monitorList = contextDTO.getMpMonthPlanMonitorList();
        List<FactoryMonthPlanFinalAdjustVo> finalAdjustList = contextDTO.getFactoryMonthPlanProdFinalList();
        // 转分组Map
        Map<String, List<FactoryMonthPlanFinalAdjustVo>> planGroupMap = convertToPlanGroupMap(contextDTO.getFactoryMonthPlanProdFinalList());
        Map<String, List<MpMonthPlanMonitor>> monitorGroupMap = convertToMonitorGroupMap(monitorList);
        Date currentDate = DateUtils.getNowDate();
        int currentDay = DateUtils.getDay(currentDate);
        // 遍历目标列表，计算赋值
        for (FactoryMonthPlanFinalAdjustVo finalAdjustVo : finalAdjustList) {
            finalAdjustVo.setProductAlignDate(currentDate);

            if (StringUtils.isEmpty(finalAdjustVo.getMaterialCode())) {
                continue;
            }
            if (!ConstructionStageEnum.FORMAL_PRODUCTION.getStage().equals(finalAdjustVo.getConstructionStage())){
                // 非正式忽略
                continue;
            }
            // 计算：day1~targetDay的累计值
            Integer totalScheduledQty = calculateQty(planGroupMap, finalAdjustVo.getMaterialCode(), currentDay - 1);
            // 获取已生产量（空值按0处理）
            List<MpMonthPlanMonitor> monthPlanMonitorList = MapUtils.getObject(monitorGroupMap, finalAdjustVo.getMaterialCode(), new ArrayList<>());
            Integer productionQty = Convert.toInt(monthPlanMonitorList.stream()
                    .filter(e -> e.getProductionQty() != null)
                    .mapToInt(MpMonthPlanMonitor::getProductionQty)
                    .sum(), 0);
            // 1、超欠产 = 累计已排产量 - 已生产量
            Integer overdueQty = totalScheduledQty - productionQty;
            finalAdjustVo.setCurrentOverdueQty(overdueQty);

            //2、待调整量 = 订单增减量（0） - 本次超欠产
            Integer pendingQty = - overdueQty;
            finalAdjustVo.setPendingQty(pendingQty);
        }
    }

    /**
     * 设置0日 生产超欠产
     * @param contextDTO
     */
    private void setZeroProductOverdueQty(MpRollAdjustContextDTO contextDTO) {
        List<FactoryMonthPlanFinalAdjustVo> finalAdjustList = contextDTO.getFactoryMonthPlanProdFinalList();
        // 转分组Map
        Map<String, List<FactoryMonthPlanFinalAdjustVo>> planGroupMap = convertToPlanGroupMap(contextDTO.getFactoryMonthPlanProdFinalList());
        Date currentDate = DateUtils.getNowDate();
        int currentDay = DateUtils.getDay(currentDate);
        // 遍历目标列表，计算赋值
        for (FactoryMonthPlanFinalAdjustVo finalAdjustVo : finalAdjustList) {

            if (StringUtils.isEmpty(finalAdjustVo.getMaterialCode())) {
                continue;
            }
            if (!ConstructionStageEnum.FORMAL_PRODUCTION.getStage().equals(finalAdjustVo.getConstructionStage())){
                // 非正式忽略
                continue;
            }
            //1、if(abs(计划差值) >= abs(欠产值) 0日超欠产值 = 旧有0日超欠产值 + 真实欠产值
            //2、if(计划差值 = 0 ) 0日超欠产值 = 旧有0日超欠产值
            //3、0日超欠产值 = 旧有0日超欠产值 - 计划差值
            Integer productOverdueQty = Convert.toInt(finalAdjustVo.getProductOverdueQty(),0);
            Integer actualAdjustQty = Convert.toInt(finalAdjustVo.getActualAdjustQty(),0);
            Integer currentOverdueQty = Convert.toInt(finalAdjustVo.getCurrentOverdueQty(),0);
            if (Math.abs(actualAdjustQty) >= Math.abs(currentOverdueQty)){
                productOverdueQty += currentOverdueQty;
            }else if (actualAdjustQty == 0){
                //productOverdueQty = finalAdjustVo.getProductOverdueQty();
            }else{
                productOverdueQty -= currentOverdueQty;
            }

            finalAdjustVo.setProductOverdueQty(productOverdueQty);
        }
    }

    @Override
    public void autoAdjust(MpRollAdjustContextDTO contextDTO) throws BusinessException {
        //1、执行自动调整
        doAutoAdjust(contextDTO);
        //2、按结构维度更新硫化机台信息；
        updateLhMachinesByStructure(contextDTO);
        //3、保存调整结果
        saveMpAdjustResult(contextDTO);
        //4、保存调整过程日志
        saveMpAdjustProcLog(contextDTO);
        //5、回填实际调整
        backfillRealAdjustResult(contextDTO);
        //6、保存月计划统计结果
        saveMonthPlanStatisticsResult(contextDTO, YesOrNoEnum.YES.getCode());
        //7、发送消息
        if (!StringUtil.isEmptyWithTrim(contextDTO.getMsgRemainQtyNoFull().toString())) {
            //发送 SKU原余量小于调整次日至锁定截止日的计划量提醒
            sendMsgRemainQtyNoFull(contextDTO);
        }
        if (!StringUtil.isEmptyWithTrim(contextDTO.getMsgStructureAdjustPreClose().toString())) {
            //发送 结构内调整减量提前收尾
            sendMsgStructAdjustPreClose(contextDTO);
        }
    }

    /**
     * 按结构维度更新硫化机台信息；
     * 存在中间插入新结构，故重新刷新一下
     *
     * @param contextDTO
     */
    private void updateLhMachinesByStructure(MpRollAdjustContextDTO contextDTO) {
        List<MpStructureAllocation> structureAllocationList = new ArrayList<>(contextDTO.getStructureAllocationList());
        //补充新增结构的机台信息
        AdjustsCxMachineVo cxMachineVo = mpStructureAllocationService.getAdjustsCxMachineFromRedis();
        if (cxMachineVo != null) {
            MpStructureAllocation newStructureAlloction = new MpStructureAllocation();
            newStructureAlloction.setStructureName(cxMachineVo.getStructureName());
            newStructureAlloction.setCxMachineCode(cxMachineVo.getCxMachineCode());
            structureAllocationList.add(newStructureAlloction);
        }
        List<FactoryMonthPlanFinalAdjustVo> saveMpProdFinalList = contextDTO.getSaveMpProdFinalList();
        if (PubUtil.isEmpty(structureAllocationList) || PubUtil.isEmpty(saveMpProdFinalList)) {
            return;
        }
        // 1. 按 structureName 分组，收集唯一的 cxMachineCode 并用逗号连接
        Map<String, String> structureToMachineCodes = structureAllocationList.stream()
                .filter(s -> s != null && StringUtils.isNotBlank(s.getStructureName()) && StringUtils.isNotBlank(s.getCxMachineCode()))
                .collect(Collectors.groupingBy(
                        MpStructureAllocation::getStructureName,
                        Collectors.mapping(MpStructureAllocation::getCxMachineCode, Collectors.toSet())
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .sorted()  // 机台排序，保证结果稳定
                                .collect(Collectors.joining(","))
                ));

        // 2. 遍历待更新的列表，匹配 structureName 并设置 cxMachineCode
        for (FactoryMonthPlanFinalAdjustVo vo : saveMpProdFinalList) {
            if (vo != null && StringUtils.isNotBlank(vo.getStructureName())) {
                String machineCodes = structureToMachineCodes.get(vo.getStructureName());
                if (machineCodes != null) {
                    vo.setCxMachineCode(machineCodes);
                }
            }
        }
    }

    /**
     * 发送 SKU原余量小于调整次日至锁定截止日的计划量提醒
     *
     * @param contextDTO
     */
    private void sendMsgRemainQtyNoFull(MpRollAdjustContextDTO contextDTO) {
        // 构建完整上下文
        MessageContext context = messageServiceAdapter.buildMessageContext(
                null,
                null,
                null,
                null,
                null,
                null,
                SecurityUtils.getUsername(),
                null
        );

        // 发送消息
        messageServiceAdapter.sendBatchMessage(
                MsgTemplateEnums.MP_SKU_REMAIN_QTY_NO_FULL.getCode(),
                MsgTypeEnums.NOTICE.getCode(),
                contextDTO.getMsgRemainQtyNoFull().toString(),
                null,
                null,
                context
        );
    }

    /**
     * 发送 结构内调整减量提前收尾
     *
     * @param contextDTO
     */
    private void sendMsgStructAdjustPreClose(MpRollAdjustContextDTO contextDTO) {
        // 构建完整上下文
        MessageContext context = messageServiceAdapter.buildMessageContext(
                null,
                null,
                null,
                null,
                null,
                null,
                SecurityUtils.getUsername(),
                null
        );

        // 发送消息
        messageServiceAdapter.sendBatchMessage(
                MsgTemplateEnums.MP_STRUCTURE_ADJUST_PRE_CLOSE.getCode(),
                MsgTypeEnums.NOTICE.getCode(),
                contextDTO.getMsgStructureAdjustPreClose().toString(),
                null,
                null,
                context
        );
    }

    /**
     * 回填实际调整
     *
     * @param contextDTO 周程滚动上下文
     */
    protected void backfillRealAdjustResult(MpRollAdjustContextDTO contextDTO) {

    }

    /**
     * 重算每日产能限制，包括硫化机台数、胎胚种类数、换模次数
     *
     * @param contextDTO      周程滚动上下文
     * @param mpProdFinalList 定稿记录列表
     */
    public void reCalcAdjustDailyCapacityLimit(MpRollAdjustContextDTO contextDTO, List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList, MpAdjustDailyCapacityLimit adjustDailyCapacityLimitObj, String mainPattern) {

        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap = contextDTO.getDailyCapacityLimitVoMap();
        for (int i = contextDTO.getStructureStartDay(); i <= contextDTO.getStructureDeadLine(); i++) {
            if (dailyCapacityLimitVoMap.get(i) == null) {
                continue;
            }
            adjustDailyCapacityLimitObj.calcLhMachinesWithEmbryoTypes(mpProdFinalList, i, dailyCapacityLimitVoMap.get(i), contextDTO.getParamMap(), mainPattern, null);
        }
    }

    /**
     * 重算每日产能限制，包括硫化机台数、胎胚种类数、换模次数以及统计硫化机台数
     *
     * @param contextDTO      周程滚动上下文
     * @param mpProdFinalList 定稿记录列表
     */
    private void reCalcAdjustDailyCapacityLimitWithStaticMachines(MpRollAdjustContextDTO contextDTO, List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList, MpAdjustDailyCapacityLimit adjustDailyCapacityLimitObj, StringBuilder sbError) {
        if (PubUtil.isEmpty(mpProdFinalList)){
            return;
        }
        String proSize = mpProdFinalList.get(0).getProSize();
        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap = contextDTO.getDailyCapacityLimitVoMap();
        MpDailyCapacityLimitVo capacityLimitVo;
        for (int i = contextDTO.getStructureStartDay(); i <= contextDTO.getStructureDeadLine(); i++) {
            if (dailyCapacityLimitVoMap.get(i) == null) {
                continue;
            }
            adjustDailyCapacityLimitObj.calcLhMachinesWithEmbryoTypes(mpProdFinalList, i, dailyCapacityLimitVoMap.get(i), contextDTO.getParamMap(), null, null);

            capacityLimitVo = dailyCapacityLimitVoMap.get(i);
            if (capacityLimitVo.getUsedLhMachines() > capacityLimitVo.getMaxLhMachines()){
                //提示： 结构:[%s]，[%s]日，硫化机台数:[%s]，超出最大硫化机台数:[%s]！
                sbError.append(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.confirm.checkLhMachinesLimit"), contextDTO.getStructureName(), i, capacityLimitVo.getUsedLhMachines() ,capacityLimitVo.getMaxLhMachines())).append(BusiConstant.WeekRollAdjust.SPLIT_FRONT_NEW_LINE);
            }
            if (capacityLimitVo.getUsedEmbryoTypes() > capacityLimitVo.getMaxEmbryoTypes()){
                //提示： 结构:[%s]，[%s]日，胎胚种类数:[%s]，超出最大胎胚种类数:[%s]！
                sbError.append(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.confirm.checkEmbryoTypesLimit"), contextDTO.getStructureName(), i, capacityLimitVo.getUsedEmbryoTypes() ,capacityLimitVo.getMaxEmbryoTypes())).append(BusiConstant.WeekRollAdjust.SPLIT_FRONT_NEW_LINE);
            }

            //补充当前结构的硫化机台数
            Map<String, Integer> mouldShellBlockMachinesMap = dailyCapacityLimitVoMap.get(i).getMouldShellBlockMachinesMap();
            if (mouldShellBlockMachinesMap != null && contextDTO.getMouldShellBlockMachinesMap() != null){
                for (Map.Entry<String, Integer> entry : mouldShellBlockMachinesMap.entrySet()) {
                    String key = entry.getKey() + BusiConstant.WeekRollAdjust.SPLIT_GROUP_KEY + i;
                    Integer blockMachines = Convert.toInt(contextDTO.getMouldShellBlockMachinesMap().get(key),0);
                    blockMachines += Convert.toInt(entry.getValue(),0);
                    contextDTO.getMouldShellBlockMachinesMap().put(key,blockMachines);
                }
            }
            if (contextDTO.getInchMachinesMap() != null && StringUtils.isNotBlank(proSize)){
                //英寸+日，硫化机台数
                String proSizeKey = proSize + BusiConstant.WeekRollAdjust.SPLIT_GROUP_KEY+ i;
                Integer lhMachines = Convert.toInt(contextDTO.getInchMachinesMap().get(proSizeKey),0);
                lhMachines += Convert.toInt(dailyCapacityLimitVoMap.get(i).getUsedLhMachines(),0);
                contextDTO.getInchMachinesMap().put(proSizeKey, lhMachines);
            }

            if (contextDTO.getStructureMachinesMap() != null){
                //结构+日，硫化机台数
                String structureKey = contextDTO.getStructureName() + BusiConstant.WeekRollAdjust.SPLIT_GROUP_KEY+ i;
                Integer lhMachines = Convert.toInt(contextDTO.getStructureMachinesMap().get(structureKey),0);
                lhMachines += Convert.toInt(dailyCapacityLimitVoMap.get(i).getUsedLhMachines(),0);
                contextDTO.getStructureMachinesMap().put(structureKey, lhMachines);
            }
        }
    }
    /**
     * 构建月计划统计结果
     *
     * @param mpProdFinalList 月计划定稿列表
     * @return 统计结果列表
     */
    public MpMonthPlanStatistics buildMonthPlanStatistics(MpRollAdjustContextDTO contextDTO, List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList, String tempFlag) {

        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityMap = contextDTO.getDailyCapacityLimitVoMap();
        List<MpStructureAllocation> oneStructureAllocationList = contextDTO.getOneStructureAllocationList();
        if (PubUtil.isEmpty(dailyCapacityMap) || PubUtil.isEmpty(oneStructureAllocationList) || PubUtil.isEmpty(mpProdFinalList)) {
            log.warn("构建月计划统计结果 ==> 日产能限制Map或者月计划结构转产表-单结构列表为空，跳过不处理");
            return null;
        }

        FactoryMonthPlanFinalAdjustVo monthPlan = mpProdFinalList.get(0);
        MpMonthPlanStatistics statistics = new MpMonthPlanStatistics();
        // 设置月计划统计相关字段
        setMonthPlanStatisticsField(monthPlan, oneStructureAllocationList.get(0), statistics);
        // 遍历日期，设置每个dayN字段
        String dayField;
        int totalQty, oemQty;
        for (int day = ProductionConstant.MONTH_START_DAY; day <= ProductionConstant.MONTH_MAX_DAY; day++) {
            totalQty = 0;
            oemQty = 0;
            for (FactoryMonthPlanFinalAdjustVo prodFinal : mpProdFinalList) {
                dayField = FactoryConstant.DAY_FIELD + day;
                if (prodFinal.getFieldValueByFieldName(dayField) == null) {
                    continue;
                }
                totalQty += (Integer) prodFinal.getFieldValueByFieldName(dayField);
                if (YesOrNoEnum.YES.getCode().equals(prodFinal.getOemFlag())) {
                    //若是贴牌，计划量进行累计
                    oemQty += (Integer) prodFinal.getFieldValueByFieldName(dayField);
                }
            }

            setDayField(statistics, day, dailyCapacityMap, totalQty, oemQty);
            statistics.setTempFlag(tempFlag);
        }
        //同步更新上下文的结构统计
        contextDTO.getStructureStatisticMap().put(contextDTO.getStructureName(), statistics);
        return statistics;
    }

    /**
     * 设置月计划统计相关字段
     */
    private void setMonthPlanStatisticsField(FactoryMonthPlanFinalAdjustVo source, MpStructureAllocation structureAllocation, MpMonthPlanStatistics target) {
        target.setFactoryCode(structureAllocation.getFactoryCode());
        target.setYear(structureAllocation.getYear());
        target.setMonth(structureAllocation.getMonth());
        target.setProductionVersion(source.getProductionVersion());
        target.setMonthPlanVersion(source.getMonthPlanVersion());
        target.setStructureName(structureAllocation.getStructureName());
        target.setYearMonth(source.getYearMonth());
        target.setProSize(source.getProSize());
        target.setStructureType(source.getStructureType());
        target.setLastMonthPlanVersion(source.getLastMonthPlanVersion());
        target.setProductTypeCode(source.getProductTypeCode());
    }

    /**
     * 根据日期获取日产能限制数据转JSON设置到对应dayN字段
     *
     * @param statistics  月计划统计实体
     * @param day         日期
     * @param capacityMap 日产能限制Map
     * @param totalQty    日总计划量
     * @param oemQty      OEM日总计划量
     */
    private void setDayField(MpMonthPlanStatistics statistics, int day, Map<Integer, MpDailyCapacityLimitVo> capacityMap, int totalQty, int oemQty) {
        MpDailyCapacityLimitVo capacityVo = capacityMap == null ? null : capacityMap.get(day);
        if (capacityVo == null) {
            return;
        }
        MpDayProductionStatisticsDetailVo dayProductionStatisticsDetailVo = new MpDayProductionStatisticsDetailVo();
        dayProductionStatisticsDetailVo.setMaxLhMachines(Convert.toInt(capacityVo.getMaxLhMachines(), 0).equals(0) ? null : capacityVo.getMaxLhMachines());
        dayProductionStatisticsDetailVo.setMaxEmbryoTypes(Convert.toInt(capacityVo.getMaxEmbryoTypes(), 0).equals(0) ? null : capacityVo.getMaxEmbryoTypes());
        dayProductionStatisticsDetailVo.setLhMachines(Convert.toInt(capacityVo.getUsedLhMachines(), 0).equals(0) ? null : capacityVo.getUsedLhMachines());
        dayProductionStatisticsDetailVo.setEmbryoCount(Convert.toInt(capacityVo.getUsedEmbryoTypes(), 0).equals(0) ? null : capacityVo.getUsedEmbryoTypes());
        dayProductionStatisticsDetailVo.setChangeMould(Convert.toInt(capacityVo.getUsedChangeMould(), 0).equals(0) ? null : capacityVo.getUsedChangeMould());
        dayProductionStatisticsDetailVo.setTotalQty(totalQty);
        dayProductionStatisticsDetailVo.setOemQty(oemQty);
        if (PubUtil.isNotEmpty(capacityVo.getMouldShellBlockMachinesMap())){
            List<MpDayProductionStatisticsShellVo> mouldShellList = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : capacityVo.getMouldShellBlockMachinesMap().entrySet()) {
                if (entry.getValue() >0){
                    MpDayProductionStatisticsShellVo shellVo = new MpDayProductionStatisticsShellVo();
                    shellVo.setMouldShell(entry.getKey());
                    shellVo.setBlockMachines(entry.getValue());
                    mouldShellList.add(shellVo);
                }
            }
            if (PubUtil.isNotEmpty(mouldShellList)){
                dayProductionStatisticsDetailVo.setMouldShellList(mouldShellList);
            }
        }
        statistics.setFieldValueByFieldName(BusiConstant.WeekRollAdjust.FIELD_PREFIX_DAY + day, JSONObject.toJSONString(dayProductionStatisticsDetailVo));
    }


    /**
     * List转换Map,按结构
     *
     * @param voList
     * @return
     */
    protected Map<String, List<FactoryMonthPlanFinalAdjustVo>> convertToMap(List<FactoryMonthPlanFinalAdjustVo> voList) {
        Map<String, List<FactoryMonthPlanFinalAdjustVo>> result = new HashMap<>();
        for (FactoryMonthPlanFinalAdjustVo vo : voList) {
            result.computeIfAbsent(vo.getStructureName(), k -> new ArrayList<>()).add(vo);
        }
        return result;
    }

    /**
     * 设置 特殊结构总的生产实际排产量
     *
     * @param contextDTO
     * @param mpFinalList
     */
    protected void setSpecStructureTotalQty(MpRollAdjustContextDTO contextDTO, List<FactoryMonthPlanFinalAdjustVo> mpFinalList) {
        if (PubUtil.isEmpty(mpFinalList)) {
            return;
        }
        Integer specStructureTotalQty = mpFinalList.stream().filter(x->x.getTotalQty() != null).mapToInt(FactoryMonthPlanFinalAdjustVo::getTotalQty).sum();
        contextDTO.setSpecStructureTotalQty(specStructureTotalQty);
    }

    /**
     * 初始化OEM标记
     *
     * @param contextDTO
     * @param mpFinalList
     */
    protected void initOemFlag(MpRollAdjustContextDTO contextDTO, List<FactoryMonthPlanFinalAdjustVo> mpFinalList) {
        if (PubUtil.isEmpty(mpFinalList)) {
            return;
        }
        for (FactoryMonthPlanFinalAdjustVo prodFinal : mpFinalList) {
            if (contextDTO.getOemBrandConfigSet().contains(prodFinal.getBrand())) {
                prodFinal.setOemFlag(YesOrNoEnum.YES.getCode());
            } else {
                prodFinal.setOemFlag(YesOrNoEnum.NO.getCode());
            }
        }
    }

    /**
     * 保存月计划统计结果
     *
     * @param contextDTO
     */
    public void saveMonthPlanStatisticsResult(MpRollAdjustContextDTO contextDTO, String tempFlag) {
        List<MpMonthPlanStatistics> monthPlanStatisticsList = contextDTO.getMonthPlanStatisticsList();
        if (PubUtil.isEmpty(monthPlanStatisticsList)) {
            return;
        }

        List<String> structureNameList = monthPlanStatisticsList.stream().filter(x -> x != null && !StringUtil.isEmptyWithTrim(x.getStructureName()))
                .map(x -> x.getStructureName()).collect(Collectors.toList());
        if (PubUtil.isEmpty(structureNameList)) {
            return;
        }
        // 删除月计划统计结果（物理删除）
        mpMonthPlanStatisticsService.deleteMonthPlanStatisticsByCondition(contextDTO.getFactoryCode(),
                String.valueOf(contextDTO.getMpYear()), String.valueOf(contextDTO.getMpMonth()), contextDTO.getProductionVersion(), tempFlag, structureNameList);
        // 去重月计划统计结果
        //monthPlanStatisticsList = distinctMonthPlanStatistics(monthPlanStatisticsList);
        // 保存月计划统计结果
        //baseDao.insertBatch(monthPlanStatisticsList);
        batchMpMonthPlanStatisticsService.insertBatchData(monthPlanStatisticsList);
        log.info("保存月计划统计结果成功，共新增:{}条记录", monthPlanStatisticsList.size());
    }

    /**
     * 去重月计划统计结果（按结构名称去重）
     *
     * @param monthPlanStatisticsList 原始列表
     * @return 去重后的列表
     */
    protected List<MpMonthPlanStatistics> distinctMonthPlanStatistics(List<MpMonthPlanStatistics> monthPlanStatisticsList) {
        if (PubUtil.isEmpty(monthPlanStatisticsList)) {
            return new ArrayList<>();
        }
        return monthPlanStatisticsList.stream()
                .filter(item -> StringUtils.isNotEmpty(item.getStructureName()))
                .collect(Collectors.toMap(
                        MpMonthPlanStatistics::getStructureName,
                        item -> item,
                        (existing, newItem) -> existing
                ))
                .values().stream()
                .collect(Collectors.toList());
    }

    /**
     * 保存调整结果
     *
     * @param contextDTO
     */
    private void saveMpAdjustResult(MpRollAdjustContextDTO contextDTO) {
        //List<FactoryMonthPlanFinalAdjustVo> factoryMonthPlanProdFinalList = contextDTO.getFactoryMonthPlanProdFinalList();
        List<FactoryMonthPlanFinalAdjustVo> saveMpProdFinalList = contextDTO.getSaveMpProdFinalList();
        if (PubUtil.isEmpty(saveMpProdFinalList)) {
            return;
        }
        String lastMonthPlanVersion = contextDTO.getAdjustMonthPlanVersion();
        //1、根据调整版本 先删除(物理)
        mpAdjustResultEntityMapper.deleteAdjustResultByVersion(contextDTO.getFactoryCode(),
                String.valueOf(contextDTO.getMpYear()), String.valueOf(contextDTO.getMpMonth()), contextDTO.getVersion(), contextDTO.getStructureName());
        //2、保存调整记录
        MpAdjustResult mpAdjustResult;
        List<MpAdjustResult> mpAdjustResultList = new ArrayList<>();
        for (FactoryMonthPlanFinalAdjustVo finalAdjustVo : saveMpProdFinalList) {
            mpAdjustResult = new MpAdjustResult();
            BeanUtils.copyProperties(finalAdjustVo, mpAdjustResult);
            mpAdjustResult.setId(null);
            mpAdjustResult.setAdjustType(contextDTO.getAdjustType());
            mpAdjustResult.setVersion(contextDTO.getVersion());
            mpAdjustResult.setMonthPlanVersion(contextDTO.getMonthPlanVersion());
            mpAdjustResult.setLastMonthPlanVersion(lastMonthPlanVersion);
            mpAdjustResult.setTotalPlanQty(finalAdjustVo.getTotalQty());

            mpAdjustResult.setAdjustFlag((finalAdjustVo.getActualAdjustQty() != null && Math.abs(finalAdjustVo.getActualAdjustQty()) > 0) ? YesOrNoEnum.YES.getCode() : YesOrNoEnum.NO.getCode());
            if (YesOrNoEnum.YES.getCode().equals(mpAdjustResult.getAdjustFlag())){
                //若有调整过标志，将上月有效标志置为否
                mpAdjustResult.setLastMonthValidFlag(YesOrNoEnum.NO.getCode());
            }
            if (StringUtil.isEmptyWithTrim(mpAdjustResult.getIsLockSchedule())) {
                mpAdjustResult.setIsLockSchedule(YesOrNoEnum.NO.getCode());
            }
            // 将日期字段中值为0的字段设为null
            handleZeroToNull(mpAdjustResult);
            mpAdjustResultList.add(mpAdjustResult);
        }
        //baseDao.insertBatch(mpAdjustResultList);
        batchMpAdjustResultService.insertBatchData(mpAdjustResultList);
        contextDTO.setAdjustResultList(mpAdjustResultList);
    }

    /**
     * 保存调整过程日志
     *
     * @param contextDTO
     */
    private void saveMpAdjustProcLog(MpRollAdjustContextDTO contextDTO) {
        List<FactoryMonthPlanFinalAdjustVo> adjustProcLogList = contextDTO.getSaveAdjustProcLogList();
        if (PubUtil.isEmpty(adjustProcLogList)) {
            return;
        }
        //1、根据调整版本 先删除(物理)
        mpAdjustMaterialLogService.deleteAdjustProcLogByVersion(contextDTO.getFactoryCode(),
                String.valueOf(contextDTO.getMpYear()), String.valueOf(contextDTO.getMpMonth()), contextDTO.getVersion());
        //2、保存调整记录
        MpAdjustMaterialLog mpMaterialLog;
        List<MpAdjustMaterialLog> mpMaterialLogList = new ArrayList<>();
        for (FactoryMonthPlanFinalAdjustVo finalAdjustVo : adjustProcLogList) {
            mpMaterialLog = new MpAdjustMaterialLog();
            BeanUtils.copyProperties(finalAdjustVo, mpMaterialLog);
            mpMaterialLog.setId(null);
            mpMaterialLog.setAdjustType(contextDTO.getAdjustType());
            mpMaterialLog.setAdjVersion(contextDTO.getVersion());
            mpMaterialLog.setAdjustDetail(finalAdjustVo.getAdjustDetail().toString());
            mpMaterialLogList.add(mpMaterialLog);
        }
        //baseDao.insertBatch(mpMaterialLogList);
        batchMpAdjustMaterialLogService.insertBatchData(mpMaterialLogList);
    }

    /**
     * 将日期字段中值为0的字段设为null
     *
     * @param monthPlan
     */
    protected void handleZeroToNull(FactoryMonthPlanProductionFinalResult monthPlan) {
        // 遍历日期，设置每个dayN字段
        for (int day = ProductionConstant.MONTH_START_DAY; day <= ProductionConstant.MONTH_MAX_DAY; day++) {
            String fieldName = BusiConstant.WeekRollAdjust.FIELD_PREFIX_DAY + day;
            if (Convert.toInt(monthPlan.getFieldValueByFieldName(fieldName), 0) == 0) {
                monthPlan.setFieldValueByFieldName(fieldName, null);
            }
        }
    }

    /**
     * 将日期字段中值为0的字段设为null
     *
     * @param result
     */
    protected void handleZeroToNull(MpAdjustResult result) {
        // 遍历日期，设置每个dayN字段
        for (int day = ProductionConstant.MONTH_START_DAY; day <= ProductionConstant.MONTH_MAX_DAY; day++) {
            String fieldName = BusiConstant.WeekRollAdjust.FIELD_PREFIX_DAY + day;
            if (Convert.toInt(result.getFieldValueByFieldName(fieldName), 0) == 0) {
                result.setFieldValueByFieldName(fieldName, null);
            }
        }
    }

    /**
     * 保存调整日志
     *
     * @param contextDTO
     */
    public void saveMpAdjustLog(MpRollAdjustContextDTO contextDTO) {
        String logDetail = contextDTO.getLogDetail().toString();
        if (StringUtil.isEmptyWithTrim(logDetail)) {
            return;
        }
        //1、根据调整版本 先删除(物理)
        mpAdjustLogService.deleteAdjustLogByVersion(contextDTO.getFactoryCode(),
                String.valueOf(contextDTO.getMpYear()), String.valueOf(contextDTO.getMpMonth()),
                contextDTO.getVersion(), contextDTO.getStructureName());
        //2、保存调整日志
        MpAdjustStructureLog structureLog = new MpAdjustStructureLog();
        structureLog.setFactoryCode(contextDTO.getFactoryCode());
        structureLog.setYear(contextDTO.getMpYear());
        structureLog.setMonth(contextDTO.getMpMonth());
        structureLog.setStructureName(contextDTO.getStructureName());
        structureLog.setProductionVersion(contextDTO.getProductionVersion());
        structureLog.setLastMonthPlanVersion(contextDTO.getVersion());
        structureLog.setAdjVersion(contextDTO.getVersion());
        structureLog.setAction(contextDTO.getAdjustType());
        structureLog.setBeforeBeginDay(contextDTO.getStartDay());
        structureLog.setBeforeEndDay(contextDTO.getEndDay());
        structureLog.setAfterBeginDay(contextDTO.getAdjustStartDay());
        structureLog.setAfterEndDay(contextDTO.getAdjustEndDay());
        structureLog.setScheduledMachines(contextDTO.getScheduledMachines());
        structureLog.setOperator(SecurityUtils.getUsername());
        structureLog.setLogDetail(logDetail);
        baseDao.insert(structureLog);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmAdjust(MpRollAdjustContextDTO contextDTO) throws BusinessException {
        log.info("开始执行周程调整确认流程，调整类型：{}，工厂：{}，年份：{}，月份：{}，版本：{}，排产版本：{}，结构名称：{}，排产机台：{}，开始日期：{}，结束日期：{}，调整开始日期：{}，调整结束日期：{}",
                contextDTO.getAdjustType(), contextDTO.getFactoryCode(), contextDTO.getMpYear(),
                contextDTO.getMpMonth(), contextDTO.getVersion(), contextDTO.getProductionVersion(),
                contextDTO.getStructureName(), contextDTO.getScheduledMachines(), contextDTO.getStartDay(),
                contextDTO.getEndDay(), contextDTO.getAdjustStartDay(), contextDTO.getAdjustEndDay());
        if (StringUtil.isEmptyWithTrim(contextDTO.getVersion())) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.versionEmpty"));
        }
        contextDTO.setAdjustMonthPlanVersion(contextDTO.getVersion());
        // 设置周程滚动参数
        contextDTO.setParamMap(mpAdjustStructureInService.getMpWeekAdjustParam(contextDTO.getFactoryCode(), ProductTypeEnum.WHOLE_STEEL.getValue()));

        // 初始化SKU排产分类
        initSkuProductionType(contextDTO);
        initMaterialInfo(contextDTO);
        initMouldInfo(contextDTO);
        initMouldShellInfo(contextDTO);
        initCapsuleChuckInfo(contextDTO);
        // 设置调整日（依赖 paramMap）
        setAdjustDate(contextDTO);

        // 1、查询周程调整结果
        queryAdjustResult(contextDTO);
        // 2、查询调整明细
        queryAdjustDetailList(contextDTO);
        // 3、查询月度生产计划
        if (StringUtil.isEmptyWithTrim(contextDTO.getProductionVersion())) {
            if (PubUtil.isNotEmpty(contextDTO.getAdjustResultList())) {
                contextDTO.setProductionVersion(contextDTO.getAdjustResultList().get(0).getProductionVersion());
            }
        }
        queryMonthPlanList(contextDTO);
        // 4、更新试制量制计划--排产日期
        updateTrialPlanList(contextDTO);
        // 5、更新调整明细--实际调整量
        updateAdjustDetailList(contextDTO);
        // 6、更新月度生产计划
        updateMonthPlanList(contextDTO);
        // 7、新增月度生产计划
        insertMonthPlanList(contextDTO);
        // 8、更新结构转产
        updateStructureAllocationList(contextDTO);
        // 初始模壳值
        initMouldShellValue(contextDTO, contextDTO.getFactoryMonthPlanProdFinalList());
        // 9、处理月计划统计结果
        String bakStructureName = contextDTO.getStructureName();
        handleMonthPlanStatistics(contextDTO, null);
        contextDTO.setStructureName(bakStructureName);
        // 10、检查日产预警限制
        checkDayAlarmLimit(contextDTO);
        // 11.检查胶囊卡盘限制
        checkCapsuleChuckLimit(contextDTO);
        // 12、检查模壳标准限制
        checkMouldShellLimit(contextDTO);
        // 13、合并至定稿月度生产计划并更新最新版本号
        // 根据优先级顺序分配生产数量
        allocateProductionByPriority(contextDTO);
        saveMpProductionFinalResult(contextDTO);
        updateMonthPlanVersion(contextDTO);

        //14、将定稿月度生产计划合并到月度硫化监控表
        mergeToMonthPlanSulfurizationMonitor(contextDTO);
        log.info("周程调整确认流程执行完成");
    }

    /**
     * 检查模壳标准限制
     *
     * @param contextDTO 滚动上下文
     */
    private void checkMouldShellLimit(MpRollAdjustContextDTO contextDTO) {
        if (PubUtil.isEmpty(contextDTO.getMdmMouldInfoMap())){
            return;
        }
        if (PubUtil.isEmpty(contextDTO.getMdmMouldShellInfoList())){
            return;
        }
        //1、根据规格+主花纹，初始模壳标准
        List<FactoryMonthPlanFinalAdjustVo> factoryMonthPlanProdFinalList = contextDTO.getFactoryMonthPlanProdFinalList();

        //2、检查模壳标准
        StringBuilder sbError = new StringBuilder();
        int maxDays = com.zlt.aps.mp.engine.utils.DateUtils.getDaysByYearMonth(contextDTO.getMpYear(), contextDTO.getMpMonth());
        List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList;
        Integer sumMouldQty,oriSumMouldQty,remainMouldQty;
        Map<String,Integer> mouldShellCombineRemainQtyMap = new HashMap<>();
        List<String> deductBlockFlag = new ArrayList<>();
        //2.1 先检查没有组合的模壳标准
        for (MdmMouldShellInfo shellInfo:contextDTO.getMdmMouldShellInfoList()){
            mpProdFinalList = factoryMonthPlanProdFinalList.stream().filter(x->shellInfo.getMouldSetCode().equals(x.getMouldShell())).collect(Collectors.toList());
            if (PubUtil.isEmpty(mpProdFinalList)){
                continue;
            }
            for (int iDay = FactoryConstant.MONTH_START_DAY; iDay <= maxDays; iDay++) {
                sumMouldQty = getMouldQtyByShellAndDay(mpProdFinalList, iDay);
                //扣除 换活块的机台数
                //注：换模的机台要分开计算视2个模壳；换活块的机台视1个模壳，一定相同；
                if (contextDTO.getMouldShellBlockMachinesMap() != null){
                    String key = shellInfo.getMouldSetCode() + BusiConstant.WeekRollAdjust.SPLIT_GROUP_KEY + iDay;
                    Integer blockMachines = Convert.toInt(contextDTO.getMouldShellBlockMachinesMap().get(key),0);
                    sumMouldQty -= blockMachines * 2;
                    if (deductBlockFlag.indexOf(shellInfo.getMouldSetCode())<0){
                        deductBlockFlag.add(shellInfo.getMouldSetCode());
                    }
                }
                if (sumMouldQty > shellInfo.getTotalQty()){
                    //提示： 模壳标准:[%s]，[%s]日，模壳数:[%s]，超出剩余模壳数:[%s]！
                    sbError.append(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.confirm.mouldShellLimit"), shellInfo.getMouldSetCode(), iDay, sumMouldQty ,shellInfo.getTotalQty())).append(BusiConstant.WeekRollAdjust.SPLIT_FRONT_NEW_LINE);
                    remainMouldQty = 0;
                }else{
                    remainMouldQty = shellInfo.getTotalQty() - sumMouldQty;
                }

                mouldShellCombineRemainQtyMap.put(shellInfo.getMouldSetCode()+iDay, remainMouldQty);
            }
        }
        //2.2 检查有组合的模壳标准
       Map<String, List<FactoryMonthPlanFinalAdjustVo>> mpCombineProdFinalMap = factoryMonthPlanProdFinalList.stream().filter(x->x.getMouldShell()!=null && x.getMouldShell().indexOf(BusiConstant.WeekRollAdjust.SPLIT_COMMA)>0).collect(Collectors.groupingBy(FactoryMonthPlanFinalAdjustVo::getMouldShell));
        if (mpCombineProdFinalMap != null &&  mpCombineProdFinalMap.size() > 0){
            for (Map.Entry<String, List<FactoryMonthPlanFinalAdjustVo>> entry1 : mpCombineProdFinalMap.entrySet()) {
                for (int iDay = FactoryConstant.MONTH_START_DAY; iDay <= maxDays; iDay++) {

                    sumMouldQty = getMouldQtyByShellAndDay(entry1.getValue(), iDay);

                    String[] mouldShellArr = entry1.getKey().split(BusiConstant.WeekRollAdjust.SPLIT_COMMA);
                    //扣除 换活块的机台数
                    if (contextDTO.getMouldShellBlockMachinesMap() != null ){
                        String key = entry1.getKey() + BusiConstant.WeekRollAdjust.SPLIT_GROUP_KEY + iDay;
                        Integer blockMachines = Convert.toInt(contextDTO.getMouldShellBlockMachinesMap().get(key),0);
                        sumMouldQty -= blockMachines * 2;
                    }
                    oriSumMouldQty = sumMouldQty;
                    Integer shellRemainQty;
                    Integer oriShellRemainQty = 0;
                    for (String shell : mouldShellArr){
                        shellRemainQty = mouldShellCombineRemainQtyMap.get(shell+iDay);
                        oriShellRemainQty += shellRemainQty;

                        if (sumMouldQty >= shellRemainQty){
                            sumMouldQty -= shellRemainQty;
                            mouldShellCombineRemainQtyMap.put(shell+iDay,0);
                        }else{
                            sumMouldQty = 0;
                            shellRemainQty -= sumMouldQty;
                            mouldShellCombineRemainQtyMap.put(shell+iDay,shellRemainQty);
                        }
                    }
                    if (sumMouldQty > 0){
                        //还有剩，表示超标，提示
                        //提示： 模壳标准:[%s]，[%s]日，模壳数:[%s]，超出剩余模壳数:[%s]！
                        sbError.append(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.confirm.mouldShellLimit"), String.join(BusiConstant.WeekRollAdjust.SPLIT_COMMA, mouldShellArr), iDay, oriSumMouldQty ,oriShellRemainQty)).append(BusiConstant.WeekRollAdjust.SPLIT_FRONT_NEW_LINE);
                    }
                }
            }
        }

        if (!StringUtil.isEmptyWithTrim(sbError.toString())) {
            throw new BusinessException(sbError.toString());
        }
    }

    /**
     * 初始化模壳数据
     * @param contextDTO
     * @param factoryMonthPlanProdFinalList
     */
    private void initMouldShellValue(MpRollAdjustContextDTO contextDTO, List<FactoryMonthPlanFinalAdjustVo> factoryMonthPlanProdFinalList) {
        String key,mouldShell;
        for (FactoryMonthPlanFinalAdjustVo adjustVo: factoryMonthPlanProdFinalList){
            key = getSpecAndMainPatternKey(adjustVo.getSpecifications(),adjustVo.getMainPattern());
            mouldShell = contextDTO.getMdmMouldInfoMap().get(key);
            if (StringUtils.isNotBlank(mouldShell)){
                adjustVo.setMouldShell(mouldShell);
            }
        }
    }

    /**
     * 检查胶囊卡盘限制
     *
     * @param contextDTO 滚动上下文
     */
    private void checkCapsuleChuckLimit(MpRollAdjustContextDTO contextDTO) {
        if (PubUtil.isEmpty(contextDTO.getMdmCapsuleChuckList())){
            return;
        }

        //1、检查胶囊卡盘
        StringBuilder sbError = new StringBuilder();
        int maxDays = com.zlt.aps.mp.engine.utils.DateUtils.getDaysByYearMonth(contextDTO.getMpYear(), contextDTO.getMpMonth());
        Integer chuckTotalQty;
        for (MdmCapsuleChuck capsuleChuck:contextDTO.getMdmCapsuleChuckList()){
            chuckTotalQty = Convert.toInt(capsuleChuck.getNewChuckQty()) + Convert.toInt(capsuleChuck.getInternalQty());
            if (StringUtils.isNotBlank(capsuleChuck.getSpecifications())){
                //1.1 按规格(其实是结构)
                checkOneCapsuleChuck(contextDTO, maxDays, chuckTotalQty, sbError,capsuleChuck.getSpecifications(),contextDTO.getStructureMachinesMap());
            }else if (StringUtils.isNotBlank(capsuleChuck.getProSize())){
                //1.2 按英寸
                checkOneCapsuleChuck(contextDTO, maxDays, chuckTotalQty, sbError,capsuleChuck.getProSize(),contextDTO.getInchMachinesMap());
            }
        }

        if (!StringUtil.isEmptyWithTrim(sbError.toString())) {
            throw new BusinessException(sbError.toString());
        }
    }

    /**
     * 检查 一个维度的胶囊卡盘
     * @param contextDTO
     * @param maxDays
     * @param chuckTotalQty
     * @param sbError
     */
    private void checkOneCapsuleChuck(MpRollAdjustContextDTO contextDTO, int maxDays, Integer chuckTotalQty, StringBuilder sbError, String spec2ProSize, Map<String, Integer> machinesMap) {
        if (PubUtil.isEmpty(machinesMap)){
            return;
        }
        String[] specArr = spec2ProSize.split(BusiConstant.WeekRollAdjust.SPLIT_COMMA);
        Integer sumMouldQty;
        for (int iDay = FactoryConstant.MONTH_START_DAY; iDay <= maxDays; iDay++) {
            sumMouldQty = 0;
            for (String spec: specArr){
                String key = spec + BusiConstant.WeekRollAdjust.SPLIT_GROUP_KEY + iDay;
                sumMouldQty += Convert.toInt(machinesMap.get(key),0);
            }
            //硫化机台数 转 模数
            sumMouldQty = sumMouldQty * 2;
            if (sumMouldQty > chuckTotalQty){
                //提示： 胶囊卡盘:[%s]，[%s]日，统计数:[%s]，超出最大卡盘数:[%s]！
                sbError.append(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.confirm.capsuleChuckLimit"), spec2ProSize, iDay, sumMouldQty ,chuckTotalQty)).append(BusiConstant.WeekRollAdjust.SPLIT_FRONT_NEW_LINE);
            }
        }
    }

    /**
     * 按模壳和日，获取模具数量
     * @param mpProdFinalList
     * @param iDay
     * @return
     */
    private Integer getMouldQtyByShellAndDay(List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList, int iDay) {
        Integer sumMouldQty;
        Integer mouldQty;
        sumMouldQty = 0;
        for (FactoryMonthPlanFinalAdjustVo adjustVo: mpProdFinalList){
            mouldQty = (Integer) adjustVo.getFieldValueByFieldName(FactoryConstant.MOULD_QTY_DAY_FIELD+ iDay);
            if (mouldQty != null && mouldQty > 0){
                sumMouldQty += mouldQty;
            }
        }
        return sumMouldQty;
    }

    /**
     * 检查日产预警限制
     *
     * @param contextDTO 滚动上下文
     */
    private void checkDayAlarmLimit(MpRollAdjustContextDTO contextDTO) {
        Map<String, MpMonthPlanStatistics> structureStatisticMap = contextDTO.getStructureStatisticMap();
        if (PubUtil.isEmpty(structureStatisticMap)) {
            return;
        }
        String dayFieldName;
        String dayStatisticsStr;
        MpMonthPlanStatistics statistics;
        MpDayProductionStatisticsDetailVo dayStatistics;
        MpDailyCapacityLimitVo dailyCapacityLimitVo;
        int dayTotalQty;
        int maxAlarmLimit = (Integer) contextDTO.getParamMap().get(MonthPlanEnums.DAY_MAX_ALARM_LIMIT.getCode());
        int minAlarmLimit = (Integer) contextDTO.getParamMap().get(MonthPlanEnums.DAY_MIN_ALARM_LIMIT.getCode());
        StringBuilder sbError = new StringBuilder();
        int maxDays = com.zlt.aps.mp.engine.utils.DateUtils.getDaysByYearMonth(contextDTO.getMpYear(), contextDTO.getMpMonth());
        for (int iDay = FactoryConstant.MONTH_START_DAY; iDay <= maxDays; iDay++) {
            dayTotalQty = 0;
            dailyCapacityLimitVo = contextDTO.getDailyCapacityLimitVoMap().get(iDay);
            if (!YesOrNoEnum.YES.getCode().equals(dailyCapacityLimitVo.getDayOpenCloseFlag())){
                continue;
            }
            for (Map.Entry<String, MpMonthPlanStatistics> entry1 : structureStatisticMap.entrySet()) {
                dayFieldName = FactoryConstant.DAY_FIELD + iDay;
                statistics = entry1.getValue();
                if (statistics == null) {
                    continue;
                }
                dayStatisticsStr = (String) statistics.getFieldValueByFieldName(dayFieldName);
                if (StringUtils.isNotEmpty(dayStatisticsStr) && JSONValidator.from(dayStatisticsStr).validate()) {
                    dayStatistics = JSONObject.parseObject(dayStatisticsStr, MpDayProductionStatisticsDetailVo.class);
                    if (dayStatistics == null) {
                        continue;
                    }
                    dayTotalQty += Optional.ofNullable(dayStatistics.getTotalQty()).orElse(0);
                }
            }
            if (dayTotalQty > maxAlarmLimit || dayTotalQty < minAlarmLimit) {
                sbError.append(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.confirm.checkDayAlarmLimit"), iDay, dayTotalQty, minAlarmLimit, maxAlarmLimit)).append(BusiConstant.WeekRollAdjust.SPLIT_FRONT_NEW_LINE);
            }
        }
        if (!StringUtil.isEmptyWithTrim(sbError.toString())) {
            throw new BusinessException(sbError.toString());
        }
    }

    /**
     * 合并至定稿月度生产计划,同时过滤掉总排产量为0的数据
     *
     * @param contextDTO
     */
    private void saveMpProductionFinalResult(MpRollAdjustContextDTO contextDTO) {
        //先删除
      /*  LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> finalLambdaQueryWrapper = new LambdaQueryWrapper<>();
        finalLambdaQueryWrapper.eq(FactoryMonthPlanProductionFinalResult::getYear, contextDTO.getMpYear());
        finalLambdaQueryWrapper.eq(FactoryMonthPlanProductionFinalResult::getMonth, contextDTO.getMpMonth());
        finalLambdaQueryWrapper.eq(!StringUtil.isEmptyWithTrim(contextDTO.getStructureName()),FactoryMonthPlanProductionFinalResult::getStructureName, contextDTO.getStructureName());
        factoryMonthPlanProdFinalMapper.delete(finalLambdaQueryWrapper);*/
        //后插入
        List<FactoryMonthPlanProductionFinalResult> finalResultList = BeanUtil.copyToList(contextDTO.getFactoryMonthPlanProdFinalList(), FactoryMonthPlanProductionFinalResult.class);
        if (!StringUtil.isEmptyWithTrim(contextDTO.getStructureName())) {
            finalResultList = finalResultList.stream().filter(x -> x.getStructureName().equals(contextDTO.getStructureName())).collect(Collectors.toList());
        }
        List<FactoryMonthPlanProductionFinalResult> insertList = new ArrayList<>();
        List<FactoryMonthPlanProductionFinalResult> updateList = new ArrayList<>();
        for (FactoryMonthPlanProductionFinalResult finalResult : finalResultList) {
            if (finalResult.getId() == null) {
                insertList.add(finalResult);
            } else {
                updateList.add(finalResult);
            }
        }
        if (PubUtil.isNotEmpty(insertList)) {
            batchMpProductionFinalResultService.insertBatchData(insertList);
        }
        if (PubUtil.isNotEmpty(updateList)) {
            batchMpProductionFinalResultService.updateBatchData(updateList);
        }
    }

    /**
     * 将定稿月度生产计划合并到月度硫化监控表
     * 合并维度：排产版本号 + 物料编码 + 产品状态
     *
     * @param contextDTO 滚动上下文
     */
    private void mergeToMonthPlanSulfurizationMonitor(MpRollAdjustContextDTO contextDTO) {
        List<FactoryMonthPlanFinalAdjustVo> finalList = contextDTO.getFactoryMonthPlanProdFinalList();
        if (PubUtil.isEmpty(finalList)) {
            return;
        }
        String productionVersion = contextDTO.getProductionVersion();
        if (StringUtil.isEmptyWithTrim(productionVersion)) {
            return;
        }

        // 1. 查询当前排产版本下已有的硫化监控记录
        LambdaQueryWrapper<MpMonthPlanMonitor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MpMonthPlanMonitor::getProductionVersion, productionVersion);
        queryWrapper.eq(MpMonthPlanMonitor::getIsDelete, YesOrNoEnum.NO.getValue());
        List<MpMonthPlanMonitor> existingMonitorList = mpMonthPlanMonitorEntityMapper.selectList(queryWrapper);

        // 2. 按 排产版本号+物料编码+产品状态 构建已有监控记录Map
        Map<String, MpMonthPlanMonitor> existingMonitorMap = new HashMap<>();
        if (PubUtil.isNotEmpty(existingMonitorList)) {
            existingMonitorMap = existingMonitorList.stream().collect(Collectors.toMap(
                    item -> GenerageMapKeyUtils.createMapKey(item.getMaterialCode(), BusiConstant.WeekRollAdjust.SPLIT_GROUP_KEY, item.getProductStatus()),
                    Function.identity(),
                    (m1, m2) -> m1));
        }

        // 3. 遍历调整结果，更新已有或新增监控记录
        List<MpMonthPlanMonitor> updateList = new ArrayList<>();
        List<MpMonthPlanMonitor> insertList = new ArrayList<>();

        for (FactoryMonthPlanFinalAdjustVo finalResult : finalList) {
            String mapKey = GenerageMapKeyUtils.createMapKey(finalResult.getMaterialCode(), BusiConstant.WeekRollAdjust.SPLIT_GROUP_KEY, finalResult.getProductStatus());
            MpMonthPlanMonitor existingMonitor = existingMonitorMap.get(mapKey);

            if (existingMonitor != null) {
                // 更新已有监控记录
                existingMonitor.setStructureName(finalResult.getStructureName());
                existingMonitor.setMainMaterialDesc(finalResult.getMainMaterialDesc());
                existingMonitor.setMaterialDesc(finalResult.getMaterialDesc());
                existingMonitor.setMesMaterialCode(finalResult.getMesMaterialCode());
                existingMonitor.setBrand(finalResult.getBrand());
                existingMonitor.setProSize(finalResult.getProSize());
                existingMonitor.setSpecifications(finalResult.getSpecifications());
                existingMonitor.setMainPattern(finalResult.getMainPattern());
                existingMonitor.setPattern(finalResult.getPattern());
                existingMonitor.setMouldQty(finalResult.getMouldChangeInfo());
                existingMonitor.setConstructionStage(finalResult.getConstructionStage());
                if (finalResult.getBeginDay() != null && finalResult.getBeginDay() != 0) {
                    LocalDate beginLocalDate = LocalDate.of(contextDTO.getMpYear(), contextDTO.getMpMonth(), finalResult.getBeginDay());
                    Date beginDate = Date.from(beginLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    existingMonitor.setOnboardDate(beginDate);
                }
                if (finalResult.getEndDay() != null && finalResult.getEndDay() != 0) {
                    LocalDate endLocalDate = LocalDate.of(contextDTO.getMpYear(), contextDTO.getMpMonth(), finalResult.getEndDay());
                    Date endDate = Date.from(endLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    existingMonitor.setPlanCloseDate(endDate);
                }
                existingMonitor.setUpdateTime(DateUtils.getNowDate());
                if (ConstructionStageEnum.FORMAL_FLAG.equals(finalResult.getProductStatus())) {
                    existingMonitor.setNetDemandQty(finalResult.getProdReqPlan());
                } else {
                    existingMonitor.setNetDemandQty(finalResult.getTrialQty());
                }
                existingMonitor.setScheduleQty(finalResult.getTotalQty());
                Integer productionQty = ObjectUtils.defaultIfNull(existingMonitor.getProductionQty(), 0);
                Integer scheduleQty = ObjectUtils.defaultIfNull(finalResult.getTotalQty(), 0);
                existingMonitor.setLhMargin(scheduleQty - productionQty);
                existingMonitor.setFinalResultId(finalResult.getId());
                updateList.add(existingMonitor);
            } else {
                // 新增监控记录
                MpMonthPlanMonitor monitor = new MpMonthPlanMonitor();
                monitor.setFactoryCode(finalResult.getFactoryCode());
                monitor.setYear(finalResult.getYear());
                monitor.setMonth(finalResult.getMonth());
                monitor.setYearMonth(finalResult.getYearMonth());
                monitor.setMonthPlanVersion(finalResult.getMonthPlanVersion());
                monitor.setProductionVersion(finalResult.getProductionVersion());
                monitor.setProductTypeCode(finalResult.getProductTypeCode());
                monitor.setProductStatus(finalResult.getProductStatus());
                monitor.setStructureName(finalResult.getStructureName());
                monitor.setMainMaterialDesc(finalResult.getMainMaterialDesc());
                monitor.setMesMaterialCode(finalResult.getMesMaterialCode());
                monitor.setMaterialCode(finalResult.getMaterialCode());
                monitor.setMaterialDesc(finalResult.getMaterialDesc());
                monitor.setBrand(finalResult.getBrand());
                monitor.setProSize(finalResult.getProSize());
                monitor.setSpecifications(finalResult.getSpecifications());
                monitor.setMainPattern(finalResult.getMainPattern());
                monitor.setPattern(finalResult.getPattern());
                monitor.setMouldQty(finalResult.getMouldChangeInfo());
                monitor.setConstructionStage(finalResult.getConstructionStage());
                if (finalResult.getBeginDay() != null && finalResult.getBeginDay() != 0) {
                    LocalDate beginLocalDate = LocalDate.of(contextDTO.getMpYear(), contextDTO.getMpMonth(), finalResult.getBeginDay());
                    Date beginDate = Date.from(beginLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    monitor.setOnboardDate(beginDate);
                }
                if (finalResult.getEndDay() != null && finalResult.getEndDay() != 0) {
                    LocalDate endLocalDate = LocalDate.of(contextDTO.getMpYear(), contextDTO.getMpMonth(), finalResult.getEndDay());
                    Date endDate = Date.from(endLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    monitor.setPlanCloseDate(endDate);
                }
                monitor.setCreateTime(DateUtils.getNowDate());
                monitor.setUpdateTime(monitor.getCreateTime());
                if (ConstructionStageEnum.FORMAL_FLAG.equals(finalResult.getProductStatus())) {
                    monitor.setNetDemandQty(finalResult.getProdReqPlan());
                } else {
                    monitor.setNetDemandQty(finalResult.getTrialQty());
                }
                monitor.setScheduleQty(finalResult.getTotalQty());
                Integer scheduleQty = ObjectUtils.defaultIfNull(finalResult.getTotalQty(), 0);
                monitor.setLhMargin(scheduleQty);
                monitor.setExpectedCloseDay(0);
                monitor.setFinalResultId(finalResult.getId());
                insertList.add(monitor);
            }
        }

        // 4. 批量更新
        if (PubUtil.isNotEmpty(updateList)) {
            baseDao.updateBatch(updateList);
        }
        // 5. 批量新增
        if (PubUtil.isNotEmpty(insertList)) {
            baseDao.insertBatch(insertList);
        }

        log.info("合并到月度硫化监控表完成，更新：{}条，新增：{}条", updateList.size(), insertList.size());
    }

    /**
     * 重新计算
     *
     * @param contextDTO 周程滚动调整上下文对象
     * @throws BusinessException 业务异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recalculate(MpRollAdjustContextDTO contextDTO, Boolean isHandleMonthPlanStatistics) throws BusinessException {
        log.info("开始执行周程调整重新计算流程，调整类型：{}，工厂：{}，年份：{}，月份：{}，版本：{}，排产版本：{}，结构名称：{}，排产机台：{}，开始日期：{}，结束日期：{}，调整开始日期：{}，调整结束日期：{}",
                contextDTO.getAdjustType(), contextDTO.getFactoryCode(), contextDTO.getMpYear(),
                contextDTO.getMpMonth(), contextDTO.getVersion(), contextDTO.getProductionVersion(),
                contextDTO.getStructureName(), contextDTO.getScheduledMachines(), contextDTO.getStartDay(),
                contextDTO.getEndDay(), contextDTO.getAdjustStartDay(), contextDTO.getAdjustEndDay());
        if (StringUtil.isEmptyWithTrim(contextDTO.getVersion())) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.versionEmpty"));
        }
        FactoryMonthPlanProductionFinalResult params = new FactoryMonthPlanProductionFinalResult();
        params.setFactoryCode(contextDTO.getFactoryCode());
        params.setYear(contextDTO.getMpYear());
        params.setMonth(contextDTO.getMpMonth());
        params.setVersion(contextDTO.getVersion());
        params.setStructureName(contextDTO.getStructureName());
        List<FactoryMonthPlanFinalAdjustVo> adjustVos = finalResultService.list4Adjust(params);
        contextDTO.setFactoryMonthPlanProdFinalList(adjustVos);
        // 2、处理月计划统计结果
        if (isHandleMonthPlanStatistics) {
            // 设置周程滚动参数
            contextDTO.setParamMap(mpAdjustStructureInService.getMpWeekAdjustParam(contextDTO.getFactoryCode(), ProductTypeEnum.WHOLE_STEEL.getValue()));
            handleMonthPlanStatistics(contextDTO, YesOrNoEnum.YES.getCode());
        }
        log.info("周程调整确认流程执行完成");
    }

    /**
     * 处理月计划统计结果
     *
     * @param contextDTO
     */
    protected void handleMonthPlanStatistics(MpRollAdjustContextDTO contextDTO, String tempFlag) {
        // 获取月度生产计划
        List<FactoryMonthPlanFinalAdjustVo> monthPLanList = contextDTO.getFactoryMonthPlanProdFinalList();
        if (PubUtil.isEmpty(monthPLanList)) {
            log.warn("处理月计划统计结果：月度生产计划列表为空，直接返回");
            return;
        }
        // 结构名称
        String structureNameParam = contextDTO.getStructureName();

        FactoryMonthPlanFinalAdjustVo monthPlan = monthPLanList.get(0);
        // 获取产品品类
        /*String productType = ProductTypeEnum.WHOLE_STEEL.getValue();
        if (StringUtils.isNotEmpty(monthPlan.getProductTypeCode())) {
            productType = monthPlan.getProductTypeCode();
        }*/
        if (StringUtils.isEmpty(contextDTO.getProductionVersion())) {
            contextDTO.setProductionVersion(monthPlan.getProductionVersion());
        }

        contextDTO.setLogDetail(new StringBuilder());
        // 设置工作日历
        contextDTO.setWorkCalendarMap(mpAdjustStructureInService.getWorkCalendarMap(contextDTO));

        // 设置月计划结构转产表-单结构
        List<MpStructureAllocation> structureAllocationList = mpAdjustStructureInService.selectMpStructureAllocationList(contextDTO);
        List<MpStructureAllocation> oneStructureAllocationList = structureAllocationList.stream()
                .filter(vo -> StringUtils.isEmpty(structureNameParam) || structureNameParam.equals(vo.getStructureName()))
                .collect(Collectors.toList());
        if (!StringUtil.isEmptyWithTrim(contextDTO.getScheduledMachines())) {
            //若有当前调整机台
            for (MpStructureAllocation structureAllocation : oneStructureAllocationList) {
                if (contextDTO.getScheduledMachines().equals(structureAllocation.getCxMachineCode())) {
                    //更新它最新的调整日期
                    structureAllocation.setBeginDay(contextDTO.getAdjustStartDay());
                    structureAllocation.setEndDay(contextDTO.getAdjustEndDay());
                }
            }
        }
        int maxDays = com.zlt.aps.mp.engine.utils.DateUtils.getDaysByYearMonth(contextDTO.getMpYear(), contextDTO.getMpMonth());
        contextDTO.setOneStructureAllocationList(oneStructureAllocationList);
        // 设置总的硫化机台数
        contextDTO.setTotalLhMachines(mpAdjustStructureInService.getLhMachineCount(contextDTO));
        // 设置OEM配置集合
        initOemParam(contextDTO);
        // 设置结构统计
        contextDTO.setStructureStatisticMap(mpAdjustStructureInService.loadMpMonthPlanStatistics(contextDTO));
        // 收集结构名称列表
        Set<String> structureNameSet = oneStructureAllocationList.stream()
                .map(MpStructureAllocation::getStructureName)
                .collect(Collectors.toSet());
        // 收集月计划列表
        monthPLanList = monthPLanList.stream()
                .filter(vo -> structureNameSet.contains(vo.getStructureName()) &&
                        vo.getDayVulcanizationQty() != null)
                .collect(Collectors.toList());

        MpAdjustDailyCapacityLimit adjustDailyCapacityLimitObj = new MpAdjustDailyCapacityLimit();
        MpWeekRollAdjustEngine weekRollAdjustEngine = new MpWeekRollAdjustEngine();
        // 月计划统计结果列表
        List<MpMonthPlanStatistics> monthPlanStatisticsList = new ArrayList<>();
        StringBuilder sbError = new StringBuilder();

        // 初始化每日型腔/活块数量
        contextDTO.setCavity2BlockMap(mpAdjustStructureInService.getCavityAndBlockQtyMap(contextDTO,false));

        for (String structureName : structureNameSet) {
            contextDTO.setStructureName(structureName);
            //每个结构都得先清一下（可能只有一个结构）
            contextDTO.setMouldShellBlockMachinesMap(new HashMap<>());
            contextDTO.setStructureMachinesMap(new HashMap<>());
            contextDTO.setInchMachinesMap(new HashMap<>());

            List<MpStructureAllocation> targetStructureAllocationList = oneStructureAllocationList.stream()
                    .filter(vo -> structureName.equals(vo.getStructureName()))
                    .collect(Collectors.toList());

            List<FactoryMonthPlanFinalAdjustVo> targetMonthPlanList = monthPLanList.stream()
                    .filter(vo -> structureName.equals(vo.getStructureName()))
                    .collect(Collectors.toList());

            contextDTO.setOneStructureAllocationList(targetStructureAllocationList);

            // 设置调整日（依赖 paramMap）
            setAdjustDate(contextDTO);
            // 初始锁定日
            contextDTO.setLockEndDay(getLockEndDay(contextDTO));
            // 初始结构开始日\收尾日
            initStructureStartAndEndDay(contextDTO);
            // 检查排产日是否超出结构起产日-收尾日
            checkStruct2MaterialDate(contextDTO, targetMonthPlanList,maxDays,sbError);

            // 初始化日产信息
            Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap = adjustDailyCapacityLimitObj.getDailyCapacityLimitMap(contextDTO);
            weekRollAdjustEngine.initDayProductionInfo(contextDTO, dailyCapacityLimitVoMap);
            // 设置日产能限制Map
            contextDTO.setDailyCapacityLimitVoMap(ObjectUtils.defaultIfNull(dailyCapacityLimitVoMap, new HashMap<Integer, MpDailyCapacityLimitVo>()));
            // 假设 targetMonthPlanList 已有数据
            checkMouldSatisfyByMainPattern(contextDTO,targetMonthPlanList,sbError);

            // 重算每日产能限制，包括硫化机台数、胎胚种类数、换模次数以及统计硫化机台数
            reCalcAdjustDailyCapacityLimitWithStaticMachines(contextDTO, targetMonthPlanList, adjustDailyCapacityLimitObj,sbError);

            //9.设置模具变化信息
            for (FactoryMonthPlanFinalAdjustVo mpFinalVo : targetMonthPlanList) {
                weekRollAdjustEngine.setMouldChangeInfo(adjustDailyCapacityLimitObj, contextDTO.getParamMap(), contextDTO.getStructureStartDay(), mpFinalVo, contextDTO.getDailyCapacityLimitVoMap());
            }

            // 构建月计划统计结果
            MpMonthPlanStatistics monthPlanStatistics = buildMonthPlanStatistics(contextDTO, targetMonthPlanList, tempFlag);
            if (Objects.nonNull(monthPlanStatistics)) {
                monthPlanStatisticsList.add(monthPlanStatistics);
            }
        }
        if (!StringUtil.isEmptyWithTrim(sbError.toString())) {
            throw new BusinessException(sbError.toString());
        }

        contextDTO.setMonthPlanStatisticsList(monthPlanStatisticsList);
        // 保存月计划统计结果
        saveMonthPlanStatisticsResult(contextDTO, tempFlag);
    }

    /**
     * 按主花纹检查型腔数、活块数
     * @param contextDTO
     * @param targetMonthPlanList
     * @param sbError
     */
    private  void checkMouldSatisfyByMainPattern(MpRollAdjustContextDTO contextDTO,List<FactoryMonthPlanFinalAdjustVo> targetMonthPlanList, StringBuilder sbError) {
        if (PubUtil.isEmpty(targetMonthPlanList)){
            return;
        }
        MpAdjustDailyCapacityLimit adjustDailyCapacityLimitObj = new MpAdjustDailyCapacityLimit();
        MpWeekRollAdjustEngine weekRollAdjustEngine = new MpWeekRollAdjustEngine();
        Map<String, List<FactoryMonthPlanFinalAdjustVo>> mainPatternMap =
                targetMonthPlanList.stream()
                        .filter(vo -> vo != null && vo.getMainPattern() != null)
                        .collect(Collectors.groupingBy(FactoryMonthPlanFinalAdjustVo::getMainPattern));
        // 遍历分组结果
        for (Map.Entry<String, List<FactoryMonthPlanFinalAdjustVo>> entry : mainPatternMap.entrySet()) {
            // 重算每日产能限制，包括硫化机台数、胎胚种类数、换模次数
            Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap = contextDTO.getDailyCapacityLimitVoMap();
            for (int i = contextDTO.getStructureStartDay(); i <= contextDTO.getStructureDeadLine(); i++) {
                if (dailyCapacityLimitVoMap.get(i) == null) {
                    continue;
                }
                adjustDailyCapacityLimitObj.calcLhMachinesWithEmbryoTypes(targetMonthPlanList, i, dailyCapacityLimitVoMap.get(i), contextDTO.getParamMap(), entry.getKey(), null);
                // 检查型腔数
                int cavityQty = weekRollAdjustEngine.getNewCavityQty(contextDTO,entry.getValue().get(0), i);
                if (!weekRollAdjustEngine.checkMouldSatisfy(dailyCapacityLimitVoMap.get(i),cavityQty)){
                    sbError.append(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.confirm.mainPatternLimit"), contextDTO.getStructureName(), entry.getKey(), i, dailyCapacityLimitVoMap.get(i).getPatternUsedLhMachines()*2,cavityQty)).append(BusiConstant.WeekRollAdjust.SPLIT_FRONT_NEW_LINE);
                }
                // 检查活块数
                for (FactoryMonthPlanFinalAdjustVo adjustVo:entry.getValue()){
                    int blockQty = weekRollAdjustEngine.getNewTypeBlockQty(contextDTO,adjustVo, i);
                    int moulds = weekRollAdjustEngine.getMouldByDay(adjustDailyCapacityLimitObj,contextDTO.getParamMap(),i,adjustVo,dailyCapacityLimitVoMap.get(i));
                    if (moulds > blockQty){
                        sbError.append(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.confirm.blockNumLimit"), contextDTO.getStructureName(), adjustVo.getMaterialCode(), i, moulds,blockQty)).append(BusiConstant.WeekRollAdjust.SPLIT_FRONT_NEW_LINE);
                    }
                    //将计算出的模具数量暂存,用于后续模壳检查
                    adjustVo.setFieldValueByFieldName(FactoryConstant.MOULD_QTY_DAY_FIELD+i,moulds);
                }
            }
        }
    }

    /**
     * 设置调整日
     *
     * @param contextDTO 周程滚动上下文
     */
    protected void setAdjustDate(MpRollAdjustContextDTO contextDTO) {
        String weekRollAdjustDate = (String) contextDTO.getParamMap().get(MonthPlanEnums.WEEK_ROLL_ADJUST_DATE.getCode());
        Date adjustDate = StringUtil.isEmptyWithTrim(weekRollAdjustDate) ? DateUtils.getNowDate() : DateUtils.parseDate(weekRollAdjustDate);
        if (contextDTO.getMpMonth() != DateUtils.getMonth(adjustDate)) {
            //若调整月不等于当前月，则将调整日设置1
            contextDTO.setAdjustDay(FactoryConstant.MONTH_START_DAY);
        } else {
            contextDTO.setAdjustDay(DateUtils.getDay(adjustDate));
        }
    }

    /**
     * 检查排产日是否超出结构起产日-收尾日
     *
     * @param contextDTO
     * @param monthPlanList
     */
    private void checkStruct2MaterialDate(MpRollAdjustContextDTO contextDTO, List<FactoryMonthPlanFinalAdjustVo> monthPlanList, int maxDays, StringBuilder beginDaySb) {
        if (PubUtil.isEmpty(monthPlanList)) {
            return;
        }
        //检查：物料编码：[%s]，排产日超出结构起产日[%s]-收尾日[%s]！
        for (FactoryMonthPlanFinalAdjustVo finalAdjustVo : monthPlanList) {
            if (finalAdjustVo.getBeginDay() == null || finalAdjustVo.getEndDay() == null) {
                continue;
            }
            if (finalAdjustVo.getBeginDay() == 0 || finalAdjustVo.getEndDay() == 0) {
                continue;
            }
            if (finalAdjustVo.getEndDay() > maxDays){
                beginDaySb.append(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.confirm.checkMaterialMaxDay"),contextDTO.getStructureName(),
                        finalAdjustVo.getMaterialCode(), maxDays)).append(BusiConstant.WeekRollAdjust.SPLIT_FRONT_NEW_LINE);
            }
            if (finalAdjustVo.getBeginDay() < contextDTO.getStructureStartDay() || finalAdjustVo.getEndDay() > contextDTO.getStructureDeadLine()) {
                beginDaySb.append(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.confirm.checkStructMaterialDay"),contextDTO.getStructureName(),
                        finalAdjustVo.getMaterialCode(), contextDTO.getStructureStartDay(), contextDTO.getStructureDeadLine())).append(BusiConstant.WeekRollAdjust.SPLIT_FRONT_NEW_LINE);
            }
        }
    }

    /**
     * 初始OEM相关参数
     *
     * @param contextDTO
     */
    public void initOemParam(MpRollAdjustContextDTO contextDTO) {
        String oemBrandConfig = (String) contextDTO.getParamMap().get(MonthPlanEnums.OEM_BRAND_CONFIG.getCode());
        Set<String> oemBrandConfigSet = Collections.emptySet();
        if (!StringUtil.isEmptyWithTrim(oemBrandConfig)) {
            oemBrandConfigSet = Stream.of(oemBrandConfig.split(StringConstant.COMMA)).collect(Collectors.toSet());
        }
        contextDTO.setOemBrandConfigSet(oemBrandConfigSet);
        if (contextDTO.getParamMap().get(MonthPlanEnums.OEM_BRAND_CAPACITY.getCode()) == null) {
            contextDTO.setTotalOemQty(0);
        } else {
            contextDTO.setTotalOemQty((Integer) contextDTO.getParamMap().get(MonthPlanEnums.OEM_BRAND_CAPACITY.getCode()));
        }
    }


    /**
     * 汇总调整明细
     *
     * @param contextDTO
     */
    protected void sumAdjustDetail(MpRollAdjustContextDTO contextDTO) {
        List<MpAdjustDetailVo> adjustDetailList = sumByStructureAndMaterial(contextDTO.getAdjustDetailList(), Boolean.TRUE);
        contextDTO.setAdjustDetailList(adjustDetailList);
    }


    /**
     * 更新结构转产
     *
     * @param contextDTO
     */
    protected void updateStructureAllocationList(MpRollAdjustContextDTO contextDTO) {
        if (StringUtil.isEmptyWithTrim(contextDTO.getScheduledMachines())) {
            //scheduledMachines,存储的是当前调整机台，若没有当前调整机台，则返回
            return;
        }
        if (PubUtil.isEmpty(contextDTO.getFactoryMonthPlanProdFinalList())) {
            return;
        }
        if (StringUtil.isEmptyWithTrim(contextDTO.getProductionVersion())) {
            FactoryMonthPlanFinalAdjustVo firstFinalVo = contextDTO.getFactoryMonthPlanProdFinalList().get(0);
            contextDTO.setProductionVersion(firstFinalVo.getProductionVersion());
            contextDTO.setMonthPlanVersion(firstFinalVo.getMonthPlanVersion());
        }
        MpStructureAllocation targetAllocation = buildTargetStructureAllocation(contextDTO);
        if (targetAllocation == null) {
            return;
        }
        List<MpStructureAllocation> structureAllocationList = queryStructureAllocationList(contextDTO);
        MpStructureAllocation existAllocation = WeekRollAdjustMachineCrossChecker.findSameMachineStructure(structureAllocationList, targetAllocation);
        if (existAllocation != null) {
            targetAllocation.setId(existAllocation.getId());
        }
        // 判断同机台不同结构是否存在连续性
        if (WeekRollAdjustMachineCrossChecker.hasTargetDifferentStructureContinue(targetAllocation, structureAllocationList)) {
            //若是连续性的，清空缓存
            mpStructureAllocationService.setAdjustsCxMachineFromRedis(null);
        }
        if (targetAllocation.getId() == null) {
            //新增结构
            targetAllocation.setIsDelete(YesOrNoEnum.NO.getValue());
            targetAllocation.setDataSource(DataSourceEnum.HAND.getCode());
            //mpStructureAllocationEntityMapper.insert(targetAllocation);
            mpStructureAllocationService.save(targetAllocation);
            //重置结构转产表数据
            contextDTO.setStructureAllocationList(mpAdjustStructureInService.selectMpStructureAllocationList(contextDTO));
            return;
        }
        mpStructureAllocationEntityMapper.updateById(targetAllocation);
    }


    /**
     * 更新试制量制计划
     *
     * @param contextDTO
     */
    protected void updateTrialPlanList(MpRollAdjustContextDTO contextDTO) {
        // 调整结果列表
        List<MpAdjustResult> adjustResultList = contextDTO.getAdjustResultList();
        // 调整明细列表
        List<MpAdjustDetailVo> adjustDetailList = contextDTO.getAdjustDetailList();
        if (PubUtil.isEmpty(adjustResultList) || PubUtil.isEmpty(adjustDetailList)) {
            log.warn("更新试制量制计划：调整结果列表或调整明细列表为空，直接返回");
            return;
        }
        // 调整明细按照物料编码分组并提取试制量试ID列表
        Map<String, List<String>> adjustDetailMap = adjustDetailList.stream()
                .filter(obj -> StringUtils.isNotEmpty(obj.getMaterialCode())
                        && StringUtils.isNotEmpty(obj.getTrialPlanId()))
                .collect(Collectors.groupingBy(
                        MpAdjustDetailVo::getMaterialCode,
                        Collectors.mapping(
                                MpAdjustDetailVo::getTrialPlanId,
                                Collectors.toList()
                        )
                ));
        if (PubUtil.isEmpty(adjustDetailMap)) {
            log.warn("更新试制量制计划：分组后调整明细为空，直接返回");
            return;
        }
        // 试制量试计划列表
        List<MpTrialPlan> trialPlanList = new ArrayList<>();
        // 遍历调整结果设置试制量制系统排程日期
        for (MpAdjustResult adjustResult : adjustResultList) {
            String materialCode = adjustResult.getMaterialCode();
            if (StringUtils.isEmpty(materialCode)) {
                continue;
            }
            if (!adjustDetailMap.containsKey(materialCode)) {
                continue;
            }
            List<String> trialPlanIdList = adjustDetailMap.get(materialCode);
            if (PubUtil.isEmpty(trialPlanIdList)) {
                continue;
            }
            // 获取最早有值的日期
            Integer day = getFirstHasValueDay(adjustResult);
            // 如果试产试制规格没有排量，day会为空，该sku不需要更新
            if (day == null || day <= 0) {
                continue;
            }
            // 获取当前日期
            Date productionDate = getCurrentDate(contextDTO.getMpYear(), contextDTO.getMpMonth(), day);
            for (String trialPlanId : trialPlanIdList) {
                MpTrialPlan trialPlan = new MpTrialPlan();
                trialPlan.setId(Long.valueOf(trialPlanId));
                trialPlan.setProductionDate(productionDate);
                trialPlanList.add(trialPlan);
            }
        }
        // 更新试制量制计划
        try {
            baseDao.updateBatch(trialPlanList);
            log.info("更新试制量制计划成功，共更新:{}条记录", trialPlanList.size());
        } catch (Exception e) {
            log.error("更新试制量制计划批量操作异常", e);
            throw new RuntimeException("更新试制量制计划失败", e);
        }
    }

    /**
     * 获取当前日期
     *
     * @param year
     * @param month
     * @param day
     * @return
     */
    protected Date getCurrentDate(Integer year, Integer month, Integer day) {
        return DateUtil.parseDate(String.format("%d-%02d-%02d", year, month, day));
    }

    /**
     * 获取最早有值的日期
     *
     * @param adjustResult
     * @return
     */
    protected Integer getFirstHasValueDay(MpAdjustResult adjustResult) {
        if (adjustResult == null) {
            return null;
        }
        // 按1~31顺序遍历，保证找到最早有值的字段
        for (int day = 1; day <= BusiConstant.WeekRollAdjust.MAX_DAY_OF_MONTH; day++) {
            // 拼接字段名：day1、day2...day31
            String dayFieldName = BusiConstant.WeekRollAdjust.FIELD_PREFIX_DAY + day;
            Integer fieldValue = Convert.toInt(adjustResult.getFieldValueByFieldName(dayFieldName), 0);
            if (fieldValue != 0) {
                return day;
            }
        }
        // 所有day1~day31均无值，返回null
        return null;
    }

    /**
     * 新增月度生产计划
     *
     * @param contextDTO
     */
    protected void insertMonthPlanList(MpRollAdjustContextDTO contextDTO) {
        List<MpAdjustDetailVo> adjustDetailList = contextDTO.getAdjustDetailList();
        List<MpAdjustResult> adjustResultList = contextDTO.getAdjustResultList();
        if (PubUtil.isEmpty(adjustResultList)) {
            log.warn("新增月度生产计划：调整明细列表或者调整结果列表为空，直接返回");
            return;
        }

        Map<String, List<FactoryMonthPlanFinalAdjustVo>> keyToListMap = contextDTO.getFactoryMonthPlanProdFinalList().stream()
                .collect(Collectors.groupingBy(vo -> {
                    String structureName = StringUtils.defaultString(vo.getStructureName());
                    String materialCode = StringUtils.defaultString(vo.getMaterialCode());
                    String constructionStage = StringUtils.defaultString(vo.getConstructionStage());
                    return String.join(BusiConstant.WeekRollAdjust.SPLIT_GROUP_KEY, structureName, materialCode, constructionStage);
                }));

        // 需要新增月度生产计划列表
        List<FactoryMonthPlanProductionFinalResult> insertMonthPlanList = new ArrayList<>();
        // 批次号前缀
        String prefixKey = IncrementConstant.MONTH_FINAL + com.ruoyi.common.core.utils.DateUtils.dateTimeNow("yyMMdd");
        // 批次号
        String batchNo = String.format("%02d", incrementService.getIncrementNumber(prefixKey));
        // 初始化SKU与施工（示方书）关系
        initSkuConstructionRef(contextDTO);
        // 初始化SKU排产分类
        //initSkuProductionType(contextDTO);
        // 汇总调整明细
        List<MpAdjustDetailVo> summaryAdjustDetailList = sumByStructureAndMaterial(adjustDetailList, Boolean.TRUE);
        Map<String, MpAdjustDetailVo> adjustDetailVoMap = summaryAdjustDetailList.stream()
                .collect(Collectors.toMap(
                        vo -> (vo.getStructureName() == null ? "" : vo.getStructureName())
                                + BusiConstant.WeekRollAdjust.SPLIT_GROUP_KEY
                                + (vo.getMaterialCode() == null ? "" : vo.getMaterialCode())
                                + BusiConstant.WeekRollAdjust.SPLIT_GROUP_KEY
                                + (vo.getConstructionStage() == null ? "" : vo.getConstructionStage()),
                        Function.identity(),
                        (existing, replacement) -> replacement  // 遇到重复 Key 时保留后出现的元素
                ));
        // 汇总调整结果
        Map<String, MpAdjustResult> summaryAdjustResultMap = summaryAdjustResult(adjustResultList, adjustDetailList);
        // 遍历调整明细，获取新增的SKU并新增到月度生产计划
        for (Map.Entry<String, MpAdjustResult> entry : summaryAdjustResultMap.entrySet()) {
            MpAdjustResult adjustResult = entry.getValue();
            // 构建分组key
            String groupKey = buildGroupKey(adjustResult);
            if (PubUtil.isNotEmpty(keyToListMap.get(groupKey))) {
                continue;
            }

            MpAdjustDetailVo adjustDetailVo = adjustDetailVoMap.get(groupKey) == null ? new MpAdjustDetailVo() : adjustDetailVoMap.get(groupKey);

            FactoryMonthPlanProductionFinalResult monthPlan = new FactoryMonthPlanProductionFinalResult();
            BeanUtils.copyProperties(adjustResult, monthPlan);
            if (!StringUtil.isEmptyWithTrim(adjustDetailVo.getMaterialCode())) {
                BeanUtils.copyProperties(adjustDetailVo, monthPlan);
            }

            if (StringUtil.isEmptyWithTrim(monthPlan.getProductStatus())) {
                //补充产品状态
                monthPlan.setProductStatus(transferStageToProductStatus(monthPlan.getConstructionStage()));
            }
            if (StringUtil.isEmptyWithTrim(monthPlan.getProductTypeCode())) {
                //补充产品品类
                monthPlan.setProductTypeCode(contextDTO.getProductType());
            }
            if (StringUtil.isEmptyWithTrim(monthPlan.getProductionType())) {
                //补充排产分类
                monthPlan.setProductionType(contextDTO.getMdmSkuProductionTypeMap().get(monthPlan.getMaterialCode()));
            }
            monthPlan.setTotalQty(adjustResult.getTotalQty());
            if (adjustDetailVo.getYear() != null && adjustDetailVo.getMonth() != null) {
                monthPlan.setYearMonth(Integer.valueOf(String.format("%d%02d", adjustDetailVo.getYear(), adjustDetailVo.getMonth())));
            }
            if (adjustResult.getYear() != null && adjustResult.getMonth() != null) {
                monthPlan.setYearMonth(Integer.valueOf(String.format("%d%02d", adjustResult.getYear(), adjustResult.getMonth())));
            }
            monthPlan.setId(null);
            monthPlan.setBaseVale(null);
            String productionNo = incrementService.getBillNoSequenceByExpire(prefixKey + batchNo, 5, 60 * 24 * 7);
            monthPlan.setProductionNo(productionNo);
            // 设置SKU与示方书关联字段：是否零度材料、制造示方书号、文字示方书号、硫化示方书号、主物料(胎胚号)
            setSkuConstructionRefField(contextDTO, monthPlan);
            // 净需求
            monthPlan.setProdReqPlan(adjustDetailVo.getCurrentNetQty());
            // 计算实际生产需求含损耗
            Integer factProdReqQty = calculateFactProdReqQty(adjustDetailVo.getCurrentNetQty());
            monthPlan.setFactProdReqQty(factProdReqQty);
            // 差异量(未排产数量) = 实际生产需求含损耗 - 生产实际排产量
            Integer differenceQty = factProdReqQty - Convert.toInt(monthPlan.getTotalQty(), 0);
            // 试制量试关联字段设置
            if (StringUtils.isNotEmpty(adjustDetailVo.getTrialPlanId())) {
                // 实际生产需求(含损耗)
                monthPlan.setFactProdReqQty(adjustDetailVo.getCurrentNetQty());
                // 试制量试计划需求数量
                monthPlan.setTrialQty(adjustDetailVo.getCurrentNetQty());
                // 差异量(未排产数量) = 实际生产需求(含损耗) - 生产实际排产量
                differenceQty = Convert.toInt(monthPlan.getFactProdReqQty(), 0) - Convert.toInt(monthPlan.getTotalQty(), 0);
            }
            // 差异量(未排产数量)
            monthPlan.setDifferenceQty(differenceQty);
            // 模具变化信息
            monthPlan.setMouldChangeInfo(adjustResult.getMouldChangeInfo());
            // 设置周调整量
            int week = getWeekNumber(contextDTO.getAdjustDay());
            setWeekAdjustQty(monthPlan, week);

            setMaterialInfoField(contextDTO,monthPlan);
            // 设置最新需求计划版本
            monthPlan.setLastMonthPlanVersion(adjustResult.getLastMonthPlanVersion());
            // 设置月度计划开始日期、结束日期
            setBeginDayAndEndDay(monthPlan);

            insertMonthPlanList.add(monthPlan);
        }
        if (PubUtil.isNotEmpty(insertMonthPlanList)) {
            // 将日期字段中值为0的字段设为null
            for (FactoryMonthPlanProductionFinalResult monthPlan : insertMonthPlanList) {
                handleZeroToNull(monthPlan);
            }
            // 添加到月计划上下文
            contextDTO.getFactoryMonthPlanProdFinalList().addAll(BeanUtil.copyToList(insertMonthPlanList, FactoryMonthPlanFinalAdjustVo.class));
        }

        // 构建搭配排产新增月度计划
        List<FactoryMonthPlanProductionFinalResult> matchingProductionMonthPlanList = buildMatchingProductionMonthPlan(adjustDetailList, adjustResultList, contextDTO);
        if (PubUtil.isNotEmpty(matchingProductionMonthPlanList)) {
            // 将日期字段中值为0的字段设为null
            for (FactoryMonthPlanProductionFinalResult monthPlan : matchingProductionMonthPlanList) {
                handleZeroToNull(monthPlan);
            }
            // 添加到月计划上下文
            contextDTO.getFactoryMonthPlanProdFinalList().addAll(BeanUtil.copyToList(matchingProductionMonthPlanList, FactoryMonthPlanFinalAdjustVo.class));
        }
    }

    /**
     * 施工阶段转换产品状态
     *
     * @param constructionStage
     * @return
     */
    protected String transferStageToProductStatus(String constructionStage) {
        String productStatus;
        if (ConstructionStageEnum.FORMAL_PRODUCTION.getStage().equals(constructionStage)) {
            productStatus = ConstructionStageEnum.FORMAL_FLAG;
        } else if (ConstructionStageEnum.TRIAL_PRODUCTION.getStage().equals(constructionStage)) {
            productStatus = ConstructionStageEnum.TRIAL_FLAG;
        } else if (ConstructionStageEnum.MEASUREMENT.getStage().equals(constructionStage)) {
            productStatus = ConstructionStageEnum.MEASUREMENT_FLAG;
        } else {
            productStatus = ConstructionStageEnum.FORMAL_FLAG;
        }
        return productStatus;
    }

    /**
     * 构建调整结果总计划量为0的月度计划
     *
     * @param adjustResultList
     * @return
     */
    private List<FactoryMonthPlanProductionFinalResult> buildAdjustResultMonthPlan(List<MpAdjustResult> adjustResultList) {
        if (PubUtil.isEmpty(adjustResultList)) {
            log.warn("调整结果列表为空，直接返回");
            return Collections.emptyList();
        }
        List<FactoryMonthPlanProductionFinalResult> monthPLanList = new ArrayList<>();
        for (MpAdjustResult result : adjustResultList) {
            if (result.getTotalPlanQty() != null && !result.getTotalPlanQty().equals(Integer.valueOf(0))) {
                continue;
            }
            FactoryMonthPlanProductionFinalResult monthPlan = new FactoryMonthPlanProductionFinalResult();
            monthPlan.setMaterialCode(result.getMaterialCode());
            monthPlan.setConstructionStage(result.getConstructionStage());
            monthPlan.setStructureName(result.getStructureName());
            monthPLanList.add(monthPlan);
        }
        return monthPLanList;
    }

    /**
     * 构建搭配排产新增月度计划
     *
     * @param adjustDetailList
     * @param adjustResultList
     * @param contextDTO
     * @return
     */
    private List<FactoryMonthPlanProductionFinalResult> buildMatchingProductionMonthPlan(List<MpAdjustDetailVo> adjustDetailList, List<MpAdjustResult> adjustResultList,
                                                                                         MpRollAdjustContextDTO contextDTO) {
        if (PubUtil.isEmpty(adjustResultList)) {
            log.warn("调整结果列表为空，直接返回");
            return Collections.emptyList();
        }
        List<FactoryMonthPlanFinalAdjustVo> monthPlanProdFinalList = contextDTO.getFactoryMonthPlanProdFinalList();
        // 筛选调整结果列表（搭配排产新增，不在调整明细及月度生产计划中）
        List<MpAdjustResult> filterAdjustResultList = filterAdjustResultList(adjustDetailList, adjustResultList, monthPlanProdFinalList);
        if (PubUtil.isEmpty(filterAdjustResultList)) {
            log.warn("过滤后调整结果列表为空，直接返回");
            return Collections.emptyList();
        }
        // 初始化试制量试计划
        initTrialPlan(contextDTO);
        // 初始化物料信息    移到前面
        //initMaterialInfo(contextDTO);
        // 初始化SKU日硫化产能
        initSkuLhCapacity(contextDTO);
        // 月度计划结果列表
        List<FactoryMonthPlanProductionFinalResult> monthPlanList = new ArrayList<>();
        // 批次号前缀
        String prefixKey = IncrementConstant.MONTH_FINAL + com.ruoyi.common.core.utils.DateUtils.dateTimeNow("yyMMdd");
        // 批次号
        String batchNo = String.format("%02d", incrementService.getIncrementNumber(prefixKey));
        // 最新需求计划版本
        String lastMonthPlanVersion = null;
        // 需求计划版本
        String monthPlanVersion = null;
        if (PubUtil.isNotEmpty(adjustDetailList)) {
            lastMonthPlanVersion = adjustDetailList.get(0).getLastMonthPlanVersion();
            monthPlanVersion = adjustDetailList.get(0).getMonthPlanVersion();
        }
        if (StringUtil.isEmptyWithTrim(lastMonthPlanVersion) && PubUtil.isNotEmpty(adjustResultList)) {
            lastMonthPlanVersion = adjustResultList.get(0).getLastMonthPlanVersion();
        }
        contextDTO.setAdjustMonthPlanVersion(lastMonthPlanVersion);
        // 获取需求计划Map（按照物料编码分组）
        Map<String, DpDemandPlan> demandPlanMap = getDemandPlanMap(contextDTO);
        // 循环构建月度计划
        for (MpAdjustResult adjustResult : filterAdjustResultList) {
            FactoryMonthPlanProductionFinalResult monthPlan = new FactoryMonthPlanProductionFinalResult();
            BeanUtils.copyProperties(adjustResult, monthPlan);
            monthPlan.setTotalQty(adjustResult.getTotalPlanQty());
            if (adjustResult.getYear() != null && adjustResult.getMonth() != null) {
                monthPlan.setYearMonth(Integer.valueOf(String.format("%d%02d", adjustResult.getYear(), adjustResult.getMonth())));
            }
            monthPlan.setId(null);
            monthPlan.setBaseVale(null);
            String productionNo = incrementService.getBillNoSequenceByExpire(prefixKey + batchNo, 5, 60 * 24 * 7);
            monthPlan.setProductionNo(productionNo);
            // 根据物料编码获取需求计划
            DpDemandPlan demandPlan = demandPlanMap.getOrDefault(adjustResult.getMaterialCode(), new DpDemandPlan());
            // 排产分类
            monthPlan.setProductionType(demandPlan.getProductionType());
            // 施工阶段
            monthPlan.setConstructionStage(ConstructionStageEnum.FORMAL_PRODUCTION.getStage());
            // 产品状态
            monthPlan.setProductStatus(ConstructionStageEnum.FORMAL_FLAG);
            // 设置物料信息关联字段
            setMaterialInfoField(contextDTO, monthPlan);
            // 设置试制量试关联字段
            setTrialPlanField(contextDTO, monthPlan);
            // 设置SKU与示方书关联字段
            setSkuConstructionRefField(contextDTO, monthPlan);
            // 设置SKU日硫化产能关联字段
            setLhCapacityField(contextDTO, monthPlan);
            // 设置活块、型腔数量
            setMoldCavityInsertField(contextDTO, monthPlan);
            // 最新需求计划版本
            monthPlan.setLastMonthPlanVersion(lastMonthPlanVersion);
            // 将日期字段中值为0的字段设为null
            handleZeroToNull(monthPlan);
            monthPlanList.add(monthPlan);
        }
        return monthPlanList;
    }


    /**
     * 查询需求计划列表
     *
     * @param queryVo
     * @return
     */
    protected List<DpDemandPlan> queryDemandPlanList(DpDemandPlan queryVo) {
        LambdaQueryWrapper<DpDemandPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DpDemandPlan::getFactoryCode, queryVo.getFactoryCode());
        wrapper.eq(DpDemandPlan::getYear, queryVo.getYear());
        wrapper.eq(DpDemandPlan::getMonth, queryVo.getMonth());
        wrapper.eq(DpDemandPlan::getMonthPlanVersion, queryVo.getMonthPlanVersion());
        wrapper.eq(DpDemandPlan::getIsDelete, YesOrNoEnum.NO.getValue());
        return demandPlanEntityMapper.selectList(wrapper);
    }

    /**
     * 获取需求计划Map（按照物料编码分组）
     *
     * @param contextDTO
     * @return
     */
    protected Map<String, DpDemandPlan> getDemandPlanMap(MpRollAdjustContextDTO contextDTO) {
        // 构建需求计划查询条件
        DpDemandPlan queryVo = new DpDemandPlan();
        queryVo.setFactoryCode(contextDTO.getFactoryCode());
        queryVo.setYear(contextDTO.getMpYear());
        queryVo.setMonth(contextDTO.getMpMonth());
        queryVo.setMonthPlanVersion(contextDTO.getAdjustMonthPlanVersion());
        // 查询需求计划列表
        List<DpDemandPlan> demandPlanList = queryDemandPlanList(queryVo);
        if (PubUtil.isEmpty(demandPlanList)) {
            return Collections.emptyMap();
        }
        // 按照物料编码分组
        return demandPlanList.stream()
                .filter(demandPlan -> StringUtils.isNotEmpty(demandPlan.getMaterialCode()))
                .collect(Collectors.toMap(
                        DpDemandPlan::getMaterialCode,
                        demandPlan -> demandPlan,
                        (existingVal, newVal) -> newVal
                ));
    }


    /**
     * 设置SKU日硫化产能关联字段
     *
     * @param contextDTO
     * @param monthPlan
     */
    private void setLhCapacityField(MpRollAdjustContextDTO contextDTO, FactoryMonthPlanProductionFinalResult monthPlan) {
        // SKU日硫化产能Map
        Map<String, MdmSkuLhCapacity> skuLhCapacityMap = contextDTO.getMdmSkuLhCapacityMap();
        if (PubUtil.isEmpty(skuLhCapacityMap)) {
            return;
        }
        // 物料编码
        String materialCode = monthPlan.getMaterialCode();
        // 通过物料编码获取SKU日硫化产能
        MdmSkuLhCapacity skuLhCapacity = skuLhCapacityMap.getOrDefault(materialCode, new MdmSkuLhCapacity());
        // 日硫化量
        monthPlan.setDayVulcanizationQty(Convert.toInt(skuLhCapacity.getStandardCapacity(), 0) / 2);
        // 单条硫化时间
        monthPlan.setCuringTime(skuLhCapacity.getVulcanizationTime());
    }


    /**
     * 设置物料信息关联字段
     *
     * @param contextDTO
     * @param monthPlan
     */
    private void setMaterialInfoField(MpRollAdjustContextDTO contextDTO, FactoryMonthPlanProductionFinalResult monthPlan) {
        // 物料信息Map
        Map<String, MdmMaterialInfo> materialInfoMap = contextDTO.getMdmMaterialInfoMap();
        if (PubUtil.isEmpty(materialInfoMap)) {
            return;
        }
        // 物料编码
        String materialCode = monthPlan.getMaterialCode();
        // 获取物料信息
        MdmMaterialInfo materialInfo = materialInfoMap.getOrDefault(materialCode, new MdmMaterialInfo());
        // MES物料编码
        monthPlan.setMesMaterialCode(materialInfo.getMesMaterialCode());
        // 物料描述
        monthPlan.setMaterialDesc(materialInfo.getMaterialDesc());
        // 产品品类
        monthPlan.setProductTypeCode(materialInfo.getProductTypeCode());
        // 品牌
        monthPlan.setBrand(materialInfo.getBrand());
        // 规格
        monthPlan.setSpecifications(materialInfo.getSpecifications());
        // 主花纹
        monthPlan.setMainPattern(materialInfo.getMainPattern());
        // 花纹
        monthPlan.setPattern(materialInfo.getPattern());
        // 产品分类
        monthPlan.setProductCategory(materialInfo.getProductCategory());
        // 英寸
        monthPlan.setProSize(materialInfo.getProSize());
        // 胎胚号
        monthPlan.setEmbryoCode(materialInfo.getEmbryoCode());
        // 胎胚描述
        monthPlan.setMainMaterialDesc(materialInfo.getEmbryoDesc());
    }

    /**
     * 设置试制量试关联字段
     *
     * @param contextDTO
     * @param monthPlan
     */
    private void setTrialPlanField(MpRollAdjustContextDTO contextDTO, FactoryMonthPlanProductionFinalResult monthPlan) {
        // 试制量试计划列表
        List<MpTrialPlan> trialPlanList = contextDTO.getMpTrialPlanList();
        if (PubUtil.isEmpty(trialPlanList)) {
            return;
        }
        // 物料编码
        String materialCode = monthPlan.getMaterialCode();
        // 获取试制量试
        MpTrialPlan trialPlan = trialPlanList.stream()
                .filter(vo -> vo.getMaterialCode().equals(materialCode))
                .findFirst()
                .orElse(null);
        if (trialPlan != null) {
            // 施工阶段
            monthPlan.setConstructionStage(trialPlan.getTrialStatus());
            // 产品状态
            String productStatus = null;
            if (ConstructionStageEnum.MEASUREMENT.getStage().equals(trialPlan.getTrialStatus())) {
                productStatus = ConstructionStageEnum.MEASUREMENT_FLAG;
            }
            if (ConstructionStageEnum.TRIAL_PRODUCTION.getStage().equals(trialPlan.getTrialStatus())) {
                productStatus = ConstructionStageEnum.TRIAL_FLAG;
            }
            monthPlan.setProductStatus(productStatus);
        }
    }


    /**
     * 筛选调整结果列表（不在调整明细及月度生产计划中）
     *
     * @param adjustDetailList
     * @param adjustResultList
     * @param monthPlanProdFinalList
     * @return
     */
    private List<MpAdjustResult> filterAdjustResultList(List<MpAdjustDetailVo> adjustDetailList, List<MpAdjustResult> adjustResultList,
                                                        List<FactoryMonthPlanFinalAdjustVo> monthPlanProdFinalList) {

        if (PubUtil.isEmpty(adjustResultList)) {
            return Collections.emptyList();
        }
        // 排除列表
        Set<String> excludeKeySet = new HashSet<>();
        if (PubUtil.isNotEmpty(adjustDetailList)) {
            for (MpAdjustDetailVo vo : adjustDetailList) {
                String key = generateKey(vo.getMaterialCode(), vo.getStructureName());
                excludeKeySet.add(key);
            }
        }
        if (PubUtil.isNotEmpty(monthPlanProdFinalList)) {
            for (FactoryMonthPlanFinalAdjustVo vo : monthPlanProdFinalList) {
                String key = generateKey(vo.getMaterialCode(), vo.getStructureName());
                excludeKeySet.add(key);
            }
        }
        List<MpAdjustResult> resultList = new ArrayList<>();
        for (MpAdjustResult result : adjustResultList) {
            if (StringUtils.isEmpty(result.getMaterialCode()) || StringUtils.isEmpty(result.getStructureName())) {
                continue;
            }
            String currentKey = generateKey(result.getMaterialCode(), result.getStructureName());
            // 不在排除集合中，加入结果集
            if (!excludeKeySet.contains(currentKey)) {
                resultList.add(result);
            }
        }
        return resultList;
    }

    /**
     * 生成唯一标识：拼接物料编码和结构名称，空值替换为空字符串
     *
     * @param materialCode  物料编码
     * @param structureName 结构名称
     * @return 拼接后的唯一标识
     */
    private String generateKey(String materialCode, String structureName) {
        String mc = materialCode == null ? "" : materialCode.trim();
        String sn = structureName == null ? "" : structureName.trim();
        return mc + ApsConstant.SPLIT_CHAR + sn;
    }

    /**
     * 删除月度生产计划
     *
     * @param contextDTO
     * @param factoryMonthPlanProdFinalList
     */
    private void deleteMonthPlanList(MpRollAdjustContextDTO contextDTO,
                                     List<FactoryMonthPlanProductionFinalResult> factoryMonthPlanProdFinalList) {
        if (PubUtil.isEmpty(factoryMonthPlanProdFinalList)) {
            return;
        }
        // 收集物料编码Set
        Set<String> materialCodeSet = factoryMonthPlanProdFinalList.stream()
                .map(FactoryMonthPlanProductionFinalResult::getMaterialCode)
                .collect(Collectors.toSet());
        // 构建查询月度生产计划条件
        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, contextDTO.getFactoryCode())
                .eq(FactoryMonthPlanProductionFinalResult::getYear, contextDTO.getMpYear())
                .eq(FactoryMonthPlanProductionFinalResult::getMonth, contextDTO.getMpMonth())
                .eq(FactoryMonthPlanProductionFinalResult::getProductionVersion, contextDTO.getProductionVersion())
                .in(FactoryMonthPlanProductionFinalResult::getMaterialCode, materialCodeSet);
        // 查询月度生产计划
        List<FactoryMonthPlanProductionFinalResult> monthPlanList = factoryMonthPlanProdFinalMapper.selectList(wrapper);
        if (PubUtil.isEmpty(monthPlanList)) {
            return;
        }
        // 收集结构名称Set
        Set<String> structureNameSet = factoryMonthPlanProdFinalList.stream()
                .map(FactoryMonthPlanProductionFinalResult::getStructureName)
                .collect(Collectors.toSet());
        // 通过结构名称过滤
        List<FactoryMonthPlanProductionFinalResult> monthPlanResultList = monthPlanList.stream()
                .filter(vo -> structureNameSet.contains(vo.getStructureName()))
                .collect(Collectors.toList());
        if (PubUtil.isEmpty(monthPlanResultList)) {
            return;
        }
        // 收集月度生产计划id Set
        List<Long> idList = monthPlanResultList.stream()
                .map(FactoryMonthPlanProductionFinalResult::getId)
                .collect(Collectors.toList());
        // 删除月度生产计划
        baseDao.deleteByIds(FactoryMonthPlanProductionFinalResult.class, idList);
        log.info("删除月度生产计划成功，共删除:{}条记录", idList.size());

    }


    /**
     * 设置月度计划开始日期、结束日期
     *
     * @param monthPlan
     * @return
     */
    private void setBeginDayAndEndDay(FactoryMonthPlanProductionFinalResult monthPlan) {
        //1、更新开始和结束日期
        String dayField;
        int realBeginDay = FactoryConstant.MONTH_MAX_DAY + 1;
        int realEndDay = 0;
        int totalQty = 0;
        for (int i = FactoryConstant.MONTH_START_DAY; i <= FactoryConstant.MONTH_MAX_DAY; i++) {
            dayField = FactoryConstant.DAY_FIELD + i;
            if (monthPlan.getFieldValueByFieldName(dayField) != null &&
                    (Integer) monthPlan.getFieldValueByFieldName(dayField) != 0) {
                if (realBeginDay > i) {
                    realBeginDay = i;
                }
                if (realEndDay < i) {
                    realEndDay = i;
                }

                totalQty += (Integer) monthPlan.getFieldValueByFieldName(dayField);
            }
        }
        monthPlan.setBeginDay(realBeginDay == FactoryConstant.MONTH_MAX_DAY + 1 ? 0 : realBeginDay);
        monthPlan.setEndDay(realEndDay);
        monthPlan.setTotalQty(totalQty);
    }


    /**
     * 汇总调整结果
     *
     * @param adjustResultList 调整结果列表
     * @param adjustDetailList 调整明细列表
     * @return Map<String, MpAdjustResult>
     */
    public Map<String, MpAdjustResult> summaryAdjustResult(List<MpAdjustResult> adjustResultList, List<MpAdjustDetailVo> adjustDetailList) {
        if (PubUtil.isEmpty(adjustResultList)) {
            return Collections.emptyMap();
        }
        // 先按物料编码分组，再按主键ID映射
       /* Map<String, Map<Long, MpAdjustDetailVo>> detailMap = adjustDetailList.stream()
                .collect(Collectors.groupingBy(
                        MpAdjustDetailVo::getMaterialCode,
                        Collectors.toMap(
                                MpAdjustDetailVo::getId,
                                vo -> vo,
                                (v1, v2) -> v1
                        )
                ));*/

        // 遍历调整结果，匹配明细并分组汇总
        Map<String, MpAdjustResult> summaryMap = new HashMap<>();
        for (MpAdjustResult result : adjustResultList) {
            if (StringUtils.isEmpty(result.getMaterialCode())) {
                continue;
            }
            // 匹配对应的调整明细VO
            //MpAdjustDetailVo detailVo = matchAdjustDetail(result, detailMap);
            // 构建分组key
            String groupKey = buildGroupKey(result);
            // 获取或初始化汇总对象
            MpAdjustResult summaryResult = summaryMap.getOrDefault(groupKey, new MpAdjustResult());
            if (summaryResult.getMaterialCode() == null) {
                BeanUtils.copyProperties(result, summaryResult);
                //summaryResult.setMaterialCode(detailVo.getMaterialCode());
                summaryResult.setAdjustDetailId(null);
                summaryResult.setTotalPlanQty(0);
                summaryResult.setTrialProductionQty(0);
                summaryResult.setTotalQty(0);
                for (int day = ProductionConstant.MONTH_START_DAY; day <= ProductionConstant.MONTH_MAX_DAY; day++) {
                    summaryResult.setFieldValueByFieldName(BusiConstant.WeekRollAdjust.FIELD_PREFIX_DAY + day, 0);
                }
            }
            // 汇总总计划量
            summaryResult.setTotalPlanQty(
                    Optional.ofNullable(summaryResult.getTotalPlanQty()).orElse(0) +
                            Optional.ofNullable(result.getTotalPlanQty()).orElse(0)
            );
            // 汇总生产实际排产量
            summaryResult.setTotalQty(
                    Optional.ofNullable(summaryResult.getTotalQty()).orElse(0) +
                            Optional.ofNullable(result.getTotalQty()).orElse(0)
            );
            // 汇总试制量试排产量
            summaryResult.setTrialProductionQty(
                    Optional.ofNullable(summaryResult.getTrialProductionQty()).orElse(0) +
                            Optional.ofNullable(result.getTrialProductionQty()).orElse(0)
            );
            // 循环汇总day1-day31字段值
            summaryDayFields(summaryResult, result);
            // 存放汇总Map
            summaryMap.put(groupKey, summaryResult);
        }
        return summaryMap;
    }

    /**
     * 匹配当前调整结果对应的明细
     */
    private MpAdjustDetailVo matchAdjustDetail(MpAdjustResult result, Map<String, Map<Long, MpAdjustDetailVo>> detailMap) {
        Map<Long, MpAdjustDetailVo> idToDetailMap = detailMap.get(result.getMaterialCode());
        if (PubUtil.isEmpty(idToDetailMap)) {
            return null;
        }
        try {
            Long adjustDetailId = result.getAdjustDetailId() != null
                    ? Long.parseLong(result.getAdjustDetailId())
                    : null;
            if (adjustDetailId != null) {
                return idToDetailMap.get(adjustDetailId);
            } else {
                // 无调整明细ID时，取该物料编码下第一条明细
                return idToDetailMap.values().iterator().next();
            }
        } catch (NumberFormatException e) {
            // ID格式错误，返回null
            return null;
        }
    }

    /**
     * 构建分组key
     */
    private String buildGroupKey(MpAdjustResult resultVo) {
        String structureName = Optional.ofNullable(resultVo.getStructureName()).orElse("");
        String materialCode = Optional.ofNullable(resultVo.getMaterialCode()).orElse("");
        String constructionStage = Optional.ofNullable(resultVo.getConstructionStage()).orElse("");
        return String.join(BusiConstant.WeekRollAdjust.SPLIT_GROUP_KEY, structureName, materialCode, constructionStage);
    }

    /**
     * 循环汇总day1-day31字段值
     */
    private void summaryDayFields(MpAdjustResult summaryResult, MpAdjustResult sourceResult) {
        // 遍历日期，设置每个dayN字段
        for (int day = ProductionConstant.MONTH_START_DAY; day <= ProductionConstant.MONTH_MAX_DAY; day++) {
            Integer sourceValue = Convert.toInt(sourceResult.getFieldValueByFieldName(BusiConstant.WeekRollAdjust.FIELD_PREFIX_DAY + day), 0);
            Integer summaryValue = Convert.toInt(summaryResult.getFieldValueByFieldName(BusiConstant.WeekRollAdjust.FIELD_PREFIX_DAY + day), 0);
            summaryResult.setFieldValueByFieldName(BusiConstant.WeekRollAdjust.FIELD_PREFIX_DAY + day, sourceValue + summaryValue);
        }
    }


    /**
     * 根据净需求奇偶性计算实际生产需求含损耗
     *
     * @param currentNetQty 净需求
     * @return 实际生产需求含损耗
     */
    private Integer calculateFactProdReqQty(Integer currentNetQty) {
        // 空值按0处理
        Integer netQty = (currentNetQty == null) ? 0 : currentNetQty;

        // 净需求为0时，直接返回0（不参与奇偶判断）
        if (netQty == 0) {
            return 0;
        }
        // 用位运算判断奇偶
        // 净需求是否为偶数
        boolean isNetEven = (netQty & 1) == 0;
        // 计算实际生产需求含损耗
        Integer factProdReqQty;
        if (isNetEven) {
            // 偶数，实际生产需求含损耗 = 净需求 + 2
            factProdReqQty = netQty + 2;
        } else {
            // 奇数，实际生产需求含损耗 = 净需求 + 3
            factProdReqQty = netQty + 3;
        }

        return factProdReqQty;
    }

    /**
     * 设置活块、型腔数量
     *
     * @param contextDTO
     * @param monthPlan
     */
    private void setMoldCavityInsertField(MpRollAdjustContextDTO contextDTO, FactoryMonthPlanProductionFinalResult monthPlan) {
        List<DailyMouldAvailabilityResult> moldCavityInsertMap = null;
        try {
            // 计算型腔、活块可用量最大值
            moldCavityInsertMap = calculateMoldCavityInsertMaxValue(contextDTO);
        } catch (Exception e) {
            log.error("构建搭配排产新增月度计划 ==> 计算型腔、活块可用量最大值失败! 原因:{}", e.getMessage(), e);
        }
        if (PubUtil.isEmpty(moldCavityInsertMap)) {
            return;
        }
        // 型腔可用量（按结构+主花纹分组）
        Map<String, Integer> cavityResults = moldCavityInsertMap.get(0).getCavityResults();
        // 活块可用量（按物料描述分组）
        Map<String, Integer> insertResults = moldCavityInsertMap.get(0).getInsertResults();
        // 设置型腔数量
        String mouldCavityKey = monthPlan.getStructureName() + monthPlan.getMainPattern();
        if (cavityResults != null && cavityResults.containsKey(mouldCavityKey)) {
            monthPlan.setMouldCavityQty(MapUtils.getInteger(cavityResults, mouldCavityKey, 0));
        } else {
            monthPlan.setMouldCavityQty(Integer.valueOf(0));
        }
        // 设置活块数量
        String typeBlockKey = monthPlan.getMaterialDesc();
        if (insertResults != null && insertResults.containsKey(typeBlockKey)) {
            monthPlan.setTypeBlockQty(MapUtils.getInteger(insertResults, typeBlockKey, 0));
        } else {
            monthPlan.setTypeBlockQty(Integer.valueOf(0));
        }
    }

    /**
     * 设置SKU与示方书关联字段：是否零度材料、制造示方书号、文字示方书号、硫化示方书号、主物料(胎胚号)
     *
     * @param contextDTO
     * @param monthPlan
     */
    private void setSkuConstructionRefField(MpRollAdjustContextDTO contextDTO, FactoryMonthPlanProductionFinalResult monthPlan) {
        // SKU与施工（示方书）关系列表
        List<MdmSkuConstructionRef> skuConstructionRefList = contextDTO.getMdmSkuConstructionRefList();
        if (PubUtil.isEmpty(skuConstructionRefList)) {
            return;
        }
        // 物料编码
        String materialCode = monthPlan.getMaterialCode();
        // 产品状态
        String productStatus = monthPlan.getProductStatus();
        // 按物料编码+产品状态优先级匹配SKU与示方书记录
        MdmSkuConstructionRef mdmSkuConstructionRef = matchSkuConstruction(materialCode, productStatus, skuConstructionRefList);
        if (mdmSkuConstructionRef != null) {
            // 是否零度材料
            monthPlan.setIsZeroRack(mdmSkuConstructionRef.getIsZeroRack());
            // 制造示方书号
            monthPlan.setEmbryoNo(mdmSkuConstructionRef.getEmbryoNo());
            // 文字示方书号
            monthPlan.setTextNo(mdmSkuConstructionRef.getTextNo());
            // 硫化示方书号
            monthPlan.setLhNo(mdmSkuConstructionRef.getLhNo());
            // 主物料(胎胚号)
            monthPlan.setMainMaterialDesc(mdmSkuConstructionRef.getMainMaterialDesc());
        }
    }

    /**
     * 根据物料编码和产品状态匹配SKU与施工关系数据
     *
     * @param skuConstructionRefList SKU与施工（示方书）关系列表
     * @param materialCode           物料编码
     * @param productStatus          产品状态
     * @return
     */
    public MdmSkuConstructionRef getSkuConstructionRefByCondition(List<MdmSkuConstructionRef> skuConstructionRefList, String materialCode,
                                                                  String productStatus) {
        if (PubUtil.isEmpty(skuConstructionRefList)) {
            return null;
        }
        MdmSkuConstructionRef matchRef = skuConstructionRefList.stream()
                .filter(ref -> StringUtils.equals(materialCode, ref.getMaterialCode())
                        && StringUtils.equals(productStatus, ref.getTrialStatus()))
                .findFirst()
                .orElse(null);
        return matchRef;
    }


    /**
     * 更新调整明细
     * 将本次调整的量，回填到"调整明细".实际调整；置换过程回填到“调整明细".调整原因
     *
     * @param contextDTO
     */
    protected void updateAdjustDetailList(MpRollAdjustContextDTO contextDTO) {
        List<MpAdjustResult> adjustResultList = contextDTO.getAdjustResultList();
        List<MpAdjustDetailVo> adjustDetailList = contextDTO.getAdjustDetailList();
        if (PubUtil.isEmpty(adjustResultList) || PubUtil.isEmpty(adjustDetailList)) {
            log.warn("更新调整明细：调整结果列表或调整明细列表为空，直接返回");
            return;
        }
        // 调整结果按照物料编号+施工阶段分组
        Map<String, List<MpAdjustResult>> adjustResultMap = buildMaterialCodeAdjustMap(adjustResultList);
        // 月度生产计划列表
        List<FactoryMonthPlanFinalAdjustVo> monthPlanProdList = contextDTO.getFactoryMonthPlanProdFinalList();
        // 生产计划列表按照物料编码进行分组
        Map<String, List<FactoryMonthPlanFinalAdjustVo>> monthPlanMap = monthPlanProdList.stream()
                .collect(Collectors.groupingBy(vo -> {
                    String materialCode = StringUtils.defaultString(vo.getMaterialCode());
                    String constructionStage = StringUtils.defaultString(vo.getConstructionStage());
                    return String.join(BusiConstant.WeekRollAdjust.SPLIT_GROUP_KEY, materialCode, constructionStage);
                }));

        // 获取调整结果计划总量为0的月度计划列表
        List<FactoryMonthPlanProductionFinalResult> adjustResultMonthPlanList = buildAdjustResultMonthPlan(adjustResultList);
        // 按照物料编码去重进行分组
        Set<String> adjustResultMonthPlanSet = adjustResultMonthPlanList.stream()
                .map(vo -> {
                    String materialCode = StringUtils.defaultString(vo.getMaterialCode());
                    String constructionStage = StringUtils.defaultString(vo.getConstructionStage());
                    return String.join(BusiConstant.WeekRollAdjust.SPLIT_GROUP_KEY, materialCode, constructionStage);
                })
                .collect(Collectors.toSet());

        // 遍历调整明细列表匹配调整结果(更新实际调整、调整原因)
        for (MpAdjustDetailVo adjustDetailVo : adjustDetailList) {
            String materialCode = adjustDetailVo.getMaterialCode();
            if (StringUtils.isEmpty(materialCode)) {
                continue;
            }
            String materialCodeKey = String.join(BusiConstant.WeekRollAdjust.SPLIT_GROUP_KEY, materialCode, adjustDetailVo.getConstructionStage());
            MpAdjustResult adjustResult = CollectionUtils.firstElement(adjustResultMap.get(materialCodeKey));
            if (adjustResult == null) {
                log.warn("更新调整明细：物料编号:{},施工阶段:{},未查询到对应调整结果，跳过", materialCode, adjustDetailVo.getConstructionStage());
                if (!monthPlanMap.containsKey(materialCodeKey)) {
                    adjustDetailVo.setActualAdjustQty(Integer.valueOf(0));
                }
                continue;
            }
            Integer totalPlanQty = Convert.toInt(adjustResult.getTotalPlanQty(), 0);
            Integer actualAdjustQty = totalPlanQty;
            if (totalPlanQty > 0 && !ApsConstant.TRUE.equals(adjustDetailVo.getIsSkuAdd())) {
                List<FactoryMonthPlanFinalAdjustVo> monthPLanList = monthPlanMap.getOrDefault(materialCodeKey, new ArrayList<>());
                Integer totalQty = monthPLanList.stream().mapToInt(v -> {
                    return v.getTotalQty() == null ? 0 : v.getTotalQty();
                }).sum();
                actualAdjustQty = totalPlanQty - totalQty;
            }
            // 设置实际调整
            adjustDetailVo.setActualAdjustQty(Convert.toInt(actualAdjustQty, 0));
            // 如果是关单的情况,实际调整量 = 实际调整量 - 月计划已排产量
            if (adjustResultMonthPlanSet.contains(materialCodeKey) && monthPlanMap.containsKey(materialCodeKey)) {
                Integer monthScheduledQty = Convert.toInt(adjustDetailVo.getMonthScheduledQty(), 0);
                adjustDetailVo.setActualAdjustQty(adjustDetailVo.getActualAdjustQty() - monthScheduledQty);
            }
            // 调整原因 TODO
//            adjustDetailVo.setAdjustReason("");
        }
        // 更新调整明细
        try {
            baseDao.updateBatch(adjustDetailList);
            log.info("更新调整明细成功，共更新:{}条记录", adjustDetailList.size());
        } catch (Exception e) {
            log.error("更新调整明细批量操作异常", e);
            throw new RuntimeException("更新调整明细失败", e);
        }
    }


    /**
     * 查询调整明细
     *
     * @param contextDTO
     */
    protected void queryAdjustDetailList(MpRollAdjustContextDTO contextDTO) {
        if (contextDTO.getMpYear() == null || contextDTO.getMpMonth() == null
                || StringUtils.isEmpty(contextDTO.getVersion())) {
            log.warn("查询调整明细：年份或者月份为空，直接返回");
            return;
        }
        // 年份
        Integer year = contextDTO.getMpYear();
        // 月份
        Integer month = contextDTO.getMpMonth();
        // 调整版本号
        String version = contextDTO.getVersion();

        MpAdjustDetailVo queryVO = new MpAdjustDetailVo();
        queryVO.setYear(year);
        queryVO.setMonth(month);
        queryVO.setVersion(version);

        LambdaQueryWrapper<MpAdjustStructureIn> queryWrapper = new LambdaQueryWrapper<>();
        buildAdjustDetailCondition(queryWrapper, queryVO);

        try {
            List<MpAdjustStructureIn> adjustStructureInList = mpAdjustStructureInEntityMapper.selectList(queryWrapper);
            List<MpAdjustDetailVo> adjustDetailList = BeanUtil.copyToList(adjustStructureInList, MpAdjustDetailVo.class);
            contextDTO.setAdjustDetailList(adjustDetailList);
            log.info("查询调整明细成功，年份：{}，月份：{}，版本：{}，共查询:{}条记录",
                    year, month, version, adjustDetailList.size());
        } catch (Exception e) {
            log.error("查询调整明细异常，年份：{}，月份：{}，版本：{}", year, month, version, e);
            throw new RuntimeException("查询调整明细失败", e);
        }
    }


    /**
     * 更新月度生产计划
     * 更新月度生产计划.1日至31日计划量，并重算开始日期和结束日期
     * 根据周次，将本次调整量合并到对应周次的月度生产计划.调整量
     *
     * @param contextDTO
     */
    protected void updateMonthPlanList(MpRollAdjustContextDTO contextDTO) {
        List<MpAdjustResult> adjustResultList = contextDTO.getAdjustResultList();
        List<FactoryMonthPlanFinalAdjustVo> factoryMonthPlanProdFinalList = contextDTO.getFactoryMonthPlanProdFinalList();
        List<MpAdjustDetailVo> adjustDetailList = contextDTO.getAdjustDetailList();
        if (PubUtil.isEmpty(adjustResultList) || PubUtil.isEmpty(factoryMonthPlanProdFinalList)) {
            log.warn("更新月度生产计划：调整结果列表或月度计划列表或调整明细列表为空，直接返回");
            return;
        }
        // 调整结果按照物料编号+施工阶段分组
        Map<String, List<MpAdjustResult>> adjustResultMap = buildMaterialCodeAdjustMap(adjustResultList);
        // 调整明细按照物料编号+施工阶段分组
        Map<String, List<MpAdjustDetailVo>> adjustDetailMap = buildMaterialCodeAdjustDetailMap(adjustDetailList);

        // 最新需求计划版本
        /*String lastMonthPlanVersion = null;
        if (PubUtil.isNotEmpty(adjustDetailList)) {
            lastMonthPlanVersion = adjustDetailList.get(0).getLastMonthPlanVersion();
            contextDTO.setAdjustMonthPlanVersion(lastMonthPlanVersion);
        }
        if (StringUtil.isEmptyWithTrim(lastMonthPlanVersion) && PubUtil.isNotEmpty(adjustResultList)) {
            lastMonthPlanVersion = adjustResultList.get(0).getLastMonthPlanVersion();
            contextDTO.setAdjustMonthPlanVersion(lastMonthPlanVersion);
        }*/
        // 通过结构过滤月计划列表
        String structureName = contextDTO.getStructureName();
        List<MpAdjustResult> updateAdjustResultValidFlagList = new ArrayList<>();
        // 获取待调整量
        Map<String, MpSkuAdjustInfoVo> skuAdjustInfoMap = getPendingQtyInfo(contextDTO);
/*        factoryMonthPlanProdFinalList = factoryMonthPlanProdFinalList.stream()
                .filter(vo -> StringUtils.isEmpty(structureName) || structureName.equals(vo.getStructureName()))
                .collect(Collectors.toList());*/
        // 需要更新的月计划结果集
        //List<FactoryMonthPlanFinalAdjustVo> monthPlanResultList = new ArrayList<>();
        // 遍历生产计划列表匹配调整结果（更新计划量、开始日期、结束日期、调整量)
        for (FactoryMonthPlanFinalAdjustVo monthPlan : factoryMonthPlanProdFinalList) {
            if (!(StringUtils.isEmpty(structureName) || structureName.equals(monthPlan.getStructureName()))) {
                continue;
            }
            String materialCode = monthPlan.getMaterialCode();
            if (StringUtils.isEmpty(materialCode)) {
                continue;
            }
            if (StringUtil.isEmptyWithTrim(monthPlan.getProductionType())) {
                //补充排产分类
                monthPlan.setProductionType(contextDTO.getMdmSkuProductionTypeMap().get(monthPlan.getMaterialCode()));
            }
            String materialCodeKey = String.join(BusiConstant.WeekRollAdjust.SPLIT_GROUP_KEY, materialCode, monthPlan.getConstructionStage());
            String oriMonthPlanVersion = monthPlan.getMonthPlanVersion();
            MpSkuAdjustInfoVo skuAdjustInfo = skuAdjustInfoMap == null ? null: skuAdjustInfoMap.get(monthPlan.getPendingQtyKey());
            MpAdjustResult adjustResult = CollectionUtils.firstElement(adjustResultMap.get(materialCodeKey));
            if (adjustResult == null) {
                log.warn("更新月度生产计划：物料编号:{},施工阶段:{},未查询到对应调整结果，跳过", materialCode, monthPlan.getConstructionStage());
                /*if (skuAdjustInfo != null && (skuAdjustInfo.getPendingQty() == null || skuAdjustInfo.getPendingQty() == 0)){
                    //若待调整量 == 0 且 调整的需求计划版本与定稿的需求计划版本不一致，将”超欠产有效标识“ = 否；
                    if (!contextDTO.getAdjustMonthPlanVersion().equals(oriMonthPlanVersion)){
                        monthPlan.setLastMonthValidFlag(YesOrNoEnum.NO.getCode());
                    }
                }*/
                //当月只要有确认调整，将上月超产欠标志 置否 sandy+ 2026.7.9
                if (contextDTO.getMpMonth() == contextDTO.getCurrentMonth()){
                    monthPlan.setLastMonthValidFlag(YesOrNoEnum.NO.getCode());
                }
                setMaterialInfoField(contextDTO,monthPlan);
                continue;
            }
            // 相同业务Key时以调整结果为准；调整独有数据转换为同一VO后追加返回。
            BeanUtil.copyProperties(adjustResult, monthPlan, "id");
            /*if (skuAdjustInfo != null && (skuAdjustInfo.getPendingQty() == null || skuAdjustInfo.getPendingQty() == 0)){
                //若待调整量 == 0 且 调整的需求计划版本与定稿的需求计划版本不一致，将”超欠产有效标识“ = 否；
                if (!contextDTO.getAdjustMonthPlanVersion().equals(oriMonthPlanVersion)){
                    monthPlan.setLastMonthValidFlag(YesOrNoEnum.NO.getCode());
                    adjustResult.setLastMonthValidFlag(YesOrNoEnum.NO.getCode());
                    updateAdjustResultValidFlagList.add(adjustResult);
                }
            }*/
            if (contextDTO.getMpMonth() == contextDTO.getCurrentMonth()){
                monthPlan.setLastMonthValidFlag(YesOrNoEnum.NO.getCode());
                adjustResult.setLastMonthValidFlag(YesOrNoEnum.NO.getCode());
                updateAdjustResultValidFlagList.add(adjustResult);
            }
            // 设置最新需求计划版本
            //monthPlan.setLastMonthPlanVersion(lastMonthPlanVersion);

            MpAdjustDetailVo adjustDetail = getFirstAdjustDetail(adjustDetailMap, materialCodeKey);

            adjustDetail = adjustDetail == null ? new MpAdjustDetailVo() : adjustDetail;

            // 更新1日至31日计划量
            for (int i = 1; i <= BusiConstant.WeekRollAdjust.MAX_DAY_OF_MONTH; i++) {
                String dayFieldName = BusiConstant.WeekRollAdjust.FIELD_PREFIX_DAY + i;
                monthPlan.setFieldValueByFieldName(dayFieldName, Convert.toInt(adjustResult.getFieldValueByFieldName(dayFieldName), 0));
            }
            monthPlan.setBeginDay(adjustResult.getBeginDay());
            monthPlan.setEndDay(adjustResult.getEndDay());

            // 生产实际排产量
            monthPlan.setTotalQty(adjustResult.getTotalQty());
            // 高优先级排产数量
            monthPlan.setHeightProductionQty(adjustResult.getHeightProductionQty());
            // 中优先级排产数量
            monthPlan.setMidProductionQty(adjustResult.getMidProductionQty());
            // 周期排产储备排产数量
            monthPlan.setCycleProductionQty(adjustResult.getCycleProductionQty());
            // 常规储备排产数量
            monthPlan.setConventionProductionQty(adjustResult.getConventionProductionQty());
            // 暂缓订单排产数量
            monthPlan.setPostponeProductionQty(adjustResult.getPostponeProductionQty());
            // 试制量试排产量
            monthPlan.setTrialProductionQty(adjustResult.getTrialProductionQty());
            // 计算实际生产需求含损耗
            Integer factProdReqQty = calculateFactProdReqQty(adjustDetail.getCurrentNetQty());
            // 差异量(未排产数量) = 实际生产需求含损耗 - 生产实际排产量
            Integer differenceQty = Convert.toInt(factProdReqQty, 0) - Convert.toInt(monthPlan.getTotalQty(), 0);
            monthPlan.setDifferenceQty(differenceQty);
            // 模具变化信息
            monthPlan.setMouldChangeInfo(adjustResult.getMouldChangeInfo());
            // 设置周调整量
            int week = getWeekNumber(contextDTO.getAdjustDay());
            setWeekAdjustQty(monthPlan, week);

            setMaterialInfoField(contextDTO,monthPlan);

            // 将日期字段中值为0的字段设为null
            handleZeroToNull(monthPlan);
        }

        if (PubUtil.isNotEmpty(updateAdjustResultValidFlagList)){
            mpAdjustResultEntityMapper.updateValidFlagBatchById(updateAdjustResultValidFlagList);
        }
    }

    /**
     * 获取待调整量
     * @param contextDTO 调整上下文
     * @return
     */
    private Map<String, MpSkuAdjustInfoVo> getPendingQtyInfo(MpRollAdjustContextDTO contextDTO){
        FactoryMonthPlanProductionFinalResult condition = new FactoryMonthPlanProductionFinalResult();
        condition.setFactoryCode(contextDTO.getFactoryCode());
        condition.setYear(contextDTO.getMpYear());
        condition.setMonth(contextDTO.getMpMonth());
        condition.setProductionVersion(contextDTO.getVersion());
        return mpSkuAdjustInfoService.getPendingQtyInfo(condition, contextDTO.getAdjustMonthPlanVersion());
    }

    /**
     * 设置周调整量
     *
     * @param monthPlan
     */
    private void setWeekAdjustQty(FactoryMonthPlanProductionFinalResult monthPlan, int week) {
        // 调整量
        int oriAdjustQty = Convert.toInt(monthPlan.getOriginalTotalQty(), 0);
        if (week > 1) {
            for (int i = 1; i < week; i++) {
                oriAdjustQty += Convert.toInt(monthPlan.getFieldValueByFieldName(BusiConstant.WeekRollAdjust.FIELD_PREFIX_ADJUST_QTY + i), 0);
            }
        }

        //Integer actualAdjustQty = adjustDetail.getActualAdjustQty();
        Integer actualAdjustQty = Convert.toInt(monthPlan.getTotalQty(),0) - oriAdjustQty;
        if (Convert.toInt(actualAdjustQty, 0) == 0) {
            actualAdjustQty = null;
        }
        monthPlan.setFieldValueByFieldName(BusiConstant.WeekRollAdjust.FIELD_PREFIX_ADJUST_QTY + week, actualAdjustQty);
    }

    /**
     * 将本次批量更新后的月计划结果回写到上下文中的全量月度生产计划列表。
     * <p>
     * {@code queryMonthPlanList} 已把全量月计划放入上下文，{@code updateMonthPlanList} 仅对局部过滤结果做
     * {@code updateBatch}，若不回写则 {@code insertMonthPlanList}、{@code handleMonthPlanStatistics} 等仍读到旧快照。
     * 此处按行 {@code set} 替换，保留列表引用，避免影响后续 {@code addAll} 新增行。
     * </p>
     * 优先按主键 id 与上下文行对齐；若更新结果中 id 为空则按产品结构+物料编码兜底。
     * 说明：同一产品结构下同一物料若存在多行（如施工阶段不同），应依赖主键匹配；仅靠结构+物料键可能无法区分多行。
     *
     * @param contextDTO      周程调整上下文
     * @param monthPlanResult 已持久化的月度生产计划更新结果集
     * @return 无
     */
    private void refreshMonthPlanProdFinalListInContext(MpRollAdjustContextDTO contextDTO,
                                                        List<FactoryMonthPlanFinalAdjustVo> monthPlanResult) {
        if (PubUtil.isEmpty(monthPlanResult)) {
            return;
        }
        List<FactoryMonthPlanFinalAdjustVo> contextList = contextDTO.getFactoryMonthPlanProdFinalList();
        if (PubUtil.isEmpty(contextList)) {
            return;
        }
        // 指定初始容量，减少 HashMap 扩容
        int initialCapacity = Math.max(16, monthPlanResult.size() * 2);
        // 主键 -> 本批更新后的 VO（正常数据均带 id，优先使用）
        Map<Long, FactoryMonthPlanFinalAdjustVo> updatedById = new HashMap<>(initialCapacity);
        // 结构+物料 -> 本批更新后的 VO（仅 id 为空时的兜底；同键多行时 putIfAbsent 保留第一条）
        Map<String, FactoryMonthPlanFinalAdjustVo> updatedByStructureMaterial = new HashMap<>(initialCapacity);
        for (FactoryMonthPlanFinalAdjustVo updated : monthPlanResult) {
            if (updated == null) {
                continue;
            }
            if (Objects.nonNull(updated.getId())) {
                updatedById.put(updated.getId(), updated);
            } else {
                String structureMaterialKey = buildMonthPlanStructureMaterialKey(updated);
                updatedByStructureMaterial.putIfAbsent(structureMaterialKey, updated);
            }
        }
        if (updatedById.isEmpty() && updatedByStructureMaterial.isEmpty()) {
            return;
        }
        // 遍历上下文全量列表，命中则替换为已更新对象，未命中行保持原引用不变
        for (int i = 0; i < contextList.size(); i++) {
            FactoryMonthPlanFinalAdjustVo row = contextList.get(i);
            if (row == null) {
                continue;
            }
            // 先按主键对齐（唯一、可靠）
            if (Objects.nonNull(row.getId())) {
                FactoryMonthPlanFinalAdjustVo replacement = updatedById.get(row.getId());
                if (replacement != null) {
                    contextList.set(i, replacement);
                    continue;
                }
            }
            // id 未命中或为空时，再尝试结构+物料兜底
            String structureMaterialKey = buildMonthPlanStructureMaterialKey(row);
            FactoryMonthPlanFinalAdjustVo replacement = updatedByStructureMaterial.get(structureMaterialKey);
            if (replacement != null) {
                contextList.set(i, replacement);
            }
        }
    }

    /**
     * 构建月计划「产品结构 + 物料编码」匹配键，空字段按空串处理。
     * 分隔符与 {@link BusiConstant.WeekRollAdjust#SPLIT_GROUP_KEY} 一致，便于与分组逻辑风格统一。
     *
     * @param vo 月计划 VO
     * @return 用于 Map 查找的匹配键字符串
     */
    private String buildMonthPlanStructureMaterialKey(FactoryMonthPlanFinalAdjustVo vo) {
        // null 转空串，避免 NPE 且保证键可比较
        String structureName = Optional.ofNullable(vo.getStructureName()).orElse("");
        String materialCode = Optional.ofNullable(vo.getMaterialCode()).orElse("");
        return String.join(BusiConstant.WeekRollAdjust.SPLIT_GROUP_KEY, structureName, materialCode);
    }

    /**
     * 更新最新的月度生产计划版本
     *
     * @param contextDTO
     */
    private void updateMonthPlanVersion(MpRollAdjustContextDTO contextDTO) {
        List<MpAdjustResult> adjustResultList = contextDTO.getAdjustResultList();
        if (PubUtil.isEmpty(adjustResultList)) {
            return;
        }
        //获取调整结果最新的需求计划版本
        String adjustMonthPlanVersion = adjustResultList.get(0).getLastMonthPlanVersion();
        if (StringUtils.isBlank(adjustMonthPlanVersion)) {
            return;
        }
        // 获取最新需求计划版本
        //String adjustMonthPlanVersion = contextDTO.getAdjustMonthPlanVersion();
        // 收集结构名称Set（筛选结构名称不为空且有调整）
        /*Set<String> structureNameSet = adjustResultList.stream()
                .filter(vo -> StringUtils.isNotEmpty(vo.getStructureName())
                        && ApsConstant.TRUE.equals(vo.getAdjustFlag()))
                .map(MpAdjustResult::getStructureName)
                .collect(Collectors.toSet());
        if (PubUtil.isEmpty(structureNameSet)) {
            return;
        }*/
        // 通过结构名称更新月度生产计划最新需求计划版本
        LambdaUpdateWrapper<FactoryMonthPlanProductionFinalResult> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, contextDTO.getFactoryCode())
                .eq(FactoryMonthPlanProductionFinalResult::getYear, contextDTO.getMpYear())
                .eq(FactoryMonthPlanProductionFinalResult::getMonth, contextDTO.getMpMonth())
                .eq(FactoryMonthPlanProductionFinalResult::getProductionVersion, contextDTO.getProductionVersion())
                .set(FactoryMonthPlanProductionFinalResult::getLastMonthPlanVersion, adjustMonthPlanVersion);
        factoryMonthPlanProdFinalMapper.update(null, wrapper);
    }


    /**
     * 根据时间获取周次
     * 范围：第1周1-7，第2周8-14，第3周15-21，第4周22-31
     *
     * @return
     */
    protected int getWeekNumber(int adjustDay) {
        //int dayOfMonth = DateUtil.dayOfMonth(date);
        int baseWeek = (adjustDay - 1) / 7 + 1;
        return Math.min(baseWeek, 4);
    }

    /**
     * 根据优先级顺序分配生产数量
     *
     * @param contextDTO
     */
    private void allocateProductionByPriority(MpRollAdjustContextDTO contextDTO) {
        MpWeekRollAdjustEngine weekRollAdjustEngine = new MpWeekRollAdjustEngine();
        List<FactoryMonthPlanFinalAdjustVo> finalAdjustVos = contextDTO.getFactoryMonthPlanProdFinalList();
        if (PubUtil.isEmpty(finalAdjustVos)) {
            return;
        }
        finalAdjustVos.forEach(adjustResult -> {
            weekRollAdjustEngine.resetTotalProductionQty(adjustResult);
        });

    }

    /**
     * 查询周程调整结果
     *
     * @param contextDTO
     */
    private void queryAdjustResult(MpRollAdjustContextDTO contextDTO) {
        if (contextDTO.getMpYear() == null || contextDTO.getMpMonth() == null
                || StringUtils.isEmpty(contextDTO.getVersion())) {
            log.warn("查询周程调整结果：年份或者月份为空，直接返回");
            return;
        }
        // 工厂编码
        String factoryCode = contextDTO.getFactoryCode();
        // 年份
        Integer year = contextDTO.getMpYear();
        // 月份
        Integer month = contextDTO.getMpMonth();
        // 版本
        String version = contextDTO.getVersion();
        // 排产版本
        //String productionVersion = contextDTO.getProductionVersion();

        MpAdjustResult queryVO = new MpAdjustResult();
        queryVO.setFactoryCode(factoryCode);
        queryVO.setYear(year);
        queryVO.setMonth(month);
        queryVO.setVersion(version);
        //queryVO.setProductionVersion(productionVersion);

        LambdaQueryWrapper<MpAdjustResult> queryWrapper = new LambdaQueryWrapper<>();
        buildAdjustResultCondition(queryWrapper, queryVO);

        try {
            List<MpAdjustResult> resultList = mpAdjustResultEntityMapper.selectList(queryWrapper);
            /*if (Boolean.TRUE.equals(contextDTO.getFrontScheduledMachinesFlag())) {
                resultList = WeekRollAdjustMachineCrossChecker.filterAdjustResultByMachine(resultList, contextDTO.getScheduledMachines());
            }*/
           /* Set<String> structureNameSet = resultList.stream().map(x -> x.getStructureName()).collect(Collectors.toSet());
            if (StringUtil.isEmptyWithTrim(contextDTO.getStructureName()) && structureNameSet != null && structureNameSet.size() == 1) {
                //若调整结果只有一个结果，直接设置为当前结构
                contextDTO.setStructureName(structureNameSet.iterator().next());
            }*/
            contextDTO.setAdjustResultList(resultList);
        } catch (Exception e) {
            log.error("查询周程调整结果异常，年份：{}，月份：{}，版本：{}", year, month, version, e);
            throw new RuntimeException("查询月度生产计划失败", e);
        }
    }

    /**
     * 查询结构转产记录，用于单台机台结构时间交叉校验。
     *
     * @param contextDTO 周滚动调整上下文对象
     * @return 结构转产记录集合
     */
    private List<MpStructureAllocation> queryStructureAllocationList(MpRollAdjustContextDTO contextDTO) {
        LambdaQueryWrapper<MpStructureAllocation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StringUtils.isNotBlank(contextDTO.getFactoryCode()), MpStructureAllocation::getFactoryCode, contextDTO.getFactoryCode());
        queryWrapper.eq(contextDTO.getMpYear() != null, MpStructureAllocation::getYear, contextDTO.getMpYear());
        queryWrapper.eq(contextDTO.getMpMonth() != null, MpStructureAllocation::getMonth, contextDTO.getMpMonth());
        queryWrapper.eq(StringUtils.isNotBlank(contextDTO.getProductionVersion()), MpStructureAllocation::getProductionVersion, contextDTO.getProductionVersion());
        queryWrapper.eq(MpStructureAllocation::getCxMachineCode, contextDTO.getScheduledMachines());
        queryWrapper.eq(MpStructureAllocation::getIsDelete, YesOrNoEnum.NO.getValue());
        return mpStructureAllocationEntityMapper.selectList(queryWrapper);
    }

    /**
     * 保存周程调整结果
     *
     * @param contextDTO
     */
    private MpStructureAllocation buildTargetStructureAllocation(MpRollAdjustContextDTO contextDTO) {
        if (StringUtils.isAnyBlank(contextDTO.getFactoryCode(), contextDTO.getProductionVersion(),
                contextDTO.getScheduledMachines(), contextDTO.getStructureName())
                || contextDTO.getMpYear() == null || contextDTO.getMpMonth() == null
                || contextDTO.getAdjustStartDay() == null || contextDTO.getAdjustEndDay() == null) {
            return null;
        }
        MpStructureAllocation targetAllocation = new MpStructureAllocation();
        targetAllocation.setFactoryCode(contextDTO.getFactoryCode());
        targetAllocation.setYear(contextDTO.getMpYear());
        targetAllocation.setMonth(contextDTO.getMpMonth());
        targetAllocation.setProductionVersion(contextDTO.getProductionVersion());
        targetAllocation.setMonthPlanVersion(contextDTO.getMonthPlanVersion());
        targetAllocation.setCxMachineCode(contextDTO.getScheduledMachines());
        targetAllocation.setStructureName(contextDTO.getStructureName());
        targetAllocation.setBeginDay(contextDTO.getAdjustStartDay());
        targetAllocation.setEndDay(contextDTO.getAdjustEndDay());
        targetAllocation.setAllotDays(contextDTO.getEndDay() - contextDTO.getStartDay() + 1);
        return targetAllocation;
    }

    private void saveAdjustResult(MpRollAdjustContextDTO contextDTO) {
        // 调整结果
        List<MpAdjustResult> adjustResultList = contextDTO.getAdjustResultList();
        if (PubUtil.isEmpty(adjustResultList)) {
            log.warn("保存周程调整结果：调整结果列表为空，直接返回");
            return;
        }
        // 保存调整结果
        try {
            baseDao.saveBatch(adjustResultList);
            log.info("保存周程调整结果成功，共保存:{}条记录", adjustResultList.size());
        } catch (Exception e) {
            log.error("保存周程调整结果批量操作异常", e);
            throw new RuntimeException("保存周程调整结果失败", e);
        }
    }

    /**
     * 查询月度生产计划
     *
     * @param contextDTO
     */
    private void queryMonthPlanList(MpRollAdjustContextDTO contextDTO) {
        if (contextDTO.getMpYear() == null || contextDTO.getMpMonth() == null) {
            log.warn("查询月度生产计划：年份或者月份为空，直接返回");
            return;
        }
        // 工厂编码
        String factoryCode = contextDTO.getFactoryCode();
        // 年份
        Integer year = contextDTO.getMpYear();
        // 月份
        Integer month = contextDTO.getMpMonth();
        // 月度计划排产版本
        //String productionVersion = contextDTO.getProductionVersion();

        FactoryMonthPlanProductionFinalResult queryVO = new FactoryMonthPlanProductionFinalResult();
        queryVO.setFactoryCode(factoryCode);
        queryVO.setYear(year);
        queryVO.setMonth(month);
        //queryVO.setProductionVersion(productionVersion);

        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper = new LambdaQueryWrapper<>();
        buildMonthPlanCondition(queryWrapper, queryVO);

        try {
            List<FactoryMonthPlanProductionFinalResult> factoryMonthPlanProdFinalList = factoryMonthPlanProdFinalMapper.selectList(queryWrapper);
            List<FactoryMonthPlanFinalAdjustVo> resultList = BeanUtil.copyToList(factoryMonthPlanProdFinalList, FactoryMonthPlanFinalAdjustVo.class);
            contextDTO.setFactoryMonthPlanProdFinalList(resultList);
            if (PubUtil.isNotEmpty(resultList)) {
                contextDTO.setProductType(resultList.get(0).getProductionType());
            }
        } catch (Exception e) {
            log.error("查询月度生产计划异常，年份：{}，月份：{}", year, month, e);
            throw new RuntimeException("查询月度生产计划失败", e);
        }
    }


    /**
     * 构建调整明细分组Map
     *
     * @param adjustDetailVoList
     * @return
     */
    protected Map<String, List<MpAdjustDetailVo>> buildMaterialCodeAdjustDetailMap(List<MpAdjustDetailVo> adjustDetailVoList) {
        if (PubUtil.isEmpty(adjustDetailVoList)) {
            return Collections.emptyMap();
        }
        return adjustDetailVoList.stream()
                .filter(detailVo -> StringUtils.isNotEmpty(detailVo.getMaterialCode()))
                .collect(Collectors.groupingBy(vo -> {
                    String materialCode = StringUtils.defaultString(vo.getMaterialCode());
                    String constructionStage = StringUtils.defaultString(vo.getConstructionStage());
                    return String.join(BusiConstant.WeekRollAdjust.SPLIT_GROUP_KEY, materialCode, constructionStage);
                }));
    }

    /**
     * 构建调整结果分组Map
     *
     * @param adjustResultList
     * @return
     */
    protected Map<String, List<MpAdjustResult>> buildMaterialCodeAdjustMap(List<MpAdjustResult> adjustResultList) {
        if (PubUtil.isEmpty(adjustResultList)) {
            return Collections.emptyMap();
        }
        return adjustResultList.stream()
                .filter(result -> StringUtils.isNotEmpty(result.getMaterialCode()))
                .collect(Collectors.groupingBy(vo -> {
                    String materialCode = StringUtils.defaultString(vo.getMaterialCode());
                    String constructionStage = StringUtils.defaultString(vo.getConstructionStage());
                    return String.join(BusiConstant.WeekRollAdjust.SPLIT_GROUP_KEY, materialCode, constructionStage);
                }));
    }

    /**
     * 获取第一个调整结果
     *
     * @param materialCodeAdjustMap
     * @param materialCodeKey
     * @return
     */
    protected MpAdjustResult getFirstAdjustResult(Map<String, List<MpAdjustResult>> materialCodeAdjustMap, String materialCodeKey) {
        if (materialCodeAdjustMap == null || StringUtils.isEmpty(materialCodeKey)) {
            return null;
        }
        List<MpAdjustResult> resultList = materialCodeAdjustMap.get(materialCodeKey);
        if (PubUtil.isEmpty(resultList)) {
            return null;
        }
        return resultList.get(0);
    }

    /**
     * 获取第一个调整明细
     *
     * @param materialCodeAdjustMap
     * @param materialCodeKey
     * @return
     */
    protected MpAdjustDetailVo getFirstAdjustDetail(Map<String, List<MpAdjustDetailVo>> materialCodeAdjustMap, String materialCodeKey) {
        if (materialCodeAdjustMap == null || StringUtils.isEmpty(materialCodeKey)) {
            return null;
        }
        List<MpAdjustDetailVo> resultList = materialCodeAdjustMap.get(materialCodeKey);
        if (PubUtil.isEmpty(resultList)) {
            return null;
        }
        return resultList.get(0);
    }


    /**
     * 生成调整明细(业务逻辑处理)
     */
    public abstract void doGenerateAdjust(MpRollAdjustContextDTO contextDTO);

    /**
     * 自动调整(业务逻辑处理)
     */
    public abstract void doAutoAdjust(MpRollAdjustContextDTO contextDTO);

    /**
     * 生产对齐(业务逻辑处理)
     */
    public abstract void doProductAlign(MpRollAdjustContextDTO contextDTO);

    /**
     * 筛选：|净需求 - 计划剩余排产量| > 0的数据
     *
     * @param adjustList
     */
    protected void filterAdjustList(List<MpAdjustDetailVo> adjustList) {
        if (PubUtil.isEmpty(adjustList)) {
            return;
        }
        adjustList.removeIf(adjust -> {
            Integer currentNetQty = Convert.toInt(adjust.getCurrentNetQty(), 0);
            Integer monthUnScheduledQty = Convert.toInt(adjust.getMonthUnScheduledQty(), 0);
            Integer adjustQty = Math.abs(currentNetQty - monthUnScheduledQty);
            boolean isOnlyConventionReserveHasValue = isOnlyConventionReserveHasValue(adjust);
            //return (Math.abs(currentNetQty - monthUnScheduledQty) == 0) || isOnlyConventionReserveHasValue;
            return (adjustQty == 0 && currentNetQty == 0 && monthUnScheduledQty == 0) || isOnlyConventionReserveHasValue;
        });
    }

    /**
     * 检查是否已执行自动调整
     *
     * @param contextDTO
     */
    protected void checkHasDoAutoAdjust(MpRollAdjustContextDTO contextDTO) {
        LambdaQueryWrapper<MpAdjustResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MpAdjustResult::getFactoryCode, contextDTO.getFactoryCode());
        queryWrapper.eq(MpAdjustResult::getVersion, contextDTO.getVersion());
        if (!StringUtil.isEmptyWithTrim(contextDTO.getStructureName())) {
            queryWrapper.eq(MpAdjustResult::getStructureName, contextDTO.getStructureName());
        }
        List<MpAdjustResult> mpAdjustResultList = mpAdjustResultEntityMapper.selectList(queryWrapper);
        if (PubUtil.isNotEmpty(mpAdjustResultList)) {
            throw new BusinessException(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.findAutoAdjustRecord"),
                    contextDTO.getVersion()));
        }
    }

    /**
     * 检查偶数
     *
     * @param number
     * @return
     */
    protected boolean isEven(int number) {
        return (number & 1) == 0;
    }

    /**
     * 生成分布式唯一版本号
     *
     * @param prefix
     * @return
     */
    protected String generateVersion(String prefix) {
        return versionGenerator.generateVersion(prefix);
    }

    /**
     * 设置分布式唯一版本号
     *
     * @param contextDTO
     * @param prefix
     * @return
     */
    protected void setVersion(MpRollAdjustContextDTO contextDTO, String prefix) {
        //prefix = "T"+prefix;
        contextDTO.setVersion(generateVersion(prefix));
    }


    /**
     * 并行初始化
     */
    private void initParallel(MpRollAdjustContextDTO contextDTO) {
        // 创建计时器
        StopWatch watch = new StopWatch();
        watch.start();

        // 初始化通用
        //initCommon(contextDTO);
        // 特殊规则初始化
        specialInit(contextDTO);

        // 获取线程池执行器
        ThreadPoolExecutor executor = ThreadPoolUtil.getThreadPool();

        // 创建初始化方法的异步任务
        // 初始化排产版本、初始化月度生产计划 (有依赖关系：先执行initVersion，再执行initMonthPlan)
        /*CompletableFuture<Void> versionAndMonthPlanFuture = CompletableFuture
                .runAsync(() -> initVersion(contextDTO), executor)
                .thenRunAsync(() -> initMonthPlan(contextDTO), executor);*/
        // 初始化上次需求计划
        CompletableFuture<Void> lastDemandPlanFuture = CompletableFuture.runAsync(() -> initDemandPlanByLastMonthPlanVersion(contextDTO), executor);
        // 初始化销售订单池
        CompletableFuture<Void> saleOrderFuture = CompletableFuture.runAsync(() -> initSaleOrderPool(contextDTO), executor);
        // 初始化试制量试计划
        CompletableFuture<Void> trialPlanFuture = CompletableFuture.runAsync(() -> initTrialPlan(contextDTO), executor);
        // 初始化sku日硫化产能
        CompletableFuture<Void> skuLhCapacityFuture = CompletableFuture.runAsync(() -> initSkuLhCapacity(contextDTO), executor);
        // 初始化SKU与施工（示方书）关系
        CompletableFuture<Void> skuConstructionRefFuture = CompletableFuture.runAsync(() -> initSkuConstructionRef(contextDTO), executor);
        // 初始化sku与结构关系
        CompletableFuture<Void> skuStructureRefFuture = CompletableFuture.runAsync(() -> initSkuStructureRef(contextDTO), executor);
        // 初始化月计划结构转产
        CompletableFuture<Void> structureAllocationFuture = CompletableFuture.runAsync(() -> initStructureAllocation(contextDTO), executor);
        // 初始化月度硫化监控
        CompletableFuture<Void> planMonitorFuture = CompletableFuture.runAsync(() -> initPlanMonitor(contextDTO), executor);
        // 初始化物料信息
        CompletableFuture<Void> materialInfoFuture = CompletableFuture.runAsync(() -> initMaterialInfo(contextDTO), executor);
        // 初始化特殊材料记录
        CompletableFuture<Void> specialMaterialRecordFuture = CompletableFuture.runAsync(() -> initSpecialMaterialRecord(contextDTO), executor);
        // 初始化BOM物料消耗明细
        CompletableFuture<Void> materialConsumeDetailFuture = CompletableFuture.runAsync(() -> initMaterialConsumeDetail(contextDTO), executor);
//        // 初始化月底计划余量
//        CompletableFuture<Void> monthSurplusFuture = CompletableFuture.runAsync(() -> initMonthSurplus(contextDTO), executor);
//        // 初始化成品实时库存
//        CompletableFuture<Void> productStockFuture = CompletableFuture.runAsync(
//                // 解决父子上下文传递问题
//                SpringContextSupplierUtil.wrap(() -> initProductStock(contextDTO)),
//                executor
//        );

        try {
            // 等待所有异步任务执行完成
            CompletableFuture.allOf(
                   // versionAndMonthPlanFuture,
                    lastDemandPlanFuture,
                    saleOrderFuture,
                    trialPlanFuture,
//                    monthSurplusFuture,
//                    productStockFuture,
                    planMonitorFuture,
                    skuLhCapacityFuture,
                    skuConstructionRefFuture,
                    skuStructureRefFuture,
                    structureAllocationFuture,
                    materialInfoFuture,
                    specialMaterialRecordFuture,
                    materialConsumeDetailFuture
            ).join();

            log.info("并行初始化任务执行完成");

        } catch (CompletionException e) {
            // 异常处理
            Throwable throwable = e.getCause();
            log.error("初始化任务执行失败! 失败原因:{}", throwable.getMessage(), throwable);
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.initDataFailure"), throwable);
        } finally {
            watch.stop();
        }
        log.info("初始化任务执行完成 ==> 耗时:{} ms", watch.getLastTaskTimeMillis());
    }
    /**
     * 初始化月计划结构转产
     *
     * @param contextDTO
     */
    private void initStructureAllocation(MpRollAdjustContextDTO contextDTO) {
        MpStructureAllocation queryVO = new MpStructureAllocation();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());
        queryVO.setYear(contextDTO.getMpYear());
        queryVO.setMonth(contextDTO.getMpMonth());
        queryVO.setProductionVersion(contextDTO.getProductionVersion());

        DataDTO dataDTO = dataManager.buildDataDTO(queryVO);
        List<MpStructureAllocation> structureAllocationList = dataManager.listStructureAllocations(dataDTO);

        contextDTO.setStructureAllocationList(structureAllocationList);
        Map<String, List<MpStructureAllocation>> structureAllocationMap = convertToStructureAllocationMap(structureAllocationList);
        contextDTO.setStructureAllocationMap(structureAllocationMap);
    }


    /**
     * 初始化月度硫化监控
     *
     * @param contextDTO
     */
    private void initPlanMonitor(MpRollAdjustContextDTO contextDTO) {
        MpMonthPlanMonitor queryVO = MpMonthPlanMonitor.builder()
                .factoryCode(contextDTO.getFactoryCode())
                .year(contextDTO.getMpYear())
                .month(contextDTO.getMpMonth())
                .productionVersion(contextDTO.getProductionVersion())
                .build();

        DataDTO dataDTO = dataManager.buildDataDTO(queryVO);
        List<MpMonthPlanMonitor> planMonitorList = dataManager.listPlanMonitors(dataDTO);
        contextDTO.setMpMonthPlanMonitorList(planMonitorList);

        //检查是否跨月，初始上月监控
        if (checkCrossMonth(contextDTO)) {
            initLastPlanMonitor(contextDTO);
        }
    }

    /**
     * 初始化上月月度硫化监控
     *
     * @param contextDTO
     */
    private void initLastPlanMonitor(MpRollAdjustContextDTO contextDTO) {
        MpMonthPlanMonitor queryVO = MpMonthPlanMonitor.builder()
                .factoryCode(contextDTO.getFactoryCode())
                .year(contextDTO.getCurrentYear())
                .month(contextDTO.getCurrentMonth())
                .productionVersion(contextDTO.getLastProductionVersion())
                .build();

        DataDTO dataDTO = dataManager.buildDataDTO(queryVO);
        List<MpMonthPlanMonitor> planMonitorList = dataManager.listPlanMonitors(dataDTO);
        contextDTO.setLastMonthPlanMonitorList(planMonitorList);
    }

    /**
     * 初始化成品实时库存
     *
     * @param contextDTO
     */
    private void initProductStock(MpRollAdjustContextDTO contextDTO) {
        MdmProductStock queryVO = new MdmProductStock();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());
        queryVO.setStockDate(DateUtil.parse(DateUtil.today()));
        Map<String, Object> param = Maps.newHashMap();
        // 传空查询所有
        param.put("materialCodeList", null);
        queryVO.setParams(param);

        // 调用接口查询实时成品库存
        List<MdmProductStock> mdmProductStockList = mesItfService.getProductStock(queryVO);
        contextDTO.setMdmProductStockList(mdmProductStockList);
    }


    /**
     * 初始化月底计划余量
     *
     * @param contextDTO
     */
    private void initMonthSurplus(MpRollAdjustContextDTO contextDTO) {
        MdmMonthSurplus queryVO = new MdmMonthSurplus();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());
        queryVO.setYear(contextDTO.getMpYear());
        queryVO.setMonth(contextDTO.getMpMonth());

        LambdaQueryWrapper<MdmMonthSurplus> queryWrapper = new LambdaQueryWrapper<>();
        buildMonthSurplusCondition(queryWrapper, queryVO);
        List<MdmMonthSurplus> mdmMonthSurplusesList = mdmMonthSurplusEntityMapper.selectList(queryWrapper);
        contextDTO.setMdmMonthSurplusesList(mdmMonthSurplusesList);
    }

    /**
     * 构建月底计划余量条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildMonthSurplusCondition(LambdaQueryWrapper<MdmMonthSurplus> queryWrapper, MdmMonthSurplus queryVO) {
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getFactoryCode()), MdmMonthSurplus::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MdmMonthSurplus::getYear, queryVO.getYear());
        queryWrapper.eq(MdmMonthSurplus::getMonth, queryVO.getMonth());
        queryWrapper.eq(MdmMonthSurplus::getIsDelete, YesOrNoEnum.NO.getValue());
    }


    /**
     * 初始化SKU与施工（示方书）关系
     *
     * @param contextDTO
     */
    private void initSkuConstructionRef(MpRollAdjustContextDTO contextDTO) {
        MdmSkuConstructionRef queryVO = new MdmSkuConstructionRef();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());

        String cacheKey = dataManager.generateCacheKey(queryVO.getFactoryCode());
        DataDTO dataDTO = dataManager.buildDataDTO(queryVO, cacheKey, Boolean.FALSE);
        List<MdmSkuConstructionRef> mdmSkuConstructionRefList = dataManager.listSkuConstructionRefs(dataDTO);

        contextDTO.setMdmSkuConstructionRefList(mdmSkuConstructionRefList);
        Map<String, MdmSkuConstructionRef> mdmSkuConstructionRefMap = convertToSkuConstructionRefMap(mdmSkuConstructionRefList);
        contextDTO.setMdmSkuConstructionRefMap(mdmSkuConstructionRefMap);
    }

    /**
     * 初始化SKU排产分类
     *
     * @param contextDTO
     */
    private void initSkuProductionType(MpRollAdjustContextDTO contextDTO) {
        Map<String, String> productionTypeMap = mdmSkuScheduleCategoryService.skuToProductionType(contextDTO.getFactoryCode());
        if (PubUtil.isEmpty(productionTypeMap)) {
            productionTypeMap = new HashMap<>();
        }
        contextDTO.setMdmSkuProductionTypeMap(productionTypeMap);
    }

    /**
     * 初始化sku与结构关系
     *
     * @param contextDTO
     */
    private void initSkuStructureRef(MpRollAdjustContextDTO contextDTO) {
        MdmSkuStructureRef queryVO = new MdmSkuStructureRef();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());

        String cacheKey = dataManager.generateCacheKey(queryVO.getFactoryCode());
        DataDTO dataDTO = dataManager.buildDataDTO(queryVO, cacheKey, Boolean.TRUE);
        List<MdmSkuStructureRef> mdmSkuStructureRefList = dataManager.listSkuStructureRefs(dataDTO);

        Map<String, MdmSkuStructureRef> mdmSkuStructureRefMap = convertToSkuStructureRefMap(mdmSkuStructureRefList);
        contextDTO.setMdmSkuStructureRefMap(mdmSkuStructureRefMap);

    }


    /**
     * 初始化BOM物料消耗明细
     *
     * @param contextDTO
     */
    private void initMaterialConsumeDetail(MpRollAdjustContextDTO contextDTO) {
        MdmMaterialConsumeDetail queryVO = new MdmMaterialConsumeDetail();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());

        String cacheKey = dataManager.generateCacheKey(queryVO.getFactoryCode());
        DataDTO dataDTO = dataManager.buildDataDTO(queryVO, cacheKey, Boolean.FALSE);
        List<MdmMaterialConsumeDetail> materialConsumeDetailList = dataManager.listMaterialConsumeDetails(dataDTO);
        contextDTO.setMdmMaterialConsumeDetailList(materialConsumeDetailList);
    }


    /**
     * 初始化特殊材料记录
     *
     * @param contextDTO
     */
    private void initSpecialMaterialRecord(MpRollAdjustContextDTO contextDTO) {
        RawSpecialMaterialRecord queryVO = new RawSpecialMaterialRecord();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());

        String cacheKey = dataManager.generateCacheKey(queryVO.getFactoryCode());
        DataDTO dataDTO = dataManager.buildDataDTO(queryVO, cacheKey, Boolean.FALSE);
        List<RawSpecialMaterialRecord> specialMaterialList = dataManager.listSpecialMaterials(dataDTO);
        contextDTO.setSpecialMaterialList(specialMaterialList);
    }

    /**
     * 初始化物料信息
     *
     * @param contextDTO
     */
    private void initMaterialInfo(MpRollAdjustContextDTO contextDTO) {
        MdmMaterialInfo queryVO = new MdmMaterialInfo();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());

        String cacheKey = dataManager.generateCacheKey(queryVO.getFactoryCode());
        DataDTO dataDTO = dataManager.buildDataDTO(queryVO, cacheKey, Boolean.FALSE);
        List<MdmMaterialInfo> mdmMaterialInfoList = dataManager.listMaterialInfos(dataDTO);

        Map<String, MdmMaterialInfo> mdmMaterialInfoMap = convertToMaterialInfoMap(mdmMaterialInfoList);
        contextDTO.setMdmMaterialInfoMap(mdmMaterialInfoMap);
    }

    /**
     * 初始化模具模壳信息
     *
     * @param contextDTO
     */
    private void initMouldInfo(MpRollAdjustContextDTO contextDTO) {
        MdmModelInfo queryVO = new MdmModelInfo();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());

        String cacheKey = dataManager.generateCacheKey(queryVO.getFactoryCode());
        DataDTO dataDTO = dataManager.buildDataDTO(queryVO, cacheKey, Boolean.FALSE);
        List<MdmModelInfo> mdmMouldList = dataManager.listMouldInfos(dataDTO);

        Map<String, String> mdmMouldInfoMap = convertToMouldInfoMap(mdmMouldList);
        contextDTO.setMdmMouldInfoMap(mdmMouldInfoMap);
    }

    /**
     * 初始化模壳台账信息
     *
     * @param contextDTO
     */
    private void initMouldShellInfo(MpRollAdjustContextDTO contextDTO) {
        MdmMouldShellInfo queryVO = new MdmMouldShellInfo();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());

        String cacheKey = dataManager.generateCacheKey(queryVO.getFactoryCode());
        DataDTO dataDTO = dataManager.buildDataDTO(queryVO, cacheKey, Boolean.FALSE);
        List<MdmMouldShellInfo> mdmMouldShellList = dataManager.listMouldShellInfos(dataDTO);
        contextDTO.setMdmMouldShellInfoList(mdmMouldShellList);
    }

    /**
     * 初始化胶囊卡盘台账信息
     *
     * @param contextDTO
     */
    private void initCapsuleChuckInfo(MpRollAdjustContextDTO contextDTO) {
        MdmCapsuleChuck queryVO = new MdmCapsuleChuck();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());

        String cacheKey = dataManager.generateCacheKey(queryVO.getFactoryCode());
        DataDTO dataDTO = dataManager.buildDataDTO(queryVO, cacheKey, Boolean.FALSE);
        List<MdmCapsuleChuck> mdmCapsuleChuckList = dataManager.listCapsuleChuckInfos(dataDTO);
        contextDTO.setMdmCapsuleChuckList(mdmCapsuleChuckList);
    }


    /**
     * 初始化sku日硫化产能
     *
     * @param contextDTO
     */
    private void initSkuLhCapacity(MpRollAdjustContextDTO contextDTO) {
        MdmSkuLhCapacity queryVO = new MdmSkuLhCapacity();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());

        String cacheKey = dataManager.generateCacheKey(queryVO.getFactoryCode());
        DataDTO dataDTO = dataManager.buildDataDTO(queryVO, cacheKey, Boolean.FALSE);
        List<MdmSkuLhCapacity> mdmSkuLhCapacityList = dataManager.listSkuLhCapacitys(dataDTO);

        Map<String, MdmSkuLhCapacity> mdmSkuLhCapacityMap = convertToSkuLhCapacityMap(mdmSkuLhCapacityList);
        contextDTO.setMdmSkuLhCapacityMap(mdmSkuLhCapacityMap);
    }


    /**
     * 初始化试制量试计划
     *
     * @param contextDTO
     */
    private void initTrialPlan(MpRollAdjustContextDTO contextDTO) {
        MpTrialPlan queryVO = new MpTrialPlan();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());
        queryVO.setYear(contextDTO.getMpYear());
        queryVO.setMonth(contextDTO.getMpMonth());

        DataDTO dataDTO = dataManager.buildDataDTO(queryVO);
        List<MpTrialPlan> mpTrialPlanList = dataManager.listTrialPlans(dataDTO);

        contextDTO.setMpTrialPlanList(mpTrialPlanList);
        Map<String, List<MpTrialPlan>> mpTrialPlanMap = convertToTrialPlanMap(mpTrialPlanList);
        contextDTO.setMpTrialPlanMap(mpTrialPlanMap);
    }


    /**
     * 初始化销售订单池
     *
     * @param contextDTO
     */
    private void initSaleOrderPool(MpRollAdjustContextDTO contextDTO) {
        SalesOrderPool queryVO = new SalesOrderPool();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());
        // 订单状态，0-关单，1-正常
//        queryVO.setOrderStatus(ApsConstant.TRUE);

        DataDTO dataDTO = dataManager.buildDataDTO(queryVO);
        List<SalesOrderPool> salesOrderPoolList = dataManager.listSalesOrderPools(dataDTO);

        contextDTO.setSalesOrderPoolList(salesOrderPoolList);
    }

    /**
     * 初始上次需求计划版本
     * @param contextDTO
     */
    private void initDemandPlanByLastMonthPlanVersion(MpRollAdjustContextDTO contextDTO) {
        MpFactoryProductionVersion queryVo = new MpFactoryProductionVersion();
        queryVo.setFactoryCode(contextDTO.getFactoryCode());
        queryVo.setYear(contextDTO.getMpYear());
        queryVo.setMonth(contextDTO.getMpMonth());
        queryVo.setMonthPlanVersion(contextDTO.getAdjustMonthPlanVersion());
        List<DpDemandPlan> dpDemandPlanList = dpDemandPlanService.findDemandPlanByMonthPlanVersion(queryVo);
        contextDTO.setDpLastDemandPlanList(dpDemandPlanList);
    }

    /**
     * 初始化排产版本
     *
     * @param contextDTO
     */
    @Override
    public void initVersion(MpRollAdjustContextDTO contextDTO) {
        if (PubUtil.isNotEmpty(contextDTO.getFactoryProductionVersionList())) {
            return;
        }
        // 查询排产版本
        MpFactoryProductionVersion version = new MpFactoryProductionVersion();
        version.setFactoryCode(contextDTO.getFactoryCode());
        version.setPlanType("01");
        version.setYear(contextDTO.getMpYear());
        version.setMonth(contextDTO.getMpMonth());
        version.setIsFinal(ApsConstant.TRUE);

        DataDTO dataDTO = dataManager.buildDataDTO(version);
        List<MpFactoryProductionVersion> versionList = dataManager.listVersions(dataDTO);
        contextDTO.setFactoryProductionVersionList(versionList);
    }

    /**
     * 初始化月度生产计划
     *
     * @param contextDTO
     */
    private void initMonthPlan(MpRollAdjustContextDTO contextDTO) {
        if (PubUtil.isNotEmpty(contextDTO.getFactoryMonthPlanProdFinalList())) {
            return;
        }
        // 查询月度生产计划
        MpFactoryProductionVersion factoryProductionVersion = getIsFinalVersion(contextDTO);
        if (factoryProductionVersion == null) {
            return;
        }
        FactoryMonthPlanProductionFinalResult queryVO = new FactoryMonthPlanProductionFinalResult();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());
        queryVO.setYear(contextDTO.getMpYear());
        queryVO.setMonth(contextDTO.getMpMonth());
        //queryVO.setMonthPlanVersion(factoryProductionVersion.getMonthPlanVersion());

        DataDTO dataDTO = dataManager.buildDataDTO(queryVO);
        List<FactoryMonthPlanProductionFinalResult> factoryMonthPlanProdFinalList = dataManager.listMonthPlans(dataDTO);

        List<FactoryMonthPlanFinalAdjustVo> resultList = BeanUtil.copyToList(factoryMonthPlanProdFinalList, FactoryMonthPlanFinalAdjustVo.class);
        contextDTO.setFactoryMonthPlanProdFinalList(resultList);
        if (PubUtil.isNotEmpty(resultList) && StringUtils.isEmpty(contextDTO.getProductionVersion())) {
            // 月度计划排产版本
            contextDTO.setProductionVersion(resultList.get(0).getProductionVersion());
        }


        //检查是否跨月,初始上月月度计划
        if (checkCrossMonth(contextDTO)) {
            initLastMonthPlan(contextDTO);
        }
    }

    /**
     * 检查是否跨月
     *
     * @param contextDTO
     * @return
     */
    protected boolean checkCrossMonth(MpRollAdjustContextDTO contextDTO) {
        Date nextMonthDate = DateUtils.addMonths(DateUtils.getNowDate(), 1);
        return contextDTO.getMpMonth() == DateUtils.getMonth(nextMonthDate);
    }

    /**
     * 初始化上月月度生产计划
     *
     * @param contextDTO
     */
    private void initLastMonthPlan(MpRollAdjustContextDTO contextDTO) {
        if (PubUtil.isNotEmpty(contextDTO.getLastFactoryMonthPlanProdFinalList())) {
            return;
        }
        // 查询上月月度生产计划
        FactoryMonthPlanProductionFinalResult queryVO = new FactoryMonthPlanProductionFinalResult();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());
        queryVO.setYear(contextDTO.getCurrentYear());
        queryVO.setMonth(contextDTO.getCurrentMonth());

        DataDTO dataDTO = dataManager.buildDataDTO(queryVO);
        List<FactoryMonthPlanProductionFinalResult> lastFactoryMonthPlanProdFinalList = dataManager.listMonthPlans(dataDTO);

        List<FactoryMonthPlanFinalAdjustVo> resultList = BeanUtil.copyToList(lastFactoryMonthPlanProdFinalList, FactoryMonthPlanFinalAdjustVo.class);
        contextDTO.setLastFactoryMonthPlanProdFinalList(resultList);
        if (PubUtil.isNotEmpty(resultList) && StringUtils.isEmpty(contextDTO.getLastProductionVersion())) {
            // 上月月度计划排产版本
            contextDTO.setLastProductionVersion(resultList.get(0).getProductionVersion());
        }
    }

    /**
     * 构建月度生产计划条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildMonthPlanCondition(LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper, FactoryMonthPlanProductionFinalResult queryVO) {
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getFactoryCode()), FactoryMonthPlanProductionFinalResult::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(queryVO.getYearMonth() != null, FactoryMonthPlanProductionFinalResult::getYearMonth, queryVO.getYearMonth());
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getMonthPlanVersion()), FactoryMonthPlanProductionFinalResult::getMonthPlanVersion, queryVO.getMonthPlanVersion());
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getProductionVersion()), FactoryMonthPlanProductionFinalResult::getProductionVersion, queryVO.getProductionVersion());
        queryWrapper.eq(FactoryMonthPlanProductionFinalResult::getIsDelete, YesOrNoEnum.NO.getValue());
        queryWrapper.eq(queryVO.getYear() != null, FactoryMonthPlanProductionFinalResult::getYear, queryVO.getYear());
        queryWrapper.eq(queryVO.getMonth() != null, FactoryMonthPlanProductionFinalResult::getMonth, queryVO.getMonth());
    }

    /**
     * 构建调整结果条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildAdjustResultCondition(LambdaQueryWrapper<MpAdjustResult> queryWrapper, MpAdjustResult queryVO) {
        queryWrapper.eq(queryVO.getYear() != null, MpAdjustResult::getYear, queryVO.getYear());
        queryWrapper.eq(queryVO.getMonth() != null, MpAdjustResult::getMonth, queryVO.getMonth());
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getFactoryCode()), MpAdjustResult::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getVersion()), MpAdjustResult::getVersion, queryVO.getVersion());
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getProductionVersion()), MpAdjustResult::getProductionVersion, queryVO.getProductionVersion());
        queryWrapper.eq(MpAdjustResult::getIsDelete, YesOrNoEnum.NO.getValue());
    }

    /**
     * 构建调整明细条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    protected void buildAdjustDetailCondition(LambdaQueryWrapper<MpAdjustStructureIn> queryWrapper, MpAdjustStructureIn queryVO) {
        queryWrapper.eq(queryVO.getYear() != null, MpAdjustStructureIn::getYear, queryVO.getYear());
        queryWrapper.eq(queryVO.getMonth() != null, MpAdjustStructureIn::getMonth, queryVO.getMonth());
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getVersion()), MpAdjustStructureIn::getVersion, queryVO.getVersion());
        queryWrapper.eq(MpAdjustStructureIn::getIsDelete, YesOrNoEnum.NO.getValue());
    }

    /**
     * 构建调整明细条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    protected void buildAdjustDetailCondition(LambdaQueryWrapper<MpAdjustStructureOut> queryWrapper, MpAdjustStructureOut queryVO) {
        queryWrapper.eq(queryVO.getYear() != null, MpAdjustStructureOut::getYear, queryVO.getYear());
        queryWrapper.eq(queryVO.getMonth() != null, MpAdjustStructureOut::getMonth, queryVO.getMonth());
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getVersion()), MpAdjustStructureOut::getVersion, queryVO.getVersion());
        queryWrapper.eq(MpAdjustStructureOut::getIsDelete, YesOrNoEnum.NO.getValue());
    }

    /**
     * 校验
     *
     * @param contextDTO
     */
    private void check(MpRollAdjustContextDTO contextDTO) {
        // 校验年月是否为空
        Assert.isFalse(contextDTO.getMpYear() == null || contextDTO.getMpMonth() == null, I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.yearMonthEmpty"));
        // 年月
        contextDTO.setYearMonth(Integer.valueOf(String.format("%d%02d", contextDTO.getMpYear(), contextDTO.getMpMonth())));
        // 工厂编码
        contextDTO.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        // 获取定稿的排产版本
        MpFactoryProductionVersion factoryProductionVersion = getIsFinalVersion(contextDTO);
        // 月度生产计划还未定稿，抛出异常
        Assert.isFalse(factoryProductionVersion == null, () -> {
            String msg = StrUtil.format(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.notFinalMonthPlan"),
                    contextDTO.getYearMonth());
            return new BusinessException(msg);
        });
        // 获取定稿的月度计划
        FactoryMonthPlanFinalAdjustVo monthPlan = getIsFinalMonthPlan(contextDTO);
        if (monthPlan != null) {
            // 排产版本号
            contextDTO.setProductionVersion(monthPlan.getProductionVersion());
            // 需求计划版本
            contextDTO.setMonthPlanVersion(monthPlan.getMonthPlanVersion());
            // 最新的需求计划版本
            contextDTO.setAdjustMonthPlanVersion(monthPlan.getLastMonthPlanVersion());
        }
        contextDTO.setMessageMap(new HashMap<>());
        // 特殊规则检查
        specialCheck(contextDTO);

        // 设置周程滚动参数
        contextDTO.setParamMap(mpAdjustStructureInService.getMpWeekAdjustParam(contextDTO.getFactoryCode(), factoryProductionVersion.getProductTypeCode()));

    }

    /**
     * 特殊规则初始化（由子类实现）
     *
     * @param contextDTO
     */
    public abstract void specialInit(MpRollAdjustContextDTO contextDTO);

    /**
     * 特殊规则检查（由子类实现）
     *
     * @param contextDTO
     */
    public abstract void specialCheck(MpRollAdjustContextDTO contextDTO);

    /**
     * 获取定稿的月度计划
     *
     * @param contextDTO
     * @return
     */
    private FactoryMonthPlanFinalAdjustVo getIsFinalMonthPlan(MpRollAdjustContextDTO contextDTO) {
        if (PubUtil.isEmpty(contextDTO.getFactoryMonthPlanProdFinalList())) {
            // 初始化月度生产计划
            initMonthPlan(contextDTO);
        }
        List<FactoryMonthPlanFinalAdjustVo> monthPlanList = contextDTO.getFactoryMonthPlanProdFinalList();
        FactoryMonthPlanFinalAdjustVo monthPlan = null;
        if (PubUtil.isNotEmpty(monthPlanList)) {
            monthPlan = monthPlanList.get(0);
        }
        return monthPlan;
    }

    /**
     * 获取定稿的排产版本
     *
     * @param contextDTO
     * @return
     */
    private MpFactoryProductionVersion getIsFinalVersion(MpRollAdjustContextDTO contextDTO) {
        if (PubUtil.isEmpty(contextDTO.getFactoryProductionVersionList())) {
            // 初始化排产版本
            initVersion(contextDTO);
        }
        List<MpFactoryProductionVersion> sourceVersionList = contextDTO.getFactoryProductionVersionList();
        if (PubUtil.isEmpty(sourceVersionList)) {
            return null;
        }
        // 筛选：定稿的排产版本
        MpFactoryProductionVersion factoryProductionVersion = sourceVersionList.stream()
                .filter(item -> ApsConstant.TRUE.equals(item.getIsFinal()))
                .findFirst()
                .orElse(null);
        return factoryProductionVersion;
    }

    /**
     * 构建调整明细
     *
     * @param contextDTO
     * @return
     */
    protected List<MpAdjustDetailVo> buildAdjustDetailList(MpRollAdjustContextDTO contextDTO) {
        // 销售订单池列表
        List<SalesOrderPool> salesOrderPoolList = contextDTO.getSalesOrderPoolList();
        // 月度生产计划列表
        List<FactoryMonthPlanFinalAdjustVo> monthPlanProdList = contextDTO.getFactoryMonthPlanProdFinalList();
        // 结果集初始化
        List<MpAdjustDetailVo> resultList = new ArrayList<>();
        // 列表为空则直接返回空结果
        if (PubUtil.isEmpty(salesOrderPoolList)) {
            return resultList;
        }

        // 按物料编码分组，合并同分组下的成型机编码（逗号分隔）
        List<FactoryMonthPlanFinalAdjustVo> mergeMonthPlanProdList = mergeMonthPlanProdList(monthPlanProdList);
        // 生产计划列表按照物料编码进行分组
        Map<String, List<FactoryMonthPlanFinalAdjustVo>> monthPlanMap = mergeMonthPlanProdList.stream()
                .collect(Collectors.groupingBy(e -> e.getMaterialCode() + BusiConstant.WeekRollAdjust.SPLIT_GROUP_KEY + e.getConstructionStage()));
        // 遍历销售订单列表，匹配生产计划
        for (SalesOrderPool salesOrder : salesOrderPoolList) {
            String materialCode = salesOrder.getOriMaterialCode();
            // 物料编码为空则跳过
            if (StringUtils.isEmpty(materialCode)) {
                continue;
            }
            matchMonthPlanList(contextDTO, resultList, materialCode, ConstructionStageEnum.FORMAL_PRODUCTION.getStage(), monthPlanMap,
                    Convert.toInt(salesOrder.getOrdQty(), 0), ApsConstant.FALSE, salesOrder.getId());
        }
        return resultList;
    }

    /**
     * 构建结构内调整明细（试制量试计划）
     *
     * @param contextDTO
     * @return
     */
    protected List<MpAdjustDetailVo> buildAdjustDetailByTrialList(MpRollAdjustContextDTO contextDTO) {
        // 试制量试计划列表
        List<MpTrialPlan> trialPlanList = contextDTO.getMpTrialPlanList();
        // 月度生产计划列表
        List<FactoryMonthPlanFinalAdjustVo> monthPlanProdList = contextDTO.getFactoryMonthPlanProdFinalList();
        // 结果集初始化
        List<MpAdjustDetailVo> resultList = new ArrayList<>();
        // 列表为空则直接返回空结果
        if (PubUtil.isEmpty(trialPlanList)) {
            return resultList;
        }
        // 生产计划列表按照物料编码进行分组
        Map<String, List<FactoryMonthPlanFinalAdjustVo>> monthPlanMap = monthPlanProdList.stream()
                .collect(Collectors.groupingBy(e -> e.getMaterialCode() + BusiConstant.WeekRollAdjust.SPLIT_GROUP_KEY + e.getConstructionStage()));
        // 遍历试制量试计划列表，匹配生产计划
        String constructionStage = ConstructionStageEnum.TRIAL_PRODUCTION.getStage();
        for (MpTrialPlan trialPlan : trialPlanList) {
            String materialCode = trialPlan.getMaterialCode();
            // 物料编码为空则跳过
            if (StringUtils.isEmpty(materialCode)) {
                continue;
            }

            if (ConstructionStageEnum.MEASUREMENT_FLAG.equals(trialPlan.getTrialStatus())) {
                constructionStage = ConstructionStageEnum.MEASUREMENT.getStage();
            }
            matchMonthPlanList(contextDTO, resultList, materialCode, constructionStage, monthPlanMap,
                    Convert.toInt(trialPlan.getTrialQty(), 0), ApsConstant.TRUE, trialPlan.getId());
        }
        return resultList;
    }

    /**
     * 构建结构调整明细（月度计划有，无订单）
     *
     * @param contextDTO
     * @return
     */
    protected List<MpAdjustDetailVo> buildAdjustDetailByMonthPlanList(MpRollAdjustContextDTO contextDTO) {
        // 销售订单池列表
        List<SalesOrderPool> salesOrderPoolList = contextDTO.getSalesOrderPoolList();
        // 试制量试计划列表
        List<MpTrialPlan> trialPlanList = contextDTO.getMpTrialPlanList();
        // 月度生产计划列表
        List<FactoryMonthPlanFinalAdjustVo> monthPlanProdList = contextDTO.getFactoryMonthPlanProdFinalList();
        // 结果集初始化
        List<MpAdjustDetailVo> resultList = new ArrayList<>();
        // 列表为空则直接返回空结果
        if (PubUtil.isEmpty(monthPlanProdList)) {
            return resultList;
        }

        // 销售订单池列表
        Set<String> salesOrderSet = salesOrderPoolList.stream()
                .map(SalesOrderPool::getOriMaterialCode)
                .collect(Collectors.toSet());
        // 试制量试计划列表
        Set<String> trialPlanSet = trialPlanList.stream()
                .map(MpTrialPlan::getMaterialCode)
                .collect(Collectors.toSet());
        // 遍历月度生产计划
        for (FactoryMonthPlanFinalAdjustVo monthPlan : monthPlanProdList) {
            String materialCode = monthPlan.getMaterialCode();
            // 物料编码为空则跳过
            if (StringUtils.isEmpty(materialCode)) {
                continue;
            }
            // 存在订单或者试制量试跳过
            if (salesOrderSet.contains(materialCode) || trialPlanSet.contains(materialCode)) {
                continue;
            }
            // 创建基础通用字段
            MpAdjustDetailVo adjustDetailVo = createBaseMpAdjustDetailVo(contextDTO, materialCode, 0, ApsConstant.FALSE);
            // 设置月度生产计划关联的字段
            setPlanRelatedFields(contextDTO, adjustDetailVo, monthPlan, monthPlan.getId());
            // 调整前净需求量
            setPreviousNetQty(adjustDetailVo, monthPlan, contextDTO.getAdjustDay());
            // 调整明细来源
            adjustDetailVo.setAdjustItemSource(AdjustItemSourceEnum.MONTH_PLAN.getCode());
            // 添加到结果集
            resultList.add(adjustDetailVo);
        }
        return resultList;
    }

    protected void matchMonthPlanList(MpRollAdjustContextDTO contextDTO, List<MpAdjustDetailVo> resultList,
                                      String materialCode, String constructureStage, Map<String, List<FactoryMonthPlanFinalAdjustVo>> monthPlanMap,
                                      Integer ordQty, String isTrial, Long busiId) {
        // 根据物料编码获取对应的月度生产计划列表
        List<FactoryMonthPlanFinalAdjustVo> matchMonthPlanProdList = monthPlanMap.get(materialCode + BusiConstant.WeekRollAdjust.SPLIT_GROUP_KEY + constructureStage);
        if (PubUtil.isEmpty(matchMonthPlanProdList)) {
            // 创建基础通用字段
            MpAdjustDetailVo emptyAdjustVo = createBaseMpAdjustDetailVo(contextDTO, materialCode, ordQty, isTrial);
            // 设置月度生产计划关联的字段
            setPlanRelatedFields(contextDTO, emptyAdjustVo, null, busiId);
            // 调整前净需求量
            setPreviousNetQty(emptyAdjustVo, null, contextDTO.getAdjustDay());
            // 设置调整明细来源
            setAdjustItemSource(emptyAdjustVo);
            // 添加到结果集
            resultList.add(emptyAdjustVo);
            return;
        }
        // 月度生产计划列表不为空时，执行逻辑
        for (FactoryMonthPlanFinalAdjustVo monthPlan : matchMonthPlanProdList) {
            // 创建基础通用字段
            MpAdjustDetailVo adjustDetailVo = createBaseMpAdjustDetailVo(contextDTO, materialCode, ordQty, isTrial);
            // 设置月度生产计划关联的字段
            setPlanRelatedFields(contextDTO, adjustDetailVo, monthPlan, busiId);
            // 调整前净需求量
            setPreviousNetQty(adjustDetailVo, monthPlan, contextDTO.getAdjustDay());
            // 设置调整明细来源
            setAdjustItemSource(adjustDetailVo);
            // 添加到结果集
            resultList.add(adjustDetailVo);
        }
    }

    /**
     * 设置调整明细来源
     *
     * @param adjustDetailVo
     */
    private void setAdjustItemSource(MpAdjustDetailVo adjustDetailVo) {
        if (ApsConstant.TRUE.equals(adjustDetailVo.getIsTrial())) {
            adjustDetailVo.setAdjustItemSource(AdjustItemSourceEnum.TRIAL.getCode());
        } else {
            adjustDetailVo.setAdjustItemSource(AdjustItemSourceEnum.SALE_POOL.getCode());
        }
    }

    /**
     * 创建基础通用字段
     *
     * @param contextDTO
     * @param materialCode
     * @param ordQty
     * @param isTrial
     * @return
     */
    protected MpAdjustDetailVo createBaseMpAdjustDetailVo(MpRollAdjustContextDTO contextDTO, String materialCode,
                                                          Integer ordQty, String isTrial) {
        MpAdjustDetailVo adjustDetailVo = new MpAdjustDetailVo();
        // 基础通用字段赋值
        adjustDetailVo.setFactoryCode(contextDTO.getFactoryCode());
        adjustDetailVo.setYear(contextDTO.getMpYear());
        adjustDetailVo.setMonth(contextDTO.getMpMonth());
        adjustDetailVo.setVersion(contextDTO.getVersion());

        adjustDetailVo.setProductionVersion(contextDTO.getProductionVersion());
        adjustDetailVo.setMonthPlanVersion(contextDTO.getMonthPlanVersion());
        adjustDetailVo.setOrdQty(ordQty);
        adjustDetailVo.setMaterialCode(materialCode);
        adjustDetailVo.setIsTrial(isTrial);
        adjustDetailVo.setHasSpecialMaterial(ApsConstant.FALSE);
        return adjustDetailVo;
    }

    /**
     * 设置关联的字段
     *
     * @param contextDTO
     * @param adjustDetailVo
     * @param monthPlan
     * @param busiId
     */
    protected void setPlanRelatedFields(MpRollAdjustContextDTO contextDTO, MpAdjustDetailVo adjustDetailVo, FactoryMonthPlanFinalAdjustVo monthPlan, Long busiId) {
        // 物料编码
        String materialCode = adjustDetailVo.getMaterialCode();
        // 试制量试计划
        Map<String, List<MpTrialPlan>> mpTrialPlanMap = contextDTO.getMpTrialPlanMap();
        List<MpTrialPlan> trialPlanList = MapUtils.getObject(mpTrialPlanMap, materialCode, new ArrayList<>());
        MpTrialPlan trialPlan = new MpTrialPlan();
        // SKU与施工（示方书）关系
        List<MdmSkuConstructionRef> mdmSkuConstructionRefList = contextDTO.getMdmSkuConstructionRefList();
        // 产品状态
        String productStatus = ConstructionStageEnum.FORMAL_FLAG;
        if (ApsConstant.TRUE.equals(adjustDetailVo.getIsTrial())) {
            // 获取试制量试
            trialPlan = getMpTrialPlan(trialPlanList, busiId);
            productStatus = trialPlan.getTrialStatus();
        }
        // 物料信息
        Map<String, MdmMaterialInfo> mdmMaterialInfoMap = contextDTO.getMdmMaterialInfoMap();
        MdmMaterialInfo materialInfo = MapUtils.getObject(mdmMaterialInfoMap, materialCode, new MdmMaterialInfo());
        // 结构名称
        adjustDetailVo.setStructureName(materialInfo.getStructureName());
        // 按物料编码+产品状态优先级匹配SKU与示方书记录
        MdmSkuConstructionRef skuConstructionRef = matchSkuConstruction(materialCode, productStatus, mdmSkuConstructionRefList);

        // 结构名称
        String structureName = contextDTO.getStructureName();
        if (StringUtils.isEmpty(structureName) || StringUtils.equals(structureName, materialInfo.getStructureName())) {
            // 检查SKU与示方书关系
            checkSkuConstructionRef(contextDTO, skuConstructionRef, materialCode);
        }

        if (skuConstructionRef == null) {
            skuConstructionRef = new MdmSkuConstructionRef();
        }

        // 胎胚号
        adjustDetailVo.setEmbryoCode(skuConstructionRef.getEmbryoCode());

        // 月计划结构转产
        Map<String, List<MpStructureAllocation>> structureAllocationMap = contextDTO.getStructureAllocationMap();
        List<MpStructureAllocation> structureAllocationList = MapUtils.getObject(structureAllocationMap, adjustDetailVo.getStructureName(), new ArrayList<>());
        // 排产机台,多个机台用逗号分隔
        adjustDetailVo.setScheduledMachines(getCxMachineCodes(structureAllocationList));
        if (monthPlan == null) {
            // SKU日硫化产能
            Map<String, MdmSkuLhCapacity> mdmSkuLhCapacityMap = contextDTO.getMdmSkuLhCapacityMap();
            MdmSkuLhCapacity skuLhCapacity = MapUtils.getObject(mdmSkuLhCapacityMap, materialCode, new MdmSkuLhCapacity());

            // 无月度生产计划时，返回
            adjustDetailVo.setIsSkuAdd(ApsConstant.TRUE);
            adjustDetailVo.setMesMaterialCode(materialInfo.getMesMaterialCode());
            adjustDetailVo.setMaterialDesc(materialInfo.getMaterialDesc());
            adjustDetailVo.setProductTypeCode(materialInfo.getProductTypeCode());
            adjustDetailVo.setBrand(materialInfo.getBrand());
            adjustDetailVo.setSpecifications(materialInfo.getSpecifications());
            adjustDetailVo.setMainPattern(materialInfo.getMainPattern());
            adjustDetailVo.setPattern(materialInfo.getPattern());
            adjustDetailVo.setProductCategory(materialInfo.getProductCategory());
            adjustDetailVo.setProSize(materialInfo.getProSize());
            adjustDetailVo.setDayVulcanizationQty(Convert.toInt(skuLhCapacity.getStandardCapacity(), 0) / 2);
            adjustDetailVo.setCuringTime(skuLhCapacity.getVulcanizationTime());
            adjustDetailVo.setEmbryoCode(skuConstructionRef.getEmbryoCode());
            adjustDetailVo.setMainMaterialDesc(skuConstructionRef.getMainMaterialDesc());
            adjustDetailVo.setProductStatus(productStatus);
            adjustDetailVo.setConstructionStage(ConstructionStageEnum.FORMAL_PRODUCTION.getStage());
            // 试制量制关联字段设置
            if (ApsConstant.TRUE.equals(adjustDetailVo.getIsTrial())) {
                // 施工阶段
                if (ConstructionStageEnum.MEASUREMENT_FLAG.equals(trialPlan.getTrialStatus())) {
                    adjustDetailVo.setConstructionStage(ConstructionStageEnum.MEASUREMENT.getStage());
                } else if (ConstructionStageEnum.TRIAL_FLAG.equals(trialPlan.getTrialStatus())) {
                    adjustDetailVo.setConstructionStage(ConstructionStageEnum.TRIAL_PRODUCTION.getStage());
                }

                // 产品状态
                adjustDetailVo.setProductStatus(productStatus);
                // 紧急程度
                adjustDetailVo.setUrgencyType(trialPlan.getUrgencyType());
                // 制造示方书号
                adjustDetailVo.setEmbryoNo(trialPlan.getEmbryoNo());
                // 试制量试ID
                adjustDetailVo.setTrialPlanId(Convert.toStr(trialPlan.getId(), null));
            }
            // 检查SKU的产品状态与【SKU与示方书】匹配到的产品状态是否一致
            checkSkuTypeAndProductStatus(contextDTO, adjustDetailVo, skuConstructionRef, materialCode);
            return;
        }
        // 有月度生产计划时，赋值关联字段
        adjustDetailVo.setIsSkuAdd(ApsConstant.FALSE);
        // 试制量试都设置为新增SKU
        if (ApsConstant.TRUE.equals(adjustDetailVo.getIsTrial())) {
            adjustDetailVo.setIsSkuAdd(ApsConstant.TRUE);
        }
        adjustDetailVo.setMesMaterialCode(monthPlan.getMesMaterialCode());
        adjustDetailVo.setMaterialDesc(monthPlan.getMaterialDesc());
        adjustDetailVo.setProductTypeCode(monthPlan.getProductTypeCode());
        adjustDetailVo.setProductStatus(productStatus);
        adjustDetailVo.setEmbryoCode(skuConstructionRef.getEmbryoCode());
        adjustDetailVo.setMainMaterialDesc(monthPlan.getMainMaterialDesc());
        adjustDetailVo.setConstructionStage(monthPlan.getConstructionStage());
        adjustDetailVo.setBrand(monthPlan.getBrand());
        adjustDetailVo.setProSize(monthPlan.getProSize());
        adjustDetailVo.setSpecifications(monthPlan.getSpecifications());
        adjustDetailVo.setMainPattern(monthPlan.getMainPattern());
        adjustDetailVo.setPattern(monthPlan.getPattern());
        adjustDetailVo.setMouldCavityQty(monthPlan.getMouldCavityQty());
        adjustDetailVo.setTypeBlockQty(monthPlan.getTypeBlockQty());
        adjustDetailVo.setDayVulcanizationQty(monthPlan.getDayVulcanizationQty());
        adjustDetailVo.setCuringTime(monthPlan.getCuringTime());
        adjustDetailVo.setProductCategory(monthPlan.getProductCategory());
        // 制造示方书号
        adjustDetailVo.setEmbryoNo(monthPlan.getEmbryoNo());
        // 试制量制关联字段设置
        if (ApsConstant.TRUE.equals(adjustDetailVo.getIsTrial())) {
            // 紧急程度
            adjustDetailVo.setUrgencyType(trialPlan.getUrgencyType());
            // 施工阶段
            if (ConstructionStageEnum.MEASUREMENT_FLAG.equals(trialPlan.getTrialStatus())) {
                adjustDetailVo.setConstructionStage(ConstructionStageEnum.MEASUREMENT.getStage());
            } else if (ConstructionStageEnum.TRIAL_FLAG.equals(trialPlan.getTrialStatus())) {
                adjustDetailVo.setConstructionStage(ConstructionStageEnum.TRIAL_PRODUCTION.getStage());
            }
            // 试制量试ID
            adjustDetailVo.setTrialPlanId(Convert.toStr(trialPlan.getId(), null));
        }
    }


    /**
     * 发送消息
     *
     * @param templateCode 消息模板编码（对应MsgTemplateEnums的code）
     * @param msgTypeCode  消息类型编码（对应MsgTypeEnums的code）
     * @param msgContent   消息内容
     */
    public void sendMessage(String templateCode, String msgTypeCode, String msgContent) {
        // 核心参数为空时直接返回
        if (StringUtils.isEmpty(templateCode) || StringUtils.isEmpty(msgTypeCode) || StringUtils.isEmpty(msgContent)) {
            log.warn("消息发送失败：核心参数为空！templateCode={}, msgTypeCode={}, msgContent={}",
                    templateCode, msgTypeCode, msgContent);
            return;
        }

        // 构建消息上下文
        MessageContext context = messageServiceAdapter.buildMessageContext(
                null,
                null,
                null,
                null,
                null,
                null,
                SecurityUtils.getUsername(),
                null
        );

        // 发送消息
        messageServiceAdapter.sendBatchMessage(
                templateCode,
                msgTypeCode,
                msgContent,
                null,
                null,
                context
        );
    }


    /**
     * 检查SKU的产品状态与【SKU与示方书】匹配到的产品状态是否一致
     *
     * @param contextDTO
     * @param adjustDetailVo
     * @param skuConstructionRef
     * @param materialCode
     */
    protected void checkSkuTypeAndProductStatus(MpRollAdjustContextDTO contextDTO, MpAdjustDetailVo adjustDetailVo, MdmSkuConstructionRef skuConstructionRef,
                                                String materialCode) {

        String productStatus = adjustDetailVo.getProductStatus();
        String matchProductStatus = skuConstructionRef.getTrialStatus();

        if (!StringUtils.equals(productStatus, matchProductStatus)) {
            Map<String, List<String>> messageMap = Optional.ofNullable(contextDTO.getMessageMap())
                    .orElseGet(HashMap::new);
            List<String> warnMsgList = messageMap.computeIfAbsent(ApsConstant.APS_STRING_0, k -> new ArrayList<>());
            contextDTO.setMessageMap(messageMap);
            String warnMsg = StrUtil.format(
                    I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.checkSkuTypeAndProductStatus"),
                    materialCode, convertToProductStatusName(productStatus), convertToProductStatusName(matchProductStatus)
            );
            warnMsgList.add(warnMsg);
        }
    }


    /**
     * 根据产品状态转换产品状态名称
     *
     * @param productStatus
     * @return
     */
    public String convertToProductStatusName(String productStatus) {
        if (StringUtils.isEmpty(productStatus)) {
            return "";
        }
        if (ConstructionStageEnum.MEASUREMENT_FLAG.equals(productStatus)) {
            return ConstructionStageEnum.MEASUREMENT.getDesc();
        } else if (ConstructionStageEnum.TRIAL_FLAG.equals(productStatus)) {
            return ConstructionStageEnum.TRIAL_PRODUCTION.getDesc();
        }
        return ConstructionStageEnum.FORMAL_PRODUCTION.getDesc();
    }


    /**
     * 根据施工阶段转换为对应的产品状态标识
     *
     * @param constructionStage
     * @return
     */
    public String convertToProductStatusFlag(String constructionStage) {
        if (StringUtils.isEmpty(constructionStage)) {
            return "";
        }
        if (ConstructionStageEnum.MEASUREMENT.getStage().equals(constructionStage)) {
            return ConstructionStageEnum.MEASUREMENT_FLAG;
        } else if (ConstructionStageEnum.TRIAL_PRODUCTION.getStage().equals(constructionStage)) {
            return ConstructionStageEnum.TRIAL_FLAG;
        }
        return ConstructionStageEnum.FORMAL_FLAG;
    }


    /**
     * 按物料编码+产品状态优先级匹配SKU与示方书记录
     *
     * @param materialCode           物料编码
     * @param trialStatus            产品状态
     * @param skuConstructionRefList SKU与示方书列表
     * @return 匹配到的记录
     */
    private MdmSkuConstructionRef matchSkuConstruction(String materialCode, String trialStatus,
                                                       List<MdmSkuConstructionRef> skuConstructionRefList) {
        // 1、基础参数校验
        if (StringUtils.isEmpty(materialCode)) {
            log.warn("物料编码为空，无法匹配！");
            return null;
        }
        if (StringUtils.isEmpty(trialStatus)) {
            log.warn("产品状态为空，无法匹配！");
            return null;
        }
        if (PubUtil.isEmpty(skuConstructionRefList)) {
            log.warn("SKU与示方书列表为空，无法匹配！");
            return null;
        }
        // 2、过滤出物料编码匹配的所有记录
        List<MdmSkuConstructionRef> materialMatchedList = skuConstructionRefList.stream()
                .filter(item -> Objects.equals(materialCode, item.getMaterialCode()))
                .collect(Collectors.toList());
        // 物料编码无匹配
        if (PubUtil.isEmpty(materialMatchedList)) {
            log.warn("物料编码:{} 未匹配到示方书！", materialCode);
            return null;
        }

        // 4. 根据产品状态确定匹配优先级
        List<String> priorityStatusList;
        switch (trialStatus) {
            case ConstructionStageEnum.FORMAL_FLAG:
                // 正式：优先级
                priorityStatusList = Arrays.asList(ConstructionStageEnum.FORMAL_FLAG, ConstructionStageEnum.TRIAL_FLAG, ConstructionStageEnum.MEASUREMENT_FLAG);
                break;
            case ConstructionStageEnum.TRIAL_FLAG:
                // 量试：优先级
                priorityStatusList = Arrays.asList(ConstructionStageEnum.TRIAL_FLAG, ConstructionStageEnum.MEASUREMENT_FLAG);
                break;
            case ConstructionStageEnum.MEASUREMENT_FLAG:
                // 试制：优先级
                priorityStatusList = Arrays.asList(ConstructionStageEnum.MEASUREMENT_FLAG);
                break;
            default:
                log.warn("产品状态[{}]不合法，仅支持:正式/量试/试制", trialStatus);
                return null;
        }
        // 4、按优先级遍历，返回第一个匹配的记录
        for (String targetStatus : priorityStatusList) {
            for (MdmSkuConstructionRef item : materialMatchedList) {
                if (Objects.equals(targetStatus, item.getTrialStatus())) {
                    return item;
                }
            }
        }
        log.warn("物料编码:{} 所有优先级均未匹配到示方书！", materialCode);
        return null;
    }

    protected MpTrialPlan getMpTrialPlan(List<MpTrialPlan> trialPlanList, Long trialId) {
        if (PubUtil.isEmpty(trialPlanList) || trialId == null) {
            return new MpTrialPlan();
        }
        return trialPlanList.stream()
                .filter(vo -> vo.getId().equals(trialId))
                .findFirst()
                .orElse(new MpTrialPlan());
    }

    /**
     * 获取机台编号（多个以,分隔）
     *
     * @param structureAllocationList
     * @return
     */
    protected String getCxMachineCodes(List<MpStructureAllocation> structureAllocationList) {
        if (PubUtil.isEmpty(structureAllocationList)) {
            return "";
        }
        return structureAllocationList.stream()
                .map(MpStructureAllocation::getCxMachineCode)
                .filter(code -> StringUtils.isNotEmpty(code))
                .collect(Collectors.joining(BusiConstant.WeekRollAdjust.SPLIT_COMMA));
    }


    /**
     * 调整前净需求量（上周）
     *
     * @param adjustDetailVo
     * @param monthPlan
     */
    protected void setPreviousNetQty(MpAdjustDetailVo adjustDetailVo, FactoryMonthPlanFinalAdjustVo monthPlan, int adjustDay) {
        if (ApsConstant.TRUE.equals(adjustDetailVo.getIsTrial())) {
            // 当为试制量试时，设置为空
            adjustDetailVo.setPreviousNetQty(null);
            return;
        }
        if (monthPlan == null) {
            return;
        }
        // 获取上周的周数
        int week = getWeekNumber(adjustDay);
        if (week > 1) {
            week = week - 1;
        }
        Integer previousNetQty = Convert.toInt(monthPlan.getTotalQty(), 0);
        Integer adjustQty = Convert.toInt(monthPlan.getFieldValueByFieldName(BusiConstant.WeekRollAdjust.FIELD_PREFIX_ADJUST_QTY + week), 0);
        if (adjustQty != 0 && week > 0) {
            previousNetQty = adjustQty;
        }
        if (adjustQty == 0 && week > 0) {
            previousNetQty = null;
        }
        adjustDetailVo.setPreviousNetQty(previousNetQty);
    }


    /**
     * 按物料编码分组，合并同分组下的成型机编码（逗号分隔）
     *
     * @param originalList
     * @return 合并后结果集
     */
    private List<FactoryMonthPlanFinalAdjustVo> mergeMonthPlanProdList(List<FactoryMonthPlanFinalAdjustVo> originalList) {
        // 结果集初始化
        List<FactoryMonthPlanFinalAdjustVo> mergedList = new ArrayList<>();
        // 原始列表为空直接返回空结果
        if (PubUtil.isEmpty(originalList)) {
            return mergedList;
        }
        // 按物料编码分组
        Map<String, List<FactoryMonthPlanFinalAdjustVo>> monthPlanGroupMap = originalList.stream()
                .filter(vo -> StringUtils.isNotBlank(vo.getMaterialCode()))
                .collect(Collectors.groupingBy(FactoryMonthPlanFinalAdjustVo::getMaterialCode));
        // 遍历分组，合并成型机编码
        monthPlanGroupMap.forEach((materialCode, list) -> {
            // 收集并合并成型机编码（多个逗号分隔）
            String mergedCxMachineCode = list.stream()
                    .map(FactoryMonthPlanFinalAdjustVo::getCxMachineCode)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .collect(Collectors.joining(","));
            // 构建合并后的月度生产计划
            FactoryMonthPlanFinalAdjustVo mergedVo = new FactoryMonthPlanFinalAdjustVo();
            FactoryMonthPlanFinalAdjustVo firstVo = list.get(0);
            BeanUtil.copyProperties(firstVo, mergedVo, false);
            mergedVo.setMaterialCode(materialCode);
            mergedVo.setCxMachineCode(mergedCxMachineCode);
            // 添加到结果集
            mergedList.add(mergedVo);
        });
        return mergedList;
    }

    /**
     * 生成调整需求计划
     *
     * @param contextDTO
     */
    protected void createAdjustRequire(MpRollAdjustContextDTO contextDTO) {
        DpDemandPlan queryVo = new DpDemandPlan();
        queryVo.setFactoryCode(contextDTO.getFactoryCode());
        queryVo.setYear(contextDTO.getMpYear());
        queryVo.setMonth(contextDTO.getMpMonth());
        queryVo.setMonthPlanVersion(contextDTO.getVersion());
        queryVo.setProductionVersion(contextDTO.getProductionVersion());

        queryVo.setNoDeductRemainQtyFlag(true);
        //queryVo.setStructureName(contextDTO.getStructureName());
        log.info("生成调整需求计划 ==> factoryCode:{} year:{} month:{} monthPlanVersion:{} productionVersion:{} structureName:{}",
                queryVo.getFactoryCode(), queryVo.getYear(), queryVo.getMonth(), queryVo.getMonthPlanVersion(), queryVo.getProductionVersion(),
                queryVo.getStructureName());
        List<DpDemandPlan> dpDemandPlanList = dpDemandPlanService.createAdjustRequire(queryVo);
        // 净需求全量生成，显示按结构
        /*if (!StringUtil.isEmptyWithTrim(contextDTO.getStructureName())){
            dpDemandPlanList = dpDemandPlanList.stream().filter(x->x.getStructureName().equals(contextDTO.getStructureName())).collect(Collectors.toList());
        }*/
        contextDTO.setDpDemandPlanList(dpDemandPlanList);
    }

    /**
     * 计算型腔、活块可用量最大值
     *
     * @param contextDTO
     */
    protected List<DailyMouldAvailabilityResult> calculateMoldCavityInsertMaxValue(MpRollAdjustContextDTO contextDTO) throws Exception {
        LocalDate monthStart = LocalDate.of(contextDTO.getMpYear(), contextDTO.getMpMonth(), ProductionConstant.MONTH_START_DAY);
        return moldCavityInsertMaxValueCalculator.moldCavityInsertMaxValueCalculator(contextDTO.getMpYear(), contextDTO.getMpMonth(),
                contextDTO.getFactoryCode(), com.zlt.aps.mp.engine.utils.DateUtils.getDate(monthStart.with(TemporalAdjusters.lastDayOfMonth())), contextDTO.getAdjustMonthPlanVersion(),true);
    }

    /**
     * 设置型腔、活块数量
     *
     * @param contextDTO
     */
    protected void setMoldCavityInsert(MpRollAdjustContextDTO contextDTO) {
        // 创建计时器
        StopWatch watch = new StopWatch();
        watch.start();
        List<DailyMouldAvailabilityResult> moldCavityInsertMap;
        try {
            // 计算型腔、活块可用量最大值
            moldCavityInsertMap = calculateMoldCavityInsertMaxValue(contextDTO);
        } catch (Exception e) {
            log.error("计算型腔、活块可用量最大值失败! 原因:{}", e.getMessage(), e);
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.calculateMoldCavityInsertMaxValueFail"));
        } finally {
            watch.stop();
        }
        log.info("计算型腔、活块可用量最大值完成 ==> 耗时:{} ms", watch.getLastTaskTimeMillis());

        if (PubUtil.isEmpty(moldCavityInsertMap)) {
            log.warn("计算型腔、活块可用量最大值 ==> 根据工厂:[{}] 年月:[{}] 需求计划版本:[{}] 型腔、活块可用量最大值列表为空，返回", contextDTO.getFactoryCode(),
                    contextDTO.getYearMonth(), contextDTO.getMonthPlanVersion());
            return;
        }
        // 调整明细列表
        List<MpAdjustDetailVo> adjustList = contextDTO.getAdjustDetailList();
        // 型腔可用量（按结构+主花纹分组）
        Map<String, Integer> cavityResults = moldCavityInsertMap.get(0).getCavityResults();
        // 活块可用量（按物料描述分组）
        Map<String, Integer> insertResults = moldCavityInsertMap.get(0).getInsertResults();
        log.info("计算型腔、活块可用量最大值 ==> 型腔可用量:{} 活块可用量:{}", JSONObject.toJSONString(cavityResults), JSONObject.toJSONString(insertResults));
        // 遍历
        for (MpAdjustDetailVo adjust : adjustList) {
            // 设置型腔数量
            String mouldCavityKey = adjust.getStructureName() + adjust.getMainPattern();
            if (cavityResults != null && cavityResults.containsKey(mouldCavityKey)) {
                adjust.setMouldCavityQty(MapUtils.getInteger(cavityResults, mouldCavityKey, 0));
            } else {
                adjust.setMouldCavityQty(0);
            }
            // 设置活块数量
            String typeBlockKey = adjust.getMaterialDesc();
            if (insertResults != null && insertResults.containsKey(typeBlockKey)) {
                adjust.setTypeBlockQty(MapUtils.getInteger(insertResults, typeBlockKey, 0));
            } else {
                adjust.setTypeBlockQty(0);
            }
        }
    }

    /**
     * 设置上次订单数量
     *
     * @param contextDTO
     */
    protected void setLastOrderQty(MpRollAdjustContextDTO contextDTO) {
        // 调整明细列表
        List<MpAdjustDetailVo> adjustList = contextDTO.getAdjustDetailList();
        if (PubUtil.isEmpty(adjustList)) {
            return;
        }
        // 上次需求计划订单
        List<DpDemandPlan> dpLastDemandPlanList = contextDTO.getDpLastDemandPlanList();
        if (PubUtil.isEmpty(dpLastDemandPlanList)) {
            return;
        }
        Map<String, DpDemandPlan> demandPlanMap = dpLastDemandPlanList.stream()
                .collect(Collectors.toMap(
                        DpDemandPlan::getMaterialCode,  // key
                        Function.identity(),            // value 为对象本身
                        (existing, replacement) -> existing // 如果有重复 key，保留旧的
                ));
        // 遍历设置是否特殊材料
        DpDemandPlan dpDemandPlan;
        for (MpAdjustDetailVo adjust : adjustList) {
            if (!ConstructionStageEnum.FORMAL_PRODUCTION.getStage().equals(adjust.getConstructionStage())){
                continue;
            }
            dpDemandPlan = demandPlanMap.get(adjust.getMaterialCode());
            adjust.setPreviousOrderQty(dpDemandPlan == null ? null:dpDemandPlan.getOrderQty());
        }
    }

    /**
     * 设置是否特殊材料
     *
     * @param contextDTO
     */
    protected void setHasSpecialMaterial(MpRollAdjustContextDTO contextDTO) {
        // 调整明细列表
        List<MpAdjustDetailVo> adjustList = contextDTO.getAdjustDetailList();
        if (PubUtil.isEmpty(adjustList)) {
            return;
        }
        // BOM物料消耗明细列表
        List<MdmMaterialConsumeDetail> materialConsumeDetailList = contextDTO.getMdmMaterialConsumeDetailList();
        // 特殊材料清单列表
        List<RawSpecialMaterialRecord> specialMaterialList = contextDTO.getSpecialMaterialList();
        // 遍历设置是否特殊材料
        for (MpAdjustDetailVo adjust : adjustList) {
            boolean hasSpecialMaterial = hasSpecialMaterial(adjust.getEmbryoCode(), materialConsumeDetailList, specialMaterialList);
            adjust.setHasSpecialMaterial(hasSpecialMaterial ? ApsConstant.TRUE : ApsConstant.FALSE);
        }
    }


    /**
     * 设置净需求
     *
     * @param contextDTO
     */
    protected void setCurrentNetQty(MpRollAdjustContextDTO contextDTO) {
        // 创建计时器
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            // 生成调整需求计划
            createAdjustRequire(contextDTO);
        } catch (Exception e) {
            log.error("生成调整需求计划失败! 原因:{}", e.getMessage(), e);
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.createAdjustRequireFail"));
        } finally {
            watch.stop();
        }
        log.info("生成调整需求计划完成 ==> 耗时:{} ms", watch.getLastTaskTimeMillis());

        // 需求计划列表
        List<DpDemandPlan> dpDemandPlanList = contextDTO.getDpDemandPlanList();
        log.warn("设置净需求 ==> 需求计划列表大小：{}", CollUtil.size(dpDemandPlanList));
        if (PubUtil.isEmpty(dpDemandPlanList)) {
            log.warn("设置净需求 ==> 根据工厂:[{}] 年月:[{}] 创建需求计划列表为空，返回", contextDTO.getFactoryCode(),
                    contextDTO.getYearMonth());
            return;
        }

        // 设置调整需求计划版本
        contextDTO.setAdjustMonthPlanVersion(dpDemandPlanList.get(0).getMonthPlanVersion());
        log.warn("设置净需求 ==> 调整需求计划版本：{}", contextDTO.getAdjustMonthPlanVersion());

        // 调整明细列表
        List<MpAdjustDetailVo> adjustList = contextDTO.getAdjustDetailList();
        // 需求计划分组Map
        Map<String, List<DpDemandPlan>> demandPlanMap = convertToDpDemandPlanMap(dpDemandPlanList);
        BigDecimal inventorySalesRatio = BigDecimal.ZERO;
        Integer orderAddDecQty;
        // 遍历计算
        for (MpAdjustDetailVo adjust : adjustList) {
            if (StringUtils.isEmpty(adjust.getMaterialCode())) {
                continue;
            }
            adjust.setLastMonthPlanVersion(contextDTO.getAdjustMonthPlanVersion());
            String materialCode = adjust.getMaterialCode();
            List<DpDemandPlan> demandPlanList = MapUtils.getObject(demandPlanMap, materialCode, new ArrayList<>());
            if (PubUtil.isNotEmpty(demandPlanList)) {
                DpDemandPlan dpDemandPlan = demandPlanList.get(0);
                // 设置排产分类
                adjust.setProductionType(dpDemandPlan.getProductionType());
                // 设置高优先级
                Integer sum = demandPlanList.stream()
                        .filter(e -> e.getHeightQty() != null)
                        .mapToInt(DpDemandPlan::getHeightQty)
                        .sum();
                adjust.setHeightQty(Convert.toInt(sum, 0));
                // 设置中优先级
                sum = demandPlanList.stream()
                        .filter(e -> e.getMidQty() != null)
                        .mapToInt(DpDemandPlan::getMidQty)
                        .sum();
                adjust.setMidQty(Convert.toInt(sum, 0));
                // 设置暂缓订单
                sum = demandPlanList.stream()
                        .filter(e -> e.getPostponeQty() != null)
                        .mapToInt(DpDemandPlan::getPostponeQty)
                        .sum();
                adjust.setPostponeQty(Convert.toInt(sum, 0));
                // 设置周期排产储备
                sum = demandPlanList.stream()
                        .filter(e -> e.getCycleReserveQty() != null)
                        .mapToInt(DpDemandPlan::getCycleReserveQty)
                        .sum();
                adjust.setCycleReserveQty(Convert.toInt(sum, 0));
                // 设置常规储备
                sum = demandPlanList.stream()
                        .filter(e -> e.getConventionReserveQty() != null)
                        .mapToInt(DpDemandPlan::getConventionReserveQty)
                        .sum();
                adjust.setConventionReserveQty(Convert.toInt(sum, 0));

                //关联带出物料优先、结构优先、库存、月均销量、库销比、订单 sandy+ 2026.4.10
                adjust.setScmPriority(dpDemandPlan.getScmPriority());
                adjust.setStructurePriority(dpDemandPlan.getStructurePriority());
                adjust.setStockQty(dpDemandPlan.getStockQty());
                adjust.setAverageSaleQty(dpDemandPlan.getAverageSaleQty());
                adjust.setCurrentOrderQty(dpDemandPlan.getOrderQty());
                if (adjust.getPreviousOrderQty() != null || adjust.getCurrentOrderQty() != null){
                    orderAddDecQty = Convert.toInt(adjust.getCurrentOrderQty(),0) - Convert.toInt(adjust.getPreviousOrderQty(),0);
                    adjust.setOrderAddDecQty(orderAddDecQty);
                }
                if (dpDemandPlan.getAverageSaleQty() > 0) {
                    inventorySalesRatio = BigDecimal.valueOf(dpDemandPlan.getStockQty()).divide(BigDecimal.valueOf(dpDemandPlan.getAverageSaleQty()), 1, RoundingMode.HALF_UP);
                }
                adjust.setInventorySalesRatio(inventorySalesRatio);
            }
            // 试制量试设置净需求为订单量
            if (ApsConstant.TRUE.equals(adjust.getIsTrial())) {
                adjust.setCurrentNetQty(adjust.getOrdQty());
            } else {
                // 汇总排产净需求
                Integer netQtySum = demandPlanList.stream()
                        .filter(e -> e.getNetQty() != null)
                        .mapToInt(DpDemandPlan::getNetQty)
                        .sum();
                adjust.setCurrentNetQty(Convert.toInt(netQtySum, 0));
            }
        }

    }

    /**
     * 判断是否只有常规储备有值，其他字段无值（0或null视为无值）
     *
     * @param adjust
     * @return
     */
    protected boolean isOnlyConventionReserveHasValue(MpAdjustDetailVo adjust) {
        // 试制量试不进行判断，直接返回false
        if (ApsConstant.TRUE.equals(adjust.getIsTrial())) {
            return Boolean.FALSE;
        }

        // 判断Integer是否为无值（null或0）
        Predicate<Integer> isZeroOrNull = num -> num == null || num == 0;

        // 判断其他字段都无值
        boolean otherFieldsAreEmpty = isZeroOrNull.test(adjust.getHeightQty())
                && isZeroOrNull.test(adjust.getMidQty())
                && isZeroOrNull.test(adjust.getPostponeQty())
                && isZeroOrNull.test(adjust.getCycleReserveQty());

        boolean conventionReserveQtyEmpty = isZeroOrNull.test(adjust.getConventionReserveQty());
        boolean isMonthPlan = AdjustItemSourceEnum.MONTH_PLAN.getCode().equals(adjust.getAdjustItemSource()) ? Boolean.TRUE : Boolean.FALSE;

        if (otherFieldsAreEmpty && conventionReserveQtyEmpty && isMonthPlan) {
            return Boolean.FALSE;
        }

        // 只要其他字段都无值，无论常规储备是否有值，都返回true
        return otherFieldsAreEmpty;
    }

    /**
     * 过滤调整明细
     *
     * @param contextDTO
     * @param adjustDetailList
     * @return
     */
    protected void filterAdjustDetailList(MpRollAdjustContextDTO contextDTO,
                                          List<MpAdjustDetailVo> adjustDetailList) {
    }

    /**
     * 将DpDemandPlan转Map
     */
    private Map<String, List<DpDemandPlan>> convertToDpDemandPlanMap(List<DpDemandPlan> dpDemandPlanList) {
        if (PubUtil.isEmpty(dpDemandPlanList)) {
            return Collections.emptyMap();
        }
        return dpDemandPlanList.stream()
                .filter(demandPlan -> demandPlan != null && demandPlan.getMaterialCode() != null)
                .collect(Collectors.groupingBy(DpDemandPlan::getMaterialCode));
    }


    /**
     * 将MdmMonthSurplus转Map
     */
    private Map<String, MdmMonthSurplus> convertToSurplusMap(List<MdmMonthSurplus> surplusList) {
        if (PubUtil.isEmpty(surplusList)) {
            return Collections.emptyMap();
        }
        return surplusList.stream()
                .filter(surplus -> StringUtils.isNotEmpty(surplus.getMaterialCode()))
                .collect(Collectors.toMap(
                        MdmMonthSurplus::getMaterialCode,
                        surplus -> surplus,
                        (existingVal, newVal) -> newVal
                ));
    }

    /**
     * 将MpTrialPlan转Map
     */
    private Map<String, List<MpTrialPlan>> convertToTrialPlanMap(List<MpTrialPlan> trialPlanList) {
        if (PubUtil.isEmpty(trialPlanList)) {
            return Collections.emptyMap();
        }
        return trialPlanList.stream()
                .filter(trialPlan -> StringUtils.isNotEmpty(trialPlan.getMaterialCode()))
                .collect(Collectors.groupingBy(MpTrialPlan::getMaterialCode));
    }

    /**
     * 将MdmSkuConstructionRef转Map
     */
    private Map<String, MdmSkuConstructionRef> convertToSkuConstructionRefMap(List<MdmSkuConstructionRef> skuConstructionRefList) {
        if (PubUtil.isEmpty(skuConstructionRefList)) {
            return Collections.emptyMap();
        }
        return skuConstructionRefList.stream()
                .filter(construction -> StringUtils.isNotEmpty(construction.getMaterialCode()))
                .collect(Collectors.toMap(
                        MdmSkuConstructionRef::getMaterialCode,
                        construction -> construction,
                        (existingVal, newVal) -> newVal
                ));
    }

    /**
     * 将MdmSkuStructureRef转Map
     */
    private Map<String, MdmSkuStructureRef> convertToSkuStructureRefMap(List<MdmSkuStructureRef> skuStructureRefList) {
        if (PubUtil.isEmpty(skuStructureRefList)) {
            return Collections.emptyMap();
        }
        return skuStructureRefList.stream()
                .filter(structure -> StringUtils.isNotEmpty(structure.getMaterialCode()))
                .collect(Collectors.toMap(
                        MdmSkuStructureRef::getMaterialCode,
                        structure -> structure,
                        (existingVal, newVal) -> newVal
                ));
    }

    /**
     * 将MdmMaterialInfo转Map
     */
    private Map<String, MdmMaterialInfo> convertToMaterialInfoMap(List<MdmMaterialInfo> materialInfoList) {
        if (PubUtil.isEmpty(materialInfoList)) {
            return Collections.emptyMap();
        }
        return materialInfoList.stream()
                .filter(material -> StringUtils.isNotEmpty(material.getMaterialCode()))
                .collect(Collectors.toMap(
                        MdmMaterialInfo::getMaterialCode,
                        material -> material,
                        (existingVal, newVal) -> newVal
                ));
    }

    /**
     * 转换模壳标准Map
     * @param mouldShellList
     * @return
     */
    private Map<String, MdmMouldShellInfo> convertToMouldShellMap(List<MdmMouldShellInfo> mouldShellList) {
        if (PubUtil.isEmpty(mouldShellList)) {
            return Collections.emptyMap();
        }
        return mouldShellList.stream()
                .filter(shell -> StringUtils.isNotEmpty(shell.getMouldSetCode()))
                .collect(Collectors.toMap(
                        MdmMouldShellInfo::getMouldSetCode,
                        shell -> shell,
                        (existingVal, newVal) -> newVal
                ));
    }

    /**
     * MdmModelInfo转Map
     */
    private Map<String, String> convertToMouldInfoMap(List<MdmModelInfo> mouldInfoList) {
        if (PubUtil.isEmpty(mouldInfoList)) {
            return Collections.emptyMap();
        }

        return mouldInfoList.stream()
                .filter(info -> info != null
                        && info.getSpecifications() != null
                        && info.getMainPattern() != null
                        && info.getShellStandard() != null)
                .collect(Collectors.groupingBy(
                        info -> getSpecAndMainPatternKey(info.getSpecifications(),info.getMainPattern()),
                        Collectors.mapping(
                                MdmModelInfo::getShellStandard,
                                Collectors.collectingAndThen(
                                        Collectors.toSet(),
                                        set -> String.join(BusiConstant.WeekRollAdjust.SPLIT_COMMA, set)
                                )
                        )
                ));
    }


    /**
     * 获取规格+主花纹Key
     * @param spec
     * @param mainPattern
     * @return
     */
    private String getSpecAndMainPatternKey(String spec, String mainPattern){
        return spec + BusiConstant.WeekRollAdjust.SPLIT_GROUP_KEY + mainPattern;
    }
    /**
     * 将MdmSkuLhCapacity转Map
     */
    private Map<String, MdmSkuLhCapacity> convertToSkuLhCapacityMap(List<MdmSkuLhCapacity> skuLhCapacityList) {
        if (PubUtil.isEmpty(skuLhCapacityList)) {
            return Collections.emptyMap();
        }
        return skuLhCapacityList.stream()
                .filter(skuLhCapacity -> StringUtils.isNotEmpty(skuLhCapacity.getMaterialCode()))
                .collect(Collectors.toMap(
                        MdmSkuLhCapacity::getMaterialCode,
                        skuLhCapacity -> skuLhCapacity,
                        (existingVal, newVal) -> newVal
                ));
    }


    /**
     * 将MdmProductStock转Map
     */
    private Map<String, MdmProductStock> convertToStockMap(List<MdmProductStock> stockList) {
        if (stockList == null || stockList.isEmpty()) {
            return Collections.emptyMap();
        }
        return stockList.stream()
                .filter(stock -> stock != null && stock.getMaterialCode() != null)
                .collect(Collectors.toMap(
                        MdmProductStock::getMaterialCode,
                        stock -> stock,
                        (existingVal, newVal) -> newVal
                ));
    }

    /**
     * 设置上月计划剩余排产量
     * 上月计划剩余排产量 =【 1日 至 月底】.计划量 - 已生产量，出现负数，默认等于0
     *
     * @param contextDTO
     */
    protected void setLastMonthRemainQty(MpRollAdjustContextDTO contextDTO) {
        // 上月月度硫化监控列表
        List<MpMonthPlanMonitor> lastMonitorList = contextDTO.getLastMonthPlanMonitorList();
        // 上月月度生产计划列表
        List<FactoryMonthPlanFinalAdjustVo> lastPlanList = contextDTO.getLastFactoryMonthPlanProdFinalList();
        // 结构内调整记录
        List<MpAdjustDetailVo> adjustList = contextDTO.getAdjustDetailList();
        // 转分组Map
        Map<String, List<FactoryMonthPlanFinalAdjustVo>> lastPlanGroupMap = convertToPlanGroupMap(lastPlanList);
        Map<String, List<MpMonthPlanMonitor>> lastMonitorGroupMap = convertToMonitorGroupMap(lastMonitorList);
        // 遍历目标列表，计算赋值
        for (MpAdjustDetailVo adjust : adjustList) {
            if (StringUtils.isEmpty(adjust.getMaterialCode())) {
                continue;
            }
            String materialCode = adjust.getMaterialCode();
            Integer totalScheduledQty = 0;
            if (ApsConstant.FALSE.equals(adjust.getIsTrial())) {
                if (PubUtil.isNotEmpty(lastPlanGroupMap.get(materialCode))) {
                    totalScheduledQty = lastPlanGroupMap.get(materialCode).get(0).getTotalQty();
                }
            }
            // 获取已生产量（空值按0处理）
            List<MpMonthPlanMonitor> monthPlanMonitorList = MapUtils.getObject(lastMonitorGroupMap, materialCode, new ArrayList<>());
            Integer productionQty = Convert.toInt(monthPlanMonitorList.stream()
                    .filter(e -> e.getProductionQty() != null)
                    .mapToInt(MpMonthPlanMonitor::getProductionQty)
                    .sum(), 0);
            // 计划剩余排产量 = 累计已排产量 - 已生产量
            Integer monthUnScheduledQty = totalScheduledQty - productionQty;
            // 计划剩余排产量为负数时，默认为0
            if (monthUnScheduledQty < 0) {
                monthUnScheduledQty = 0;
            }
            adjust.setLastMonthRemainQty(monthUnScheduledQty == 0 ? null : monthUnScheduledQty);
        }

    }


    /**
     * 设置计划剩余排产量
     * 计划剩余排产量 =【 1日 至 月底】.计划量 - 已生产量，出现负数，默认等于0
     *
     * @param contextDTO
     */
    protected void setMonthUnScheduledQty(MpRollAdjustContextDTO contextDTO) {
        // 月度硫化监控列表
        List<MpMonthPlanMonitor> monitorList = contextDTO.getMpMonthPlanMonitorList();
        // 月度生产计划列表
        List<FactoryMonthPlanFinalAdjustVo> planList = contextDTO.getFactoryMonthPlanProdFinalList();
        // 结构内调整记录
        List<MpAdjustDetailVo> adjustList = contextDTO.getAdjustDetailList();
        // 转分组Map
        Map<String, List<FactoryMonthPlanFinalAdjustVo>> planGroupMap = convertToPlanGroupMap(planList);
        Map<String, List<MpMonthPlanMonitor>> monitorGroupMap = convertToMonitorGroupMap(monitorList);
        // 获取当前日期所属月份的最大天数
        LocalDate currentDate = LocalDate.now();
        int maxDayOfMonth = currentDate.lengthOfMonth();
        // 遍历目标列表，计算赋值
        for (MpAdjustDetailVo adjust : adjustList) {
            if (StringUtils.isEmpty(adjust.getMaterialCode())) {
                continue;
            }
            String materialCode = adjust.getMaterialCode();
            Integer totalScheduledQty = 0;
            if (ApsConstant.FALSE.equals(adjust.getIsTrial())) {
                // 计算：day1~targetDay的累计值
                totalScheduledQty = calculateQty(planGroupMap, materialCode, maxDayOfMonth);
            }
            // 获取已生产量（空值按0处理）
            List<MpMonthPlanMonitor> monthPlanMonitorList = MapUtils.getObject(monitorGroupMap, materialCode, new ArrayList<>());
            Integer productionQty = Convert.toInt(monthPlanMonitorList.stream()
                    .filter(e -> e.getProductionQty() != null)
                    .mapToInt(MpMonthPlanMonitor::getProductionQty)
                    .sum(), 0);
            // 计划已排产量
            adjust.setMonthScheduledQty(totalScheduledQty);
            // 已生产量
            adjust.setProductionQty(productionQty);
            // 计划剩余排产量 = 累计已排产量 - 已生产量
            Integer monthUnScheduledQty = totalScheduledQty - productionQty;
            // 计划剩余排产量为负数时，默认为0
            if (monthUnScheduledQty < 0) {
                monthUnScheduledQty = 0;
            }
            adjust.setMonthUnScheduledQty(monthUnScheduledQty);
        }

    }

    /**
     * 计算day1~targetDay的累计已排产量
     */
    private Integer calculateQty(Map<String, List<FactoryMonthPlanFinalAdjustVo>> planGroupMap, String materialCode, int targetDay) {
        // 从分组Map中获取当前物料的计划列表（空则返回0）
        List<FactoryMonthPlanFinalAdjustVo> planList = Optional.ofNullable(planGroupMap.get(materialCode))
                .filter(list -> PubUtil.isNotEmpty(list))
                .orElse(Collections.emptyList());
        if (PubUtil.isEmpty(planList)) {
            return 0;
        }
        int total = 0;
        // 遍历day1~targetDay字段，累加值
        for (int day = 1; day <= targetDay; day++) {
            try {
                // 拼接字段名
                String fieldName = "day" + day;
                List<FactoryMonthPlanFinalAdjustVo> monthPlanList = MapUtils.getObject(planGroupMap, materialCode, new ArrayList<>());
                Integer dayValue = monthPlanList.stream()
                        .filter(e -> e.getFieldValueByFieldName(fieldName) != null)
                        .mapToInt(e -> ((Integer) e.getFieldValueByFieldName(fieldName)))
                        .sum();
                total += Convert.toInt(dayValue, 0);
            } catch (Exception e) {
                // 异常时跳过
                continue;
            }
        }
        return total;
    }

    /**
     * 设置其他字段
     *
     * @param contextDTO
     */
    protected void setOtherField(MpRollAdjustContextDTO contextDTO) {
        // 调整明细
        List<MpAdjustDetailVo> adjustList = contextDTO.getAdjustDetailList();
        // 循环设置
        adjustList.stream().forEach(vo -> {
            // 计算: 调整量 = 净需求 - 计划剩余排产量 -上月计划余量
            Integer pendingQty = Convert.toInt(vo.getCurrentNetQty(), 0) - Convert.toInt(vo.getMonthUnScheduledQty(), 0) - Convert.toInt(vo.getLastMonthRemainQty(), 0);
            vo.setPendingQty(pendingQty);
            // 确认调整量默认等于待调整量
            vo.setConfirmAdjustQty(pendingQty);
            // 计算：净需求变动 = 净需求 - 调整前净需求量
            Integer netQtyChange = Convert.toInt(vo.getCurrentNetQty(), 0) - Convert.toInt(vo.getPreviousNetQty(), 0);
            vo.setNetQtyChange(netQtyChange);
        });
    }

    /**
     * 转MpStructureAllocation分组Map
     */
    private Map<String, List<MpStructureAllocation>> convertToStructureAllocationMap(List<MpStructureAllocation> structureAllocationList) {
        if (PubUtil.isEmpty(structureAllocationList)) {
            return Collections.emptyMap();
        }
        return structureAllocationList.stream()
                .filter(allocation -> StringUtils.isNotEmpty(allocation.getStructureName()))
                .collect(Collectors.groupingBy(MpStructureAllocation::getStructureName));
    }


    /**
     * 转FactoryMonthPlanFinalAdjustVo分组Map
     */
    private Map<String, List<FactoryMonthPlanFinalAdjustVo>> convertToPlanGroupMap(List<FactoryMonthPlanFinalAdjustVo> planList) {
        if (PubUtil.isEmpty(planList)) {
            return Collections.emptyMap();
        }
        return planList.stream()
                .filter(plan -> plan != null && plan.getMaterialCode() != null)
                .collect(Collectors.groupingBy(FactoryMonthPlanFinalAdjustVo::getMaterialCode));
    }

    /**
     * 转MpMonthPlanMonitor分组Map
     */
    private Map<String, List<MpMonthPlanMonitor>> convertToMonitorGroupMap(List<MpMonthPlanMonitor> monitorList) {
        if (PubUtil.isEmpty(monitorList)) {
            return Collections.emptyMap();
        }
        return monitorList.stream()
                .filter(monitor -> monitor != null && monitor.getMaterialCode() != null)
                .collect(Collectors.groupingBy(MpMonthPlanMonitor::getMaterialCode));
    }


    /**
     * 检查SKU与示方书
     *
     * @param contextDTO
     * @param skuConstructionRef
     * @param materialCode
     * @return
     */
    protected List<String> checkSkuConstructionRef(MpRollAdjustContextDTO contextDTO, MdmSkuConstructionRef skuConstructionRef, String materialCode) {
        Map<String, List<String>> messageMap = Optional.ofNullable(contextDTO.getMessageMap())
                .orElseGet(HashMap::new);
        List<String> errorMsgList = messageMap.computeIfAbsent(ApsConstant.APS_STRING_1, k -> new ArrayList<>());
        contextDTO.setMessageMap(messageMap);

        List<String> msgResultList = new ArrayList<>();
        // 未匹配到SKU与示方书记录
        if (skuConstructionRef == null) {
            String errorMsg = StrUtil.format(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.notMatchSkuConstructionRef"),
                    materialCode);
            errorMsgList.add(errorMsg);
            msgResultList.add(errorMsg);
        }
        //  匹配到SKU与示方书记录，检查制造示方、硫化示方、文字示方是否为空
        if (skuConstructionRef != null) {
            if (StringUtils.isEmpty(skuConstructionRef.getEmbryoNo())) {
                String errorMsg = StrUtil.format(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.checkEmbryoNoEmpty"),
                        materialCode);
                errorMsgList.add(errorMsg);
                msgResultList.add(errorMsg);
            }
            if (StringUtils.isEmpty(skuConstructionRef.getLhNo())) {
                String errorMsg = StrUtil.format(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.checkLhNoEmpty"),
                        materialCode);
                errorMsgList.add(errorMsg);
                msgResultList.add(errorMsg);
            }
            if (StringUtils.isEmpty(skuConstructionRef.getTextNo())) {
                String errorMsg = StrUtil.format(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.checkTextNoEmpty"),
                        materialCode);
                errorMsgList.add(errorMsg);
                msgResultList.add(errorMsg);
            }
        }
        return msgResultList;
    }

    /**
     * 检查调整明细列表中的产品结构字段是否为空
     *
     * @param adjustDetailList 调整明细列表
     * @return 错误信息列表
     */
    protected List<String> checkStructNameEmpty(List<MpAdjustDetailVo> adjustDetailList) {
        // 错误信息列表
        List<String> errorMsgList = new ArrayList<>();
        if (PubUtil.isEmpty(adjustDetailList)) {
            return errorMsgList;
        }
        // 遍历调整明细列表
        for (MpAdjustDetailVo detail : adjustDetailList) {
            String materialCode = detail.getMaterialCode();
            if (StringUtils.isEmpty(materialCode)) {
                continue;
            }
            // 判断字段值是否为空
            if (StringUtils.isBlank(detail.getStructureName())) {
                String errorMsg = StrUtil.format(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.structNameEmpty"),
                        materialCode);
                errorMsgList.add(errorMsg);
            }
        }
        return errorMsgList;
    }


    /**
     * 根据结构名称和物料编码分组汇总订单量
     *
     * @param originalList
     * @param isMergeTrial
     * @return
     */
    protected List<MpAdjustDetailVo> sumByStructureAndMaterial(List<MpAdjustDetailVo> originalList, boolean isMergeTrial) {
        if (PubUtil.isEmpty(originalList)) {
            return Collections.emptyList();
        }
        // 分组汇总Map
        Map<String, MpAdjustDetailVo> summaryMap = new HashMap<>();
        // 未合并的试制数据列表
        List<MpAdjustDetailVo> trialList = new ArrayList<>();
        // 遍历原始列表进行分组汇总
        for (MpAdjustDetailVo vo : originalList) {
            boolean isTrial = ApsConstant.TRUE.equals(vo.getIsTrial());
            // 试制数据且不合并，跳过汇总逻辑
            if (isTrial && !isMergeTrial) {
                trialList.add(vo);
                continue;
            }
            // 生成分组Key：试制量试用：结构 + 物料 + 施工阶段，非试制量试用：结构 + 物料
            String groupKey = generateGroupKey(vo, isTrial);
            // 订单量
            Integer currentOrdQty = Convert.toInt(vo.getOrdQty(), 0);
            // 当前净需求量
            Integer currentNetQty = Convert.toInt(vo.getCurrentNetQty(), 0);
            // 实际调整
            Integer actualAdjustQty = Convert.toInt(vo.getActualAdjustQty(), 0);
            // 分组已存在，累加订单量并拼接试制计划ID
            if (summaryMap.containsKey(groupKey)) {
                MpAdjustDetailVo existVo = summaryMap.get(groupKey);
                // 累加订单量
                Integer existOrdQty = Convert.toInt(existVo.getOrdQty(), 0);
                existVo.setOrdQty(existOrdQty + currentOrdQty);
                // 累加当前净需求量
                Integer existCurrentNetQty = Convert.toInt(existVo.getCurrentNetQty(), 0);
                existVo.setCurrentNetQty(existCurrentNetQty + currentNetQty);
                // 累加实际调整量
                Integer existActualAdjustQty = Convert.toInt(existVo.getActualAdjustQty(), 0);
                existVo.setActualAdjustQty(existActualAdjustQty + actualAdjustQty);
                // 拼接试制计划ID
                mergeTrialPlanId(existVo, vo.getTrialPlanId());
            } else {
                // 分组不存在
                MpAdjustDetailVo newVo = new MpAdjustDetailVo();
                BeanUtil.copyProperties(vo, newVo, false);
                newVo.setStructureName(vo.getStructureName());
                newVo.setMaterialCode(vo.getMaterialCode());
                newVo.setConstructionStage(vo.getConstructionStage());
                newVo.setOrdQty(currentOrdQty);
                newVo.setCurrentNetQty(currentNetQty);
                newVo.setActualAdjustQty(actualAdjustQty);
                newVo.setStockCaptureDate(DateUtils.getNowDate());
                summaryMap.put(groupKey, newVo);
            }
        }
        // 结果集：汇总数据 + 未合并的试制量试数据
        List<MpAdjustDetailVo> resultList = new ArrayList<>(summaryMap.values());
        resultList.addAll(trialList);
        return resultList;
    }


    /**
     * 生成分组Key
     *
     * @param vo      待分组的VO
     * @param isTrial 是否为试制量试
     * @return 分组key
     */
    private String generateGroupKey(MpAdjustDetailVo vo, boolean isTrial) {
        if (isTrial || StringUtils.isNotEmpty(vo.getTrialPlanId())) {
            // 试制量试：结构名称 + 物料编码 + 施工阶段
            String constructStage = StringUtils.defaultString(vo.getConstructionStage(), "");
            return String.join(BusiConstant.WeekRollAdjust.SPLIT_GROUP_KEY, vo.getGroupKey(), constructStage);
        } else {
            // 非试制量试：结构名称 + 物料编码
            return vo.getGroupKey();
        }
    }


    /**
     * 合并试制量试计划ID
     *
     * @param existVo        已存在的VO
     * @param newTrialPlanId 新的试制量试计划ID
     */
    private void mergeTrialPlanId(MpAdjustDetailVo existVo, String newTrialPlanId) {
        if (StringUtils.isEmpty(newTrialPlanId)) {
            return;
        }
        // 已存在的ID列表
        Set<String> existIds = new HashSet<>();
        String existTrialPlanId = existVo.getTrialPlanId();
        if (StringUtils.isNotEmpty(existTrialPlanId)) {
            existIds.addAll(Arrays.asList(existTrialPlanId.split(BusiConstant.WeekRollAdjust.SPLIT_COMMA)));
        }
        // 添加新ID并去重
        existIds.add(newTrialPlanId);
        // 重新拼接为字符串
        String mergedId = String.join(BusiConstant.WeekRollAdjust.SPLIT_COMMA, existIds);
        existVo.setTrialPlanId(mergedId);
    }


    /**
     * 初始锁定日
     *
     * @param contextDTO 周程滚动调整上下文对象
     */
    public Integer getLockEndDay(MpRollAdjustContextDTO contextDTO) {
        return mpAdjustStructureInService.getLockEndDay(contextDTO);
    }

    /**
     * 初始结构开始日\收尾日
     *
     * @param contextDTO 周程滚动调整上下文对象
     */
    protected void initStructureStartAndEndDay(MpRollAdjustContextDTO contextDTO) {
        mpAdjustStructureInService.initStructureStartAndEndDay(contextDTO);
    }

    /**
     * 判断是否特殊材料
     *
     * @param targetEmbryoCode             目标胚胎编码
     * @param mdmMaterialConsumeDetailList BOM物料消耗明细列表
     * @param specialMaterialList          特殊材料清单列表
     * @return
     */
    protected boolean hasSpecialMaterial(String targetEmbryoCode, List<MdmMaterialConsumeDetail> mdmMaterialConsumeDetailList,
                                         List<RawSpecialMaterialRecord> specialMaterialList) {

        if (StringUtils.isEmpty(targetEmbryoCode) || PubUtil.isEmpty(mdmMaterialConsumeDetailList)
                || PubUtil.isEmpty(specialMaterialList)) {
            return Boolean.FALSE;
        }

        // 从BOM物料消耗明细列表中通过胎胚代码筛选出匹配的所有数据
        Set<String> childMaterialCodes = mdmMaterialConsumeDetailList.stream()
                .filter(detail -> StringUtils.equals(targetEmbryoCode, detail.getEmbryoCode()))
                .map(MdmMaterialConsumeDetail::getChildMaterialCode)
                .collect(Collectors.toSet());

        // 如果没有匹配到直接返回false
        if (PubUtil.isEmpty(childMaterialCodes)) {
            return Boolean.FALSE;
        }

        // 检查特殊材料清单列表中是否存在匹配的数据
        return specialMaterialList.stream()
                .map(RawSpecialMaterialRecord::getMaterialCode)
                .anyMatch(childMaterialCodes::contains);
    }

}
