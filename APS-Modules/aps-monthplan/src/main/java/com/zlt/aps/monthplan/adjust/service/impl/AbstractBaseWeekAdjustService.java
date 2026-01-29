package com.zlt.aps.monthplan.adjust.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Maps;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.ConstructionStageEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.exception.BusinessException;
import com.tlt.aps.utils.ThreadPoolUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.constant.BusiConstant;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.maindata.mapper.MdmMaterialConsumeDetailMapper;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMonthSurplusEntityMapper;
import com.zlt.aps.maindata.mapper.MdmSkuConstructionRefEntityMapper;
import com.zlt.aps.maindata.mapper.MdmSkuLhCapacityEntityMapper;
import com.zlt.aps.maindata.mapper.MdmSkuStructureRefEntityMapper;
import com.zlt.aps.maindata.mapper.MpMonthPlanMonitorEntityMapper;
import com.zlt.aps.maindata.mapper.MpTrialPlanEntityMapper;
import com.zlt.aps.maindata.mapper.RawSpecialMaterialRecordEntityMapper;
import com.zlt.aps.monthplan.adjust.mapper.MpAdjustResultEntityMapper;
import com.zlt.aps.monthplan.adjust.mapper.MpAdjustStructureInEntityMapper;
import com.zlt.aps.monthplan.adjust.mapper.MpAdjustStructureOutEntityMapper;
import com.zlt.aps.monthplan.adjust.service.IMpAdjustResultService;
import com.zlt.aps.monthplan.adjust.service.IMpAdjustStructureInService;
import com.zlt.aps.monthplan.adjust.service.IMpAdjustStructureLogService;
import com.zlt.aps.monthplan.adjust.service.IMpWeekAdjustService;
import com.zlt.aps.monthplan.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialConsumeDetail;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmMonthSurplus;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductStock;
import com.zlt.aps.monthplan.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.aps.monthplan.api.domain.entity.MdmSkuLhCapacity;
import com.zlt.aps.monthplan.api.domain.entity.MdmSkuStructureRef;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustResult;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustStructureIn;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustStructureLog;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustStructureOut;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MpMonthPlanMonitor;
import com.zlt.aps.monthplan.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.monthplan.api.domain.entity.MpTrialPlan;
import com.zlt.aps.monthplan.api.domain.entity.RawSpecialMaterialRecord;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.monthplan.api.domain.vo.MpAdjustDetailVo;
import com.zlt.aps.monthplan.common.utils.DistributedVersionGenerator;
import com.zlt.aps.monthplan.common.utils.StringUtil;
import com.zlt.aps.monthplan.demand.mapper.SalesOrderPoolEntityMapper;
import com.zlt.aps.monthplan.demand.service.IDpDemandPlanService;
import com.zlt.aps.monthplan.factory.mapper.FactoryMonthPlanProductionFinalResultEntityMapper;
import com.zlt.aps.monthplan.factory.mapper.MpFactoryProductionVersionMapper;
import com.zlt.aps.monthplan.factory.mapper.MpStructureAllocationEntityMapper;
import com.zlt.aps.monthplan.factory.service.impl.MoldCavityInsertMaxValueCalculatorImpl;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.dao.basedao.BaseDao;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.getIntOrDefault;

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
    protected SalesOrderPoolEntityMapper salesOrderPoolEntityMapper;

    @Autowired
    protected MpFactoryProductionVersionMapper factoryProductionVersionMapper;

    @Autowired
    protected MpTrialPlanEntityMapper mpTrialPlanEntityMapper;

    @Autowired
    protected MdmMonthSurplusEntityMapper mdmMonthSurplusEntityMapper;

    @Autowired
    protected MdmMaterialInfoEntityMapper mdmMaterialInfoEntityMapper;

    @Autowired
    protected MdmSkuLhCapacityEntityMapper mdmSkuLhCapacityEntityMapper;

    @Autowired
    protected MdmSkuConstructionRefEntityMapper mdmSkuConstructionRefEntityMapper;

    @Autowired
    protected MdmSkuStructureRefEntityMapper mdmSkuStructureRefEntityMapper;

    @Autowired
    protected IMesItfService mesItfService;

    @Autowired
    protected DistributedVersionGenerator versionGenerator;

    @Autowired
    protected MpMonthPlanMonitorEntityMapper mpMonthPlanMonitorEntityMapper;

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
    protected IMpAdjustStructureLogService mpAdjustLogService;

    @Autowired
    protected IMpAdjustStructureInService mpAdjustStructureInService;

    @Autowired
    protected MoldCavityInsertMaxValueCalculatorImpl moldCavityInsertMaxValueCalculator;

    @Autowired
    protected RawSpecialMaterialRecordEntityMapper rawSpecialMaterialRecordMapper;

    @Autowired
    protected MdmMaterialConsumeDetailMapper mdmMaterialConsumeDetailMapper;

    @Autowired
    protected BaseDao baseDao;


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
        // 排序调整明细
        sortAdjustDetailList(contextDTO);
        // 保存调整明细
        saveAdjustDetailList(contextDTO);
    }


    /**
     * 后置检查
     * @param contextDTO
     */
    protected void postCheck(MpRollAdjustContextDTO contextDTO) {
        // 检查调整明细列表中的必填字段是否为空
        List<String> errorMsgList = checkEmptyFields(contextDTO.getAdjustDetailList());
        Assert.isFalse(PubUtil.isNotEmpty(errorMsgList), () -> {
            return new BusinessException(String.join(BusiConstant.WeekRollAdjust.SPLIT_NEW_LINE, errorMsgList));
        });
        // 检查sku与施工示方书关系是否有数据
        List<String> notExistMsgList = checkExistSkuConstructionRef(contextDTO);
        Assert.isFalse(PubUtil.isNotEmpty(notExistMsgList), () -> {
            return new BusinessException(String.join(BusiConstant.WeekRollAdjust.SPLIT_NEW_LINE, notExistMsgList));
        });
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
        return Collections.emptyMap();
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
        return Comparator
                .comparing(MpAdjustDetailVo::getStructureName,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(MpAdjustDetailVo::getPendingQty,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MpAdjustDetailVo::getMaterialCode,
                        Comparator.nullsLast(Comparator.naturalOrder()));
    }

    @Override
    public void autoAdjust(MpRollAdjustContextDTO contextDTO) throws BusinessException {
        //1、执行自动调整
        doAutoAdjust(contextDTO);
        //2、保存调整结果
        saveMpAdjustResult(contextDTO);
        //3、回填实际调整
        backfillRealAdjustResult(contextDTO);
    }

    /**
     * 回填实际调整
     * @param contextDTO 周程滚动上下文
     */
    protected void backfillRealAdjustResult(MpRollAdjustContextDTO contextDTO){

    }

    /**
     * 保存调整结果
     * @param contextDTO
     */
    private void saveMpAdjustResult(MpRollAdjustContextDTO contextDTO){
        List<FactoryMonthPlanFinalAdjustVo> factoryMonthPlanProdFinalList = contextDTO.getFactoryMonthPlanProdFinalList();
        if (PubUtil.isEmpty(factoryMonthPlanProdFinalList)){
            return;
        }
        //1、根据调整版本 先删除(物理)
        mpAdjustResultService.deleteAdjustResultByVersion(contextDTO.getFactoryCode(),
                String.valueOf(contextDTO.getMpYear()),String.valueOf(contextDTO.getMpMonth()),contextDTO.getVersion());
        //2、保存调整记录
        MpAdjustResult mpAdjustResult;
        List<MpAdjustResult> mpAdjustResultList = new ArrayList<>();
        for (FactoryMonthPlanFinalAdjustVo finalAdjustVo:factoryMonthPlanProdFinalList){
            mpAdjustResult = new MpAdjustResult();
            BeanUtils.copyProperties(finalAdjustVo,mpAdjustResult);
            mpAdjustResult.setId(null);
            mpAdjustResult.setAdjustType(contextDTO.getAdjustType());
            mpAdjustResult.setVersion(contextDTO.getVersion());
            mpAdjustResult.setTotalPlanQty(finalAdjustVo.getTotalQty());
            if (StringUtil.isEmptyWithTrim(mpAdjustResult.getIsLockSchedule())){
                mpAdjustResult.setIsLockSchedule(YesOrNoEnum.NO.getCode());
            }
            mpAdjustResultList.add(mpAdjustResult);
        }
        baseDao.insertBatch(mpAdjustResultList);
        contextDTO.setAdjustResultList(mpAdjustResultList);
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
            // 4、更新月度生产计划
            updateMonthPlanList(contextDTO);
            // 5、新增月度生产计划
            insertMonthPlanList(contextDTO);
            // 6、更新调整明细
            updateAdjustDetailList(contextDTO);
            // 7、更新试制量制计划
            updateTrialPlanList(contextDTO);
            // 8、记录调整操作日志 TODO
            log.info("周程调整确认流程执行完成");
        } catch (Exception e) {
            log.error("周程调整确认流程执行异常", e);
            throw new BusinessException("周程调整确认失败：" + e.getMessage());
        }
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
        Map<String, List<Long>> adjustDetailMap = adjustDetailList.stream()
                .filter(obj -> StringUtils.isNotEmpty(obj.getMaterialCode())
                        && obj.getTrialPlanId() != null)
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
            List<Long> trialPlanIdList = adjustDetailMap.get(materialCode);
            if (PubUtil.isEmpty(trialPlanIdList)) {
                continue;
            }
            // 获取最早有值的日期
            Integer day = getFirstHasValueDay(adjustResult);
            // 获取当前日期
            Date productionDate = getCurrentDate(contextDTO.getMpYear(), contextDTO.getMpMonth(), day);
            for (Long trialPlanId : trialPlanIdList) {
                MpTrialPlan trialPlan = new MpTrialPlan();
                trialPlan.setId(trialPlanId);
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
        List<FactoryMonthPlanFinalAdjustVo> monthPlanProdFinalList = contextDTO.getFactoryMonthPlanProdFinalList();
        if (PubUtil.isEmpty(adjustDetailList) || PubUtil.isEmpty(adjustResultList)) {
            log.warn("新增月度生产计划：调整明细列表或者调整结果列表为空，直接返回");
            return;
        }
        // 工单号
        String productionNo = "";
        // 最新需求计划版本
        String lastMonthPlanVersion = "";
        if (PubUtil.isNotEmpty(monthPlanProdFinalList)) {
            productionNo = monthPlanProdFinalList.get(0).getProductionNo();
            lastMonthPlanVersion = monthPlanProdFinalList.get(0).getLastMonthPlanVersion();
        }
        // 调整结果按照物料编码分组
        Map<String, List<MpAdjustResult>> adjustDetailMap = buildMaterialCodeAdjustMap(adjustResultList);
        // 月度生产计划排程结果
        List<FactoryMonthPlanProductionFinalResult> factoryMonthPlanProdFinalList = new ArrayList<>();
        // 遍历调整明细，获取新增的SKU并新增到月度生产计划
        for (MpAdjustDetailVo adjustDetailVo : adjustDetailList) {
            String isSkuAdd = adjustDetailVo.getIsSkuAdd();
            if (!ApsConstant.TRUE.equals(isSkuAdd)) {
                continue;
            }
            String materialCode = adjustDetailVo.getMaterialCode();
            MpAdjustResult adjustResult = getFirstAdjustResult(adjustDetailMap, materialCode);
            if (adjustResult == null) {
                continue;
            }
            FactoryMonthPlanProductionFinalResult monthPlan = new FactoryMonthPlanProductionFinalResult();
            BeanUtils.copyProperties(adjustResult, monthPlan);
            monthPlan.setProductionNo(productionNo);
            monthPlan.setLastMonthPlanVersion(lastMonthPlanVersion);
            monthPlan.setTotalQty(adjustResult.getTotalPlanQty());
            monthPlan.setYearMonth(Integer.valueOf(adjustResult.getYear() + "" + String.format("%02d",adjustResult.getMonth())));
            factoryMonthPlanProdFinalList.add(monthPlan);
        }
        // 新增月度生产计划
        try {
            baseDao.insertBatch(factoryMonthPlanProdFinalList);
            log.info("新增月度生产计划成功，共新增:{}条记录", factoryMonthPlanProdFinalList.size());
        } catch (Exception e) {
            log.error("新增月度生产计划批量操作异常", e);
            throw new RuntimeException("新增月度生产计划失败", e);
        }

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
                Integer totalQty = monthPLanList.stream().mapToInt(v -> getIntOrDefault(v.getTotalQty())).sum();
                actualAdjustQty = totalPlanQty - totalQty;
            }
            // 设置实际调整
            adjustDetailVo.setActualAdjustQty(actualAdjustQty);
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
        if (PubUtil.isEmpty(adjustResultList) || PubUtil.isEmpty(factoryMonthPlanProdFinalList)) {
            log.warn("更新月度生产计划：调整结果列表或月度计划列表为空，直接返回");
            return;
        }
        // 调整结果按照物料编号分组
        Map<String, List<MpAdjustResult>> adjustDetailMap = buildMaterialCodeAdjustMap(adjustResultList);
        // 遍历生产计划列表匹配调整结果（更新计划量、开始日期、结束日期、调整量)
        for (FactoryMonthPlanFinalAdjustVo monthPlanVo : factoryMonthPlanProdFinalList) {
            String materialCode = monthPlanVo.getMaterialCode();
            if (StringUtils.isEmpty(materialCode)) {
                continue;
            }
            MpAdjustResult adjustResult = getFirstAdjustResult(adjustDetailMap, materialCode);
            if (adjustResult == null) {
                log.warn("更新月度生产计划：物料编号:{}未查询到对应调整结果，跳过", materialCode);
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
            // 获取周数
            int week = getWeekNumber(new Date());
            monthPlanVo.setFieldValueByFieldName(BusiConstant.WeekRollAdjust.FIELD_PREFIX_ADJUST_QTY + week, adjustResult.getTotalPlanQty());
        }
        // 更新月度生产计划
        try {
            baseDao.updateBatch(factoryMonthPlanProdFinalList);
            log.info("更新月度生产计划成功，共更新:{}条记录", factoryMonthPlanProdFinalList.size());
        } catch (Exception e) {
            log.error("更新月度生产计划批量操作异常", e);
            throw new RuntimeException("更新月度生产计划失败", e);
        }

    }

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
     * 生成调整明细(业务逻辑处理)
     */
    public abstract void doGenerateAdjust(MpRollAdjustContextDTO contextDTO);

    /**
     * 自动调整(业务逻辑处理)
     */
    public abstract void doAutoAdjust(MpRollAdjustContextDTO contextDTO);

    /**
     * 调整确认(业务逻辑处理)
     */
    public abstract void doConfirmAdjust(MpRollAdjustContextDTO contextDTO);


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
            ThreadPoolUtil.shutdown();
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
            contextDTO.setYearMonth(Integer.valueOf(contextDTO.getMpYear() + "" + String.format("%02d",contextDTO.getMpMonth())));
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

        LambdaQueryWrapper<MpStructureAllocation> queryWrapper = new LambdaQueryWrapper<>();
        buildStructureAllocationCondition(queryWrapper, queryVO);
        List<MpStructureAllocation> structureAllocationList = mpStructureAllocationEntityMapper.selectList(queryWrapper);
        contextDTO.setStructureAllocationList(structureAllocationList);
    }

    /**
     * 构建月计划结构转产条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildStructureAllocationCondition(LambdaQueryWrapper<MpStructureAllocation> queryWrapper, MpStructureAllocation queryVO) {
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getFactoryCode()), MpStructureAllocation::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MpStructureAllocation::getYear, queryVO.getYear());
        queryWrapper.eq(MpStructureAllocation::getMonth, queryVO.getMonth());
        queryWrapper.eq(MpStructureAllocation::getIsDelete, YesOrNoEnum.NO.getValue());
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getProductionVersion()), MpStructureAllocation::getProductionVersion, queryVO.getProductionVersion());
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

        LambdaQueryWrapper<MpMonthPlanMonitor> queryWrapper = new LambdaQueryWrapper<>();
        buildPlanMonitorCondition(queryWrapper, queryVO);
        List<MpMonthPlanMonitor> planMonitorList = mpMonthPlanMonitorEntityMapper.selectList(queryWrapper);
        contextDTO.setMpMonthPlanMonitorList(planMonitorList);
    }

    /**
     * 构建月度硫化监控条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildPlanMonitorCondition(LambdaQueryWrapper<MpMonthPlanMonitor> queryWrapper, MpMonthPlanMonitor queryVO) {
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getFactoryCode()), MpMonthPlanMonitor::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MpMonthPlanMonitor::getYear, queryVO.getYear());
        queryWrapper.eq(MpMonthPlanMonitor::getMonth, queryVO.getMonth());
        queryWrapper.eq(MpMonthPlanMonitor::getIsDelete, YesOrNoEnum.NO.getValue());
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getMonthPlanVersion()), MpMonthPlanMonitor::getMonthPlanVersion, queryVO.getMonthPlanVersion());
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getProductionVersion()), MpMonthPlanMonitor::getProductionVersion, queryVO.getProductionVersion());
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

        LambdaQueryWrapper<MdmSkuConstructionRef> queryWrapper = new LambdaQueryWrapper<>();
        buildSkuConstructionRefCondition(queryWrapper, queryVO);
        List<MdmSkuConstructionRef> mdmSkuConstructionRefList = mdmSkuConstructionRefEntityMapper.selectList(queryWrapper);
        contextDTO.setMdmSkuConstructionRefList(mdmSkuConstructionRefList);
    }

    /**
     * 构建SKU与施工（示方书）关系条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildSkuConstructionRefCondition(LambdaQueryWrapper<MdmSkuConstructionRef> queryWrapper, MdmSkuConstructionRef queryVO) {
        queryWrapper.eq(MdmSkuConstructionRef::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MdmSkuConstructionRef::getIsDelete, YesOrNoEnum.NO.getValue());
    }

    /**
     * 初始化sku与结构关系
     *
     * @param contextDTO
     */
    private void initSkuStructureRef(MpRollAdjustContextDTO contextDTO) {
        MdmSkuStructureRef queryVO = new MdmSkuStructureRef();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());

        LambdaQueryWrapper<MdmSkuStructureRef> queryWrapper = new LambdaQueryWrapper<>();
        buildSkuStructureRefCondition(queryWrapper, queryVO);
        List<MdmSkuStructureRef> mdmSkuStructureRefList = mdmSkuStructureRefEntityMapper.selectList(queryWrapper);
        contextDTO.setMdmSkuStructureRefList(mdmSkuStructureRefList);
    }

    /**
     * 构建sku与结构关系条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildSkuStructureRefCondition(LambdaQueryWrapper<MdmSkuStructureRef> queryWrapper, MdmSkuStructureRef queryVO) {
        queryWrapper.eq(MdmSkuStructureRef::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MdmSkuStructureRef::getIsDelete, YesOrNoEnum.NO.getValue());
    }

    /**
     * 初始化BOM物料消耗明细
     *
     * @param contextDTO
     */
    private void initMaterialConsumeDetail(MpRollAdjustContextDTO contextDTO) {
        MdmMaterialConsumeDetail queryVO = new MdmMaterialConsumeDetail();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());

        LambdaQueryWrapper<MdmMaterialConsumeDetail> queryWrapper = new LambdaQueryWrapper<>();
        buildMaterialConsumeDetailCondition(queryWrapper, queryVO);
        List<MdmMaterialConsumeDetail> materialConsumeDetailList = mdmMaterialConsumeDetailMapper.selectList(queryWrapper);
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

        LambdaQueryWrapper<RawSpecialMaterialRecord> queryWrapper = new LambdaQueryWrapper<>();
        buildSpecialMaterialCondition(queryWrapper, queryVO);
        List<RawSpecialMaterialRecord> specialMaterialList = rawSpecialMaterialRecordMapper.selectList(queryWrapper);
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

        LambdaQueryWrapper<MdmMaterialInfo> queryWrapper = new LambdaQueryWrapper<>();
        buildMaterialInfoCondition(queryWrapper, queryVO);
        List<MdmMaterialInfo> mdmMaterialInfoList = mdmMaterialInfoEntityMapper.selectList(queryWrapper);
        contextDTO.setMdmMaterialInfoList(mdmMaterialInfoList);
    }

    /**
     * 构建BOM物料消耗明细条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildMaterialConsumeDetailCondition(LambdaQueryWrapper<MdmMaterialConsumeDetail> queryWrapper, MdmMaterialConsumeDetail queryVO) {
        queryWrapper.eq(MdmMaterialConsumeDetail::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MdmMaterialConsumeDetail::getIsDelete, YesOrNoEnum.NO.getValue());
    }

    /**
     * 构建特殊材料条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildSpecialMaterialCondition(LambdaQueryWrapper<RawSpecialMaterialRecord> queryWrapper, RawSpecialMaterialRecord queryVO) {
        queryWrapper.eq(RawSpecialMaterialRecord::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(RawSpecialMaterialRecord::getIsDelete, YesOrNoEnum.NO.getValue());
    }

    /**
     * 构建物料信息条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildMaterialInfoCondition(LambdaQueryWrapper<MdmMaterialInfo> queryWrapper, MdmMaterialInfo queryVO) {
        queryWrapper.eq(MdmMaterialInfo::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MdmMaterialInfo::getIsDelete, YesOrNoEnum.NO.getValue());
    }

    /**
     * 初始化sku日硫化产能
     *
     * @param contextDTO
     */
    private void initSkuLhCapacity(MpRollAdjustContextDTO contextDTO) {
        MdmSkuLhCapacity queryVO = new MdmSkuLhCapacity();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());

        LambdaQueryWrapper<MdmSkuLhCapacity> queryWrapper = new LambdaQueryWrapper<>();
        buildSkuLhCapacityCondition(queryWrapper, queryVO);
        List<MdmSkuLhCapacity> mdmSkuLhCapacityList = mdmSkuLhCapacityEntityMapper.selectList(queryWrapper);
        contextDTO.setMdmSkuLhCapacityList(mdmSkuLhCapacityList);
    }

    /**
     * 构建sku日硫化产能条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildSkuLhCapacityCondition(LambdaQueryWrapper<MdmSkuLhCapacity> queryWrapper, MdmSkuLhCapacity queryVO) {
        queryWrapper.eq(MdmSkuLhCapacity::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MdmSkuLhCapacity::getIsDelete, YesOrNoEnum.NO.getValue());
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

        LambdaQueryWrapper<MpTrialPlan> queryWrapper = new LambdaQueryWrapper<>();
        buildTrialPlanCondition(queryWrapper, queryVO);
        List<MpTrialPlan> mpTrialPlanList = mpTrialPlanEntityMapper.selectList(queryWrapper);
        contextDTO.setMpTrialPlanList(mpTrialPlanList);
    }

    /**
     * 构建试制量试计划条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildTrialPlanCondition(LambdaQueryWrapper<MpTrialPlan> queryWrapper, MpTrialPlan queryVO) {
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getFactoryCode()), MpTrialPlan::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MpTrialPlan::getYear, queryVO.getYear());
        queryWrapper.eq(MpTrialPlan::getMonth, queryVO.getMonth());
        queryWrapper.eq(MpTrialPlan::getIsDelete, YesOrNoEnum.NO.getValue());
        queryWrapper.isNull(MpTrialPlan::getProductionDate);
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

        LambdaQueryWrapper<SalesOrderPool> queryWrapper = new LambdaQueryWrapper<>();
        buildSaleOrderPoolCondition(queryWrapper, queryVO);
        List<SalesOrderPool> salesOrderPoolList = salesOrderPoolEntityMapper.selectList(queryWrapper);
        // 排除暂缓订单
        CollUtil.filter(salesOrderPoolList, pool -> !"5".equals(pool.getScmPriority()));
        contextDTO.setSalesOrderPoolList(salesOrderPoolList);
    }


    /**
     * 构建销售订单池条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildSaleOrderPoolCondition(LambdaQueryWrapper<SalesOrderPool> queryWrapper, SalesOrderPool queryVO) {
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getFactoryCode()), SalesOrderPool::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(StringUtils.isNotEmpty(queryVO.getOrderStatus()), SalesOrderPool::getOrderStatus, queryVO.getOrderStatus());
        queryWrapper.eq(SalesOrderPool::getIsDelete, YesOrNoEnum.NO.getValue());
    }


    /**
     * 初始化排产版本
     *
     * @param contextDTO
     */
    @Override
    public void initVersion(MpRollAdjustContextDTO contextDTO) {
        // 查询排产版本
        MpFactoryProductionVersion version = new MpFactoryProductionVersion();
        version.setFactoryCode(contextDTO.getFactoryCode());
        // 暂时写死 01 正常
        version.setPlanType("01");
        version.setYear(contextDTO.getMpYear());
        version.setMonth(contextDTO.getMpMonth());
        version.setIsFinal(ApsConstant.TRUE);

        LambdaQueryWrapper<MpFactoryProductionVersion> wrapper = new LambdaQueryWrapper<>();
        buildVersionCondition(wrapper, version);
        List<MpFactoryProductionVersion> versionList = factoryProductionVersionMapper.selectList(wrapper);
        contextDTO.setFactoryProductionVersionList(versionList);
    }

    /**
     * 初始化月度生产计划
     *
     * @param contextDTO
     */
    private void initMonthPlan(MpRollAdjustContextDTO contextDTO) {
        // 查询月度生产计划
        MpFactoryProductionVersion factoryProductionVersion = getIsFinalVersion(contextDTO);
        if (factoryProductionVersion == null) {
            return;
        }
        FactoryMonthPlanProductionFinalResult queryVO = new FactoryMonthPlanProductionFinalResult();
        queryVO.setFactoryCode(contextDTO.getFactoryCode());
        queryVO.setYearMonth(contextDTO.getYearMonth());
        queryVO.setMonthPlanVersion(factoryProductionVersion.getMonthPlanVersion());

        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper = new LambdaQueryWrapper<>();
        buildMonthPlanCondition(queryWrapper, queryVO);
        List<FactoryMonthPlanProductionFinalResult> factoryMonthPlanProdFinalList = factoryMonthPlanProdFinalMapper.selectList(queryWrapper);
        List<FactoryMonthPlanFinalAdjustVo> resultList = BeanUtil.copyToList(factoryMonthPlanProdFinalList, FactoryMonthPlanFinalAdjustVo.class);
        contextDTO.setFactoryMonthPlanProdFinalList(resultList);
        if (PubUtil.isNotEmpty(resultList) && StringUtils.isEmpty(contextDTO.getProductionVersion())) {
            // 月度计划排产版本
            contextDTO.setProductionVersion(resultList.get(0).getProductionVersion());
        }
    }


    /**
     * 构建排产版本条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildVersionCondition(LambdaQueryWrapper<MpFactoryProductionVersion> queryWrapper, MpFactoryProductionVersion queryVO) {
        queryWrapper.eq(MpFactoryProductionVersion::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MpFactoryProductionVersion::getYear, queryVO.getYear());
        queryWrapper.eq(MpFactoryProductionVersion::getMonth, queryVO.getMonth());
        queryWrapper.eq(MpFactoryProductionVersion::getPlanType, queryVO.getPlanType());
        queryWrapper.eq(queryVO.getIsFinal() != null, MpFactoryProductionVersion::getIsFinal, queryVO.getIsFinal());
        queryWrapper.eq(MpFactoryProductionVersion::getIsDelete, YesOrNoEnum.NO.getValue());
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
                    Convert.toInt(salesOrder.getOrdQty(),0), ApsConstant.FALSE);
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
            setPlanRelatedFields(contextDTO, adjustDetailVo, monthPlan);
            // 调整前净需求量
            setPreviousNetQty(adjustDetailVo, monthPlan);
            // 添加到结果集
            resultList.add(adjustDetailVo);
        }
        return resultList;
    }

    protected void matchMonthPlanList(MpRollAdjustContextDTO contextDTO, List<MpAdjustDetailVo> resultList,
                                      String materialCode, Map<String, List<FactoryMonthPlanFinalAdjustVo>> monthPlanMap,
                                      Integer ordQty, String isTrial) {
        // 根据物料编码获取对应的月度生产计划列表
        List<FactoryMonthPlanFinalAdjustVo> matchMonthPlanProdList = monthPlanMap.get(materialCode);
        if (PubUtil.isEmpty(matchMonthPlanProdList)) {
            // 创建基础通用字段
            MpAdjustDetailVo emptyAdjustVo = createBaseMpAdjustDetailVo(contextDTO, materialCode, ordQty, isTrial);
            // 设置月度生产计划关联的字段
            setPlanRelatedFields(contextDTO, emptyAdjustVo, null);
            // 调整前净需求量
            setPreviousNetQty(emptyAdjustVo, null);
            // 添加到结果集
            resultList.add(emptyAdjustVo);
            return;
        }
        // 月度生产计划列表不为空时，执行逻辑
        for (FactoryMonthPlanFinalAdjustVo monthPlan : matchMonthPlanProdList) {
            // 创建基础通用字段
            MpAdjustDetailVo adjustDetailVo = createBaseMpAdjustDetailVo(contextDTO, materialCode, ordQty, isTrial);
            // 设置月度生产计划关联的字段
            setPlanRelatedFields(contextDTO, adjustDetailVo, monthPlan);
            // 调整前净需求量
            setPreviousNetQty(adjustDetailVo, monthPlan);
            // 添加到结果集
            resultList.add(adjustDetailVo);
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
     * @param adjustDetailVo
     * @param monthPlan
     */
    protected void setPlanRelatedFields(MpRollAdjustContextDTO contextDTO, MpAdjustDetailVo adjustDetailVo, FactoryMonthPlanFinalAdjustVo monthPlan) {
        // 物料编码
        String materialCode = adjustDetailVo.getMaterialCode();
        // SKU与施工（示方书）关系
        Map<String, MdmSkuConstructionRef> mdmSkuConstructionRefMap = convertToSkuConstructionRefMap(contextDTO.getMdmSkuConstructionRefList());
        MdmSkuConstructionRef skuConstructionRef = MapUtils.getObject(mdmSkuConstructionRefMap, materialCode, new MdmSkuConstructionRef());
        // 胎胚号
        adjustDetailVo.setEmbryoCode(skuConstructionRef.getEmbryoCode());
        // SKU与结构关系列表
        Map<String, MdmSkuStructureRef> mdmSkuStructureRefMap = convertToSkuStructureRefMap(contextDTO.getMdmSkuStructureRefList());
        MdmSkuStructureRef skuStructureRef = MapUtils.getObject(mdmSkuStructureRefMap, materialCode, new MdmSkuStructureRef());
        // 结构名称
        adjustDetailVo.setStructureName(skuStructureRef.getStructureName());
        // 月计划结构转产
        Map<String, List<MpStructureAllocation>> structureAllocationMap = convertToStructureAllocationMap(contextDTO.getStructureAllocationList());
        List<MpStructureAllocation> structureAllocationList = MapUtils.getObject(structureAllocationMap, adjustDetailVo.getStructureName(), new ArrayList<>());
        // 排产机台,多个机台用逗号分隔
        adjustDetailVo.setScheduledMachines(getCxMachineCodes(structureAllocationList));

        if (monthPlan == null) {
            // SKU日硫化产能
            Map<String, MdmSkuLhCapacity> mdmSkuLhCapacityMap = convertToSkuLhCapacityMap(contextDTO.getMdmSkuLhCapacityList());
            // 物料信息
            Map<String, MdmMaterialInfo> mdmMaterialInfoMap = convertToMaterialInfoMap(contextDTO.getMdmMaterialInfoList());
            // 试制量试计划
            Map<String, MpTrialPlan> mpTrialPlanMap = convertToTrialPlanMap(contextDTO.getMpTrialPlanList());
            MdmSkuLhCapacity skuLhCapacity = MapUtils.getObject(mdmSkuLhCapacityMap, materialCode, new MdmSkuLhCapacity());
            MdmMaterialInfo materialInfo = MapUtils.getObject(mdmMaterialInfoMap, materialCode, new MdmMaterialInfo());
            MpTrialPlan trialPlan = MapUtils.getObject(mpTrialPlanMap, materialCode, new MpTrialPlan());

            // 无月度生产计划时，返回
            adjustDetailVo.setIsSkuAdd(ApsConstant.TRUE);
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
            // 试制量制关联字段设置
            if (ApsConstant.TRUE.equals(adjustDetailVo.getIsTrial())) {
                // 施工阶段
                adjustDetailVo.setConstructionStage(trialPlan.getTrialStatus());
                // 产品状态
                adjustDetailVo.setProductStatus(trialPlan.getTrialStatus());
                // 紧急程度
                adjustDetailVo.setUrgencyType(trialPlan.getUrgencyType());
                // 制造示方书号
                adjustDetailVo.setEmbryoNo(trialPlan.getEmbryoNo());
                // 试制量试ID
                adjustDetailVo.setTrialPlanId(trialPlan.getId());
            }
            adjustDetailVo.setProductStatus(skuConstructionRef.getTrialStatus());
            adjustDetailVo.setConstructionStage(ConstructionStageEnum.FORMAL_PRODUCTION.getStage());
            // 型腔数量、活块数量
            adjustDetailVo.setMouldCavityQty(0);
            adjustDetailVo.setTypeBlockQty(0);
            adjustDetailVo.setHeightQty(0);
            adjustDetailVo.setMidQty(0);
            adjustDetailVo.setPostponeQty(0);
            adjustDetailVo.setCycleReserveQty(0);
            adjustDetailVo.setConventionReserveQty(0);
            return;
        }
        // 有月度生产计划时，赋值关联字段
        adjustDetailVo.setIsSkuAdd(ApsConstant.FALSE);
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
        adjustDetailVo.setHeightQty(Convert.toInt(monthPlan.getHeightProductionQty(),0));
        adjustDetailVo.setMidQty(Convert.toInt(monthPlan.getMidProductionQty(),0));
        adjustDetailVo.setPostponeQty(Convert.toInt(monthPlan.getPostponeProductionQty(),0));
        adjustDetailVo.setCycleReserveQty(Convert.toInt(monthPlan.getCycleProductionQty(),0));
        adjustDetailVo.setConventionReserveQty(Convert.toInt(monthPlan.getConventionProductionQty(),0));
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
            // 当为试制量试时，设置为订单量
            adjustDetailVo.setPreviousNetQty(adjustDetailVo.getOrdQty());
            return;
        }
        if (monthPlan == null) {
            return;
        }
        // 获取上周的周数
        int week = getWeekNumber(new Date()) - 1;
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
        List<DpDemandPlan> dpDemandPlanList = dpDemandPlanService.createAdjustRequire(queryVo);
        contextDTO.setDpDemandPlanList(dpDemandPlanList);
    }

    /**
     * 计算型腔、活块可用量最大值
     * @param contextDTO
     */
    protected Map<String, Object> calculateMoldCavityInsertMaxValue(MpRollAdjustContextDTO contextDTO) throws Exception {
        return moldCavityInsertMaxValueCalculator.moldCavityInsertMaxValueCalculator(contextDTO.getMpYear(), contextDTO.getMpMonth(),
                contextDTO.getFactoryCode(), new Date(), contextDTO.getMonthPlanVersion());
    }

    /**
     * 设置型腔、活块数量
     * @param contextDTO
     */
    protected void setMoldCavityInsert(MpRollAdjustContextDTO contextDTO) {
        // 创建计时器
        StopWatch watch = new StopWatch();
        watch.start();
        Map<String, Object> moldCavityInsertMap;
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
            log.warn("计算型腔、活块可用量最大值 ==> 根据工厂:[{}] 年月:[{}] 型腔、活块可用量最大值列表为空，返回", contextDTO.getFactoryCode(),
                    contextDTO.getYearMonth());
            return;
        }
        // 调整明细列表
        List<MpAdjustDetailVo> adjustList = contextDTO.getAdjustDetailList();
        // 型腔可用量（按结构+主花纹分组）
        Map<String, Integer> cavityResults = (Map<String, Integer>) MapUtils.getObject(moldCavityInsertMap, "cavityResults", new HashMap<>());
        // 活块可用量（按物料描述分组）
        Map<String, Integer> insertResults = (Map<String, Integer>) MapUtils.getObject(moldCavityInsertMap, "insertResults", new HashMap<>());
        log.info("计算型腔、活块可用量最大值 ==> 型腔可用量:{} 活块可用量:{}", JSONObject.toJSONString(cavityResults), JSONObject.toJSONString(insertResults));
        // 遍历
        for (MpAdjustDetailVo adjust : adjustList) {
            // 设置型腔数量
            String mouldCavityKey = adjust.getStructureName() + adjust.getMainPattern();
            if (Convert.toInt(adjust.getMouldCavityQty(),0) == 0) {
                adjust.setMouldCavityQty(MapUtils.getInteger(cavityResults, mouldCavityKey, 0));
            }
            // 设置活块数量
            String typeBlockKey = adjust.getMaterialDesc();
            if (Convert.toInt(adjust.getTypeBlockQty(),0) == 0) {
                adjust.setTypeBlockQty(MapUtils.getInteger(insertResults, typeBlockKey, 0));
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
        // 调整明细列表
        List<MpAdjustDetailVo> adjustList = contextDTO.getAdjustDetailList();
        // 需求计划分组Map
        Map<String, List<DpDemandPlan>> demandPlanMap = convertToDpDemandPlanMap(dpDemandPlanList);
        // 遍历计算
        for (MpAdjustDetailVo adjust : adjustList) {
            if (StringUtils.isEmpty(adjust.getMaterialCode())) {
                continue;
            }
            // 试制量试设置净需求为订单量
            if (ApsConstant.TRUE.equals(adjust.getIsTrial())) {
                adjust.setCurrentNetQty(adjust.getOrdQty());
            } else {
                String materialCode = adjust.getMaterialCode();
                List<DpDemandPlan> dpDemandPlan = MapUtils.getObject(demandPlanMap, materialCode, new ArrayList<>());
                // 汇总排产净需求
                Integer netQtySum = dpDemandPlan.stream()
                        .filter(e -> e.getNetQty() != null)
                        .mapToInt(DpDemandPlan::getNetQty)
                        .sum();
                adjust.setCurrentNetQty(Convert.toInt(netQtySum,0));
            }
        }
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
    private Map<String, MpTrialPlan> convertToTrialPlanMap(List<MpTrialPlan> trialPlanList) {
        if (PubUtil.isEmpty(trialPlanList)) {
            return Collections.emptyMap();
        }
        return trialPlanList.stream()
                .filter(trialPlan -> StringUtils.isNotEmpty(trialPlan.getMaterialCode()))
                .collect(Collectors.toMap(
                        MpTrialPlan::getMaterialCode,
                        trialPlan -> trialPlan,
                        (existingVal, newVal) -> newVal
                ));
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


    protected List<MpAdjustDetailVo> sumByStructureAndMaterial(List<MpAdjustDetailVo> originalList) {
        if (PubUtil.isEmpty(originalList)) {
            return Collections.emptyList();
        }
        Map<String, MpAdjustDetailVo> sumMap = new HashMap<>();
        // 遍历集合，进行分组汇总
        for (MpAdjustDetailVo vo : originalList) {
            String structureName = vo.getStructureName();
            String materialCode = vo.getMaterialCode();
            String groupKey = vo.getGroupKey();
            Integer ordQty = Convert.toInt(vo.getOrdQty(),0);
            if (sumMap.containsKey(groupKey)) {
                MpAdjustDetailVo existVo = sumMap.get(groupKey);
                existVo.setOrdQty(existVo.getOrdQty() + ordQty);
            } else {
                MpAdjustDetailVo newVo = new MpAdjustDetailVo();
                BeanUtil.copyProperties(vo, newVo, Boolean.FALSE);
                newVo.setStructureName(structureName);
                newVo.setMaterialCode(materialCode);
                newVo.setOrdQty(ordQty);
                sumMap.put(groupKey, newVo);
            }
        }
        // 将Map中的汇总结果转换为List返回
        return new ArrayList<>(sumMap.values());
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
