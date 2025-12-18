package com.zlt.aps.monthplan.factory.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.constant.IncrementConstant;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.BeanCopyUtils;
import com.tlt.aps.utils.IncrementService;
import com.zlt.aps.maindata.domain.dto.MdmProductConstructionDto;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.service.IMdmProductConstructionService;
import com.zlt.aps.maindata.service.IPlanOrderSortConfigurationService;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.api.domain.vo.*;
import com.zlt.aps.monthplan.api.enums.MonthPlanAdjustNoticeStatusEnum;
import com.zlt.aps.monthplan.api.enums.MonthPlanAdjustTypeEnum;
import com.zlt.aps.monthplan.demand.mapper.SaleMonthPlanRequireStockMapper;
import com.zlt.aps.monthplan.factory.dto.MouldProductRelationDto;
import com.zlt.aps.monthplan.factory.helper.*;
import com.zlt.aps.monthplan.factory.mapper.FactoryMonthPlanProdFinalMapper;
import com.zlt.aps.monthplan.factory.mapper.MonthPlanAdjustDetailMapper;
import com.zlt.aps.monthplan.factory.mapper.MonthPlanAdjustNoticeOrderMapper;
import com.zlt.aps.monthplan.factory.service.*;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zlt.common.utils.ImportExcelValidatedUtils.addImportErrorLog;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanAdjustNoticeOrderServiceImpl.java
 * 描    述：MonthPlanAdjustNoticeOrderServiceImpl-月计划调整通知单业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-05-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class MonthPlanAdjustNoticeOrderServiceImpl implements IMonthPlanAdjustNoticeOrderService {

    private final MdmMaterialInfoEntityMapper productInfoMapper;

    private final MonthPlanAdjustDetailMapper monthPlanAdjustDetailMapper;

    private final MonthPlanAdjustNoticeOrderMapper monthPlanAdjustNoticeOrderMapper;

    private final SaleMonthPlanRequireStockMapper saleMonthPlanRequireStockMapper;

    private final FactoryMonthPlanProdFinalMapper factoryMonthPlanProdFinalMapper;

    private final BaseDao baseDao;

    private final IncrementService incrementService;

    private final IFactoryParamService factoryParamService;

    private final IMdmProductConstructionService mdmProductConstructionService;

    private final IFactoryProductionVersionService factoryProductionVersionService;

    private final IPlanOrderSortConfigurationService sortConfigurationService;

    private final IFactoryMonthPlanProdFinalService factoryMonthPlanProdFinalService;

    private final IFactoryMonthPlanAdjustPlanBusinessService factoryMonthPlanAdjustPlanBusinessService;

    private final String ERROR_REPEAT = "repeat";

    private final String ERROR_PRODUCT_CODE = "productCodeNotExist";

    /**
     * 列表查询
     */
    @Override
    public List<MonthPlanNoticeOrder> selectList(MonthPlanNoticeOrder queryVO) {
        QueryWrapper<MonthPlanNoticeOrder> queryWrapper = new QueryWrapper<>();
        builderCondition(queryWrapper, queryVO);
        queryWrapper.orderByAsc("UPDATE_TIME");
        return monthPlanAdjustNoticeOrderMapper.selectList(queryWrapper);
    }

    @Override
    public List<MonthPlanAdjustDetailVo> getNoticeDetail(String noticeNo) {
        if (StringUtils.isBlank(noticeNo)) {
            return Collections.emptyList();
        }
        return monthPlanAdjustDetailMapper.getNoticeDetail(noticeNo);
    }

    @Override
    public MonthPlanNoticeOrderVo getMonthPlanNoticeInfo(Long id) {
        if (null == id) {
            return new MonthPlanNoticeOrderVo();
        }
        MonthPlanNoticeOrder noticeOrder = monthPlanAdjustNoticeOrderMapper.selectById(id);
        if (null == noticeOrder) {
            return new MonthPlanNoticeOrderVo();
        }
        MonthPlanNoticeOrderVo info = BeanCopyUtils.copyBean(noticeOrder, MonthPlanNoticeOrderVo.class);
        Long stockQty = BigDecimal.ZERO.longValue();
        MonthPlanRequireStock leftOverStock = getLeftOverStock(info.getProductCode(), info.getMonthPlanVersion());
        if (null != leftOverStock) {
            stockQty = leftOverStock.getRemainingQty();
        }
        info.setStockQty(stockQty);
        return info;
    }

    @Override
    public MonthPlanNoticeOrderVo getMonthPlanNoticeStockInfo(MonthPlanNoticeOrder noticeOrder) {
        if (null == noticeOrder) {
            return new MonthPlanNoticeOrderVo();
        }
        String factoryCode = noticeOrder.getFactoryCode();
        String productCode = noticeOrder.getProductCode();
        Integer year = noticeOrder.getYear();
        Integer month = noticeOrder.getMonth();
        if (StringUtils.isBlank(factoryCode) || StringUtils.isBlank(productCode)) {
            return new MonthPlanNoticeOrderVo();
        }
        if (null == year || null == month) {
            return new MonthPlanNoticeOrderVo();
        }
        FactoryProductionVersion finalVersion = factoryProductionVersionService.getFinalVersionByYearMonth(factoryCode, year, month);
        if (null == finalVersion) {
            return new MonthPlanNoticeOrderVo();
        }
        MonthPlanNoticeOrderVo info = BeanCopyUtils.copyBean(noticeOrder, MonthPlanNoticeOrderVo.class);
        Long stockQty = BigDecimal.ZERO.longValue();
        MonthPlanRequireStock leftOverStock = getLeftOverStock(productCode, finalVersion.getMonthPlanVersion());
        if (null != leftOverStock) {
            stockQty = leftOverStock.getRemainingQty();
        }
        info.setStockQty(stockQty);
        return info;
    }

    @Override
    public AjaxResult save(MonthPlanNoticeOrder noticeOrder) {
        //先根据分厂、年份、月份确定是否已经定稿
        String factoryCode = noticeOrder.getFactoryCode();
        Integer year = noticeOrder.getYear();
        Integer month = noticeOrder.getMonth();
        FactoryProductionVersion finalVersion = factoryProductionVersionService.getFinalVersionByYearMonth(factoryCode, year, month);
        if (null == finalVersion) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.finalized.noFinalized"));
        }
        String productCode = noticeOrder.getProductCode();
        MdmMaterialInfo productInfo = getProductInfo(factoryCode, productCode);
        if (null == productInfo) {
            return AjaxResult.error(String.format(I18nUtil.getMessage("ui.data.column.monthStock.productCode.notExist"), productCode));
        }
        String monthPlanVersion = finalVersion.getMonthPlanVersion();
        noticeOrder.setMonthPlanVersion(monthPlanVersion);
        noticeOrder.setProductionVersion(finalVersion.getProductionVersion());
        AdjustNoticeUtils.setProductInfo(noticeOrder, productInfo);
        Long id = noticeOrder.getId();
        //如果ID为空，则自动生成调整通知单号
        if (null == id) {
            noticeOrder.setStatus(MonthPlanAdjustNoticeStatusEnum.NEW.getStatus());
            String noticeNo = buildNoticePrefix();
            noticeOrder.setNoticeNo(String.format("%s%06d", noticeNo, 1));
            noticeOrder.setIsImport(YesOrNoEnum.NO.getValue());
            baseDao.save(noticeOrder);
            return AjaxResult.success();
        }
        //校验状态
        MonthPlanNoticeOrder old = monthPlanAdjustNoticeOrderMapper.selectById(id);
        if (null == old) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.noticeOrder.noExits"));
        }
        if (!MonthPlanAdjustNoticeStatusEnum.NEW.getStatus().equals(old.getStatus())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.noticeOrder.noEdit"));
        }
        baseDao.save(noticeOrder);
        return AjaxResult.success();
    }

    @Override
    public AjaxResult submit(Long id) {
        if (null == id) {
            return AjaxResult.success();
        }
        MonthPlanNoticeOrder noticeOrder = monthPlanAdjustNoticeOrderMapper.selectById(id);
        if (null == noticeOrder) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.noticeOrder.noExits"));
        }
        if (!MonthPlanAdjustNoticeStatusEnum.NEW.getStatus().equals(noticeOrder.getStatus())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.noticeOrder.noSubmit"));
        }
        setPlanQtyAndUpdateRemainingQty(noticeOrder);
        baseDao.update(noticeOrder);
        return AjaxResult.success();
    }

    @Override
    public AjaxResult cancel(Long id) {
        if (null == id) {
            return AjaxResult.success();
        }
        MonthPlanNoticeOrder noticeOrder = monthPlanAdjustNoticeOrderMapper.selectById(id);
        if (null == noticeOrder) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.noticeOrder.noExits"));
        }
        if (!MonthPlanAdjustNoticeStatusEnum.NEW.getStatus().equals(noticeOrder.getStatus())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.noticeOrder.noCancel"));
        }
        noticeOrder.setStatus(MonthPlanAdjustNoticeStatusEnum.CANCEL.getStatus());
        baseDao.update(noticeOrder);
        return AjaxResult.success();
    }

    @Override
    public AjaxResult getAdjustNoticeAdjustPlan(MonthPlanNoticeOrder noticeOrderOperate) {
        AjaxResult checkResult = checkControlInfo(noticeOrderOperate.getNoticeNo());
        if (AjaxResult.Type.ERROR.value() == (Integer) checkResult.get(AjaxResult.CODE_TAG)) {
            return checkResult;
        }
        AdjustNoticeCheckHelper checkHelper = (AdjustNoticeCheckHelper) checkResult.get(AjaxResult.DATA_TAG);
        MonthPlanNoticeOrder noticeOrder = checkHelper.getNoticeOrder();
        FactoryProductionVersion productionVersion = checkHelper.getProductionVersion();
        String factoryCode = noticeOrder.getFactoryCode();
        Integer delayDays = getAdjustDelayDays(factoryCode, ProductTypeEnum.SEMI_STEEL.getValue());
        Date adjustStartDate = AdjustNoticeUtils.getMinStartAdjustDate(delayDays);
        //版本开始日 <= 开始调整日期 <= 版本结束日
        if (!AdjustNoticeUtils.canAdjust(adjustStartDate, productionVersion)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.noticeOrder.noAdjust"));
        }
        MonthPlanAdjustNoticeOrderOperateVo data = BeanCopyUtils.copyBean(noticeOrder, MonthPlanAdjustNoticeOrderOperateVo.class);
        data.setStartDate(adjustStartDate);
        String twoDigitNumbersMonth = String.format("%02d", noticeOrder.getMonth());
        data.setAdjustNumber(noticeOrder.getPlanQty());
        data.setYearMonth(String.format("%s-%s", noticeOrder.getYear(), twoDigitNumbersMonth));
        data.setProductionStartDate(productionVersion.getProductionStartDate());
        data.setProductionEndDate(productionVersion.getProductionEndDate());
        data.setIsNaturalMonth(productionVersion.getIsNaturalMonth());
        return AjaxResult.success(data);
    }

    @Override
    public AjaxResult getOperatePlanList(MonthPlanAdjustNoticeOrderOperateVo noticeOrderOperate) {
        AjaxResult checkResult = checkOperateAdjustControl(noticeOrderOperate);
        if (AjaxResult.Type.ERROR.value() == (Integer) checkResult.get(AjaxResult.CODE_TAG)) {
            return checkResult;
        }
        AdjustNoticeCheckHelper checkHelper = (AdjustNoticeCheckHelper) checkResult.get(AjaxResult.DATA_TAG);
        Long adjustNumber = noticeOrderOperate.getAdjustNumber();
        if (adjustNumber < BigDecimal.ZERO.longValue()) {
            //调减
            return getSubtractOperatePlanList(noticeOrderOperate, checkHelper);
        }
        //调增 数据推荐
        return getAddOperatePlanList(noticeOrderOperate, checkHelper);
    }

    @Override
    public AjaxResult calculateAddQty(MonthPlanAdjustNoticeApplyOperateVo param) {
        MonthPlanAdjustNoticeAdjustApplyVo result = new MonthPlanAdjustNoticeAdjustApplyVo();
        result.setAddAdjustQty(BigDecimal.ZERO.longValue());
        AjaxResult checkResult = checkControlInfo(param.getNoticeNo());
        if (AjaxResult.Type.ERROR.value() == (Integer) checkResult.get(AjaxResult.CODE_TAG)) {
            return checkResult;
        }
        MonthPlanNeedAdjustPlanVo applySubtract = param.getApplySubtract();
        AdjustNoticeCheckHelper checkHelper = (AdjustNoticeCheckHelper) checkResult.get(AjaxResult.DATA_TAG);
        FactoryMonthPlanProdFinal subtractPlan = getProductionPlanList(checkHelper.getProductionVersion(), applySubtract.getProductionNo());
        if (null == subtractPlan) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.adjustPlanNoExist"));
        }
        FactoryProductionVersion productionVersion = checkHelper.getProductionVersion();
        Date productionStartDate = productionVersion.getProductionStartDate();
        Integer maxDays = AdjustNoticeUtils.getDatePhaseDiff(productionStartDate, productionVersion.getProductionEndDate());
        Date startDate = applySubtract.getStartAdjustDate();
        Integer startDay = AdjustNoticeUtils.getDatePhaseDiff(productionStartDate, startDate);
        Long productionTotalQty = AdjustProductionUtils.getTotalProductionQty(subtractPlan, startDay, maxDays);
        Long subtractQty = applySubtract.getNeedAdjustNumber();
        //减的量大于排产量，则不可减
        if (subtractQty > productionTotalQty) {
            String dateFormat = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, startDate);
            String errorInfo = I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.executeSubtract.subtractError");
            return AjaxResult.error(String.format(errorInfo, dateFormat, productionTotalQty, subtractQty));
        }
        //返回最新的减数据
        AdjustProductionUtils.subtractQtyByPlan(maxDays, productionStartDate, applySubtract, subtractPlan);
        result.setUpdateData(subtractPlan);
        //换算数量
        MonthPlanNoticeOrder noticeOrder = checkHelper.getNoticeOrder();
        String factoryCode = noticeOrder.getFactoryCode();
        String productCode = noticeOrder.getProductCode();
        String specCode = param.getSpecCode();
        Integer month = noticeOrder.getMonth();
        //如果物料编码一致，则不用换算
        if (productCode.equals(subtractPlan.getProductCode())) {
            result.setAddAdjustQty(subtractQty);
            return AjaxResult.success(result);
        }
        //校验获取施工信息
        AjaxResult checkConstructionInfoResult = getProductConstructionInfo(factoryCode, productCode, specCode, month);
        if (AjaxResult.Type.ERROR.value() == (Integer) checkConstructionInfoResult.get(AjaxResult.CODE_TAG)) {
            return checkConstructionInfoResult;
        }
        String subtractProductCode = subtractPlan.getProductCode();
        String subtractSpecCode = subtractPlan.getSpecCode();
        if (StringUtils.isBlank(subtractProductCode) || StringUtils.isBlank(subtractSpecCode)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.apply.subtractPlan.lackMessage"));
        }
        AjaxResult checkSubtractConstructionInfoResult = getProductConstructionInfo(factoryCode, subtractProductCode, subtractSpecCode, month);
        if (AjaxResult.Type.ERROR.value() == (Integer) checkSubtractConstructionInfoResult.get(AjaxResult.CODE_TAG)) {
            return checkConstructionInfoResult;
        }
        AdjustProductConstructionInfoHelper helper = (AdjustProductConstructionInfoHelper) checkConstructionInfoResult.get(AjaxResult.DATA_TAG);
        AdjustProductConstructionInfoHelper subtractHelper = (AdjustProductConstructionInfoHelper) checkConstructionInfoResult.get(AjaxResult.DATA_TAG);
        BigDecimal addCuringTime = helper.getCuringTime();
        BigDecimal subtractCuringTime = subtractHelper.getCuringTime();
        if (null == addCuringTime || null == subtractCuringTime) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.apply.subtractPlan.lackMessage"));
        }
        Long addQty = subtractCuringTime.multiply(BigDecimal.valueOf(subtractQty)).divide(addCuringTime, BigDecimal.ZERO.intValue(), RoundingMode.DOWN).longValue();
        result.setAddAdjustQty(addQty);
        return AjaxResult.success(result);
    }

    @Override
    public AjaxResult confirmAdjust(MonthPlanAdjustNoticeOrderOperateVo noticeOrderOperate) {
        AjaxResult checkResult = checkOperateAdjustControl(noticeOrderOperate);
        if (AjaxResult.Type.ERROR.value() == (Integer) checkResult.get(AjaxResult.CODE_TAG)) {
            return checkResult;
        }
        AdjustNoticeCheckHelper checkHelper = (AdjustNoticeCheckHelper) checkResult.get(AjaxResult.DATA_TAG);
        Long adjustNumber = noticeOrderOperate.getAdjustNumber();
        if (adjustNumber < BigDecimal.ZERO.longValue()) {
            //调减
            return confirmSubtractOperatePlanList(noticeOrderOperate, checkHelper);
        }
        //调增
        return confirmAddOperatePlanList(noticeOrderOperate, checkHelper);
    }

    @Override
    public AjaxResult confirmAdjustByDetail(MonthPlanAdjustNoticeOrderConfirmOperateVo noticeOrderConfirmOperate) {
        if (null == noticeOrderConfirmOperate || StringUtils.isBlank(noticeOrderConfirmOperate.getNoticeNo()) || CollectionUtils.isEmpty(noticeOrderConfirmOperate.getAdjustPlanList())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.noticeOrder.param.noEmpty"));
        }
        AjaxResult checkResult = checkControlInfo(noticeOrderConfirmOperate.getNoticeNo());
        if (AjaxResult.Type.ERROR.value() == (Integer) checkResult.get(AjaxResult.CODE_TAG)) {
            return checkResult;
        }
        AdjustNoticeCheckHelper checkHelper = (AdjustNoticeCheckHelper) checkResult.get(AjaxResult.DATA_TAG);
        FactoryProductionVersion productionVersion = checkHelper.getProductionVersion();
        MonthPlanNoticeOrder noticeOrder = checkHelper.getNoticeOrder();
        List<FactoryMonthPlanProdFinal> adjustPlanList = noticeOrderConfirmOperate.getAdjustPlanList();
        Long planQty = noticeOrder.getPlanQty();
        if (planQty < BigDecimal.ZERO.longValue()) {
            return confirmAdjustSubtractPlan(productionVersion, adjustPlanList, noticeOrder);
        }
        //TODO 已经采用V3版本实现方案
        return confirmAdjustAddPlan(productionVersion, adjustPlanList, noticeOrder);
    }

    @Override
    public AjaxResult importData(List<MonthPlanNoticeOrder> excelDataList, boolean updateSupport, Long importLogId) {
        if (CollectionUtils.isEmpty(excelDataList)) {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + 0);
        }
        AjaxResult checkBaseResult = checkBaseInfo(excelDataList);
        if (AjaxResult.Type.ERROR.value() == (Integer) checkBaseResult.get(AjaxResult.CODE_TAG)) {
            return checkBaseResult;
        }
        MonthPlanNoticeOrder first = (MonthPlanNoticeOrder) checkBaseResult.get(AjaxResult.DATA_TAG);
        String factoryCode = first.getFactoryCode();
        Integer year = first.getYear();
        Integer month = first.getMonth();
        FactoryProductionVersion finalVersion = factoryProductionVersionService.getFinalVersionByYearMonth(factoryCode, year, month);
        if (null == finalVersion) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.finalized.noFinalized"));
        }
        MonthPlanAdjustNoticeOrderHelper helper = new MonthPlanAdjustNoticeOrderHelper();
        helper.setProductCodeSet(new HashSet<>());
        helper.setExistProductCodeMap(new HashMap<>());
        helper.setNoExistProductInfo(I18nUtil.getMessage("ui.data.column.saleOrder.check.noExistCheck.productInfo"));
        //唯一键分组
        Function<MonthPlanNoticeOrder, String> duplicateKeyFunction = MonthPlanNoticeOrder::getImportDuplicateKey;
        Map<String, Long> duplicateGroupMap = excelDataList.stream().collect(Collectors.groupingBy(duplicateKeyFunction, Collectors.counting()));
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        // 国际化提示
        Map<String, String> errorInfoMap = buildErrorInfoMap();
        //excel数据验证
        int rowIndex = 2;
        int successNum = 0;
        int failureNum = 0;
        List<MonthPlanNoticeOrder> importDataList = new ArrayList<>();
        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < excelDataList.size(); i++) {
            int errorNum = i + 2;
            MonthPlanNoticeOrder item = excelDataList.get(i);
            helper.setRowIndex(rowIndex);
            //数据校验
            boolean checkDataResult = checkDataAndFullInfo(item, importLogId, errorNum, importErrorLogs, errorInfoMap, duplicateGroupMap, duplicateKeyFunction, helper);
            rowIndex = rowIndex + 1;
            if (!checkDataResult) {
                failureNum++;
                continue;
            }
            //加入数据
            importDataList.add(item);
        }
        if (!CollectionUtils.isEmpty(importDataList)) {
            String noticeNo = buildNoticePrefix();
            int index = 1;
            for (MonthPlanNoticeOrder addData : importDataList) {
                addData.setNoticeNo(String.format("%s%06d", noticeNo, index));
                addData.setMonthPlanVersion(finalVersion.getMonthPlanVersion());
                addData.setProductionVersion(finalVersion.getProductionVersion());
                addData.setStatus(MonthPlanAdjustNoticeStatusEnum.NEW.getStatus());
                addData.setIsImport(YesOrNoEnum.YES.getValue());
                index = index + 1;
            }
            try {
                successNum = importDataList.size();
                baseDao.insertBatch(importDataList);
            } catch (Exception e) {
                log.error("调整通知单-导入异常", e);
                successNum = 0;
                failureNum = excelDataList.size();
                importErrorLogs.clear();
                addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
            }
        } else {
            successNum = 0;
        }
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    /**
     * 确认调整计划--计划减量
     *
     * @param productionVersion 定稿版本信息
     * @param adjustPlanList    调减计划集合
     * @param noticeOrder       调整通知单
     * @return
     */
    private AjaxResult confirmAdjustSubtractPlan(FactoryProductionVersion productionVersion, List<FactoryMonthPlanProdFinal> adjustPlanList, MonthPlanNoticeOrder noticeOrder) {
        if (CollectionUtils.isEmpty(adjustPlanList)) {
            //没有可调减的计划
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.noticeOrder.param.noSubtractPlanList"));
        }
        List<FactoryMonthPlanProdFinal> needSubtractPlanList = new ArrayList<>();
        adjustPlanList.stream().forEach(subtractPlan -> {
            String productionNo = subtractPlan.getProductionNo();
            if (StringUtils.isNotBlank(productionNo)) {
                needSubtractPlanList.add(subtractPlan);
            }
        });
        if (CollectionUtils.isEmpty(needSubtractPlanList)) {
            //没有可调减的计划
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.noticeOrder.param.noSubtractPlanList"));
        }
        //获取原有计划信息
        List<String> subtractProductionNoList = needSubtractPlanList.stream().map(FactoryMonthPlanProdFinal::getProductionNo).collect(Collectors.toList());
        List<FactoryMonthPlanProdFinal> subtractProductionPlanList = getProductionPlanList(productionVersion, subtractProductionNoList);
        if (CollectionUtils.isEmpty(subtractProductionPlanList)) {
            //调整计划不存在，请确认!
            return AjaxResult.error(I18nUtil.getMessage("ui.data.adjust.data.noExistPlan"));
        }
        Integer maxDays = AdjustNoticeUtils.getProductionVersionCycleDays(productionVersion);
        String factoryCode = noticeOrder.getFactoryCode();
        Integer delayDays = getAdjustDelayDays(factoryCode, ProductTypeEnum.SEMI_STEEL.getValue());
        Date adjustStartDate = AdjustNoticeUtils.getMinStartAdjustDate(delayDays);
        Integer startDays = AdjustNoticeUtils.getDatePhaseDiff(adjustStartDate, productionVersion.getProductionEndDate());
        AjaxResult needAdjustPlanResult = AdjustNoticeUtils.getNeedAdjustPlanInfoList(startDays, maxDays, subtractProductionPlanList, needSubtractPlanList, MonthPlanAdjustTypeEnum.SUBTRACT);
        if (AjaxResult.Type.ERROR.value() == (Integer) needAdjustPlanResult.get(AjaxResult.CODE_TAG)) {
            //调整计划不存在，请确认!
            return needAdjustPlanResult;
        }
        List<MonthPlanNeedAdjustPlanVo> needAdjustPlanInfoList = (List<MonthPlanNeedAdjustPlanVo>) needAdjustPlanResult.get(AjaxResult.DATA_TAG);
        //重新构建开始日，结束日及排产总量
        AdjustProductionUtils.resetInfo(maxDays, needSubtractPlanList);
        //更新计划
        baseDao.updateBatch(needSubtractPlanList);
        //更新调整通知单及明细记录
        confirmWorkNo(noticeOrder, needAdjustPlanInfoList);
        return AjaxResult.success();
    }

    /**
     * 确认调整计划--计划增量
     *
     * @param productionVersion
     * @param adjustPlanList
     * @param noticeOrder
     * @return
     */
    @Deprecated
    private AjaxResult confirmAdjustAddPlan(FactoryProductionVersion productionVersion, List<FactoryMonthPlanProdFinal> adjustPlanList, MonthPlanNoticeOrder noticeOrder) {
        return null;
    }

    /**
     * 设置计划调整量，如果是调增，则需要先进行库存对冲
     *
     * @param noticeOrder
     */
    private void setPlanQtyAndUpdateRemainingQty(MonthPlanNoticeOrder noticeOrder) {
        Long needQty = noticeOrder.getNeedQty();
        if (needQty < BigDecimal.ZERO.longValue()) {
            //调减
            noticeOrder.setPlanQty(needQty);
            noticeOrder.setStatus(MonthPlanAdjustNoticeStatusEnum.SUBMIT.getStatus());
            return;
        }
        //调增，需要与结余库存对冲
        MonthPlanRequireStock leftOverStock = getLeftOverStock(noticeOrder.getProductCode(), noticeOrder.getMonthPlanVersion());
        if (null == leftOverStock) {
            noticeOrder.setStockAllocationQty(BigDecimal.ZERO.longValue());
            noticeOrder.setPlanQty(needQty);
            noticeOrder.setStatus(MonthPlanAdjustNoticeStatusEnum.SUBMIT.getStatus());
            return;
        }
        Long remainingQty = leftOverStock.getRemainingQty();
        //没有结余库存
        if (remainingQty <= BigDecimal.ZERO.longValue()) {
            noticeOrder.setStockAllocationQty(BigDecimal.ZERO.longValue());
            noticeOrder.setPlanQty(needQty);
            noticeOrder.setStatus(MonthPlanAdjustNoticeStatusEnum.SUBMIT.getStatus());
            return;
        }
        //结余库存可满足，则直接结束
        if (remainingQty >= needQty) {
            noticeOrder.setStockAllocationQty(needQty);
            noticeOrder.setPlanQty(BigDecimal.ZERO.longValue());
            noticeOrder.setStatus(MonthPlanAdjustNoticeStatusEnum.CONFIRM.getStatus());
            leftOverStock.setRemainingQty(remainingQty - needQty);
            baseDao.update(leftOverStock);
            return;
        }
        //结余库存不足
        leftOverStock.setRemainingQty(BigDecimal.ZERO.longValue());
        baseDao.update(leftOverStock);
        noticeOrder.setPlanQty(needQty - remainingQty);
        noticeOrder.setStockAllocationQty(remainingQty);
        noticeOrder.setStatus(MonthPlanAdjustNoticeStatusEnum.SUBMIT.getStatus());
    }

    /**
     * 对规格进行调减时，推荐需要调减的计划信息
     *
     * @return
     */
    private AjaxResult getSubtractOperatePlanList(MonthPlanAdjustNoticeOrderOperateVo noticeOrderOperate, AdjustNoticeCheckHelper checkHelper) {
        MonthPlanNoticeOrder noticeOrder = checkHelper.getNoticeOrder();
        FactoryProductionVersion productionVersion = checkHelper.getProductionVersion();
        String productCode = noticeOrder.getProductCode();
        String factoryVersion = productionVersion.getProductionVersion();
        String factoryCode = productionVersion.getFactoryCode();
        List<String> productCodeList = new ArrayList<>();
        productCodeList.add(productCode);
        List<FactoryMonthPlanProdFinal> productionList = getProductionInfoByProductCode(factoryCode, factoryVersion, productCodeList);
        if (CollectionUtils.isEmpty(productionList)) {
            String noOperateRemark = "规格没有进行排产，不可调减";
            AdjustNoticeUtils.setAutoConfirm(noticeOrder, noOperateRemark);
            monthPlanAdjustNoticeOrderMapper.updateById(noticeOrder);
            //规格没有进行排产，不可调减
            return AjaxResult.error(AjaxResult.Type.WARN.value(), I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.adjustSubtract.noAdjustByNoProductionAndAutoConfirm"));
        }
        Date startAdjustDate = noticeOrderOperate.getStartDate();
        Date productionStartDate = productionVersion.getProductionStartDate();
        Long diff = Duration.between(startAdjustDate.toInstant(), productionStartDate.toInstant()).toDays();
        Integer startAdjustDay = Math.abs(diff.intValue()) + BigDecimal.ONE.intValue();
        Integer monthMaxDay = AdjustNoticeUtils.getProductionVersionCycleDays(productionVersion);
        //统计排产量
        Long sumProductionQty = AdjustProductionUtils.totalProductionQty(productionList, startAdjustDay, monthMaxDay);
        if (sumProductionQty <= BigDecimal.ZERO.longValue()) {
            //规格在[%s]后没有排产量，不可调减
            String errorInfo = I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.adjustSubtract.noAdjustByDate");
            return AjaxResult.error(String.format(errorInfo, DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, startAdjustDate)));
        }
        List<FactoryMonthPlanProdFinal> recommendList = productionList.stream().filter(recommend -> {
            Long totalProductionQty = AdjustProductionUtils.getTotalProductionQty(recommend, startAdjustDay, monthMaxDay);
            if (totalProductionQty > BigDecimal.ZERO.longValue()) {
                return true;
            }
            return false;
        }).collect(Collectors.toList());
        AdjustNoticeSubtractPlanVo result = new AdjustNoticeSubtractPlanVo();
        List<FactoryMonthFinalPlanHelperVo> sortList = BeanCopyUtils.copyBeanList(recommendList, FactoryMonthFinalPlanHelperVo.class);
        List<PlanOrderSortConfiguration> sortConfigurationList = sortConfigurationService.getProductionConfiguration(factoryCode);
        List<PlanOrderSortConfiguration> locationSortConfiguration = AdjustNoticeUtils.getLocationSortConfiguration(sortConfigurationList);
        if (CollectionUtils.isEmpty(locationSortConfiguration)) {
            result.setSubtractPlanList(sortList);
            //排序？
            return AjaxResult.success(result);
        }
        sortList.stream().forEach(finalPlan -> AdjustNoticeUtils.setSortValue(finalPlan, locationSortConfiguration));
        sortList.sort(Comparator.comparing(FactoryMonthFinalPlanHelperVo::getSortValue, Comparator.reverseOrder()));
        result.setSubtractPlanList(sortList);
        return AjaxResult.success(result);
    }

    /**
     * 对规格进行调增时，需要调减的计划信息推荐
     *
     * @param noticeOrderOperate
     * @param checkHelper
     * @return
     */
    private AjaxResult getAddOperatePlanList(MonthPlanAdjustNoticeOrderOperateVo noticeOrderOperate, AdjustNoticeCheckHelper checkHelper) {
        MonthPlanNoticeOrder noticeOrder = checkHelper.getNoticeOrder();
        String factoryCode = noticeOrder.getFactoryCode();
        String productCode = noticeOrder.getProductCode();
        String specCode = noticeOrderOperate.getSpecCode();
        Integer month = noticeOrder.getMonth();
        //校验SAP信息
        MdmMaterialInfo productInfo = getProductInfo(noticeOrder.getFactoryCode(), noticeOrder.getProductCode());
        if (null == productInfo) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthStock.productCode.notExist"));
        }
        //校验获取施工信息
        AjaxResult checkConstructionInfoResult = getProductConstructionInfo(factoryCode, productCode, specCode, month);
        if (AjaxResult.Type.ERROR.value() == (Integer) checkConstructionInfoResult.get(AjaxResult.CODE_TAG)) {
            return checkConstructionInfoResult;
        }
        AdjustProductConstructionInfoHelper helper = (AdjustProductConstructionInfoHelper) checkConstructionInfoResult.get(AjaxResult.DATA_TAG);
        AddQtyAdjustPlanHelper addQtyInfo = AdjustNoticeUtils.buildAddQtyInfo(noticeOrderOperate, checkHelper);
        addQtyInfo.setProSize(productInfo.getProSize());
        //20250616 修改为只考虑模具产能
        AjaxResult checkQtyResult = factoryMonthPlanAdjustPlanBusinessService.checkMaxMouldQtyByStartDate(helper, addQtyInfo);
        if (AjaxResult.Type.ERROR.value() == (Integer) checkQtyResult.get(AjaxResult.CODE_TAG)) {
            return checkQtyResult;
        }
        //自动确认
        if (AjaxResult.Type.WARN.value() == (Integer) checkQtyResult.get(AjaxResult.CODE_TAG)) {
            String noOperateRemark = "规格没有剩余模具产能，不可调增";
            AdjustNoticeUtils.setAutoConfirm(noticeOrder, noOperateRemark);
            monthPlanAdjustNoticeOrderMapper.updateById(noticeOrder);
            return checkQtyResult;
        }
        AdjustNoticeSubtractPlanVo result = (AdjustNoticeSubtractPlanVo) checkQtyResult.get(AjaxResult.DATA_TAG);
        List<FactoryMonthFinalPlanHelperVo> subtractPlanList = result.getSubtractPlanList();
        if (CollectionUtils.isEmpty(subtractPlanList)) {
            return AjaxResult.success(result);
        }
        List<PlanOrderSortConfiguration> sortConfigurationList = sortConfigurationService.getProductionConfiguration(factoryCode);
        List<PlanOrderSortConfiguration> locationSortConfiguration = AdjustNoticeUtils.getLocationSortConfiguration(sortConfigurationList);
        if (CollectionUtils.isEmpty(locationSortConfiguration)) {
            return AjaxResult.success(result);
        }
        subtractPlanList.stream().forEach(finalPlan -> AdjustNoticeUtils.setSortValue(finalPlan, locationSortConfiguration));
        subtractPlanList.sort(Comparator.comparing(FactoryMonthFinalPlanHelperVo::getSortValue, Comparator.reverseOrder()));
        result.setSubtractPlanList(subtractPlanList);
        return AjaxResult.success(result);
    }

    /**
     * 确认调减
     *
     * @param noticeOrderOperate
     * @param checkHelper
     * @return
     */
    private AjaxResult confirmSubtractOperatePlanList(MonthPlanAdjustNoticeOrderOperateVo noticeOrderOperate, AdjustNoticeCheckHelper checkHelper) {
        List<MonthPlanNeedAdjustPlanVo> subtractProductionList = noticeOrderOperate.getConfirmSubtractList();
        if (CollectionUtils.isEmpty(subtractProductionList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.adjustSubtract.noEmpty"));
        }
        Integer isIgnoreInconsistent = noticeOrderOperate.getIsIgnoreInconsistent();
        if (null == isIgnoreInconsistent) {
            isIgnoreInconsistent = YesOrNoEnum.NO.getValue();
        }
        //汇总明细调减总量
        Long detailSubtractQty = BigDecimal.ZERO.longValue();
        for (MonthPlanNeedAdjustPlanVo subtractPlan : subtractProductionList) {
            detailSubtractQty = detailSubtractQty + subtractPlan.getNeedAdjustNumber();
        }
        //不忽略，则需要校验明细调减量与预计调减量值不一致
        if (YesOrNoEnum.NO.getValue().equals(isIgnoreInconsistent)) {
            Long adjustNumber = Math.abs(noticeOrderOperate.getAdjustNumber());
            if (!adjustNumber.equals(detailSubtractQty)) {
                String errorInfo = I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.adjustSubtract.numberInconsistent");
                return AjaxResult.error(AjaxResult.Type.WARN.value(), String.format(errorInfo, detailSubtractQty, adjustNumber));
            }
        }
        //对调减计划进行校验并按要求减量
        AjaxResult subtractPlanResult = subtractProductionQty(subtractProductionList, checkHelper);
        if (AjaxResult.Type.ERROR.value() == (Integer) subtractPlanResult.get(AjaxResult.CODE_TAG)) {
            return subtractPlanResult;
        }
        List<FactoryMonthPlanProdFinal> updateList = (List<FactoryMonthPlanProdFinal>) subtractPlanResult.get(AjaxResult.DATA_TAG);
        MonthPlanNoticeOrder noticeOrder = checkHelper.getNoticeOrder();
        //实际调整量--为汇总明细量
        noticeOrder.setProductionQty(-detailSubtractQty);
        if (!CollectionUtils.isEmpty(updateList)) {
            baseDao.updateBatch(updateList);
            //更新月度剩余量
            factoryMonthPlanProdFinalService.finalUpdatePlanSurplusList(updateList);
        }
        //调整单确认
        confirm(noticeOrder, subtractProductionList);
        return AjaxResult.success();
    }

    /**
     * 确认进行调增
     *
     * @param noticeOrderOperate
     * @param checkHelper
     * @return
     */
    private AjaxResult confirmAddOperatePlanList(MonthPlanAdjustNoticeOrderOperateVo noticeOrderOperate, AdjustNoticeCheckHelper checkHelper) {
        MonthPlanNoticeOrder noticeOrder = checkHelper.getNoticeOrder();
        String factoryCode = noticeOrder.getFactoryCode();
        String productCode = noticeOrder.getProductCode();
        //设置SAP代码、库位、渠道
        noticeOrderOperate.setProductCode(productCode);
        noticeOrderOperate.setLocationType(noticeOrder.getLocationType());
        noticeOrderOperate.setChannel(noticeOrder.getChannel());
        String specCode = noticeOrderOperate.getSpecCode();
        Integer month = noticeOrder.getMonth();
        //校验SAP信息
        MdmMaterialInfo productInfo = getProductInfo(factoryCode, productCode);
        if (null == productInfo) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthStock.productCode.notExist"));
        }
        noticeOrderOperate.setProductDesc(productInfo.getMaterialDesc());
        //校验施工信息
        AjaxResult checkConstructionInfoResult = getProductConstructionInfo(factoryCode, productCode, specCode, month);
        if (AjaxResult.Type.ERROR.value() == (Integer) checkConstructionInfoResult.get(AjaxResult.CODE_TAG)) {
            return checkConstructionInfoResult;
        }
        //先对调减计划进行调减
        List<MonthPlanNeedAdjustPlanVo> subtractPlanList = noticeOrderOperate.getConfirmSubtractList();
        List<FactoryMonthPlanProdFinal> updateList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(subtractPlanList)) {
            AjaxResult subtractPlanResult = subtractProductionQty(subtractPlanList, checkHelper);
            if (AjaxResult.Type.ERROR.value() == (Integer) subtractPlanResult.get(AjaxResult.CODE_TAG)) {
                return subtractPlanResult;
            }
            List<FactoryMonthPlanProdFinal> needUpdateList = (List<FactoryMonthPlanProdFinal>) subtractPlanResult.get(AjaxResult.DATA_TAG);
            if (!CollectionUtils.isEmpty(needUpdateList)) {
                updateList.addAll(needUpdateList);
            }
        }
        //校验调减后的模具产能、寸口产能、每日产能
        AdjustProductConstructionInfoHelper helper = (AdjustProductConstructionInfoHelper) checkConstructionInfoResult.get(AjaxResult.DATA_TAG);
        AddQtyAdjustPlanHelper addQtyInfo = AdjustNoticeUtils.buildAddQtyInfo(noticeOrderOperate, checkHelper);
        addQtyInfo.setProSize(productInfo.getProSize());
        AjaxResult checkAfterSubtractResult = factoryMonthPlanAdjustPlanBusinessService.checkAfterSubtractOtherPlanByMould(helper, addQtyInfo, updateList);
        if (AjaxResult.Type.ERROR.value() == (Integer) checkAfterSubtractResult.get(AjaxResult.CODE_TAG)) {
            return checkAfterSubtractResult;
        }
        //更新月度剩余量
        List<FactoryMonthPlanProdFinal> finalList = new ArrayList<>();
        //调减计划更新
        if (!CollectionUtils.isEmpty(updateList)) {
            baseDao.updateBatch(updateList);
            //调减计划加入月度剩余量集合中
            finalList.addAll(updateList);
        }
        AfterSubtractPlanInfoHelper infoHelper = (AfterSubtractPlanInfoHelper) checkAfterSubtractResult.get(AjaxResult.DATA_TAG);
        //日排产剩余量
        Map<Integer, Long> dayLimitQtyMap = infoHelper.getDayLimitQtyMap();
        //可用模具
        Map<String, MouldProductRelationDto> maxEnableMouldMap = infoHelper.getMaxEnableMouldMap();
        //根据SAP及规格代号，模具，得到模具信息，并从开始调整日排产
        FactoryProductionVersion productionVersion = checkHelper.getProductionVersion();
        List<MonthPlanNeedAdjustPlanVo> adjustDetailList = new ArrayList<>();
        //新增一条增量计划
        String newProductionNo = buildProductionNo(1);
        FactoryMonthPlanProdFinal insertPlan = AdjustNoticeUtils.buildNewProductionPlan(productionVersion, noticeOrderOperate, helper);
        insertPlan.setProductionNo(newProductionNo);
        AdjustNoticeUtils.fillProductInfo(insertPlan, productInfo);
        //设置日排产信息
        AdjustProductionUtils.addDayProductionInfo(insertPlan, helper, addQtyInfo, maxEnableMouldMap, dayLimitQtyMap, infoHelper.getStopDays());
        baseDao.insert(insertPlan);
        finalList.add(insertPlan);
        //更新月度剩余量
        if (!CollectionUtils.isEmpty(finalList)) {
            factoryMonthPlanProdFinalService.finalUpdatePlanSurplusList(finalList);
        }
        //更新实际调整量及新增调整量的明细
