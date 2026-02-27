package com.zlt.aps.mp.adjust.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.common.collect.Maps;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.constant.IncrementConstant;
import com.zlt.aps.enums.ConstructionStageEnum;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.mp.factory.service.impl.MoldCavityInsertMaxValueCalculatorImpl;
import com.zlt.aps.utils.IncrementService;
import com.zlt.aps.utils.ThreadPoolUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.constant.BusiConstant;
import com.zlt.aps.mp.engine.capacity.MpAdjustDailyCapacityLimit;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.utils.DateUtils;
import com.zlt.aps.maindata.enums.MsgTemplateEnums;
import com.zlt.aps.maindata.service.IMpMonthPlanStatisticsService;
import com.zlt.aps.maindata.utils.MessageServiceUtils;
import com.zlt.aps.mp.adjust.service.IMpAdjustMaterialLogService;
import com.zlt.aps.mp.api.domain.capacity.MpDailyCapacityLimitVo;
import com.zlt.aps.mp.api.domain.entity.MpAdjustMaterialLog;
import com.zlt.aps.mp.api.domain.entity.MpMonthPlanStatistics;
import com.zlt.aps.mp.api.domain.vo.DailyMouldAvailabilityResult;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.maindata.mapper.MdmMonthSurplusEntityMapper;
import com.zlt.aps.mp.adjust.mapper.MpAdjustResultEntityMapper;
import com.zlt.aps.mp.adjust.mapper.MpAdjustStructureInEntityMapper;
import com.zlt.aps.mp.adjust.mapper.MpAdjustStructureOutEntityMapper;
import com.zlt.aps.mp.adjust.service.IMpAdjustResultService;
import com.zlt.aps.mp.adjust.service.IMpAdjustStructureInService;
import com.zlt.aps.mp.adjust.service.IMpAdjustStructureLogService;
import com.zlt.aps.mp.adjust.service.IMpWeekAdjustService;
import com.zlt.aps.mp.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.mp.api.domain.entity.DpDemandPlan;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialConsumeDetail;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.MdmMonthSurplus;
import com.zlt.aps.mp.api.domain.entity.MdmProductStock;
import com.zlt.aps.mp.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.aps.mp.api.domain.entity.MdmSkuLhCapacity;
import com.zlt.aps.mp.api.domain.entity.MdmSkuStructureRef;
import com.zlt.aps.mp.api.domain.entity.MpAdjustResult;
import com.zlt.aps.mp.api.domain.entity.MpAdjustStructureIn;
import com.zlt.aps.mp.api.domain.entity.MpAdjustStructureLog;
import com.zlt.aps.mp.api.domain.entity.MpAdjustStructureOut;
import com.zlt.aps.mp.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.mp.api.domain.entity.MpMonthPlanMonitor;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.api.domain.entity.MpTrialPlan;
import com.zlt.aps.mp.api.domain.entity.RawSpecialMaterialRecord;
import com.zlt.aps.mp.api.domain.entity.SalesOrderPool;
import com.zlt.aps.mp.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.mp.api.domain.vo.MpAdjustDetailVo;
import com.zlt.aps.mp.api.domain.vo.MpDayProductionStatisticsDetailVo;
import com.zlt.aps.mp.api.enums.AdjustItemSourceEnum;
import com.zlt.aps.mp.common.utils.DistributedVersionGenerator;
import com.zlt.aps.mp.common.utils.StringUtil;
import com.zlt.aps.mp.demand.service.IDpDemandPlanService;
import com.zlt.aps.mp.factory.mapper.FactoryMonthPlanProductionFinalResultEntityMapper;
import com.zlt.aps.mp.factory.mapper.MpStructureAllocationEntityMapper;
import com.zlt.aps.mp.mdm.dto.DataDTO;
import com.zlt.aps.mp.mdm.handler.DataManager;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.dao.basedao.BaseDao;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.zlt.msg.message.domain.vo.MessageContext;
import com.zlt.msg.message.enums.MsgTypeEnums;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;

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
    protected MdmMonthSurplusEntityMapper mdmMonthSurplusEntityMapper;

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
    protected IMpAdjustResultService mpAdjustResultService;

    @Autowired
    protected IMpAdjustMaterialLogService mpAdjustMaterialLogService;

    @Autowired
    protected IMpAdjustStructureLogService mpAdjustLogService;

    @Autowired
    protected IMpAdjustStructureInService mpAdjustStructureInService;

    @Autowired
    protected MoldCavityInsertMaxValueCalculatorImpl moldCavityInsertMaxValueCalculator;

    @Autowired
    protected IMpMonthPlanStatisticsService mpMonthPlanStatisticsService;

    @Autowired
    protected BaseDao baseDao;

    @Autowired
    protected IncrementService incrementService;

    @Autowired
    private MessageServiceUtils messageServiceAdapter;

    @Autowired
    private DataManager dataManager;


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
    }


    /**
     * 将集合中指定字段的0值替换为null
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
     * @param contextDTO
     */
    public abstract void saveAdjustDetailList(MpRollAdjustContextDTO contextDTO);

    protected void sortAdjustDetailList(MpRollAdjustContextDTO contextDTO) {
        List<MpAdjustDetailVo> adjustDetailList = contextDTO.getAdjustDetailList();
        if (PubUtil.isEmpty(adjustDetailList)) {
            return;
        }
        Collections.sort(adjustDetailList, getSortComparator());
    }

    protected Comparator<MpAdjustDetailVo> getSortComparator() {
        // 定义施工阶段自定义排序权重：正式(03) -> 试制(01) -> 量试(02) -> 无工艺(00)，空值排最后
        Map<String, Integer> stageSortWeights = new HashMap<>();
        // 正式：权重1
        stageSortWeights.put(ConstructionStageEnum.FORMAL_PRODUCTION.getStage(), 1);
        // 试制：权重2
        stageSortWeights.put(ConstructionStageEnum.MEASUREMENT.getStage(), 2);
        // 量试：权重3
        stageSortWeights.put(ConstructionStageEnum.TRIAL_PRODUCTION.getStage(), 3);
        // 无施工：权重4
        stageSortWeights.put(ConstructionStageEnum.NO_CONSTRUCTION.getStage(), 4);
        // 一级排序：结构名称升序，空值排最后
        return Comparator.comparing(MpAdjustDetailVo::getStructureName, Comparator.nullsLast(String::compareTo))
                // 二级排序：施工阶段按自定义权重升序（权重小排前）
                .thenComparing(vo -> stageSortWeights.getOrDefault(vo.getConstructionStage(), 5))
                // 三级排序：负数排前 -> 正数次之 -> 0（含null）最后，同组内绝对值从大到小
                // 负数排前，非负数整体在后
                .thenComparing(vo -> {
                    // null统一视为0
                    Integer qty = Optional.ofNullable(vo.getPendingQty()).orElse(0);
                    // 负数返回0，非负数返回1，升序实现负数排前
                    return qty < 0 ? 0 : 1;
                })
                // 非负数内部区分 正数排前，0最后
                .thenComparing(vo -> {
                    Integer qty = Optional.ofNullable(vo.getPendingQty()).orElse(0);
                    // 正数返回0，0返回1，升序实现正数排前、0最后
                    return qty > 0 ? 0 : 1;
                })
                // 同分组内（负数、正数、0）按绝对值降序（从大到小）
                .thenComparing(vo -> {
                    Integer qty = Optional.ofNullable(vo.getPendingQty()).orElse(0);
                    return Math.abs(qty);
                }, Comparator.reverseOrder());

    }

    @Override
    public void autoAdjust(MpRollAdjustContextDTO contextDTO) throws BusinessException {
        //1、执行自动调整
        doAutoAdjust(contextDTO);
        //2、保存调整结果
        saveMpAdjustResult(contextDTO);
        //3、保存调整过程日志
        saveMpAdjustProcLog(contextDTO);
        //4、回填实际调整
        backfillRealAdjustResult(contextDTO);
        //5、保存月计划统计结果
        saveMonthPlanStatisticsResult(contextDTO);
        //6、发送消息
        if (PubUtil.isNotEmpty(contextDTO.getMsgRemainQtyNoFull())){
            sendMsgRemainQtyNoFull(contextDTO);
        }
    }

    /**
     * 发送 SKU原余量小于调整次日至锁定截止日的计划量提醒
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
     * 回填实际调整
     * @param contextDTO 周程滚动上下文
     */
    protected void backfillRealAdjustResult(MpRollAdjustContextDTO contextDTO){

    }

    /**
     * 重算每日产能限制，包括硫化机台数、胎胚种类数
     * @param contextDTO 周程滚动上下文
     * @param mpProdFinalList 定稿记录列表
     */
    protected void reCalcAdjustDailyCapacityLimit(MpRollAdjustContextDTO contextDTO, List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList,MpAdjustDailyCapacityLimit adjustDailyCapacityLimitObj) {

        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap = contextDTO.getDailyCapacityLimitVoMap();
        for (int i = contextDTO.getStructureStartDay(); i<= contextDTO.getStructureDeadLine(); i++){
            if (dailyCapacityLimitVoMap.get(i) == null){
                continue;
            }
            adjustDailyCapacityLimitObj.calcLhMachinesWithEmbryoTypes(mpProdFinalList,i, dailyCapacityLimitVoMap.get(i), contextDTO.getParamMap(),null);
        }
    }

    /**
     * 构建月计划统计结果
     * @param dailyCapacityMap 日产能限制Map（key=1-31日期，value=日产能限制实体）
     * @param mpProdFinalList  月计划定稿列表
     * @param oneStructureAllocationList 月计划结构转产表-单结构列表
     * @return 统计结果列表
     */
    protected List<MpMonthPlanStatistics> buildMonthPlanStatistics(Map<Integer, MpDailyCapacityLimitVo> dailyCapacityMap, List<FactoryMonthPlanFinalAdjustVo> mpProdFinalList,
                                                                   List<MpStructureAllocation> oneStructureAllocationList) {
        List<MpMonthPlanStatistics> resultList = new ArrayList<>();
        if (PubUtil.isEmpty(dailyCapacityMap) || PubUtil.isEmpty(oneStructureAllocationList)) {
            log.warn("构建月计划统计结果 ==> 日产能限制Map或者月计划结构转产表-单结构列表为空，跳过不处理");
            return resultList;
        }
        FactoryMonthPlanFinalAdjustVo monthPlan = new FactoryMonthPlanFinalAdjustVo();
        if (PubUtil.isNotEmpty(mpProdFinalList)) {
            monthPlan = mpProdFinalList.get(0);
        }
        for (MpStructureAllocation structureAllocation : oneStructureAllocationList) {
            MpMonthPlanStatistics statistics = new MpMonthPlanStatistics();
            // 设置月计划统计相关字段
            setMonthPlanStatisticsField(monthPlan, structureAllocation, statistics);
            // 遍历日期，设置每个dayN字段
            for (int day = ProductionConstant.MONTH_START_DAY; day <= ProductionConstant.MONTH_MAX_DAY; day++) {
                setDayField(statistics, day, dailyCapacityMap);
            }
            // 添加到结果列表
            resultList.add(statistics);
        }
        return resultList;
    }

    /**
     * 设置月计划统计相关字段
     */
    private void setMonthPlanStatisticsField(FactoryMonthPlanFinalAdjustVo source, MpStructureAllocation structureAllocation, MpMonthPlanStatistics target) {
        target.setFactoryCode(structureAllocation.getFactoryCode());
        target.setYear(structureAllocation.getYear());
        target.setMonth(structureAllocation.getMonth());
        target.setProductionVersion(structureAllocation.getProductionVersion());
        target.setMonthPlanVersion(structureAllocation.getMonthPlanVersion());
        target.setStructureName(structureAllocation.getStructureName());
        target.setYearMonth(source.getYearMonth());
        target.setProSize(source.getProSize());
        target.setStructureType(source.getStructureType());
        target.setLastMonthPlanVersion(source.getLastMonthPlanVersion());
        target.setProductTypeCode(source.getProductTypeCode());
    }

    /**
     * 根据日期获取日产能限制数据转JSON设置到对应dayN字段
     * @param statistics 月计划统计实体
     * @param day 日期
     * @param capacityMap 日产能限制Map
     */
    private void setDayField(MpMonthPlanStatistics statistics, int day, Map<Integer, MpDailyCapacityLimitVo> capacityMap) {
        MpDailyCapacityLimitVo capacityVo = capacityMap == null ? null : capacityMap.get(day);
        if (capacityVo == null) {
            return;
        }
        MpDayProductionStatisticsDetailVo dayProductionStatisticsDetailVo = new MpDayProductionStatisticsDetailVo();
        dayProductionStatisticsDetailVo.setLhMachines(capacityVo.getUsedLhMachines() == 0 ? null: capacityVo.getUsedLhMachines());
        dayProductionStatisticsDetailVo.setEmbryoCount(capacityVo.getUsedEmbryoTypes() == 0 ? null: capacityVo.getUsedEmbryoTypes());
        dayProductionStatisticsDetailVo.setChangeMould(null);
        statistics.setFieldValueByFieldName(BusiConstant.WeekRollAdjust.FIELD_PREFIX_DAY + day, JSONObject.toJSONString(dayProductionStatisticsDetailVo));
    }


    /**
     * List转换Map,按结构
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
     * @param contextDTO
     * @param mpFinalList
     */
    protected void setSpecStructureTotalQty(MpRollAdjustContextDTO contextDTO,List<FactoryMonthPlanFinalAdjustVo> mpFinalList){
        if (PubUtil.isEmpty(mpFinalList)){
            return;
        }
        Integer specStructureTotalQty = mpFinalList.stream().mapToInt(FactoryMonthPlanFinalAdjustVo::getTotalQty).sum();
        contextDTO.setSpecStructureTotalQty(specStructureTotalQty);
    }

    /**
     * 保存月计划统计结果
     * @param contextDTO
     */
    private void saveMonthPlanStatisticsResult(MpRollAdjustContextDTO contextDTO){
        List<MpMonthPlanStatistics> monthPlanStatisticsList = contextDTO.getMonthPlanStatisticsList();
        if (PubUtil.isEmpty(monthPlanStatisticsList)){
            return;
        }
        // 删除月计划统计结果（物理删除）
        mpMonthPlanStatisticsService.deleteMonthPlanStatisticsByCondition(contextDTO.getFactoryCode(),
                String.valueOf(contextDTO.getMpYear()),String.valueOf(contextDTO.getMpMonth()),contextDTO.getProductionVersion());
        // 去重月计划统计结果
        monthPlanStatisticsList = distinctMonthPlanStatistics(monthPlanStatisticsList);
        // 保存月计划统计结果
        baseDao.insertBatch(monthPlanStatisticsList);
        log.info("保存月计划统计结果成功，共新增:{}条记录", monthPlanStatisticsList.size());
    }

    /**
     * 去重月计划统计结果（按结构名称去重）
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
     * @param contextDTO
     */
    private void saveMpAdjustResult(MpRollAdjustContextDTO contextDTO){
        //List<FactoryMonthPlanFinalAdjustVo> factoryMonthPlanProdFinalList = contextDTO.getFactoryMonthPlanProdFinalList();
        List<FactoryMonthPlanFinalAdjustVo> saveMpProdFinalList = contextDTO.getSaveMpProdFinalList();
        if (PubUtil.isEmpty(saveMpProdFinalList)){
            return;
        }
        //1、根据调整版本 先删除(物理)
        mpAdjustResultService.deleteAdjustResultByVersion(contextDTO.getFactoryCode(),
                String.valueOf(contextDTO.getMpYear()),String.valueOf(contextDTO.getMpMonth()),contextDTO.getVersion());
        //2、保存调整记录
        MpAdjustResult mpAdjustResult;
        List<MpAdjustResult> mpAdjustResultList = new ArrayList<>();
        for (FactoryMonthPlanFinalAdjustVo finalAdjustVo:saveMpProdFinalList){
            mpAdjustResult = new MpAdjustResult();
            BeanUtils.copyProperties(finalAdjustVo,mpAdjustResult);
            mpAdjustResult.setId(null);
            mpAdjustResult.setAdjustType(contextDTO.getAdjustType());
            mpAdjustResult.setVersion(contextDTO.getVersion());
            mpAdjustResult.setTotalPlanQty(finalAdjustVo.getTotalQty());

            mpAdjustResult.setAdjustFlag((finalAdjustVo.getActualAdjustQty() != null && Math.abs(finalAdjustVo.getActualAdjustQty())>0) ? YesOrNoEnum.YES.getCode():YesOrNoEnum.NO.getCode());
            if (StringUtil.isEmptyWithTrim(mpAdjustResult.getIsLockSchedule())){
                mpAdjustResult.setIsLockSchedule(YesOrNoEnum.NO.getCode());
            }
            // 将日期字段中值为0的字段设为null
            handleZeroToNull(mpAdjustResult);
            mpAdjustResultList.add(mpAdjustResult);
        }
        baseDao.insertBatch(mpAdjustResultList);
        contextDTO.setAdjustResultList(mpAdjustResultList);
    }

    /**
     * 保存调整过程日志
     * @param contextDTO
     */
    private void saveMpAdjustProcLog(MpRollAdjustContextDTO contextDTO){
        List<FactoryMonthPlanFinalAdjustVo> adjustProcLogList = contextDTO.getSaveAdjustProcLogList();
        if (PubUtil.isEmpty(adjustProcLogList)){
            return;
        }
        //1、根据调整版本 先删除(物理)
        mpAdjustMaterialLogService.deleteAdjustProcLogByVersion(contextDTO.getFactoryCode(),
                String.valueOf(contextDTO.getMpYear()),String.valueOf(contextDTO.getMpMonth()),contextDTO.getVersion());
        //2、保存调整记录
        MpAdjustMaterialLog mpMaterialLog;
        List<MpAdjustMaterialLog> mpMaterialLogList = new ArrayList<>();
        for (FactoryMonthPlanFinalAdjustVo finalAdjustVo:adjustProcLogList){
            mpMaterialLog = new MpAdjustMaterialLog();
            BeanUtils.copyProperties(finalAdjustVo,mpMaterialLog);
            mpMaterialLog.setId(null);
            mpMaterialLog.setAdjustType(contextDTO.getAdjustType());
            mpMaterialLog.setAdjVersion(contextDTO.getVersion());
            mpMaterialLog.setAdjustDetail(finalAdjustVo.getAdjustDetail().toString());
            mpMaterialLogList.add(mpMaterialLog);
        }
        baseDao.insertBatch(mpMaterialLogList);
    }

    /**
     * 将日期字段中值为0的字段设为null
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
     * @param contextDTO
     */
    public void saveMpAdjustLog(MpRollAdjustContextDTO contextDTO){
        String logDetail = contextDTO.getLogDetail().toString();
        if (StringUtil.isEmptyWithTrim(logDetail)){
            return;
        }
        //1、根据调整版本 先删除(物理)
        mpAdjustLogService.deleteAdjustLogByVersion(contextDTO.getFactoryCode(),
                String.valueOf(contextDTO.getMpYear()),String.valueOf(contextDTO.getMpMonth()),
                contextDTO.getVersion(),contextDTO.getStructureName());
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
    public void confirmAdjust(MpRollAdjustContextDTO contextDTO) throws BusinessException {
        log.info("开始执行周程调整确认流程，工厂：{}，年份：{}，月份：{}，版本：{}，排产版本：{}", contextDTO.getFactoryCode(), contextDTO.getMpYear(),
                contextDTO.getMpMonth(), contextDTO.getVersion(), contextDTO.getProductionVersion());
        try {
            // 1、查询周程调整结果
            queryAdjustResult(contextDTO);
            // 2、查询调整明细
            queryAdjustDetailList(contextDTO);
            // 3、查询月度生产计划
            queryMonthPlanList(contextDTO);
            // 4、更新试制量制计划
            updateTrialPlanList(contextDTO);
            // 5、更新调整明细
            updateAdjustDetailList(contextDTO);
            // 6、更新月度生产计划
            updateMonthPlanList(contextDTO);
            // 7、新增月度生产计划
            insertMonthPlanList(contextDTO);
            // 8、更新结构转产
            updateStructureAllocationList(contextDTO);
            log.info("周程调整确认流程执行完成");
        } catch (Exception e) {
            log.error("周程调整确认流程执行异常", e);
            throw new BusinessException("周程调整确认失败：" + e.getMessage());
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
    private void insertMonthPlanList(MpRollAdjustContextDTO contextDTO) {
        List<MpAdjustDetailVo> adjustDetailList = contextDTO.getAdjustDetailList();
        List<MpAdjustResult> adjustResultList = contextDTO.getAdjustResultList();
        if (PubUtil.isEmpty(adjustDetailList) || PubUtil.isEmpty(adjustResultList)) {
            log.warn("新增月度生产计划：调整明细列表或者调整结果列表为空，直接返回");
            return;
        }
        // 月度生产计划排程结果
        List<FactoryMonthPlanProductionFinalResult> factoryMonthPlanProdFinalList = new ArrayList<>();
        // 批次号前缀
        String prefixKey = IncrementConstant.MONTH_FINAL + com.ruoyi.common.core.utils.DateUtils.dateTimeNow("yyMMdd");
        // 批次号
        String batchNo = String.format("%02d", incrementService.getIncrementNumber(prefixKey));
        // 初始化SKU与施工（示方书）关系
        initSkuConstructionRef(contextDTO);
        // 汇总调整明细
        List<MpAdjustDetailVo> summaryAdjustDetailList = sumByStructureAndMaterial(adjustDetailList, Boolean.TRUE);
        // 汇总调整结果
        Map<String, MpAdjustResult> summaryAdjustResult = summaryAdjustResult(adjustResultList, adjustDetailList);
        // 遍历调整明细，获取新增的SKU并新增到月度生产计划
        for (MpAdjustDetailVo adjustDetailVo : summaryAdjustDetailList) {
            String isSkuAdd = adjustDetailVo.getIsSkuAdd();
            if (!ApsConstant.TRUE.equals(isSkuAdd)) {
                continue;
            }
            // 构建分组key
            String groupKey = buildGroupKey(adjustDetailVo);
            // 匹配汇总后调整结果
            MpAdjustResult adjustResult = summaryAdjustResult.getOrDefault(groupKey, new MpAdjustResult());

            FactoryMonthPlanProductionFinalResult monthPlan = new FactoryMonthPlanProductionFinalResult();
            BeanUtils.copyProperties(adjustResult, monthPlan);
            BeanUtils.copyProperties(adjustDetailVo, monthPlan);
            monthPlan.setTotalQty(adjustResult.getTotalPlanQty());
            if (adjustResult.getYear() != null && adjustResult.getMonth() != null) {
                monthPlan.setYearMonth(Integer.valueOf(String.format("%d%02d", adjustResult.getYear(), adjustResult.getMonth())));
            }
            monthPlan.setId(null);
            monthPlan.setBaseVale(null);
            String productionNo = incrementService.getBillNoSequenceByExpire(prefixKey + batchNo, 5, 60 * 24 * 7);
            monthPlan.setProductionNo(productionNo);
            // 设置SKU与示方书关联字段：是否零度材料、制造示方书号、文字示方书号、硫化示方书号
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
            // 获取周数
            int week = getWeekNumber(new Date());
            // 调整量
            Integer actualAdjustQty = adjustDetailVo.getActualAdjustQty();
            if (Convert.toInt(actualAdjustQty, 0) == 0) {
                actualAdjustQty = null;
            }
            monthPlan.setFieldValueByFieldName(BusiConstant.WeekRollAdjust.FIELD_PREFIX_ADJUST_QTY + week, actualAdjustQty);
            // 设置最新需求计划版本
            monthPlan.setLastMonthPlanVersion(adjustDetailVo.getLastMonthPlanVersion());
            // 设置月度计划开始日期、结束日期
            setBeginDayAndEndDay(monthPlan);
            factoryMonthPlanProdFinalList.add(monthPlan);
        }
        try {
            // 删除月度生产计划
            deleteMonthPlanList(contextDTO, factoryMonthPlanProdFinalList);
            // 新增月度生产计划
            baseDao.insertBatch(factoryMonthPlanProdFinalList);
            log.info("新增月度生产计划成功，共新增:{}条记录", factoryMonthPlanProdFinalList.size());
        } catch (Exception e) {
            log.error("新增月度生产计划批量操作异常", e);
            throw new RuntimeException("新增月度生产计划失败", e);
        }

    }

    /**
     * 删除月度生产计划
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
     * @param monthPlan
     * @return
     */
    private void setBeginDayAndEndDay(FactoryMonthPlanProductionFinalResult monthPlan) {
        Integer beginDay = null;
        Integer endDay = null;
        // 按1~31顺序遍历
        for (int day = 1; day <= BusiConstant.WeekRollAdjust.MAX_DAY_OF_MONTH; day++) {
            // 拼接字段名：day1、day2...day31
            String dayFieldName = BusiConstant.WeekRollAdjust.FIELD_PREFIX_DAY + day;
            Integer fieldValue = Convert.toInt(monthPlan.getFieldValueByFieldName(dayFieldName), 0);
            if (fieldValue != 0) {
                if (beginDay == null) {
                    beginDay = day;
                }
                endDay = day;
            }
        }
        monthPlan.setBeginDay(beginDay);
        monthPlan.setEndDay(endDay);
    }


    /**
     * 汇总调整结果
     * @param adjustResultList 调整结果列表
     * @param adjustDetailList 调整明细列表
     * @return Map<String, MpAdjustResult>
     */
    public Map<String, MpAdjustResult> summaryAdjustResult(List<MpAdjustResult> adjustResultList, List<MpAdjustDetailVo> adjustDetailList) {
        if (PubUtil.isEmpty(adjustResultList) || PubUtil.isEmpty(adjustDetailList)) {
            return Collections.emptyMap();
        }
        // 先按物料编码分组，再按主键ID映射
        Map<String, Map<Long, MpAdjustDetailVo>> detailMap = adjustDetailList.stream()
                .collect(Collectors.groupingBy(
                        MpAdjustDetailVo::getMaterialCode,
                        Collectors.toMap(
                                MpAdjustDetailVo::getId,
                                vo -> vo,
                                (v1, v2) -> v1
                        )
                ));

        // 遍历调整结果，匹配明细并分组汇总
        Map<String, MpAdjustResult> summaryMap = new HashMap<>();
        for (MpAdjustResult result : adjustResultList) {
            if (StringUtils.isEmpty(result.getMaterialCode())) {
                continue;
            }
            // 匹配对应的调整明细VO
            MpAdjustDetailVo detailVo = matchAdjustDetail(result, detailMap);
            // 无匹配明细，跳过
            if (detailVo == null) {
                continue;
            }
            // 构建分组key
            String groupKey = buildGroupKey(detailVo);
            // 获取或初始化汇总对象
            MpAdjustResult summaryResult = summaryMap.getOrDefault(groupKey, new MpAdjustResult());
            if (summaryResult.getMaterialCode() == null) {
                BeanUtils.copyProperties(result, summaryResult);
                summaryResult.setMaterialCode(detailVo.getMaterialCode());
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
    private String buildGroupKey(MpAdjustDetailVo detailVo) {
        String structureName = Optional.ofNullable(detailVo.getStructureName()).orElse("");
        String materialCode = Optional.ofNullable(detailVo.getMaterialCode()).orElse("");
        String constructionStage = Optional.ofNullable(detailVo.getConstructionStage()).orElse("");
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
     * 设置SKU与示方书关联字段：是否零度材料、制造示方书号、文字示方书号、硫化示方书号
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
        // 根据物料编码和产品状态匹配SKU与施工关系数据
        MdmSkuConstructionRef mdmSkuConstructionRef = getSkuConstructionRefByCondition(skuConstructionRefList, materialCode, productStatus);
        if (mdmSkuConstructionRef != null) {
            // 是否零度材料
            monthPlan.setIsZeroRack(mdmSkuConstructionRef.getIsZeroRack());
            // 制造示方书号
            monthPlan.setEmbryoNo(mdmSkuConstructionRef.getEmbryoNo());
            // 文字示方书号
            monthPlan.setTextNo(mdmSkuConstructionRef.getTextNo());
            // 硫化示方书号
            monthPlan.setLhNo(mdmSkuConstructionRef.getLhNo());
        }
    }

    /**
     * 根据物料编码和产品状态匹配SKU与施工关系数据
     * @param skuConstructionRefList SKU与施工（示方书）关系列表
     * @param materialCode 物料编码
     * @param productStatus 产品状态
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
        // 调整结果按照物料编号分组
        Map<String, List<MpAdjustResult>> adjustResultMap = buildMaterialCodeAdjustMap(adjustResultList);
        // 月度生产计划列表
        List<FactoryMonthPlanFinalAdjustVo> monthPlanProdList = contextDTO.getFactoryMonthPlanProdFinalList();
        // 生产计划列表按照物料编码进行分组
        Map<String, List<FactoryMonthPlanFinalAdjustVo>> monthPlanMap = monthPlanProdList.stream()
                .collect(Collectors.groupingBy(FactoryMonthPlanFinalAdjustVo::getMaterialCode));
        // 遍历调整明细列表匹配调整结果(更新实际调整、调整原因)
        for (MpAdjustDetailVo adjustDetailVo : adjustDetailList) {
            String materialCode = adjustDetailVo.getMaterialCode();
            if (StringUtils.isEmpty(materialCode)) {
                continue;
            }
            MpAdjustResult adjustResult = getFirstAdjustResult(adjustResultMap, materialCode);
            if (adjustResult == null) {
                log.warn("更新调整明细：物料编号:{}未查询到对应调整结果，跳过", materialCode);
                continue;
            }
            Integer totalPlanQty = Convert.toInt(adjustResult.getTotalPlanQty(), 0);
            Integer actualAdjustQty = totalPlanQty;
            if (totalPlanQty > 0 && !ApsConstant.TRUE.equals(adjustDetailVo.getIsSkuAdd())) {
                List<FactoryMonthPlanFinalAdjustVo> monthPLanList = monthPlanMap.getOrDefault(materialCode, new ArrayList<>());
                Integer totalQty = monthPLanList.stream().mapToInt(v -> {
                    return v.getTotalQty() == null ? 0: v.getTotalQty();
                }).sum();
                actualAdjustQty = totalPlanQty - totalQty;
            }
            // 设置实际调整
            adjustDetailVo.setActualAdjustQty(Convert.toInt(actualAdjustQty, 0));
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
    private void updateMonthPlanList(MpRollAdjustContextDTO contextDTO) {
        List<MpAdjustResult> adjustResultList = contextDTO.getAdjustResultList();
        List<FactoryMonthPlanFinalAdjustVo> factoryMonthPlanProdFinalList = contextDTO.getFactoryMonthPlanProdFinalList();
        List<MpAdjustDetailVo> adjustDetailList = contextDTO.getAdjustDetailList();
        if (PubUtil.isEmpty(adjustResultList) || PubUtil.isEmpty(factoryMonthPlanProdFinalList) || PubUtil.isEmpty(adjustDetailList)) {
            log.warn("更新月度生产计划：调整结果列表或月度计划列表或调整明细列表为空，直接返回");
            return;
        }
        // 调整结果按照物料编号分组
        Map<String, List<MpAdjustResult>> adjustResultMap = buildMaterialCodeAdjustMap(adjustResultList);
        // 调整明细按照物料编号分组
        Map<String, List<MpAdjustDetailVo>> adjustDetailMap = buildMaterialCodeAdjustDetailMap(adjustDetailList);

        // 最新需求计划版本
        String lastMonthPlanVersion = null;
        if (PubUtil.isNotEmpty(adjustDetailList)) {
            lastMonthPlanVersion = adjustDetailList.get(0).getLastMonthPlanVersion();
            contextDTO.setAdjustMonthPlanVersion(lastMonthPlanVersion);
        }

        // 遍历生产计划列表匹配调整结果（更新计划量、开始日期、结束日期、调整量)
        for (FactoryMonthPlanFinalAdjustVo monthPlanVo : factoryMonthPlanProdFinalList) {
            String materialCode = monthPlanVo.getMaterialCode();
            if (StringUtils.isEmpty(materialCode)) {
                continue;
            }
            MpAdjustResult adjustResult = getFirstAdjustResult(adjustResultMap, materialCode);
            if (adjustResult == null) {
                log.warn("更新月度生产计划：物料编号:{}未查询到对应调整结果，跳过", materialCode);
                continue;
            }

            // 设置最新需求计划版本
            monthPlanVo.setLastMonthPlanVersion(lastMonthPlanVersion);

            MpAdjustDetailVo adjustDetail = getFirstAdjustDetail(adjustDetailMap, materialCode);
            if (adjustDetail == null) {
                log.warn("更新月度生产计划：物料编号:{}未查询到对应调整明细，跳过", materialCode);
                continue;
            }

            // 更新1日至31日计划量
            for (int i = 1; i <= BusiConstant.WeekRollAdjust.MAX_DAY_OF_MONTH; i++) {
                String dayFieldName = BusiConstant.WeekRollAdjust.FIELD_PREFIX_DAY + i;
                monthPlanVo.setFieldValueByFieldName(dayFieldName, Convert.toInt(adjustResult.getFieldValueByFieldName(dayFieldName),0));
            }
            // 重算开始日期和结束日期
            if (adjustResult.getBeginDay() != null) {
                try {
                    monthPlanVo.setBeginDay(adjustResult.getBeginDay());
                } catch (Exception e) {
                    log.error("更新月度生产计划：物料:{}的开始日期转换失败，跳过", materialCode, e);
                }
            }
            if (adjustResult.getEndDay() != null) {
                try {
                    monthPlanVo.setEndDay(adjustResult.getEndDay());
                } catch (Exception e) {
                    log.error("更新月度生产计划：物料:{}的结束日期转换失败，跳过", materialCode, e);
                }
            }
            // 生产实际排产量
            monthPlanVo.setTotalQty(adjustResult.getTotalQty());
            // 高优先级排产数量
            monthPlanVo.setHeightProductionQty(adjustResult.getHeightProductionQty());
            // 中优先级排产数量
            monthPlanVo.setMidProductionQty(adjustResult.getMidProductionQty());
            // 周期排产储备排产数量
            monthPlanVo.setCycleProductionQty(adjustResult.getCycleProductionQty());
            // 常规储备排产数量
            monthPlanVo.setConventionProductionQty(adjustResult.getConventionProductionQty());
            // 暂缓订单排产数量
            monthPlanVo.setPostponeProductionQty(adjustResult.getPostponeProductionQty());
            // 试制量试排产量
            monthPlanVo.setTrialProductionQty(adjustResult.getTrialProductionQty());
            // 计算实际生产需求含损耗
            Integer factProdReqQty = calculateFactProdReqQty(adjustDetail.getCurrentNetQty());
            // 差异量(未排产数量) = 实际生产需求含损耗 - 生产实际排产量
            Integer differenceQty = Convert.toInt(factProdReqQty, 0) - Convert.toInt(monthPlanVo.getTotalQty(), 0);
            monthPlanVo.setDifferenceQty(differenceQty);
            // 模具变化信息
            monthPlanVo.setMouldChangeInfo(adjustResult.getMouldChangeInfo());
            // 获取周数
            int week = getWeekNumber(new Date());
            // 调整量
            Integer actualAdjustQty = adjustDetail.getActualAdjustQty();
            if (Convert.toInt(actualAdjustQty, 0) == 0) {
                actualAdjustQty = null;
            }
            monthPlanVo.setFieldValueByFieldName(BusiConstant.WeekRollAdjust.FIELD_PREFIX_ADJUST_QTY + week, actualAdjustQty);
        }

        try {
            // 更新月度生产计划
            baseDao.updateBatch(factoryMonthPlanProdFinalList);
            log.info("更新月度生产计划成功，共更新:{}条记录", factoryMonthPlanProdFinalList.size());
            // 通过结构名称更新月度生产计划
            updateMonthPlanByStructureName(contextDTO, adjustResultList);
        } catch (Exception e) {
            log.error("更新月度生产计划批量操作异常", e);
            throw new RuntimeException("更新月度生产计划失败", e);
        }

    }


    /**
     * 通过结构名称更新月度生产计划
     * @param contextDTO
     * @param adjustResultList
     */
    private void updateMonthPlanByStructureName(MpRollAdjustContextDTO contextDTO, List<MpAdjustResult> adjustResultList) {
        if (PubUtil.isEmpty(adjustResultList) || StringUtils.isEmpty(contextDTO.getAdjustMonthPlanVersion())) {
            return;
        }
        // 获取最新需求计划版本
        String adjustMonthPlanVersion = contextDTO.getAdjustMonthPlanVersion();
        // 收集结构名称Set（筛选结构名称不为空且有调整）
        Set<String> structureNameSet = adjustResultList.stream()
                .filter(vo -> StringUtils.isNotEmpty(vo.getStructureName())
                        && ApsConstant.TRUE.equals(vo.getAdjustFlag()))
                .map(MpAdjustResult::getStructureName)
                .collect(Collectors.toSet());
        if (PubUtil.isEmpty(structureNameSet)) {
            return;
        }
        // 通过结构名称更新月度生产计划最新需求计划版本
        LambdaUpdateWrapper<FactoryMonthPlanProductionFinalResult> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, contextDTO.getFactoryCode())
                .eq(FactoryMonthPlanProductionFinalResult::getYear, contextDTO.getMpYear())
                .eq(FactoryMonthPlanProductionFinalResult::getMonth, contextDTO.getMpMonth())
                .eq(FactoryMonthPlanProductionFinalResult::getProductionVersion, contextDTO.getProductionVersion())
                .in(FactoryMonthPlanProductionFinalResult::getStructureName, structureNameSet)
                .set(FactoryMonthPlanProductionFinalResult::getLastMonthPlanVersion, adjustMonthPlanVersion);
        factoryMonthPlanProdFinalMapper.update(null, wrapper);
    }


    /**
     * 根据时间获取周次
     * 范围：第1周1-7，第2周8-14，第3周15-21，第4周22-31
     * @param date
     * @return
     */
    protected int getWeekNumber(Date date) {
        int dayOfMonth = DateUtil.dayOfMonth(date);
        int baseWeek = (dayOfMonth - 1) / 7 + 1;
        return Math.min(baseWeek, 4);
    }

    /**
     * 查询周程调整结果
     *
     * @param contextDTO
     */
    private void queryAdjustResult(MpRollAdjustContextDTO contextDTO) {
        if (contextDTO.getMpYear() == null || contextDTO.getMpMonth() == null
                || StringUtils.isEmpty(contextDTO.getVersion()) || StringUtils.isEmpty(contextDTO.getProductionVersion())) {
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
        String productionVersion = contextDTO.getProductionVersion();

        MpAdjustResult queryVO = new MpAdjustResult();
        queryVO.setFactoryCode(factoryCode);
        queryVO.setYear(year);
        queryVO.setMonth(month);
        queryVO.setVersion(version);
        queryVO.setProductionVersion(productionVersion);

        LambdaQueryWrapper<MpAdjustResult> queryWrapper = new LambdaQueryWrapper<>();
        buildAdjustResultCondition(queryWrapper, queryVO);

        try {
            List<MpAdjustResult> resultList = mpAdjustResultEntityMapper.selectList(queryWrapper);
            contextDTO.setAdjustResultList(resultList);
        } catch (Exception e) {
            log.error("查询周程调整结果异常，年份：{}，月份：{}，版本：{}", year, month, version, e);
            throw new RuntimeException("查询月度生产计划失败", e);
        }
    }

    /**
     * 保存周程调整结果
     *
     * @param contextDTO
     */
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
        if (contextDTO.getMpYear() == null || contextDTO.getMpMonth() == null
                || StringUtils.isEmpty(contextDTO.getProductionVersion())) {
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
        String productionVersion = contextDTO.getProductionVersion();

        FactoryMonthPlanProductionFinalResult queryVO = new FactoryMonthPlanProductionFinalResult();
        queryVO.setFactoryCode(factoryCode);
        queryVO.setYear(year);
        queryVO.setMonth(month);
        queryVO.setProductionVersion(productionVersion);

        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper = new LambdaQueryWrapper<>();
        buildMonthPlanCondition(queryWrapper, queryVO);

        try {
            List<FactoryMonthPlanProductionFinalResult> factoryMonthPlanProdFinalList = factoryMonthPlanProdFinalMapper.selectList(queryWrapper);
            List<FactoryMonthPlanFinalAdjustVo> resultList = BeanUtil.copyToList(factoryMonthPlanProdFinalList, FactoryMonthPlanFinalAdjustVo.class);
            contextDTO.setFactoryMonthPlanProdFinalList(resultList);
        } catch (Exception e) {
            log.error("查询月度生产计划异常，年份：{}，月份：{}，版本：{}", year, month, productionVersion, e);
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
                .collect(Collectors.groupingBy(MpAdjustDetailVo::getMaterialCode));
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
                .collect(Collectors.groupingBy(MpAdjustResult::getMaterialCode));
    }

    /**
     * 获取第一个调整结果
     *
     * @param materialCodeAdjustMap
     * @param materialCode
     * @return
     */
    protected MpAdjustResult getFirstAdjustResult(Map<String, List<MpAdjustResult>> materialCodeAdjustMap, String materialCode) {
        if (materialCodeAdjustMap == null || StringUtils.isEmpty(materialCode)) {
            return null;
        }
        List<MpAdjustResult> resultList = materialCodeAdjustMap.get(materialCode);
        if (PubUtil.isEmpty(resultList)) {
            return null;
        }
        return resultList.get(0);
    }

    /**
     * 获取第一个调整明细
     *
     * @param materialCodeAdjustMap
     * @param materialCode
     * @return
     */
    protected MpAdjustDetailVo getFirstAdjustDetail(Map<String, List<MpAdjustDetailVo>> materialCodeAdjustMap, String materialCode) {
        if (materialCodeAdjustMap == null || StringUtils.isEmpty(materialCode)) {
            return null;
        }
        List<MpAdjustDetailVo> resultList = materialCodeAdjustMap.get(materialCode);
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
        initCommon(contextDTO);
        // 特殊规则初始化
        specialInit(contextDTO);

        // 获取线程池执行器
        ThreadPoolExecutor executor = ThreadPoolUtil.getThreadPool();

        // 创建初始化方法的异步任务
        // 初始化排产版本、初始化月度生产计划 (有依赖关系：先执行initVersion，再执行initMonthPlan)
        CompletableFuture<Void> versionAndMonthPlanFuture = CompletableFuture
                .runAsync(() -> initVersion(contextDTO), executor)
                .thenRunAsync(() -> initMonthPlan(contextDTO), executor);
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
                    versionAndMonthPlanFuture,
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
     * 初始化通用
     *
     * @param contextDTO
     */
    private void initCommon(MpRollAdjustContextDTO contextDTO) {
        // 工厂编码
        contextDTO.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        if (contextDTO.getMpYear() != null && contextDTO.getMpMonth() != null) {
            // 年月
            contextDTO.setYearMonth(Integer.valueOf(String.format("%d%02d", contextDTO.getMpYear(), contextDTO.getMpMonth())));
            // 获取定稿的月度计划
            FactoryMonthPlanFinalAdjustVo monthPlan = getIsFinalMonthPlan(contextDTO);
            if (monthPlan != null) {
                // 排产版本号
                contextDTO.setProductionVersion(monthPlan.getProductionVersion());
                // 需求计划版本
                contextDTO.setMonthPlanVersion(monthPlan.getMonthPlanVersion());
            }
        }
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
        DataDTO dataDTO = dataManager.buildDataDTO(queryVO, cacheKey, Boolean.TRUE);
        List<MdmSkuConstructionRef> mdmSkuConstructionRefList = dataManager.listSkuConstructionRefs(dataDTO);

        contextDTO.setMdmSkuConstructionRefList(mdmSkuConstructionRefList);
        Map<String, MdmSkuConstructionRef> mdmSkuConstructionRefMap = convertToSkuConstructionRefMap(mdmSkuConstructionRefList);
        contextDTO.setMdmSkuConstructionRefMap(mdmSkuConstructionRefMap);
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
        DataDTO dataDTO = dataManager.buildDataDTO(queryVO, cacheKey, Boolean.TRUE);
        List<MdmMaterialInfo> mdmMaterialInfoList = dataManager.listMaterialInfos(dataDTO);

        Map<String, MdmMaterialInfo> mdmMaterialInfoMap = convertToMaterialInfoMap(mdmMaterialInfoList);
        contextDTO.setMdmMaterialInfoMap(mdmMaterialInfoMap);
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
        DataDTO dataDTO = dataManager.buildDataDTO(queryVO, cacheKey, Boolean.TRUE);
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

        // 排除订单优先级：暂缓订单
        CollUtil.filter(salesOrderPoolList, pool -> !"5".equals(pool.getScmPriority()));
        contextDTO.setSalesOrderPoolList(salesOrderPoolList);
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
        queryVO.setMonthPlanVersion(factoryProductionVersion.getMonthPlanVersion());

        DataDTO dataDTO = dataManager.buildDataDTO(queryVO);
        List<FactoryMonthPlanProductionFinalResult> factoryMonthPlanProdFinalList = dataManager.listMonthPlans(dataDTO);

        List<FactoryMonthPlanFinalAdjustVo> resultList = BeanUtil.copyToList(factoryMonthPlanProdFinalList, FactoryMonthPlanFinalAdjustVo.class);
        contextDTO.setFactoryMonthPlanProdFinalList(resultList);
        if (PubUtil.isNotEmpty(resultList) && StringUtils.isEmpty(contextDTO.getProductionVersion())) {
            // 月度计划排产版本
            contextDTO.setProductionVersion(resultList.get(0).getProductionVersion());
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
        // 初始化通用
        initCommon(contextDTO);
        // 校验年月是否为空
        Assert.isFalse(contextDTO.getMpYear() == null || contextDTO.getMpMonth() == null, I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.yearMonthEmpty"));
        // 特殊规则检查
        specialCheck(contextDTO);
        // 获取定稿的排产版本
        MpFactoryProductionVersion factoryProductionVersion = getIsFinalVersion(contextDTO);
        // 月度生产计划还未定稿，抛出异常
        Assert.isFalse(factoryProductionVersion == null, () -> {
            String msg = StrUtil.format(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.notFinalMonthPlan"),
                    contextDTO.getYearMonth());
            return new BusinessException(msg);
        });
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
                .collect(Collectors.groupingBy(FactoryMonthPlanFinalAdjustVo::getMaterialCode));
        // 遍历销售订单列表，匹配生产计划
        for (SalesOrderPool salesOrder : salesOrderPoolList) {
            String materialCode = salesOrder.getOriMaterialCode();
            // 物料编码为空则跳过
            if (StringUtils.isEmpty(materialCode)) {
                continue;
            }
            matchMonthPlanList(contextDTO, resultList, materialCode, monthPlanMap,
                    Convert.toInt(salesOrder.getOrdQty(),0), ApsConstant.FALSE, salesOrder.getId());
        }
        return resultList;
    }

    /**
     * 构建结构调整明细（月度计划有，无订单）
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
            setPreviousNetQty(adjustDetailVo, monthPlan);
            // 调整明细来源
            adjustDetailVo.setAdjustItemSource(AdjustItemSourceEnum.MONTH_PLAN.getCode());
            // 添加到结果集
            resultList.add(adjustDetailVo);
        }
        return resultList;
    }

    protected void matchMonthPlanList(MpRollAdjustContextDTO contextDTO, List<MpAdjustDetailVo> resultList,
                                      String materialCode, Map<String, List<FactoryMonthPlanFinalAdjustVo>> monthPlanMap,
                                      Integer ordQty, String isTrial, Long busiId) {
        // 根据物料编码获取对应的月度生产计划列表
        List<FactoryMonthPlanFinalAdjustVo> matchMonthPlanProdList = monthPlanMap.get(materialCode);
        if (PubUtil.isEmpty(matchMonthPlanProdList)) {
            // 创建基础通用字段
            MpAdjustDetailVo emptyAdjustVo = createBaseMpAdjustDetailVo(contextDTO, materialCode, ordQty, isTrial);
            // 设置月度生产计划关联的字段
            setPlanRelatedFields(contextDTO, emptyAdjustVo, null, busiId);
            // 调整前净需求量
            setPreviousNetQty(emptyAdjustVo, null);
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
            setPreviousNetQty(adjustDetailVo, monthPlan);
            // 设置调整明细来源
            setAdjustItemSource(adjustDetailVo);
            // 添加到结果集
            resultList.add(adjustDetailVo);
        }
    }

    /**
     * 设置调整明细来源
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
     * @param contextDTO
     * @param adjustDetailVo
     * @param monthPlan
     * @param busiId
     */
    protected void setPlanRelatedFields(MpRollAdjustContextDTO contextDTO, MpAdjustDetailVo adjustDetailVo, FactoryMonthPlanFinalAdjustVo monthPlan, Long busiId) {
        // 物料编码
        String materialCode = adjustDetailVo.getMaterialCode();
        // SKU与施工（示方书）关系
        Map<String, MdmSkuConstructionRef> mdmSkuConstructionRefMap = contextDTO.getMdmSkuConstructionRefMap();
        MdmSkuConstructionRef skuConstructionRef = MapUtils.getObject(mdmSkuConstructionRefMap, materialCode, new MdmSkuConstructionRef());
        // 胎胚号
        adjustDetailVo.setEmbryoCode(skuConstructionRef.getEmbryoCode());
        // SKU与结构关系列表
        Map<String, MdmSkuStructureRef> mdmSkuStructureRefMap = contextDTO.getMdmSkuStructureRefMap();
        MdmSkuStructureRef skuStructureRef = MapUtils.getObject(mdmSkuStructureRefMap, materialCode, new MdmSkuStructureRef());
        // 结构名称
        adjustDetailVo.setStructureName(skuStructureRef.getStructureName());
        // 月计划结构转产
        Map<String, List<MpStructureAllocation>> structureAllocationMap = contextDTO.getStructureAllocationMap();
        List<MpStructureAllocation> structureAllocationList = MapUtils.getObject(structureAllocationMap, adjustDetailVo.getStructureName(), new ArrayList<>());
        // 排产机台,多个机台用逗号分隔
        adjustDetailVo.setScheduledMachines(getCxMachineCodes(structureAllocationList));
        // 试制量试计划
        Map<String, List<MpTrialPlan>> mpTrialPlanMap = contextDTO.getMpTrialPlanMap();
        List<MpTrialPlan> trialPlanList = MapUtils.getObject(mpTrialPlanMap, materialCode, new ArrayList<>());
        if (monthPlan == null) {
            // SKU日硫化产能
            Map<String, MdmSkuLhCapacity> mdmSkuLhCapacityMap = contextDTO.getMdmSkuLhCapacityMap();
            // 物料信息
            Map<String, MdmMaterialInfo> mdmMaterialInfoMap = contextDTO.getMdmMaterialInfoMap();
            MdmSkuLhCapacity skuLhCapacity = MapUtils.getObject(mdmSkuLhCapacityMap, materialCode, new MdmSkuLhCapacity());
            MdmMaterialInfo materialInfo = MapUtils.getObject(mdmMaterialInfoMap, materialCode, new MdmMaterialInfo());

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
            adjustDetailVo.setDayVulcanizationQty(Convert.toInt(skuLhCapacity.getStandardCapacity(),0) / 2);
            adjustDetailVo.setCuringTime(skuLhCapacity.getVulcanizationTime());
            adjustDetailVo.setMainMaterialDesc(skuConstructionRef.getMainMaterialDesc());
            adjustDetailVo.setProductStatus(skuConstructionRef.getTrialStatus());
            adjustDetailVo.setConstructionStage(ConstructionStageEnum.FORMAL_PRODUCTION.getStage());
            // 试制量制关联字段设置
            if (ApsConstant.TRUE.equals(adjustDetailVo.getIsTrial())) {
                // 获取试制量试
                MpTrialPlan trialPlan = getMpTrialPlan(trialPlanList, busiId);
                // 施工阶段
                adjustDetailVo.setConstructionStage(trialPlan.getTrialStatus());
                // 产品状态
                String productStatus = null;
                if (ConstructionStageEnum.MEASUREMENT.getStage().equals(trialPlan.getTrialStatus())) {
                    productStatus = ConstructionStageEnum.MEASUREMENT_FLAG;
                }
                if (ConstructionStageEnum.TRIAL_PRODUCTION.getStage().equals(trialPlan.getTrialStatus())) {
                    productStatus = ConstructionStageEnum.TRIAL_FLAG;
                }
                adjustDetailVo.setProductStatus(productStatus);
                // 紧急程度
                adjustDetailVo.setUrgencyType(trialPlan.getUrgencyType());
                // 制造示方书号
                adjustDetailVo.setEmbryoNo(trialPlan.getEmbryoNo());
                // 试制量试ID
                adjustDetailVo.setTrialPlanId(Convert.toStr(trialPlan.getId(), null));
            }
            return;
        }
        // 有月度生产计划时，赋值关联字段
        adjustDetailVo.setIsSkuAdd(ApsConstant.FALSE);
        adjustDetailVo.setMesMaterialCode(monthPlan.getMesMaterialCode());
        adjustDetailVo.setMaterialDesc(monthPlan.getMaterialDesc());
        adjustDetailVo.setProductTypeCode(monthPlan.getProductTypeCode());
        adjustDetailVo.setProductStatus(monthPlan.getProductStatus());
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
            // 获取试制量试
            MpTrialPlan trialPlan = getMpTrialPlan(trialPlanList, busiId);
            // 紧急程度
            adjustDetailVo.setUrgencyType(trialPlan.getUrgencyType());
            // 施工阶段
            adjustDetailVo.setConstructionStage(trialPlan.getTrialStatus());
            // 试制量试ID
            adjustDetailVo.setTrialPlanId(Convert.toStr(trialPlan.getId(), null));
        }
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
     * @param adjustDetailVo
     * @param monthPlan
     */
    protected void setPreviousNetQty(MpAdjustDetailVo adjustDetailVo, FactoryMonthPlanFinalAdjustVo monthPlan) {
        if (ApsConstant.TRUE.equals(adjustDetailVo.getIsTrial())) {
            // 当为试制量试时，设置为空
            adjustDetailVo.setPreviousNetQty(null);
            return;
        }
        if (monthPlan == null) {
            return;
        }
        // 获取上周的周数
        int week = getWeekNumber(new Date());
        if (week > 1) {
            week = week - 1;
        }
        Integer previousNetQty = Convert.toInt(monthPlan.getTotalQty(),0);
        Integer adjustQty = Convert.toInt(monthPlan.getFieldValueByFieldName(BusiConstant.WeekRollAdjust.FIELD_PREFIX_ADJUST_QTY + week),0);
        if (adjustQty != 0 && week > 0) {
            previousNetQty = adjustQty;
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
     * @param contextDTO
     */
    protected void createAdjustRequire(MpRollAdjustContextDTO contextDTO) {
        DpDemandPlan queryVo = new DpDemandPlan();
        queryVo.setFactoryCode(contextDTO.getFactoryCode());
        queryVo.setYear(contextDTO.getMpYear());
        queryVo.setMonth(contextDTO.getMpMonth());
        queryVo.setMonthPlanVersion(contextDTO.getMonthPlanVersion());
        queryVo.setProductionVersion(contextDTO.getProductionVersion());
        queryVo.setStructureName(contextDTO.getStructureName());
        log.info("生成调整需求计划 ==> factoryCode:{} year:{} month:{} monthPlanVersion:{} productionVersion:{} structureName:{}",
                queryVo.getFactoryCode(), queryVo.getYear(), queryVo.getMonth(), queryVo.getMonthPlanVersion(), queryVo.getProductionVersion(),
                queryVo.getStructureName());
        List<DpDemandPlan> dpDemandPlanList = dpDemandPlanService.createAdjustRequire(queryVo);
        contextDTO.setDpDemandPlanList(dpDemandPlanList);
    }

    /**
     * 计算型腔、活块可用量最大值
     * @param contextDTO
     */
    protected List<DailyMouldAvailabilityResult> calculateMoldCavityInsertMaxValue(MpRollAdjustContextDTO contextDTO) throws Exception {
        LocalDate monthStart = LocalDate.of(contextDTO.getMpYear(), contextDTO.getMpMonth(), ProductionConstant.MONTH_START_DAY);
        return moldCavityInsertMaxValueCalculator.moldCavityInsertMaxValueCalculator(contextDTO.getMpYear(), contextDTO.getMpMonth(),
                contextDTO.getFactoryCode(),  DateUtils.getDate(monthStart.with(TemporalAdjusters.lastDayOfMonth())), contextDTO.getAdjustMonthPlanVersion());
    }

    /**
     * 设置型腔、活块数量
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
            }else {
                adjust.setMouldCavityQty(0);
            }
            // 设置活块数量
            String typeBlockKey = adjust.getMaterialDesc();
            if (insertResults != null && insertResults.containsKey(typeBlockKey)) {
                adjust.setTypeBlockQty(MapUtils.getInteger(insertResults, typeBlockKey, 0));
            }else {
                adjust.setTypeBlockQty(0);
            }
        }
    }

    /**
     * 设置是否特殊材料
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
                adjust.setHeightQty(Convert.toInt(sum,0));
                // 设置中优先级
                sum = demandPlanList.stream()
                        .filter(e -> e.getMidQty() != null)
                        .mapToInt(DpDemandPlan::getMidQty)
                        .sum();
                adjust.setMidQty(Convert.toInt(sum,0));
                // 设置暂缓订单
                sum = demandPlanList.stream()
                        .filter(e -> e.getPostponeQty() != null)
                        .mapToInt(DpDemandPlan::getPostponeQty)
                        .sum();
                adjust.setPostponeQty(Convert.toInt(sum,0));
                // 设置周期排产储备
                sum = demandPlanList.stream()
                        .filter(e -> e.getCycleReserveQty() != null)
                        .mapToInt(DpDemandPlan::getCycleReserveQty)
                        .sum();
                adjust.setCycleReserveQty(Convert.toInt(sum,0));
                // 设置常规储备
                sum = demandPlanList.stream()
                        .filter(e -> e.getConventionReserveQty() != null)
                        .mapToInt(DpDemandPlan::getConventionReserveQty)
                        .sum();
                adjust.setConventionReserveQty(Convert.toInt(sum,0));
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
                adjust.setCurrentNetQty(Convert.toInt(netQtySum,0));
            }
        }

    }

    /**
     * 判断是否只有常规储备有值，其他字段无值（0或null视为无值）
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
            // 计算：day1~targetDay的累计值
            Integer totalScheduledQty = calculateQty(planGroupMap, materialCode, maxDayOfMonth);
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
            // 计算: 调整量 = 净需求 - 计划剩余排产量
            Integer pendingQty = Convert.toInt(vo.getCurrentNetQty(),0) - Convert.toInt(vo.getMonthUnScheduledQty(),0);
            vo.setPendingQty(pendingQty);
            // 确认调整量默认等于待调整量
            vo.setConfirmAdjustQty(pendingQty);
            // 计算：净需求变动 = 净需求 - 调整前净需求量
            Integer netQtyChange = Convert.toInt(vo.getCurrentNetQty(),0) - Convert.toInt(vo.getPreviousNetQty(),0);
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
     * 根据结构名称和物料编码分组汇总订单量
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
     * @param vo 待分组的VO
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
     * @param existVo 已存在的VO
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
     * @param contextDTO 周程滚动调整上下文对象
     */
    protected Integer getLockEndDay(MpRollAdjustContextDTO contextDTO){
        return mpAdjustStructureInService.getLockEndDay(contextDTO);
    }

    /**
     * 初始结构开始日\收尾日
     * @param contextDTO 周程滚动调整上下文对象
     */
    protected void initStructureStartAndEndDay(MpRollAdjustContextDTO contextDTO){
        mpAdjustStructureInService.initStructureStartAndEndDay(contextDTO);
    }

    /**
     * 判断是否特殊材料
     * @param targetEmbryoCode 目标胚胎编码
     * @param mdmMaterialConsumeDetailList BOM物料消耗明细列表
     * @param specialMaterialList 特殊材料清单列表
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
