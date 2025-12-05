package com.zlt.aps.monthplan.factory.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.constant.IncrementConstant;
import com.tlt.aps.constant.StringConstant;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.BeanCopyUtils;
import com.tlt.aps.utils.IncrementService;
import com.zlt.aps.factory.domain.vo.ProductionCalendarVO;
import com.zlt.aps.factory.utils.DateUtils;
import com.zlt.aps.maindata.domain.dto.MdmProductConstructionDto;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.service.IMdmProductConstructionService;
import com.zlt.aps.maindata.service.IMdmProductionCalendarService;
import com.zlt.aps.monthplan.api.domain.dto.FactoryMonthPlanProdResultDto;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.api.domain.vo.*;
import com.zlt.aps.monthplan.api.enums.MouldProductionLogType;
import com.zlt.aps.monthplan.factory.dto.AdjustAddQtyOtherSubtractDto;
import com.zlt.aps.monthplan.factory.dto.AdjustCalculateDto;
import com.zlt.aps.monthplan.factory.dto.MouldProductRelationDto;
import com.zlt.aps.monthplan.factory.helper.AdjustUtils;
import com.zlt.aps.monthplan.factory.helper.SingleMouldAdjustCalculateHelper;
import com.zlt.aps.monthplan.factory.mapper.FactoryMonthPlanAdjustMapper;
import com.zlt.aps.monthplan.factory.mapper.FactoryMonthPlanProdFinalMapper;
import com.zlt.aps.monthplan.factory.mapper.FactoryMouldingProductionResultMapper;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanAdjustService;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProdFinalService;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分厂计划调整业务实现类
 *
 * @author ZLT
 * @date 20250320
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FactoryMonthPlanAdjustServiceImpl implements IFactoryMonthPlanAdjustService {

    private final BaseDao baseDao;

    private final IncrementService incrementService;

    private final IFactoryParamService factoryParamService;

    private final IMdmProductionCalendarService productionCalendarService;

    private final IFactoryMonthPlanProdFinalService factoryMonthPlanProdFinalService;

    private final MdmMaterialInfoEntityMapper productInfoMapper;

    private final FactoryMonthPlanAdjustMapper factoryMonthPlanAdjustMapper;

    private final IMdmProductConstructionService productConstructionService;

    private final FactoryMonthPlanProdFinalMapper factoryMonthPlanProdFinalMapper;

    private final FactoryMouldingProductionResultMapper factoryMouldingProductionResultMapper;

    @Override
    public MonthPlanAdjustInfoVo getAdjustControlInfo(FactoryMonthPlanProdResultDto param) {
        MonthPlanAdjustInfoVo adjust = new MonthPlanAdjustInfoVo();
        Date currentDate = new Date();
        String dayFormat = com.ruoyi.common.core.utils.DateUtils.YYYY_MM_DD;
        String currentDateFormat = com.ruoyi.common.core.utils.DateUtils.parseDateToStr(dayFormat, currentDate);
        Date matchDate = com.ruoyi.common.core.utils.DateUtils.dateTime(dayFormat, currentDateFormat);
        String factoryCode = param.getFactoryCode();
        FactoryMonthPlanFinalVersionInfoVo finalVersion = factoryMonthPlanProdFinalService.getFinalVersionInfoByDate(factoryCode, matchDate);
        if (null == finalVersion) {
            adjust.setStartAdjustDay(-1);
            return adjust;
        }
        Integer delayDays = getAdjustDelayDays(factoryCode, ProductTypeEnum.SEMI_STEEL.getValue());
        Date adjustStartDate = com.ruoyi.common.core.utils.DateUtils.addDays(matchDate, delayDays);
        Date productionStartDate = finalVersion.getProductionStartDate();
        //版本开始日 <= 调整日期 <= 版本结束日
        if (productionStartDate.compareTo(adjustStartDate) >= 0 && finalVersion.getProductionEndDate().compareTo(adjustStartDate) <= 0) {
            adjust.setStartAdjustDay(-1);
            return adjust;
        }
        adjust.setFinalVersionInfo(finalVersion);
        adjust.setOperateDate(currentDate);
        //设置可调整的起始天数
        adjust.setStartAdjustDate(adjustStartDate);
        Integer diffDays = com.ruoyi.common.core.utils.DateUtils.getDayInterval(adjustStartDate, productionStartDate);
        adjust.setStartAdjustDay(diffDays + BigDecimal.ONE.intValue());
        //获取工厂停开工日历
        adjust.setNoAdjustDayList(getStopDays(finalVersion));
        return adjust;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult adjustMonthPlan(FactoryMonthPlanAdjustPlanVo adjustPlan) {
        adjustPlan.setMaxAddQty(null);
        String factoryCode = adjustPlan.getFactoryCode();
        Integer year = adjustPlan.getYear();
        Integer month = adjustPlan.getMonth();
        //校验调整控制信息
        AjaxResult checkAdjustControlResult = checkAdjustControlInfo(factoryCode, year, month);
        if (AdjustUtils.isCheckNoPass(checkAdjustControlResult)) {
            return checkAdjustControlResult;
        }
        MonthPlanAdjustInfoVo adjustControlInfo = (MonthPlanAdjustInfoVo) checkAdjustControlResult.get(AjaxResult.DATA_TAG);
        //比较起始调整日期
        Integer startAdjustDay = DateUtils.getDaysByMonth(adjustPlan.getStartDate());
        Integer minStartAdjustDay = adjustControlInfo.getStartAdjustDay();
        if (startAdjustDay < minStartAdjustDay) {
            String errorInfo = I18nUtil.getMessage("ui.data.adjust.param.startDays.noAdjust");
            return AjaxResult.error(String.format(errorInfo, minStartAdjustDay));
        }
        //设置记录日志信息
        String paramInfo = "计划调整参数：%s";
        String paramLogContent = String.format(paramInfo, JSON.toJSONString(adjustPlan));
        adjustPlan.setLogBuilder(new StringBuilder());
        adjustPlan.setFinalVersionInfo(adjustControlInfo.getFinalVersionInfo());
        adjustPlan.addAdjustProductionLog(adjustPlan.getProductionNo(), paramLogContent);
        //开始进行调整
        Integer adjustNumber = adjustPlan.getAdjustNumber();
        //调减
        if (adjustNumber < 0) {
            return adjustSubtract(adjustControlInfo, adjustPlan);
        }
        //调增
        return adjustAddNumber(adjustControlInfo, adjustPlan);
    }

    /**
     * 获取可调整的起始天数，当前日期，往后延缓n天
     * 通过分厂参数配置 SYS001
     *
     * @param currentDate 当前时间
     * @param factoryCode 分厂编码
     * @param year        年份
     * @param month       月份
     * @return
     */
    private Integer getStartDays(Date currentDate, String factoryCode, Integer year, Integer month) {
        Integer days = DateUtils.getDaysByMonth(currentDate);
        String productTypeCode = ProductTypeEnum.SEMI_STEEL.getValue();
        Integer monthDays = LocalDate.of(year, month, 1).with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth();
        //获取分厂调整延迟参数
        Integer delayDays = getAdjustDelayDays(factoryCode, productTypeCode);
        Integer beginDays = days + delayDays;
        if (beginDays > monthDays) {
            return -BigDecimal.ONE.intValue();
        }
        return beginDays;
    }

    /**
     * 根据排产版本获取其停开工日历，并转化成不可调整日信息集合
     *
     * @param finalVersion 分厂编码
     * @return
     */
    private List<Integer> getStopDays(FactoryMonthPlanFinalVersionInfoVo finalVersion) {
        if (null == finalVersion) {
            return Collections.emptyList();
        }
        String factoryCode = finalVersion.getFactoryCode();
        //自然月
        if (YesOrNoEnum.YES.getValue().equals(finalVersion.getIsNaturalMonth())) {
            //获取工厂停开工日历
            MdmProductionCalendar factoryMonthQuery = new MdmProductionCalendar();
            factoryMonthQuery.setFactoryCode(factoryCode);
            factoryMonthQuery.setYear(finalVersion.getYear());
            factoryMonthQuery.setMonth(finalVersion.getMonth());
            List<MdmProductionCalendar> calendarList = productionCalendarService.selectMdmProductionCalendarList(factoryMonthQuery);
            if (CollectionUtils.isEmpty(calendarList)) {
                return Collections.emptyList();
            }
            Set<Integer> stopList = DateUtils.calculateStopDays(BeanCopyUtils.copyBeanList(calendarList, ProductionCalendarVO.class));
            return new ArrayList<>(stopList);
        }
        //非自然月
        List<MdmProductionCalendar> calendarList = productionCalendarService.getDateRangeCalendarList(factoryCode, finalVersion.getProductionStartDate(), finalVersion.getProductionEndDate());
        if (CollectionUtils.isEmpty(calendarList)) {
            return Collections.emptyList();
        }
        Set<Integer> stopList = DateUtils.calculateStopDays(BeanCopyUtils.copyBeanList(calendarList, ProductionCalendarVO.class), finalVersion);
        return new ArrayList<>(stopList);
    }

    /**
     * 校验调整的控制信息
     * 只有定稿版本才可调整
     * 是否过了调整期或是还没到调整期
     *
     * @param factoryCode 分厂
     * @param year        年份
     * @param month       月份
     * @return
     */
    private AjaxResult checkAdjustControlInfo(String factoryCode, Integer year, Integer month) {
        FactoryMonthPlanProdResultDto param = new FactoryMonthPlanProdResultDto();
        param.setFactoryCode(factoryCode);
        param.setYear(year);
        param.setMonth(month);
        MonthPlanAdjustInfoVo adjustControlInfo = getAdjustControlInfo(param);
        if (null == adjustControlInfo) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.adjust.date.noAdjust"));
        }
        FactoryMonthPlanFinalVersionInfoVo finalResult = adjustControlInfo.getFinalVersionInfo();
        if (null == finalResult) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.adjust.param.checkFinalVersion"));
        }
        Integer startDay = adjustControlInfo.getStartAdjustDay();
        if (startDay == -1) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.adjust.date.noAdjust"));
        }
        QueryWrapper<FactoryMonthPlanProdFinal> queryIsImportWrapper = new QueryWrapper<>();
        queryIsImportWrapper.eq("FACTORY_CODE", finalResult.getFactoryCode());
        queryIsImportWrapper.eq("YEAR", finalResult.getYear());
        queryIsImportWrapper.eq("MONTH", finalResult.getMonth());
        queryIsImportWrapper.eq("PRODUCTION_VERSION", finalResult.getProductionVersion());
        queryIsImportWrapper.eq("MONTH_PLAN_VERSION", finalResult.getMonthPlanVersion());
        queryIsImportWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        queryIsImportWrapper.eq("IS_IMPORT", YesOrNoEnum.YES.getValue());
        Long importCount = factoryMonthPlanProdFinalMapper.selectCount(queryIsImportWrapper);
        if (importCount > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.adjust.date.importAdjust"));
        }
        return AjaxResult.success(adjustControlInfo);
    }

    /**
     * 对计划进行调减操作，从起始日开始，逐日进行调减，直到完成整体调减量
     *
     * @param adjustControlInfo 控制信息
     * @param adjustPlan        调减计划信息
     * @return
     */
    private AjaxResult adjustSubtract(MonthPlanAdjustInfoVo adjustControlInfo, FactoryMonthPlanAdjustPlanVo adjustPlan) {
        //根据制造单号获取原有排产计划
        String productionNo = adjustPlan.getProductionNo();
        FactoryMonthPlanFinalVersionInfoVo finalVersionInfo = adjustControlInfo.getFinalVersionInfo();
        AjaxResult productionPlanCheckResult = checkAdjustProductionPlan(finalVersionInfo, productionNo);
        if (AjaxResult.Type.ERROR.value() == (Integer) productionPlanCheckResult.get(AjaxResult.CODE_TAG)) {
            return productionPlanCheckResult;
        }
        FactoryMonthPlanProdFinal productionPlan = (FactoryMonthPlanProdFinal) productionPlanCheckResult.get(AjaxResult.DATA_TAG);
        //增加调减量不可超出其排产量
        AjaxResult checkAdjustNumberResult = AdjustUtils.checkAdjustNumberByProductionPlan(productionPlan, adjustPlan);
        if (AdjustUtils.isCheckNoPass(checkAdjustNumberResult)) {
            return checkAdjustNumberResult;
        }
        List<String> mouldCodeList = new ArrayList<>();
        String mouldCodeInfo = productionPlan.getMouldInfo();
        if (StringUtils.isNotBlank(mouldCodeInfo)) {
            mouldCodeList = Arrays.asList(mouldCodeInfo.split(StringConstant.COMMA));
        }
        //排产计划对应的已排模具信息
        Map<String, MouldingProductionResultHelper> productionMouldInfoMap = getMouldProductionResultInfo(finalVersionInfo, mouldCodeList);
        if (CollectionUtils.isEmpty(productionMouldInfoMap)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.adjust.check.noProductionMouldInfo"));
        }
        //从起始日开始调减，逐日调减，直到达到总调减量
        AdjustUtils.productionPlanSubtractQty(finalVersionInfo, adjustPlan, adjustPlan, productionPlan, productionMouldInfoMap);
        //更新数据
        baseDao.update(productionPlan);
        if (!CollectionUtils.isEmpty(productionMouldInfoMap)) {
            baseDao.updateBatch(productionMouldInfoMap.values().stream().collect(Collectors.toList()));
        }
        //保存日志
        saveLogs(adjustPlan, productionPlan.getProductionNo());
        //更新月度剩余量
        List<FactoryMonthPlanProdFinal> finalList = new ArrayList<>();
        finalList.add(productionPlan);
        factoryMonthPlanProdFinalService.finalUpdatePlanSurplusList(finalList);
        return AjaxResult.success();
    }

    /**
     * 计划调增
     * 包含原有计划增量，也包含新规格新增
     *
     * @param adjustControlInfo 调整控制信息
     * @param adjustPlan        调整计划
     * @return
     */
    private AjaxResult adjustAddNumber(MonthPlanAdjustInfoVo adjustControlInfo, FactoryMonthPlanAdjustPlanVo adjustPlan) {
        //校验物料
        AjaxResult checkProductAndCuringTimeResult = checkProductInfoAndSetCuringTime(adjustControlInfo, adjustPlan);
        if (AdjustUtils.isCheckNoPass(checkProductAndCuringTimeResult)) {
            return checkProductAndCuringTimeResult;
        }
        //校验最大模具产能
        FactoryMonthPlanFinalVersionInfoVo finalVersionInfo = adjustControlInfo.getFinalVersionInfo();
        String factoryCode = finalVersionInfo.getFactoryCode();
        Integer year = finalVersionInfo.getYear();
        Integer month = finalVersionInfo.getMonth();
        String mouldNo = adjustPlan.getMouldNo();
        Map<String, MouldProductRelationDto> maxEnableMouldMap = getMaxEnableMould(factoryCode, year, month, adjustPlan.getProductCode(), mouldNo);
        if (CollectionUtils.isEmpty(maxEnableMouldMap)) {
            return AjaxResult.error(I18nUtil.getMessage("alg.data.mould.noProductionMould"));
        }
        //得到模具最大产能
        Long maxAdjustNumber = getMaxAdjustNumber(adjustControlInfo, adjustPlan, maxEnableMouldMap);
        //获取模具剩余可增加量
        Set<Integer> stopDays = adjustControlInfo.getStopDays();
        String productionNo = adjustPlan.getProductionNo();
        AdjustAddQtyOtherSubtractDto maxAddQtyResult = getAddMaxQty(finalVersionInfo, adjustPlan, stopDays, maxEnableMouldMap);
        String errorInfo = maxAddQtyResult.getCheckSubtractErrorInfo();
        if (StringUtils.isNotBlank(errorInfo)) {
            return AjaxResult.error(errorInfo);
        }
        Long maxAddQty = maxAddQtyResult.getMaxAddQty();
        Long realMaxAddQty = Math.min(maxAdjustNumber, maxAddQty);
        Integer adjustQty = adjustPlan.getAdjustNumber();
        //不能直接调增，还需其它计划调减
        if (adjustQty > realMaxAddQty) {
            return buildNeedSubtractPlanInfo(finalVersionInfo, adjustPlan, realMaxAddQty, maxEnableMouldMap.keySet(), maxAdjustNumber);
        }
        //更新月度剩余量
        List<FactoryMonthPlanProdFinal> finalList = new ArrayList<>();
        //如果有需要调减的计划，则先调减
        List<FactoryMonthPlanProdFinal> subtractPlanList = maxAddQtyResult.getSubtractPlanList();
        if (!CollectionUtils.isEmpty(subtractPlanList)) {
            finalList.addAll(subtractPlanList);
            baseDao.updateBatch(subtractPlanList);
        }
        Map<String, MouldingProductionResultHelper> updateMouldMap = maxAddQtyResult.getSubtractCuringTimeMouldMap();
        //新规格增量
        if (StringUtils.isBlank(productionNo)) {
            MdmMaterialInfo productInfo = (MdmMaterialInfo) checkProductAndCuringTimeResult.get(AjaxResult.DATA_TAG);
            FactoryMonthPlanProdFinal addPlan = adjustAddQtyByNewProductionNo(finalVersionInfo, adjustPlan, stopDays, productInfo, updateMouldMap);
            //保存日志
            saveLogs(adjustPlan, addPlan.getProductionNo());
            //更新月度剩余量
            finalList.add(addPlan);
            factoryMonthPlanProdFinalService.finalUpdatePlanSurplusList(finalList);
            return AjaxResult.success();
        }
        //现有规格调增
        FactoryMonthPlanProdFinal originPlan = (FactoryMonthPlanProdFinal) checkProductAndCuringTimeResult.get(AjaxResult.DATA_TAG);
        AjaxResult adjustAddQtyByExistPlanResult = adjustAddQtyByProductionNo(finalVersionInfo, originPlan, adjustPlan, stopDays, updateMouldMap);
        if (AdjustUtils.isCheckNoPass(adjustAddQtyByExistPlanResult)) {
            return adjustAddQtyByExistPlanResult;
        }
        //更新月度剩余量
        finalList.add(originPlan);
        factoryMonthPlanProdFinalService.finalUpdatePlanSurplusList(finalList);
        return adjustAddQtyByExistPlanResult;
    }

    /**
     * 根据排产版本及排产计划单号，校验排产计划信息
     *
     * @param finalVersionInfo 排产版本信息
     * @param productionNo     排产计划单号
     * @return
     */
    private AjaxResult checkAdjustProductionPlan(FactoryMonthPlanFinalVersionInfoVo finalVersionInfo, String productionNo) {
        //根据制造单号获取原有排产计划
        List<String> productionPlanListCondition = new ArrayList<>();
        productionPlanListCondition.add(productionNo);
        List<FactoryMonthPlanProdFinal> productionPlanList = getProductionPlanList(finalVersionInfo, productionPlanListCondition);
        if (CollectionUtils.isEmpty(productionPlanList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.adjustPlanNoExist"));
        }
        return AjaxResult.success(productionPlanList.get(0));
    }

    /**
     * 调增数量不可超出模具最大产能
     * 计算模具最大产能
     *
     * @param adjustControlInfo 调整控制对象
     * @param adjustPlan        调整计划
     * @param maxEnableMouldMap 最大可用模具集合
     * @return
     */
    private Long getMaxAdjustNumber(MonthPlanAdjustInfoVo adjustControlInfo, FactoryMonthPlanAdjustPlanVo adjustPlan, Map<String, MouldProductRelationDto> maxEnableMouldMap) {
        FactoryMonthPlanFinalVersionInfoVo finalVersionInfo = adjustControlInfo.getFinalVersionInfo();
        Integer year = finalVersionInfo.getYear();
        Integer month = finalVersionInfo.getMonth();
        Set<Integer> stopDays = adjustControlInfo.getStopDays();
        Integer startDay = DateUtils.getDaysByMonth(adjustPlan.getStartDate());
        Integer maxDay = DateUtils.getDaysByYearMonth(year, month);
        BigDecimal singleCuringTime = adjustPlan.getCuringTime();
        BigDecimal dayMaxCuringTime = adjustPlan.getDayMaxCuringTime();
        //最大可排产量
        Long maxQty = BigDecimal.ZERO.longValue();
        for (Integer day = startDay; day <= maxDay; day++) {
            if (stopDays.contains(day)) {
                continue;
            }
            for (Map.Entry<String, MouldProductRelationDto> entry : maxEnableMouldMap.entrySet()) {
                MouldProductRelationDto mouldProductRelation = entry.getValue();
                Set<Integer> noProductionSet = mouldProductRelation.getNoProductionList();
                if (!CollectionUtils.isEmpty(noProductionSet) && noProductionSet.contains(day)) {
                    continue;
                }
                Long dayQty = dayMaxCuringTime.divide(singleCuringTime, 0, RoundingMode.DOWN).longValue();
                maxQty = maxQty + dayQty;
            }
        }
        return maxQty;
    }

    /**
     * 获取最大剩余可增加排产量
     *
     * @param finalVersionInfo  版本信息
     * @param adjustPlan        调整计划
     * @param stopDays          停工日
     * @param maxEnableMouldMap 最大可用模具
     * @return
     */
    private AdjustAddQtyOtherSubtractDto getAddMaxQty(FactoryMonthPlanFinalVersionInfoVo finalVersionInfo, FactoryMonthPlanAdjustPlanVo adjustPlan, Set<Integer> stopDays, Map<String, MouldProductRelationDto> maxEnableMouldMap) {
        Integer year = finalVersionInfo.getYear();
        Integer month = finalVersionInfo.getMonth();
        if (CollectionUtils.isEmpty(maxEnableMouldMap)) {
            return new AdjustAddQtyOtherSubtractDto(BigDecimal.ZERO.longValue(), Collections.emptyList(), Collections.emptyMap(), null);
        }
        //获取模具已排产信息
        Map<String, MouldingProductionResultHelper> productionMap = getProductionMouldMap(finalVersionInfo, adjustPlan, maxEnableMouldMap);
        //需调减量的计划--模具剩余产能加上
        List<FactoryMonthPlanAdjustPlanVo> confirmSubtractList = adjustPlan.getConfirmSubtractList();
        AjaxResult subtractPlanResult = addSubtractPlanCuringTime(finalVersionInfo, adjustPlan, confirmSubtractList, productionMap);
        if (AdjustUtils.isCheckNoPass(subtractPlanResult)) {
            return new AdjustAddQtyOtherSubtractDto(BigDecimal.ZERO.longValue(), Collections.emptyList(), Collections.emptyMap(), (String) subtractPlanResult.get(AjaxResult.MSG_TAG));
        }
        List<FactoryMonthPlanProdFinal> subtractPlanList = (List<FactoryMonthPlanProdFinal>) subtractPlanResult.get(AjaxResult.DATA_TAG);
        //重新计算量
        Integer startDay = DateUtils.getDaysByMonth(adjustPlan.getStartDate());
        Integer maxDay = DateUtils.getDaysByYearMonth(year, month);
        BigDecimal singleCuringTime = adjustPlan.getCuringTime();
        //最大可增加排产量
        Long maxQty = BigDecimal.ZERO.longValue();
        for (Integer day = startDay; day <= maxDay; day++) {
            if (stopDays.contains(day)) {
                continue;
            }
            for (Map.Entry<String, MouldProductRelationDto> entry : maxEnableMouldMap.entrySet()) {
                String mouldCode = entry.getKey();
                MouldingProductionResultHelper productionResult = productionMap.get(mouldCode);
                MouldProductRelationDto mouldProductRelation = entry.getValue();
                BigDecimal leftOverCuringTime = getDayLeftOverCuringTime(productionResult, day, mouldProductRelation, adjustPlan.getDayMaxCuringTime());
                Long dayQty = leftOverCuringTime.divide(singleCuringTime, 0, RoundingMode.DOWN).longValue();
                maxQty = maxQty + dayQty;
            }
        }
        return new AdjustAddQtyOtherSubtractDto(maxQty, subtractPlanList, productionMap, null);
    }

    /**
     * 对已排计划减量，需要对模具的剩余时间加量
     *
     * @param finalVersionInfo    版本控制信息
     * @param adjustPlan          需要调增的计划
     * @param confirmSubtractList 减量计划
     * @param productionMap       排产模具
     */
    private AjaxResult addSubtractPlanCuringTime(FactoryMonthPlanFinalVersionInfoVo finalVersionInfo, FactoryMonthPlanAdjustPlanVo adjustPlan, List<FactoryMonthPlanAdjustPlanVo> confirmSubtractList, Map<String, MouldingProductionResultHelper> productionMap) {
        if (CollectionUtils.isEmpty(confirmSubtractList)) {
            return AjaxResult.success(Collections.emptyList());
        }
        Map<String, FactoryMonthPlanAdjustPlanVo> subtractPlanMap = confirmSubtractList.stream().collect(Collectors.toMap(FactoryMonthPlanAdjustPlanVo::getProductionNo, Function.identity()));
        List<String> productionNoList = new ArrayList<>(subtractPlanMap.keySet());
        List<FactoryMonthPlanProdFinal> subtractPlanList = getProductionPlanList(finalVersionInfo, productionNoList);
        if (CollectionUtils.isEmpty(subtractPlanList)) {
            return AjaxResult.success(Collections.emptyList());
        }
        StringBuilder checkBuilder = new StringBuilder();
        boolean checkFlag = false;
        for (FactoryMonthPlanProdFinal originPlan : subtractPlanList) {
            AjaxResult checkResult = AdjustUtils.checkAdjustNumberByProductionPlan(originPlan, subtractPlanMap.get(originPlan.getProductionNo()));
            if (AdjustUtils.isCheckNoPass(checkResult)) {
                String errorInfo = (String) checkResult.get(AjaxResult.MSG_TAG);
                if (checkFlag) {
                    checkBuilder.append("\n" + errorInfo);
                } else {
                    checkBuilder.append(errorInfo);
                }
                checkFlag = true;
            }
        }
        if (checkFlag) {
            return AjaxResult.error(checkBuilder.toString());
        }
        subtractPlanList.forEach(subtractPlan -> {
            String productionNo = subtractPlan.getProductionNo();
            FactoryMonthPlanAdjustPlanVo subtractAdjustPlan = subtractPlanMap.get(productionNo);
            if (null == subtractAdjustPlan) {
                return;
            }
            subtractAdjustPlan.setProductCode(subtractPlan.getProductCode());
            subtractAdjustPlan.setYear(subtractPlan.getYear());
            subtractAdjustPlan.setMonth(subtractPlan.getMonth());
            subtractAdjustPlan.setMouldNo(subtractPlan.getMouldNo());
            //逐条计划逐日调减
            AdjustUtils.productionPlanSubtractQty(finalVersionInfo, adjustPlan, subtractAdjustPlan, subtractPlan, productionMap);
        });
        return AjaxResult.success(subtractPlanList);
    }

    /**
     * 构建现有模具剩余最大产能不可满足调增时，
     * 需要给出推荐的调减计划列表
     *
     * @param finalVersionInfo  版本信息
     * @param adjustPlan        调增计划
     * @param maxAddQty         剩余最大产能
     * @param maxEnableMouldSet 调增计划最大的可用模具
     * @param maxAdjustNumber   最大可增加量
     * @return
     */
    private AjaxResult buildNeedSubtractPlanInfo(FactoryMonthPlanFinalVersionInfoVo finalVersionInfo, FactoryMonthPlanAdjustPlanVo adjustPlan, Long maxAddQty, Set<String> maxEnableMouldSet, Long maxAdjustNumber) {
        List<FactoryMonthPlanProdFinal> otherPlanList = getOtherProductionPlanByMouldNo(finalVersionInfo, adjustPlan);
        if (CollectionUtils.isEmpty(otherPlanList)) {
            String errorInfo = I18nUtil.getMessage("ui.data.query.param.adjustNumberMax");
            return AjaxResult.error(String.format(errorInfo, maxAddQty));
        }
        //按模具号过滤
        List<FactoryMonthPlanProdFinal> matchList = new ArrayList<>();
        otherPlanList.forEach(otherPlan -> {
            String mouldInfo = otherPlan.getMouldInfo();
            if (StringUtils.isBlank(mouldInfo)) {
                return;
            }
            boolean isMatch = true;
            String[] mouldArray = mouldInfo.split(StringConstant.COMMA);
            for (String mouldCode : mouldArray) {
                if (!maxEnableMouldSet.contains(mouldCode)) {
                    isMatch = false;
                }
            }
            if (isMatch) {
                matchList.add(otherPlan);
            }
        });
        if (CollectionUtils.isEmpty(matchList)) {
            String errorInfo = I18nUtil.getMessage("ui.data.query.param.adjustNumberMax");
            return AjaxResult.error(String.format(errorInfo, maxAddQty));
        }
        List<FactoryMonthPlanProdFinalVo> sortList = BeanCopyUtils.copyBeanList(matchList, FactoryMonthPlanProdFinalVo.class);
        sortList.forEach(productionFinal -> {
            List<SinglePlanInfoHelper> planList = productionFinal.getMergePlanList();
            if (CollectionUtils.isEmpty(planList)) {
                productionFinal.setProductionSequence(BigDecimal.ZERO.longValue());
                return;
            }
            Comparator comparator = Comparator.comparing(SinglePlanInfoHelper::getSeq, Comparator.nullsFirst(Comparator.naturalOrder()));
            Optional<SinglePlanInfoHelper> max = planList.stream().max(comparator);
            Long seq = max.get().getSeq();
            if (null == seq) {
                seq = BigDecimal.ZERO.longValue();
            }
            productionFinal.setProductionSequence(seq);
        });
        Comparator comparator = Comparator.comparing(FactoryMonthPlanProdFinalVo::getProductionSequence, Comparator.reverseOrder())
                .thenComparing(FactoryMonthPlanProdFinalVo::getTotalQty)
                .thenComparing(FactoryMonthPlanProdFinalVo::getProductCode);
        sortList.sort(comparator);
        adjustPlan.setPlanSubtractList(sortList);
        //清空信息
        AdjustUtils.clearInfo(adjustPlan);
        adjustPlan.setMaxAddQty(maxAddQty);
        return AjaxResult.success(adjustPlan);
    }


    /**
     * 新规格调增--可满足量
     *
     * @param finalVersionInfo           版本信息
     * @param adjustPlan                 调增计划
     * @param stopDays                   停开工日历
     * @param productInfo                物料信息
     * @param subtractCuringTimeMouldMap 扣减过硫化时间的模具信息
     */
    private FactoryMonthPlanProdFinal adjustAddQtyByNewProductionNo(FactoryMonthPlanFinalVersionInfoVo finalVersionInfo, FactoryMonthPlanAdjustPlanVo adjustPlan, Set<Integer> stopDays, MdmMaterialInfo productInfo, Map<String, MouldingProductionResultHelper> subtractCuringTimeMouldMap) {
        String factoryCode = finalVersionInfo.getFactoryCode();
        Integer year = finalVersionInfo.getYear();
        Integer month = finalVersionInfo.getMonth();
        String mouldNo = adjustPlan.getMouldNo();
        Map<String, MouldProductRelationDto> maxEnableMouldMap = getMaxEnableMould(factoryCode, year, month, adjustPlan.getProductCode(), mouldNo);
        if (CollectionUtils.isEmpty(maxEnableMouldMap)) {
            return null;
        }
        //获取模具已排产信息
        Map<String, MouldingProductionResultHelper> productionMap = getProductionMouldMap(finalVersionInfo, adjustPlan, maxEnableMouldMap);
        //更新模具信息
        productionMap.entrySet().forEach(entry -> {
            String mouldCode = entry.getKey();
            if (subtractCuringTimeMouldMap.containsKey(mouldCode)) {
                productionMap.put(mouldCode, subtractCuringTimeMouldMap.get(mouldCode));
            }
        });
        FactoryMonthPlanProdFinal addPlan = AdjustUtils.buildNewProductionPlan(finalVersionInfo, adjustPlan);
        AdjustUtils.fillProductInfo(addPlan, productInfo);
        //生成新的排产单号
        String newProductionNo = buildProductionNo(1);
        addPlan.setProductionNo(newProductionNo);
        addPlan.setRemark("计划调整-插入新规格");
        Long sumAddQty = addPlan.getTotalQty();
        Integer startDay = DateUtils.getDaysByMonth(adjustPlan.getStartDate());
        Integer maxDay = DateUtils.getDaysByYearMonth(year, month);
        Set<String> productionMouldSet = new HashSet<>();
        Integer beginDate = maxDay;
        Integer endDay = startDay;
        //已排模具先增
        AdjustCalculateDto calculate = new AdjustCalculateDto(sumAddQty, beginDate, endDay, productionMouldSet, startDay, maxDay, stopDays, maxEnableMouldMap);
        productionMouldAddQty(adjustPlan, calculate, productionMap, addPlan);
        sumAddQty = calculate.getSumAddQty();
        addPlan.setBeginDate(calculate.getBeginDate());
        addPlan.setEndDay(calculate.getEndDay());
        addPlan.setMouldQty(productionMouldSet.size());
        addPlan.setMouldInfo(new ArrayList<>(productionMouldSet).stream().collect(Collectors.joining(StringConstant.COMMA)));
        if (sumAddQty == 0) {
            baseDao.insert(addPlan);
            //更新已排产模具排产信息
            updateProductionMouldInfo(productionMouldSet, productionMap);
            return addPlan;
        }
        //新模具后排
        List<MouldingProductionResultHelper> addMouldList = new ArrayList<>();
        calculate.setAddMouldList(addMouldList);
        calculate.setFinalVersionInfo(finalVersionInfo);
        calculate.setDayMaxCuringTime(adjustPlan.getDayMaxCuringTime());
        addNewMouldQty(adjustPlan, calculate, addPlan);
        //重新赋值，可能会有新的起始-结束日期及模具信息
        addPlan.setBeginDate(calculate.getBeginDate());
        addPlan.setEndDay(calculate.getEndDay());
        addPlan.setMouldQty(productionMouldSet.size());
        addPlan.setMouldInfo(new ArrayList<>(productionMouldSet).stream().collect(Collectors.joining(StringConstant.COMMA)));
        baseDao.insert(addPlan);
        //更新已排产模具排产信息
        updateProductionMouldInfo(productionMouldSet, productionMap);
        //新模具排产信息
        if (!CollectionUtils.isEmpty(addMouldList)) {
            baseDao.insertBatch(addMouldList);
        }
        return addPlan;
    }

    /**
     * 对已排产的计划进行调增
     *
     * @param finalVersionInfo           版本信息
     * @param originPlan                 原计划信息
     * @param adjustPlan                 调整计划调整信息
     * @param stopDays                   停开工日集合
     * @param subtractCuringTimeMouldMap 调减进行调增硫化时间的模具信息
     */
    private AjaxResult adjustAddQtyByProductionNo(FactoryMonthPlanFinalVersionInfoVo finalVersionInfo, FactoryMonthPlanProdFinal originPlan, FactoryMonthPlanAdjustPlanVo adjustPlan, Set<Integer> stopDays, Map<String, MouldingProductionResultHelper> subtractCuringTimeMouldMap) {
        String factoryCode = finalVersionInfo.getFactoryCode();
        Integer year = finalVersionInfo.getYear();
        Integer month = finalVersionInfo.getMonth();
        String mouldNo = adjustPlan.getMouldNo();
        Integer adjustNumber = adjustPlan.getAdjustNumber();
        Map<String, MouldProductRelationDto> maxEnableMouldMap = getMaxEnableMould(factoryCode, year, month, adjustPlan.getProductCode(), mouldNo);
        if (CollectionUtils.isEmpty(maxEnableMouldMap)) {
            return AjaxResult.error(I18nUtil.getMessage("alg.data.initCheck.noMouldQty"));
        }
        //获取模具已排产信息
        Map<String, MouldingProductionResultHelper> productionMap = getProductionMouldMap(finalVersionInfo, adjustPlan, maxEnableMouldMap);
        //更新信息
        productionMap.entrySet().forEach(entry -> {
            String mouldCode = entry.getKey();
            if (subtractCuringTimeMouldMap.containsKey(mouldCode)) {
                productionMap.put(mouldCode, subtractCuringTimeMouldMap.get(mouldCode));
            }
        });
        Long sumAddQty = Long.valueOf(adjustNumber);
        Integer startDay = DateUtils.getDaysByMonth(adjustPlan.getStartDate());
        Integer maxDay = DateUtils.getDaysByYearMonth(year, month);
        //已增排量-模具集合初始为空
        Set<String> productionMouldSet = new HashSet<>();
        Integer beginDate = maxDay;
        Integer endDay = startDay;
        //已排模具先增
        AdjustCalculateDto calculate = new AdjustCalculateDto(sumAddQty, beginDate, endDay, productionMouldSet, startDay, maxDay, stopDays, maxEnableMouldMap);
        productionMouldAddQty(adjustPlan, calculate, productionMap, originPlan);
        sumAddQty = calculate.getSumAddQty();
        //已排模具可满足调整增量
        if (sumAddQty == 0) {
            //更新计划及模具信息
            updateProductionMouldInfo(originPlan, calculate, productionMouldSet, productionMap, adjustNumber);
            //保存日志
            saveLogs(adjustPlan, originPlan.getProductionNo());
            return AjaxResult.success();
        }
        //新模具后排
        List<MouldingProductionResultHelper> addMouldList = new ArrayList<>();
        calculate.setAddMouldList(addMouldList);
        calculate.setFinalVersionInfo(finalVersionInfo);
        calculate.setDayMaxCuringTime(adjustPlan.getDayMaxCuringTime());
        addNewMouldQty(adjustPlan, calculate, originPlan);
        //更新排产计划
        updateProductionPlan(originPlan, calculate, productionMouldSet, adjustNumber);
        //已排模具信息更新
        if (!CollectionUtils.isEmpty(productionMouldSet)) {
            List<MouldingProductionResultHelper> updateList = new ArrayList<>();
            productionMouldSet.forEach(mouldCode -> {
                if (!productionMap.containsKey(mouldCode)) {
                    return;
                }
                updateList.add(productionMap.get(mouldCode));
            });
            baseDao.updateBatch(updateList);
        }
        //插入新增模具信息
        if (!CollectionUtils.isEmpty(addMouldList)) {
            baseDao.insertBatch(addMouldList);
        }
        //保存日志
        saveLogs(adjustPlan, originPlan.getProductionNo());
        return AjaxResult.success();
    }

    /**
     * 已排模具先进行调增量
     *
     * @param adjustPlanLog 调增计划--日志
     * @param calculate     增量信息
     * @param productionMap 已排产模具
     * @param addNumberPlan 需要增量的计划
     */
    private void productionMouldAddQty(FactoryMonthPlanAdjustPlanVo adjustPlanLog, AdjustCalculateDto calculate, Map<String, MouldingProductionResultHelper> productionMap, FactoryMonthPlanProdFinal addNumberPlan) {
        Long sumAddQty = calculate.getSumAddQty();
        Integer beginDate = calculate.getBeginDate();
        Integer endDay = calculate.getEndDay();
        Integer startDay = calculate.getStartDay();
        Integer maxDay = calculate.getMaxDay();
        Set<String> productionMouldSet = calculate.getProductionMouldSet();
        Map<String, MouldProductRelationDto> maxEnableMouldMap = calculate.getMaxEnableMouldMap();
        List<String> productionMouldList = new ArrayList<>(productionMap.keySet());
        int mouldSize = productionMouldList.size();
        if (mouldSize == 1) {
            MouldingProductionResultHelper helper = productionMap.get(productionMouldList.get(0));
            //单模具逐日增量
            SingleMouldAdjustCalculateHelper calculateHelper = new SingleMouldAdjustCalculateHelper(beginDate, endDay, startDay, maxDay, helper.getMouldCode(), sumAddQty);
            calculateHelper.setProductionMouldSet(productionMouldSet);
            calculateHelper.setAddMouldSet(null);
            calculateHelper.setMaxEnableMouldMap(maxEnableMouldMap);
            AdjustUtils.addSingleMouldQty(adjustPlanLog, helper, addNumberPlan, calculateHelper);
            sumAddQty = calculateHelper.getNeedAddQty();
            beginDate = calculateHelper.getBeginDate();
            endDay = calculateHelper.getEndDay();
            //修改值
            calculate.setSumAddQty(sumAddQty);
            calculate.setBeginDate(beginDate);
            calculate.setEndDay(endDay);
            return;
        }
        //按日逐副模具增量
        for (Integer day = startDay; day <= maxDay; day++) {
            Map<String, MouldingProductionResultHelper> dayProductionMouldMap = AdjustUtils.getDayProductionMouldInfo(productionMap, day, addNumberPlan.getCuringTime());
            if (CollectionUtils.isEmpty(dayProductionMouldMap)) {
                continue;
            }

            for (Map.Entry<String, MouldingProductionResultHelper> entry : dayProductionMouldMap.entrySet()) {
                MouldingProductionResultHelper helper = entry.getValue();
                //单模具逐日增量
                SingleMouldAdjustCalculateHelper calculateHelper = new SingleMouldAdjustCalculateHelper(beginDate, endDay, startDay, maxDay, helper.getMouldCode(), sumAddQty);
                calculateHelper.setProductionMouldSet(productionMouldSet);
                calculateHelper.setAddMouldSet(null);
                calculateHelper.setMaxEnableMouldMap(maxEnableMouldMap);
                AdjustUtils.addSingleMouldDayQty(adjustPlanLog, helper, addNumberPlan, calculateHelper, day);
                sumAddQty = calculateHelper.getNeedAddQty();
                beginDate = calculateHelper.getBeginDate();
                endDay = calculateHelper.getEndDay();
                //修改值
                calculate.setSumAddQty(sumAddQty);
                calculate.setBeginDate(beginDate);
                calculate.setEndDay(endDay);
                if (sumAddQty == 0) {
                    break;
                }
            }
            if (calculate.getSumAddQty() == 0) {
                break;
            }
        }
    }

    /**
     * 对已排模具计划增量更新信息
     *
     * @param productionPlan     调整的排产计划
     * @param calculate          调整计算后信息
     * @param productionMouldSet 排产模具集合
     * @param productionMap      排产模具信息
     * @param adjustNumber       调整量
     */
    private void updateProductionMouldInfo(FactoryMonthPlanProdFinal productionPlan, AdjustCalculateDto calculate, Set<String> productionMouldSet, Map<String, MouldingProductionResultHelper> productionMap, Integer adjustNumber) {
        updateProductionPlan(productionPlan, calculate, productionMouldSet, adjustNumber);
        updateProductionMouldInfo(productionMouldSet, productionMap);
    }

    /**
     * 更新排产计划信息
     * 排产计划的起始-结束日期
     * 日排产信息
     * 排产总量，计划量，含损耗量
     * 销售需求计划量、分厂排产需求量、实际排产量都需加量 即同增量
     *
     * @param productionPlan     排产计划
     * @param calculate          排产起始，结束日信息
     * @param productionMouldSet 排产模具信息
     * @param adjustNumber       增量
     */
    private void updateProductionPlan(FactoryMonthPlanProdFinal productionPlan, AdjustCalculateDto calculate, Set<String> productionMouldSet, Integer adjustNumber) {
        productionPlan.setMouldQty(productionMouldSet.size());
        productionPlan.setMouldInfo(new ArrayList<>(productionMouldSet).stream().collect(Collectors.joining(StringConstant.COMMA)));
        if (productionPlan.getBeginDate() > calculate.getBeginDate()) {
            productionPlan.setBeginDate(calculate.getBeginDate());
        }
        if (productionPlan.getEndDay() < calculate.getEndDay()) {
            productionPlan.setEndDay(calculate.getEndDay());
        }
        //总排产量
        Long totalQty = productionPlan.getTotalQty();
        if (null == totalQty) {
            totalQty = BigDecimal.ZERO.longValue();
        }
        totalQty = totalQty + adjustNumber;
        productionPlan.setTotalQty(totalQty);
        //总需求计划量
        Long reqPlanQty = productionPlan.getProdReqPlan();
        reqPlanQty = reqPlanQty + adjustNumber;
        productionPlan.setProdReqPlan(reqPlanQty);
        //含损耗
        Long factoryReqPlanQty = productionPlan.getFactProdReqQty();
        factoryReqPlanQty = factoryReqPlanQty + adjustNumber;
        productionPlan.setFactProdReqQty(factoryReqPlanQty);
        BigDecimal totalCuringTime = productionPlan.getTotalVulcanizationMinutes();
        if (null == totalCuringTime) {
            totalCuringTime = BigDecimal.ZERO;
        }
        BigDecimal adjustNumberCuringTime = productionPlan.getCuringTime().multiply(BigDecimal.valueOf(adjustNumber)).divide(BigDecimal.valueOf(FactoryConstant.MINUTE_SECOND), 2, RoundingMode.HALF_UP);
        totalCuringTime = totalCuringTime.add(adjustNumberCuringTime);
        productionPlan.setTotalVulcanizationMinutes(totalCuringTime);
        baseDao.update(productionPlan);
    }

    /**
     * 使用新模具，排产计划调增量
     *
     * @param adjustPlanLog 调整计划日志存储对象
     * @param calculate     调增信息
     * @param addNumberPlan 调增计划
     */
    private void addNewMouldQty(FactoryMonthPlanAdjustPlanVo adjustPlanLog, AdjustCalculateDto calculate, FactoryMonthPlanProdFinal addNumberPlan) {
        Long sumAddQty = calculate.getSumAddQty();
        Integer beginDate = calculate.getBeginDate();
        Integer endDay = calculate.getEndDay();
        Integer startDay = calculate.getStartDay();
        Integer maxDay = calculate.getMaxDay();
        BigDecimal dayMaxCuringTime = calculate.getDayMaxCuringTime();
        Set<Integer> stopDays = calculate.getStopDays();
        Set<String> productionMouldSet = calculate.getProductionMouldSet();
        Set<String> addMouldSet = new HashSet<>();
        Map<String, MouldProductRelationDto> maxEnableMouldMap = calculate.getMaxEnableMouldMap();
        Map<String, MouldingProductionResultHelper> addMouldHelperMap = new HashMap<>();
        for (Map.Entry<String, MouldProductRelationDto> entry : maxEnableMouldMap.entrySet()) {
            String mouldCode = entry.getKey();
            //新模具已排跳过
            if (productionMouldSet.contains(mouldCode)) {
                continue;
            }
            //构建全新的模具排产信息
            MouldProductRelationDto relation = entry.getValue();
            MouldingProductionResultHelper helper = addMouldHelperMap.get(mouldCode);
            if (null == helper) {
                helper = AdjustUtils.buildMouldingProductionResult(calculate.getFinalVersionInfo(), relation, stopDays, dayMaxCuringTime, maxDay);
                addMouldHelperMap.put(mouldCode, helper);
            }
        }
        //按日逐副模具增量
        for (Integer day = startDay; day <= maxDay; day++) {
            Map<String, MouldingProductionResultHelper> dayProductionMouldMap = AdjustUtils.getDayProductionMouldInfo(addMouldHelperMap, day, addNumberPlan.getCuringTime());
            if (CollectionUtils.isEmpty(dayProductionMouldMap)) {
                continue;
            }
            //单模具逐日增量
            for (Map.Entry<String, MouldingProductionResultHelper> entry : dayProductionMouldMap.entrySet()) {
                MouldingProductionResultHelper helper = entry.getValue();
                SingleMouldAdjustCalculateHelper calculateHelper = new SingleMouldAdjustCalculateHelper(beginDate, endDay, startDay, maxDay, helper.getMouldCode(), sumAddQty);
                calculateHelper.setProductionMouldSet(productionMouldSet);
                calculateHelper.setAddMouldSet(addMouldSet);
                calculateHelper.setMaxEnableMouldMap(maxEnableMouldMap);
                AdjustUtils.addSingleMouldDayQty(adjustPlanLog, helper, addNumberPlan, calculateHelper, day);
                sumAddQty = calculateHelper.getNeedAddQty();
                beginDate = calculateHelper.getBeginDate();
                endDay = calculateHelper.getEndDay();
                //修改值
                calculate.setSumAddQty(sumAddQty);
                calculate.setBeginDate(beginDate);
                calculate.setEndDay(endDay);
                if (sumAddQty == 0) {
                    break;
                }
            }
            if (calculate.getSumAddQty() == 0) {
                break;
            }
        }
        if (!CollectionUtils.isEmpty(addMouldSet)) {
            addMouldSet.forEach(mouldCode -> calculate.getAddMouldList().add(addMouldHelperMap.get(mouldCode)));
        }
    }

    /**
     * 对已排产模具信息更新
     *
     * @param productionMouldSet 本次排产模具信息
     * @param productionMap      之前已排产模具信息--最新
     */
    private void updateProductionMouldInfo(Set<String> productionMouldSet, Map<String, MouldingProductionResultHelper> productionMap) {
        if (CollectionUtils.isEmpty(productionMouldSet) || CollectionUtils.isEmpty(productionMap)) {
            return;
        }
        List<MouldingProductionResultHelper> updateList = new ArrayList<>();
        productionMouldSet.forEach(mouldCode -> {
            MouldingProductionResultHelper updateHelper = productionMap.get(mouldCode);
            if (null != updateHelper) {
                updateList.add(updateHelper);
            }
        });
        if (CollectionUtils.isEmpty(updateList)) {
            return;
        }
        baseDao.updateBatch(updateList);
    }

    /**
     * 获取已排产模具信息
     *
     * @param finalVersionInfo  版本信息
     * @param adjustPlan        调整计划
     * @param maxEnableMouldMap 最大可用模具
     * @return
     */
    private Map<String, MouldingProductionResultHelper> getProductionMouldMap(FactoryMonthPlanFinalVersionInfoVo finalVersionInfo, FactoryMonthPlanAdjustPlanVo adjustPlan, Map<String, MouldProductRelationDto> maxEnableMouldMap) {
        String factoryCode = finalVersionInfo.getFactoryCode();
        Integer year = finalVersionInfo.getYear();
        Integer month = finalVersionInfo.getMonth();
        String mouldNo = adjustPlan.getMouldNo();
        String productionVersion = finalVersionInfo.getProductionVersion();
        String monthPlanVersion = finalVersionInfo.getMonthPlanVersion();
        QueryWrapper<MouldingProductionResultHelper> mouldProductionResultQuery = new QueryWrapper<>();
        mouldProductionResultQuery.eq("FACTORY_CODE", factoryCode);
        mouldProductionResultQuery.eq("YEAR", year);
        mouldProductionResultQuery.eq("MONTH", month);
        mouldProductionResultQuery.eq("MONTH_PLAN_VERSION", monthPlanVersion);
        mouldProductionResultQuery.eq("PRODUCTION_VERSION", productionVersion);
        mouldProductionResultQuery.eq("MOULD_NO", mouldNo);
        mouldProductionResultQuery.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        List<MouldingProductionResultHelper> productionList = factoryMouldingProductionResultMapper.selectList(mouldProductionResultQuery);
        Map<String, MouldingProductionResultHelper> productionMap = new HashMap<>();
        if (CollectionUtils.isEmpty(productionList)) {
            return productionMap;
        }
        productionList.forEach(productionMould -> {
            String mouldCode = productionMould.getMouldCode();
            if (maxEnableMouldMap.containsKey(mouldCode)) {
                productionMap.put(mouldCode, productionMould);
            }
        });
        return productionMap;
    }

    /**
     * 获取最大的可用模具信息
     *
     * @param factoryCode 分厂编码
     * @param year        年份
     * @param month       月份
     * @param productCode 物料编码
     * @param mouldNo     模具
     * @return
     */
    private Map<String, MouldProductRelationDto> getMaxEnableMould(String factoryCode, Integer year, Integer month, String productCode, String mouldNo) {
        List<MouldProductRelationDto> monthEnableList = factoryMonthPlanAdjustMapper.getMonthEnableMouldConfiguration(factoryCode, year, month, mouldNo, productCode);
        List<MouldProductRelationDto> monthMaintenanceList = factoryMonthPlanAdjustMapper.getFactoryMouldMaintenanceConfiguration(factoryCode, year, month, mouldNo, productCode);
        return AdjustUtils.getMaxEnableMould(monthEnableList, monthMaintenanceList);
    }

    /**
     * 校验调整新增规格的物料信息
     *
     * @param adjustControlInfo 调整控制信息对象
     * @param adjustPlan        调整计划
     * @return
     */
    private AjaxResult checkProductInfoAndSetCuringTime(MonthPlanAdjustInfoVo adjustControlInfo, FactoryMonthPlanAdjustPlanVo adjustPlan) {
        String productionNo = adjustPlan.getProductionNo();
        String factoryCode = adjustPlan.getFactoryCode();
        //设置一天最大硫化时间
        BigDecimal dayWorkHourTime = factoryParamService.getDayMaxCuringTime(factoryCode);
        adjustPlan.setDayMaxCuringTime(dayWorkHourTime);
        if (!StringUtils.isBlank(productionNo)) {
            //原有计划增量
            FactoryMonthPlanFinalVersionInfoVo finalVersionInfo = adjustControlInfo.getFinalVersionInfo();
            AjaxResult productionPlanCheckResult = checkAdjustProductionPlan(finalVersionInfo, productionNo);
            if (AdjustUtils.isCheckNoPass(productionPlanCheckResult)) {
                return productionPlanCheckResult;
            }
            FactoryMonthPlanProdFinal originPlan = (FactoryMonthPlanProdFinal) productionPlanCheckResult.get(AjaxResult.DATA_TAG);
            if (null == originPlan.getCuringTime()) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.adjust.check.noHasCuringTime"));
            }
            adjustPlan.setCuringTime(originPlan.getCuringTime());
            adjustPlan.setProductCode(originPlan.getProductCode());
            adjustPlan.setMouldNo(originPlan.getMouldNo());
            return AjaxResult.success(originPlan);
        }
        //新插入规格
        String productCode = adjustPlan.getProductCode();
        QueryWrapper<MdmMaterialInfo> productQuery = new QueryWrapper<>();
        productQuery.eq("PRODUCT_CODE", productCode);
        productQuery.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        productQuery.eq("FACTORY_CODE", factoryCode);
        MdmMaterialInfo productInfo = productInfoMapper.selectOne(productQuery);
        if (null == productInfo) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.TEstimateExceedShort.notExist.productInfo"));
        }
        //获取硫化时间--通过SAP与施工关系获取
        MdmProductConstructionDto constructionConfiguration = productConstructionService.getCuringTime(factoryCode, productCode, adjustPlan.getSpecCode());
        if (null == constructionConfiguration) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.adjust.check.noHasCuringTime"));
        }
        Map<String, Integer> changeConfiguration = factoryParamService.getChangeSummerMonth(factoryCode);
        if (CollectionUtils.isEmpty(changeConfiguration)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.adjust.check.noHasCuringTime"));
        }
        BigDecimal curingTime = constructionConfiguration.getRealCuringTime(adjustPlan.getMonth(), changeConfiguration.get(FactoryConstant.SYS_PARAM_SUMMER_MONTH), changeConfiguration.get(FactoryConstant.SYS_PARAM_WINTER_MONTH));
        if (null == curingTime) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.adjust.check.noHasCuringTime"));
        }
        //获取单条硫化时间，加上间隔单条硫化时间，设置硫化时间
        BigDecimal addCuringTimeValue = factoryParamService.getSingleAddCuringTime(factoryCode);
        adjustPlan.setCuringTime(curingTime.add(addCuringTimeValue));
        //设置施工信息
        adjustPlan.setConstructionCode(constructionConfiguration.getConstructionCode());
        List<ProductSpecInfoVo> productSpecCodeInfoList = constructionConfiguration.getProductSpecCodeInfoList();
        if (!CollectionUtils.isEmpty(productSpecCodeInfoList)) {
            adjustPlan.setSpecCodeInfo(JSON.toJSONString(productSpecCodeInfoList));
        }
        return AjaxResult.success(productInfo);
    }

    /**
     * 获取模具的日剩余硫化时间
     *
     * @param productionResult  已排模具
     * @param day               日
     * @param noProductionMould 未排模具
     * @param dayMaxCuringTime  日最大硫化时间
     * @return
     */
    private BigDecimal getDayLeftOverCuringTime(MouldingProductionResultHelper productionResult, Integer day, MouldProductRelationDto noProductionMould, BigDecimal dayMaxCuringTime) {
        BigDecimal leftOverCuringTime;
        if (null != productionResult) {
            DayLeftOverCuringTimeVo dayLeftOverCuringTime = productionResult.getDayLeftOverCuringTime().get(day);
            if (null == dayLeftOverCuringTime) {
                leftOverCuringTime = BigDecimal.ZERO;
            } else {
                leftOverCuringTime = dayLeftOverCuringTime.getLeftOverCuringTime();
            }
            return leftOverCuringTime;
        }
        Set<Integer> noProductionDaySet = noProductionMould.getNoProductionList();
        if (CollectionUtils.isEmpty(noProductionDaySet)) {
            return dayMaxCuringTime;
        }
        if (noProductionDaySet.contains(day)) {
            return BigDecimal.ZERO;
        }
        return dayMaxCuringTime;
    }

    /**
     * 获取延迟天数参数
     *
     * @param factoryCode
     * @param productTypeCode
     * @return
     */
    private Integer getAdjustDelayDays(String factoryCode, String productTypeCode) {
        FactoryParam query = new FactoryParam();
        query.setFactoryCode(factoryCode);
        query.setProductTypeCode(productTypeCode);
        query.setParamCode(FactoryConstant.SYS_PARAM_ADJUST_DELAY_DAYS);
        FactoryParam result = factoryParamService.getFacParamSingle(query);
        if (null == result) {
            return BigDecimal.ZERO.intValue();
        }
        String paramValue = result.getParamValue();
        if (StringUtils.isBlank(paramValue)) {
            return BigDecimal.ZERO.intValue();
        }
        return Integer.parseInt(paramValue);
    }


    /**
     * 根据制造单号，获取排产计划
     *
     * @param finalVersionInfo 版本控制信息
     * @param productionNoList 制造单号集合
     * @return
     */
    private List<FactoryMonthPlanProdFinal> getProductionPlanList(FactoryMonthPlanFinalVersionInfoVo finalVersionInfo, List<String> productionNoList) {
        if (CollectionUtils.isEmpty(productionNoList)) {
            return Collections.emptyList();
        }
        QueryWrapper<FactoryMonthPlanProdFinal> productionPlanQuery = new QueryWrapper<>();
        productionPlanQuery.eq("FACTORY_CODE", finalVersionInfo.getFactoryCode());
        productionPlanQuery.eq("YEAR", finalVersionInfo.getYear());
        productionPlanQuery.eq("MONTH", finalVersionInfo.getMonth());
        productionPlanQuery.eq("MONTH_PLAN_VERSION", finalVersionInfo.getMonthPlanVersion());
        productionPlanQuery.eq("PRODUCTION_VERSION", finalVersionInfo.getProductionVersion());
        productionPlanQuery.in("PRODUCTION_NO", productionNoList);
        productionPlanQuery.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return factoryMonthPlanProdFinalMapper.selectList(productionPlanQuery);
    }

    /**
     * 获取分厂年月定稿版本对应模具号的其它物料的排产计划
     * 即PRODUCT_CODE != productCode
     * MOULD_NO = mouldNo
     * BEGIN_DATE <= startDate <= END_DAY
     *
     * @param finalVersionInfo 版本信息
     * @param adjustPlan       调整计划
     * @return
     */
    private List<FactoryMonthPlanProdFinal> getOtherProductionPlanByMouldNo(FactoryMonthPlanFinalVersionInfoVo finalVersionInfo, FactoryMonthPlanAdjustPlanVo adjustPlan) {
        Date startDate = adjustPlan.getStartDate();
        Integer startDay = DateUtils.getDaysByMonth(startDate);
        QueryWrapper<FactoryMonthPlanProdFinal> productionPlanQuery = new QueryWrapper<>();
        productionPlanQuery.eq("FACTORY_CODE", finalVersionInfo.getFactoryCode());
        productionPlanQuery.eq("YEAR", finalVersionInfo.getYear());
        productionPlanQuery.eq("MONTH", finalVersionInfo.getMonth());
        productionPlanQuery.eq("MONTH_PLAN_VERSION", finalVersionInfo.getMonthPlanVersion());
        productionPlanQuery.eq("PRODUCTION_VERSION", finalVersionInfo.getProductionVersion());
        productionPlanQuery.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        productionPlanQuery.eq("MOULD_NO", adjustPlan.getMouldNo());
        productionPlanQuery.ne("PRODUCT_CODE", adjustPlan.getProductCode());
        productionPlanQuery.le("BEGIN_DATE", startDay);
        productionPlanQuery.ge("END_DAY", startDay);
        return factoryMonthPlanProdFinalMapper.selectList(productionPlanQuery);
    }

    /**
     * 根据版本及模具编码集合，获取对应模具的已排产信息
     *
     * @param finalVersionInfo 版本信息
     * @param mouldCodeList    模具集合
     * @return
     */
    private Map<String, MouldingProductionResultHelper> getMouldProductionResultInfo(FactoryMonthPlanFinalVersionInfoVo finalVersionInfo, List<String> mouldCodeList) {
        if (CollectionUtils.isEmpty(mouldCodeList)) {
            return Collections.emptyMap();
        }
        QueryWrapper<MouldingProductionResultHelper> mouldProductionResultQuery = new QueryWrapper<>();
        mouldProductionResultQuery.eq("FACTORY_CODE", finalVersionInfo.getFactoryCode());
        mouldProductionResultQuery.eq("YEAR", finalVersionInfo.getYear());
        mouldProductionResultQuery.eq("MONTH", finalVersionInfo.getMonth());
        mouldProductionResultQuery.eq("MONTH_PLAN_VERSION", finalVersionInfo.getMonthPlanVersion());
        mouldProductionResultQuery.eq("PRODUCTION_VERSION", finalVersionInfo.getProductionVersion());
        mouldProductionResultQuery.in("MOULD_CODE", mouldCodeList);
        mouldProductionResultQuery.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        List<MouldingProductionResultHelper> resultList = factoryMouldingProductionResultMapper.selectList(mouldProductionResultQuery);
        if (CollectionUtils.isEmpty(resultList)) {
            return Collections.emptyMap();
        }
        return resultList.stream().collect(Collectors.toMap(MouldingProductionResultHelper::getMouldCode, Function.identity()));
    }

    /**
     * 生成排产单号
     *
     * @param index 序号
     * @return
     */
    private String buildProductionNo(int index) {
        String monthPlanVersion = incrementService
                .getBillNoSequenceByExpire(IncrementConstant.MONTH_FINAL + com.ruoyi.common.core.utils.DateUtils.dateTimeNow("yyyyMMdd"), 3, 60 * 24 * 7);
        return monthPlanVersion + String.format("%06d", index);
    }

    /**
     * 最后，保存调整日志
     *
     * @param adjustPlan 调整计划信息
     */
    private void saveLogs(FactoryMonthPlanAdjustPlanVo adjustPlan, String productionNo) {
        StringBuilder logBuilder = adjustPlan.getLogBuilder();
        String logContent = logBuilder.toString();
        if (StringUtils.isBlank(logContent)) {
            return;
        }
        FactoryMonthPlanFinalVersionInfoVo finalVersionInfo = adjustPlan.getFinalVersionInfo();
        MouldProductionLog log = new MouldProductionLog();
        log.setWorkNo(productionNo);
        log.setLogContent(logContent);
        log.setLogType(MouldProductionLogType.PLAN_ADJUST_LOG.getTypeValue());
        log.setMonthPlanVersion(finalVersionInfo.getMonthPlanVersion());
        log.setProductionVersion(finalVersionInfo.getProductionVersion());
        log.setFactoryCode(finalVersionInfo.getFactoryCode());
        log.setYear(finalVersionInfo.getYear());
        log.setMonth(finalVersionInfo.getMonth());
        baseDao.insert(log);
    }

}