//        MonthPlanNeedAdjustPlanVo addProductionPlan = AdjustNoticeUtils.buildAddAdjustPlan(noticeOrderOperate, newProductionNo);
//        addProductionPlan.setNeedAdjustNumber(insertPlan.getTotalQty());
//        noticeOrder.setProductionQty(insertPlan.getTotalQty());
//        adjustDetailList.add(addProductionPlan);
        //调整单确认
        confirm(noticeOrder, adjustDetailList);
        return AjaxResult.success();
    }

    /**
     * 对需要调减的计划进行调减，从每个计划的开始调整日进行数量调减
     * 需要校验调减数量在原计划的可排产量是否能满足
     *
     * @param subtractProductionList 需调减的计划
     * @param checkHelper            校验数据
     * @return
     */
    private AjaxResult subtractProductionQty(List<MonthPlanNeedAdjustPlanVo> subtractProductionList, AdjustNoticeCheckHelper checkHelper) {
        FactoryProductionVersion productionVersion = checkHelper.getProductionVersion();
        Integer productionCycleDay = AdjustNoticeUtils.getProductionVersionCycleDays(productionVersion);
        Date productionStartDate = productionVersion.getProductionStartDate();
        //校验调减计划是否存在及对应调减量
        List<String> subtractProductionNoList = subtractProductionList.stream().map(MonthPlanNeedAdjustPlanVo::getProductionNo).collect(Collectors.toList());
        List<FactoryMonthPlanProdFinal> subtractProductionPlanList = getProductionPlanList(checkHelper.getProductionVersion(), subtractProductionNoList);
        AjaxResult checkPlanResult = AdjustProductionUtils.checkSubtractPlanInfo(productionCycleDay, productionStartDate, subtractProductionList, subtractProductionPlanList);
        if (AjaxResult.Type.ERROR.value() == (Integer) checkPlanResult.get(AjaxResult.CODE_TAG)) {
            return checkPlanResult;
        }
        Map<String, FactoryMonthPlanProdFinal> originPlanMap = (Map<String, FactoryMonthPlanProdFinal>) checkPlanResult.get(AjaxResult.DATA_TAG);
        List<FactoryMonthPlanProdFinal> updateList = new ArrayList<>();
        //对每条计划执行调减
        for (MonthPlanNeedAdjustPlanVo subtractProduction : subtractProductionList) {
            String productionNo = subtractProduction.getProductionNo();
            FactoryMonthPlanProdFinal originPlanInfo = originPlanMap.get(productionNo);
            AdjustProductionUtils.subtractQtyByPlan(productionCycleDay, productionStartDate, subtractProduction, originPlanInfo);
            //设置为调减
            subtractProduction.setAdjustType(MonthPlanAdjustTypeEnum.SUBTRACT);
            subtractProduction.setProductCode(originPlanInfo.getProductCode());
            subtractProduction.setProductDesc(originPlanInfo.getProductDesc());
            updateList.add(originPlanInfo);
        }
        return AjaxResult.success(updateList);
    }

    /**
     * 检测调整操作
     * 包括调整日期是否可调整，
     * 调整通知单状态，
     * 对应年月是否定稿
     *
     * @param noticeOrderOperate
     * @return
     */
    private AjaxResult checkOperateAdjustControl(MonthPlanAdjustNoticeOrderOperateVo noticeOrderOperate) {
        AjaxResult checkResult = checkControlInfo(noticeOrderOperate.getNoticeNo());
        if (AjaxResult.Type.ERROR.value() == (Integer) checkResult.get(AjaxResult.CODE_TAG)) {
            return checkResult;
        }
        AdjustNoticeCheckHelper checkHelper = (AdjustNoticeCheckHelper) checkResult.get(AjaxResult.DATA_TAG);
        MonthPlanNoticeOrder noticeOrder = checkHelper.getNoticeOrder();
        Date startAdjustDate = noticeOrderOperate.getStartDate();
        //版本开始日 <= 开始调整日期 <= 版本结束日
        if (!AdjustNoticeUtils.canAdjust(startAdjustDate, checkHelper.getProductionVersion())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.adjustDate.noAdjust"));
        }
        String factoryCode = noticeOrder.getFactoryCode();
        Integer delayDays = getAdjustDelayDays(factoryCode, ProductTypeEnum.SEMI_STEEL.getValue());
        Date minStartAdjustDate = AdjustNoticeUtils.getMinStartAdjustDate(delayDays);
        if (startAdjustDate.before(minStartAdjustDate)) {
            String errorInfo = I18nUtil.getMessage("ui.data.adjust.param.startDays.noAdjust");
            return AjaxResult.error(String.format(errorInfo, DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, minStartAdjustDate)));
        }
        //调减通知单，调整量只能小于0， 调增通知单，调整量只能大于0
        boolean isSameDirection = AdjustNoticeUtils.isSameDirection(noticeOrderOperate, noticeOrder);
        if (!isSameDirection) {
            Long planQty = noticeOrder.getPlanQty();
            String errorInfo = I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.noSameDirection.error");
            String text = "<";
            if (planQty < BigDecimal.ZERO.longValue()) {
                text = ">";
            }
            return AjaxResult.error(String.format(errorInfo, text));
        }
        return AjaxResult.success(checkHelper);
    }

    /**
     * 调整通知单操作调整基本校验：
     * 调整单状态校验--提交状态及调整单信息获取
     * 定稿版本校验及定稿版本信息获取
     *
     * @param noticeNo 调整通知单号
     * @return
     */
    private AjaxResult checkControlInfo(String noticeNo) {
        if (StringUtils.isBlank(noticeNo)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.noticeOrder.noEmpty"));
        }
        MonthPlanNoticeOrder noticeOrder = getMonthPlanNoticeOrderByOrderNo(noticeNo);
        if (null == noticeOrder) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.noticeOrder.noExits"));
        }
        if (!MonthPlanAdjustNoticeStatusEnum.SUBMIT.getStatus().equals(noticeOrder.getStatus())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.noticeOrder.noConfirm"));
        }
        String factoryCode = noticeOrder.getFactoryCode();
        Integer year = noticeOrder.getYear();
        Integer month = noticeOrder.getMonth();
        FactoryProductionVersion productionVersion = factoryProductionVersionService.getFinalVersionByYearMonth(factoryCode, year, month);
        if (null == productionVersion) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.finalized.noFinalized"));
        }
        return AjaxResult.success(new AdjustNoticeCheckHelper(noticeOrder, productionVersion));
    }

    /**
     * 通知单调整确认
     *
     * @param noticeOrder
     * @return
     */
    private AjaxResult confirm(MonthPlanNoticeOrder noticeOrder, List<MonthPlanNeedAdjustPlanVo> adjustPlanList) {
        if (null == noticeOrder || null == noticeOrder.getId()) {
            return AjaxResult.success();
        }
        MonthPlanNoticeOrder old = monthPlanAdjustNoticeOrderMapper.selectById(noticeOrder.getId());
        if (null == noticeOrder) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.noticeOrder.noExits"));
        }
        if (!MonthPlanAdjustNoticeStatusEnum.SUBMIT.getStatus().equals(old.getStatus())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.noticeOrder.noConfirm"));
        }
        old.setStatus(MonthPlanAdjustNoticeStatusEnum.CONFIRM.getStatus());
        old.setProductionQty(noticeOrder.getProductionQty());
        baseDao.update(old);
        //记录调整明细，即哪些其它规格调减，本身规格调整量
        if (!CollectionUtils.isEmpty(adjustPlanList)) {
            List<MonthPlanAdjustDetail> detailList = new ArrayList<>();
            adjustPlanList.stream().forEach(needAdjustPlan -> {
                MonthPlanAdjustDetail detail = AdjustNoticeUtils.buildAdjustDetail(noticeOrder, needAdjustPlan);
                detailList.add(detail);
            });
            //记录调整明细
            baseDao.insertBatch(detailList);
        }
        return AjaxResult.success();
    }

    /**
     * 通知单调整确认
     *
     * @param noticeOrder
     * @return
     */
    private AjaxResult confirmWorkNo(MonthPlanNoticeOrder noticeOrder, List<MonthPlanNeedAdjustPlanVo> adjustPlanList) {
        Long adjustNumber = BigDecimal.ZERO.longValue();
        String workNo = DateUtils.dateTimeNow();
        String productCode = noticeOrder.getProductCode();
        List<MonthPlanAdjustDetail> workNoDetailList = new ArrayList<>();
        for (MonthPlanNeedAdjustPlanVo needAdjustInfo : adjustPlanList) {
            MonthPlanAdjustDetail detail = AdjustNoticeUtils.buildAdjustDetail(noticeOrder, needAdjustInfo, workNo);
            workNoDetailList.add(detail);
            String adjustProductCode = needAdjustInfo.getProductCode();
            if (!productCode.equals(adjustProductCode)) {
                continue;
            }
            Long singleAdjustNumber = needAdjustInfo.getNeedAdjustNumber();
            if (null == singleAdjustNumber || BigDecimal.ZERO.longValue() == singleAdjustNumber) {
                continue;
            }
            MonthPlanAdjustTypeEnum adjustType = needAdjustInfo.getAdjustType();
            if (MonthPlanAdjustTypeEnum.ADD == adjustType) {
                adjustNumber = adjustNumber + singleAdjustNumber;
            } else {
                adjustNumber = adjustNumber - singleAdjustNumber;
            }
        }
        Long realAdjustNumber = noticeOrder.getProductionQty();
        if (null == realAdjustNumber) {
            realAdjustNumber = BigDecimal.ZERO.longValue();
        }
        realAdjustNumber = realAdjustNumber + adjustNumber;
        noticeOrder.setProductionQty(realAdjustNumber);
        baseDao.update(noticeOrder);
        //记录调整明细，即哪些其它规格调减，本身规格调整量
        baseDao.insertBatch(workNoDetailList);
        return AjaxResult.success();
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
     * 获取调整通知单号前缀
     *
     * @return
     */
    private String buildNoticePrefix() {
        return incrementService.getBillNoSequenceByExpire(IncrementConstant.MONTH_PLAN_ADJUST_NOTICE + com.ruoyi.common.core.utils.DateUtils.dateTimeNow("yyyyMMdd"), 3, 60 * 24 * 7);
    }

    /**
     * 根据调整通知单号，获取调整单信息
     *
     * @param noticeNo
     * @return
     */
    private MonthPlanNoticeOrder getMonthPlanNoticeOrderByOrderNo(String noticeNo) {
        QueryWrapper<MonthPlanNoticeOrder> orderQueryWrapper = new QueryWrapper<>();
        orderQueryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        orderQueryWrapper.eq("NOTICE_NO", noticeNo);
        return monthPlanAdjustNoticeOrderMapper.selectOne(orderQueryWrapper);
    }

    /**
     * 根据需求版本，获取剩余库存
     *
     * @param productCode      物料编码
     * @param monthPlanVersion 销售需求版本号
     * @return
     */
    private MonthPlanRequireStock getLeftOverStock(String productCode, String monthPlanVersion) {
        QueryWrapper<MonthPlanRequireStock> stockQuery = new QueryWrapper<>();
        stockQuery.eq("PRODUCT_CODE", productCode);
        stockQuery.eq("MONTH_PLAN_VERSION", monthPlanVersion);
        stockQuery.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return saleMonthPlanRequireStockMapper.selectOne(stockQuery);
    }

    /**
     * 根据物料编码获取物料信息
     *
     * @param factoryCode 分厂编号
     * @param productCode 物料编码
     * @return
     */
    private MdmMaterialInfo getProductInfo(String factoryCode, String productCode) {
        QueryWrapper<MdmMaterialInfo> productQuery = new QueryWrapper<>();
        productQuery.eq("FACTORY_CODE", factoryCode);
        productQuery.eq("PRODUCT_CODE", productCode);
        productQuery.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return productInfoMapper.selectOne(productQuery);
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
     * 获取productCodeList的排产计划
     *
     * @param factoryCode       分厂编码
     * @param productionVersion 排产版本
     * @param productCodeList   物料编码集合
     * @return
     */
    private List<FactoryMonthPlanProdFinal> getProductionInfoByProductCode(String factoryCode, String productionVersion, List<String> productCodeList) {
        if (StringUtils.isBlank(factoryCode) || StringUtils.isBlank(productionVersion) || CollectionUtils.isEmpty(productCodeList)) {
            return Collections.emptyList();
        }
        QueryWrapper<FactoryMonthPlanProdFinal> productionInfoQuery = new QueryWrapper<>();
        productionInfoQuery.eq("FACTORY_CODE", factoryCode);
        productionInfoQuery.eq("PRODUCTION_VERSION", productionVersion);
        productionInfoQuery.in("PRODUCT_CODE", productCodeList);
        productionInfoQuery.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return factoryMonthPlanProdFinalMapper.selectList(productionInfoQuery);
    }

    /**
     * 排产模具数
     *
     * @param productionVersion
     * @param productCode
     * @param mouldNo
     * @param startAdjustDay    开始调整日
     * @return
     */
    private Integer getMouldSize(FactoryProductionVersion productionVersion, String productCode, String mouldNo, Integer startAdjustDay) {
        QueryWrapper<FactoryMonthPlanProdFinal> productionInfoQuery = new QueryWrapper<>();
        productionInfoQuery.eq("FACTORY_CODE", productionVersion.getFactoryCode());
        productionInfoQuery.eq("PRODUCTION_VERSION", productionVersion.getProductionVersion());
        productionInfoQuery.eq("PRODUCT_CODE", productCode);
        productionInfoQuery.eq("MOULD_NO", mouldNo);
        productionInfoQuery.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        List<FactoryMonthPlanProdFinal> plannedPlanList = factoryMonthPlanProdFinalMapper.selectList(productionInfoQuery);
        if (CollectionUtils.isEmpty(plannedPlanList)) {
            return BigDecimal.ZERO.intValue();
        }
        Long startDayTotalProductionQty = BigDecimal.ZERO.longValue();
        Integer mouldSize = BigDecimal.ZERO.intValue();
        for (FactoryMonthPlanProdFinal plannedPlan : plannedPlanList) {


            Integer size = plannedPlan.getMouldQty();
            if (null == size) {
                continue;
            }
            if (size > mouldSize) {
                mouldSize = size;
            }
        }
        return mouldSize;
    }

    /**
     * 根据制造单号，获取排产计划
     *
     * @param productionVersion 定稿版本信息
     * @param productionNoList  制造单号集合
     * @return
     */
    private List<FactoryMonthPlanProdFinal> getProductionPlanList(FactoryProductionVersion productionVersion, List<String> productionNoList) {
        if (CollectionUtils.isEmpty(productionNoList)) {
            return Collections.emptyList();
        }
        QueryWrapper<FactoryMonthPlanProdFinal> productionPlanQuery = new QueryWrapper<>();
        productionPlanQuery.eq("FACTORY_CODE", productionVersion.getFactoryCode());
        productionPlanQuery.eq("YEAR", productionVersion.getYear());
        productionPlanQuery.eq("MONTH", productionVersion.getMonth());
        productionPlanQuery.eq("MONTH_PLAN_VERSION", productionVersion.getMonthPlanVersion());
        productionPlanQuery.eq("PRODUCTION_VERSION", productionVersion.getProductionVersion());
        productionPlanQuery.in("PRODUCTION_NO", productionNoList);
        productionPlanQuery.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return factoryMonthPlanProdFinalMapper.selectList(productionPlanQuery);
    }

    /**
     * 根据制造单号，获取排产计划
     *
     * @param productionVersion 定稿版本信息
     * @param productionNo      制造单号
     * @return
     */
    private FactoryMonthPlanProdFinal getProductionPlanList(FactoryProductionVersion productionVersion, String productionNo) {
        if (StringUtils.isBlank(productionNo)) {
            return null;
        }
        QueryWrapper<FactoryMonthPlanProdFinal> productionPlanQuery = new QueryWrapper<>();
        productionPlanQuery.eq("FACTORY_CODE", productionVersion.getFactoryCode());
        productionPlanQuery.eq("YEAR", productionVersion.getYear());
        productionPlanQuery.eq("MONTH", productionVersion.getMonth());
        productionPlanQuery.eq("MONTH_PLAN_VERSION", productionVersion.getMonthPlanVersion());
        productionPlanQuery.eq("PRODUCTION_VERSION", productionVersion.getProductionVersion());
        productionPlanQuery.eq("PRODUCTION_NO", productionNo);
        productionPlanQuery.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return factoryMonthPlanProdFinalMapper.selectOne(productionPlanQuery);
    }

    /**
     * 获取施工相关信息
     *
     * @param factoryCode 分厂编码
     * @param productCode SAP代码
     * @param specCode    规格代号
     * @return
     */
    private AjaxResult getProductConstructionInfo(String factoryCode, String productCode, String specCode, Integer month) {
        MdmProductConstructionDto productConstructionInfo = mdmProductConstructionService.getCuringTime(factoryCode, productCode, specCode);
        if (null == productConstructionInfo) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.adjust.check.noHasCuringTime"));
        }
        Map<String, Integer> changeConfiguration = factoryParamService.getChangeSummerMonth(factoryCode);
        if (CollectionUtils.isEmpty(changeConfiguration)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.adjust.check.noHasCuringTime"));
        }
        BigDecimal curingTime = productConstructionInfo.getRealCuringTime(month, changeConfiguration.get(FactoryConstant.SYS_PARAM_SUMMER_MONTH), changeConfiguration.get(FactoryConstant.SYS_PARAM_WINTER_MONTH));
        if (null == curingTime) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.adjust.check.noHasCuringTime"));
        }
        AdjustProductConstructionInfoHelper helper = new AdjustProductConstructionInfoHelper();
        //设置一天最大硫化时间
        BigDecimal dayWorkHourTime = factoryParamService.getDayMaxCuringTime(factoryCode);
        helper.setDayMaxCuringTime(dayWorkHourTime);
        //获取单条硫化时间，加上间隔单条硫化时间，设置硫化时间
        BigDecimal addCuringTimeValue = factoryParamService.getSingleAddCuringTime(factoryCode);
        helper.setCuringTime(curingTime.add(addCuringTimeValue));
        //设置施工信息
        helper.setConstructionCode(productConstructionInfo.getConstructionCode());
        List<ProductSpecInfoVo> productSpecCodeInfoList = productConstructionInfo.getProductSpecCodeInfoList();
        if (!CollectionUtils.isEmpty(productSpecCodeInfoList)) {
            helper.setSpecCodeInfo(JSON.toJSONString(productSpecCodeInfoList));
        }
        return AjaxResult.success(helper);
    }

    /**
     * 基础校验，保存一个分厂，一个年月的一个版本
     *
     * @param excelDataList
     * @return
     */
    private AjaxResult checkBaseInfo(List<MonthPlanNoticeOrder> excelDataList) {
        // 只能导入相同年、月、分厂的
        List<MonthPlanNoticeOrder> effectiveData = excelDataList.stream().filter(noticeOrder -> {
            if (StringUtils.isBlank(noticeOrder.getFactoryCode())) {
                return false;
            }
            if (null == noticeOrder.getYear()) {
                return false;
            }
            if (null == noticeOrder.getMonth()) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(effectiveData)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.checkFactoryYearMonth"));
        }
        if (excelDataList.stream().map(MonthPlanNoticeOrder::getFactoryCode).filter(com.ruoyi.common.utils.StringUtils::isNotBlank).distinct().count() > 1) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.importCheck"));
        }
        if (excelDataList.stream().map(MonthPlanNoticeOrder::getYear).filter(Objects::nonNull).distinct().count() > 1) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.importCheck"));
        }
        if (excelDataList.stream().map(MonthPlanNoticeOrder::getMonth).filter(Objects::nonNull).distinct().count() > 1) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.importCheck"));
        }
        return AjaxResult.success(effectiveData.get(0));
    }

    /**
     * 数据行校验
     *
     * @param item                 数据行
     * @param importLogId          导入日志ID
     * @param errorNum             错误行
     * @param importErrorLogs      错误日志集合对象
     * @param buildErrorInfo       错误信息集合
     * @param duplicateGroupMap    重复数据集合
     * @param duplicateKeyFunction 重复键集合
     * @param helper               辅助信息对象
     * @return
     */
    private boolean checkDataAndFullInfo(MonthPlanNoticeOrder item, Long importLogId, Integer errorNum, List<ImportErrorLog> importErrorLogs, Map<String, String> buildErrorInfo, Map<String, Long> duplicateGroupMap, Function<MonthPlanNoticeOrder, String> duplicateKeyFunction, MonthPlanAdjustNoticeOrderHelper helper) {
        //数据基本校验
        List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, item);
        if (!CollectionUtils.isEmpty(validated)) {
            importErrorLogs.addAll(validated);
            return false;
        }
        // 重复记录校验
        Long hasValue = duplicateGroupMap.get(duplicateKeyFunction.apply(item));
        if (hasValue > BigDecimal.ONE.longValue()) {
            addImportErrorLog(importLogId, errorNum, buildErrorInfo.get(ERROR_REPEAT), importErrorLogs);
            return false;
        }
        //校验SAP代码
        return checkProductionInfoAndFull(helper, item, importErrorLogs);
    }

    /**
     * 信息校验
     *
     * @param helper          校验辅助类
     * @param noticeOrder     销售订单
     * @param importErrorLogs 错误信息集合
     * @return
     */
    private boolean checkProductionInfoAndFull(MonthPlanAdjustNoticeOrderHelper helper, MonthPlanNoticeOrder noticeOrder, List<ImportErrorLog> importErrorLogs) {
        Long importLogId = helper.getImportLogId();
        int rowIndex = helper.getRowIndex();
        String productCode = noticeOrder.getProductCode();
        String noExistProductInfo = helper.getNoExistProductInfo();
        String factoryCode = noticeOrder.getFactoryCode();
        //不存在
        if (helper.getProductCodeSet().contains(productCode)) {
            String errorInfo = String.format(noExistProductInfo, rowIndex, productCode);
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, rowIndex, errorInfo, importErrorLogs);
            return false;
        }
        //存在
        Map<String, MdmMaterialInfo> existProductCodeMap = helper.getExistProductCodeMap();
        if (existProductCodeMap.containsKey(productCode)) {
            MdmMaterialInfo productInfo = existProductCodeMap.get(productCode);
            AdjustNoticeUtils.setProductInfo(noticeOrder, productInfo);
            return true;
        }
        //查找
        MdmMaterialInfo productionInfo = hasExistProductCode(factoryCode, productCode);
        if (null != productionInfo) {
            existProductCodeMap.put(productCode, productionInfo);
            AdjustNoticeUtils.setProductInfo(noticeOrder, productionInfo);
        } else {
            helper.getProductCodeSet().add(productCode);
            String errorInfo = String.format(noExistProductInfo, rowIndex, productCode);
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, rowIndex, errorInfo, importErrorLogs);
            return false;
        }
        return true;
    }

    /**
     * 构建检验错误提示信息集合
     *
     * @return
     */
    private Map<String, String> buildErrorInfoMap() {
        Map<String, String> errorInfoMap = new HashMap<>();
        String repeat = I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.repeat");
        String productCodeNotExist = I18nUtil.getMessage("ui.data.column.monthPlanNoticeOrder.productCode.notExist");
        errorInfoMap.put(ERROR_REPEAT, repeat);
        errorInfoMap.put(ERROR_PRODUCT_CODE, productCodeNotExist);
        return errorInfoMap;
    }

    /**
     * 是否存在
     *
     * @param factoryCode 分厂编码
     * @param productCode 物料编号
     * @return
     */
    private MdmMaterialInfo hasExistProductCode(String factoryCode, String productCode) {
        QueryWrapper<MdmMaterialInfo> productInfoQueryWrapper = new QueryWrapper<>();
        productInfoQueryWrapper.eq("FACTORY_CODE", factoryCode);
        productInfoQueryWrapper.eq("PRODUCT_CODE", productCode);
        productInfoQueryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return productInfoMapper.selectOne(productInfoQueryWrapper);
    }

    /**
     * 构建查询条件
     *
     * @param queryWrapper   查询条件构建器
     * @param queryCondition 查询条件值对象
     */
    protected void builderCondition(QueryWrapper<MonthPlanNoticeOrder> queryWrapper, MonthPlanNoticeOrder queryCondition) {
        queryWrapper.like(PubUtil.isNotEmpty(queryCondition.getFieldValueByFieldName("noticeNo")), "NOTICE_NO", queryCondition.getFieldValueByFieldName("noticeNo"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryCondition.getFieldValueByFieldName("status")), "STATUS", queryCondition.getFieldValueByFieldName("status"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryCondition.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryCondition.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryCondition.getFieldValueByFieldName("year")), "YEAR", queryCondition.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryCondition.getFieldValueByFieldName("month")), "MONTH", queryCondition.getFieldValueByFieldName("month"));
        queryWrapper.like(PubUtil.isNotEmpty(queryCondition.getFieldValueByFieldName("productCode")), "PRODUCT_CODE", queryCondition.getFieldValueByFieldName("productCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryCondition.getFieldValueByFieldName("productDesc")), "PRODUCT_DESC", queryCondition.getFieldValueByFieldName("productDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryCondition.getFieldValueByFieldName("locationType")), "LOCATION_TYPE", queryCondition.getFieldValueByFieldName("locationType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryCondition.getFieldValueByFieldName("channel")), "CHANNEL", queryCondition.getFieldValueByFieldName("channel"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryCondition.getFieldValueByFieldName("brand")), "BRAND", queryCondition.getFieldValueByFieldName("brand"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryCondition.getFieldValueByFieldName("proSize")), "PRO_SIZE", queryCondition.getFieldValueByFieldName("proSize"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryCondition.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryCondition.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryCondition.getFieldValueByFieldName("productTypeName")), "PRODUCT_TYPE_NAME", queryCondition.getFieldValueByFieldName("productTypeName"));
        queryWrapper.like(PubUtil.isNotEmpty(queryCondition.getFieldValueByFieldName("specifications")), "SPECIFICATIONS", queryCondition.getFieldValueByFieldName("specifications"));
        queryWrapper.like(PubUtil.isNotEmpty(queryCondition.getFieldValueByFieldName("pattern")), "PATTERN", queryCondition.getFieldValueByFieldName("pattern"));
        queryWrapper.like(PubUtil.isNotEmpty(queryCondition.getFieldValueByFieldName("hierarchy")), "HIERARCHY", queryCondition.getFieldValueByFieldName("hierarchy"));
    }
}
