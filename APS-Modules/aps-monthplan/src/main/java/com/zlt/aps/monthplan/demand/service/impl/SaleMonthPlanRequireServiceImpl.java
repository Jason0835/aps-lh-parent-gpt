package com.zlt.aps.monthplan.demand.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.BeanCopyUtils;
import com.zlt.aps.monthplan.api.domain.entity.SaleMonthPlanRequire;
import com.zlt.aps.monthplan.api.domain.vo.SaleMonthPlanRequireReportVo;
import com.zlt.aps.monthplan.demand.mapper.SaleMonthPlanRequireMapper;
import com.zlt.aps.monthplan.demand.service.ISaleMonthPlanRequireService;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：SaleMonthPlanRequireServiceImpl.java
 * 描    述：SaleMonthPlanRequireServiceImpl月度生产需求计划业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-14
 */
@Slf4j
@Service
public class SaleMonthPlanRequireServiceImpl implements ISaleMonthPlanRequireService {

    /**
     * 最大长度不能大于8
     */
    private final String MAX_LENGTH_ERROR_INFO = "最大长度不能大于8";

    private final SaleMonthPlanRequireMapper saleMonthPlanRequireMapper;

    private final BaseDao baseDao;

    public SaleMonthPlanRequireServiceImpl(SaleMonthPlanRequireMapper saleMonthPlanRequireMapper,
                                           BaseDao baseDao) {
        this.saleMonthPlanRequireMapper = saleMonthPlanRequireMapper;
        this.baseDao = baseDao;
    }

    @Override
    public List<SaleMonthPlanRequire> getList(Wrapper<SaleMonthPlanRequire> queryWrapper) {
        return saleMonthPlanRequireMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public int removeByIds(List<Long> ids) {
        return saleMonthPlanRequireMapper.deleteBatchIds(ids);
    }

    @Override
    public AjaxResult importData(List<SaleMonthPlanRequire> excelDataList, boolean updateSupport, Long importLogId) {
        if (CollectionUtils.isEmpty(excelDataList)) {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + 0);
        }
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        //excel数据验证
        int rowIndex = 2;
        int successNum = 0;
        int failureNum = 0;
        List<SaleMonthPlanRequire> importDataList = new ArrayList<>();
        for (SaleMonthPlanRequire saleMonthPlanRequire : excelDataList) {
            saleMonthPlanRequire.setIsImport(YesOrNoEnum.YES.getValue());
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, rowIndex, saleMonthPlanRequire);
            rowIndex = rowIndex + 1;
            if (!CollectionUtils.isEmpty(validated)) {
                failureNum = failureNum + 1;
                convertError(validated);
                importErrorLogs.addAll(validated);
                continue;
            }
            importDataList.add(saleMonthPlanRequire);
        }
        //没有数据能导入，则表示校验没有通过
        if (CollectionUtils.isEmpty(importDataList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }
        String monthPlanVersion = importDataList.get(0).getMonthPlanVersion();
        //更新处理
        if (updateSupport) {
            return updateImport(importDataList);
        }
        saleMonthPlanRequireMapper.deleteByVersion(monthPlanVersion);
        baseDao.insertBatch(importDataList);
        successNum = importDataList.size();
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    /**
     * 查询对应年月+分厂的需求计划版本
     */
    @Override
    public List<String> versionList(SaleMonthPlanRequire query) {
        if (query.getYear() == null || query.getMonth() == null || StringUtils.isBlank(query.getFactoryCode())) {
            return Collections.emptyList();
        }
        return saleMonthPlanRequireMapper.versionList(query);
    }

    /**
     * 错误提示信息转化处理
     *
     * @param validated
     */
    private void convertError(List<ImportErrorLog> validated) {
        validated.stream().forEach(importErrorLog -> {
            String errorDetail = importErrorLog.getErrorDetail();
            String[] temp = errorDetail.split("：");
            if (temp.length > 1 && MAX_LENGTH_ERROR_INFO.equals(temp[1])) {
                temp[1] = "：只能输入8位以内的整数";
                importErrorLog.setErrorDetail(temp[0] + temp[1]);
            }
        });
    }

    /**
     * 更新的方式导入
     *
     * @param importDataList
     * @return
     */
    private AjaxResult updateImport(List<SaleMonthPlanRequire> importDataList) {
        SaleMonthPlanRequire firstRow = importDataList.get(0);
        Integer year = firstRow.getYear();
        Integer month = firstRow.getMonth();
        String factoryCode = firstRow.getFactoryCode();
        QueryWrapper<SaleMonthPlanRequire> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(true, "FACTORY_CODE", factoryCode);
        queryWrapper.eq(true, "YEAR", year);
        queryWrapper.eq(true, "MONTH", month);
        List<SaleMonthPlanRequire> oldList = getList(queryWrapper);
        if (CollectionUtils.isEmpty(oldList)) {
            baseDao.insertBatch(importDataList);
            Integer successNum = importDataList.size();
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
        Map<String, Long> oldDataMap = new HashMap<>();
        oldList.stream().forEach(oldData -> {
            String importUpdateKey = oldData.getMergeGroupKey();
            if (org.apache.commons.lang3.StringUtils.isBlank(importUpdateKey)) {
                return;
            }
            oldDataMap.put(importUpdateKey, oldData.getId());
        });
        List<SaleMonthPlanRequire> insertList = new ArrayList<>();
        List<SaleMonthPlanRequire> updateList = new ArrayList<>();
        importDataList.stream().forEach(importData -> {
            String updateKey = importData.getImportUpdateKey();
            //更新
            if (oldDataMap.containsKey(updateKey)) {
                importData.setId(oldDataMap.get(updateKey));
                updateList.add(importData);
                return;
            }
            //插入
            insertList.add(importData);
        });
        if (!CollectionUtils.isEmpty(insertList)) {
            baseDao.insertBatch(insertList);
        }
        if (!CollectionUtils.isEmpty(updateList)) {
            baseDao.updateBatch(updateList);
        }
        Integer successNum = importDataList.size();
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    /**
     * 根据条件查询统计数据
     *
     * @param queryVO 查询条件
     * @return 结果
     */
    @Override
    public SaleMonthPlanRequireReportVo getSummaryVo(SaleMonthPlanRequire queryVO) {
        SaleMonthPlanRequireReportVo reportVo = saleMonthPlanRequireMapper.getSummaryVo(queryVO);
        SaleMonthPlanRequire ensureQueryVO = BeanCopyUtils.copyBean(queryVO, SaleMonthPlanRequire.class);
        ensureQueryVO.setIsEnsurePlan(YesOrNoEnum.YES.getValue());
        SaleMonthPlanRequireReportVo ensurePlanReportVo = saleMonthPlanRequireMapper.getSummaryVo(ensureQueryVO);
        reportVo.setEnsurePlanSkuCount(ensurePlanReportVo.getSkuCount());
        reportVo.setEnsurePlanSkuQty(ensurePlanReportVo.getSkuQty());
        SaleMonthPlanRequire deliveryDateQueryVO = BeanCopyUtils.copyBean(queryVO, SaleMonthPlanRequire.class);
        deliveryDateQueryVO.setIsDeliveryDateDue(YesOrNoEnum.YES.getValue());
        SaleMonthPlanRequireReportVo deliveryDateDueReportVo = saleMonthPlanRequireMapper.getSummaryVo(deliveryDateQueryVO);
        reportVo.setDeliveryDateSkuCount(deliveryDateDueReportVo.getSkuCount());
        reportVo.setDeliveryDateSkuQty(deliveryDateDueReportVo.getSkuQty());
        SaleMonthPlanRequire stockUpQueryVO = BeanCopyUtils.copyBean(queryVO, SaleMonthPlanRequire.class);
        stockUpQueryVO.setIsStockUp(YesOrNoEnum.YES.getValue());
        SaleMonthPlanRequireReportVo stockUpReportVo = saleMonthPlanRequireMapper.getSummaryVo(stockUpQueryVO);
        reportVo.setStockUpQty(stockUpReportVo.getSkuQty());
        SaleMonthPlanRequire notStockUpQueryVO = BeanCopyUtils.copyBean(queryVO, SaleMonthPlanRequire.class);
        notStockUpQueryVO.setIsStockUp(YesOrNoEnum.NO.getValue());
        SaleMonthPlanRequireReportVo netDemandReportVo = saleMonthPlanRequireMapper.getSummaryVo(notStockUpQueryVO);
        reportVo.setNetDemandQty(netDemandReportVo.getSkuQty());
        return reportVo;
    }
}
