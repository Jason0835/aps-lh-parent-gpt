package com.zlt.aps.itf.mes.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.itf.mes.enums.ItfSyncKeyEnum;
import com.zlt.aps.itf.mes.service.IMonthPlanIssueService;
import com.zlt.aps.itf.vo.MonthPlanIssue;
import com.zlt.aps.itf.vo.SyncDataLogs;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sync.handle.SyncDataHandle;
import com.zlt.sync.povo.SyncParamsVO;
import com.zlt.sync.service.SyncDataLogsService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanIssueServiceImpl.java
 * 描    述：MonthPlanIssueServiceImpl月计划下发业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-24
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MonthPlanIssueServiceImpl extends AbstractDocService<MonthPlanIssue> implements IMonthPlanIssueService {

    @Autowired
    private SyncDataHandle syncDataHandle;

    @Autowired
    private SyncDataLogsService syncDataLogsService;

    @Override
    protected String getDocTypeCode() {
        return "ITF1001";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("ITF1001");
        return sysDocType;
    }

    @Override
    public String checkUnique(MonthPlanIssue docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.monthPlanIssue.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    /**
     * 下发月计划
     *
     * @param monthPlanIssueList 列表
     * @return 列表
     */
    @Override
    public AjaxResult issueMonthPlan(List<FactoryMonthPlanProductionFinalResult> monthPlanIssueList) {
        if (CollectionUtils.isEmpty(monthPlanIssueList)) {
            return AjaxResult.success();
        }
        List<MonthPlanIssue> monthPlanIssues = new ArrayList<>();
        for (FactoryMonthPlanProductionFinalResult finalResult : monthPlanIssueList) {
            MonthPlanIssue monthPlanIssue = new MonthPlanIssue();
            monthPlanIssue.setMpVersionNo(finalResult.getProductionVersion());
            monthPlanIssue.setMpYear(String.valueOf(finalResult.getYear()));
            monthPlanIssue.setMpMonth(String.valueOf(finalResult.getMonth()));
            monthPlanIssue.setOrderNo(finalResult.getProductionNo());
            monthPlanIssue.setStrucCode(finalResult.getStructureName());
            monthPlanIssue.setMesMaterialCode(finalResult.getMaterialCode());
            monthPlanIssue.setSpecDesc(finalResult.getMaterialDesc());
            monthPlanIssue.setEmbryoSpec(finalResult.getMainMaterialDesc());
            monthPlanIssue.setPattern(finalResult.getPattern());
            monthPlanIssue.setCavity(finalResult.getMouldCavityQty());
            monthPlanIssue.setLiveBlock(finalResult.getTypeBlockQty());
            monthPlanIssue.setNetDemand(finalResult.getFactProdReqQty());
            monthPlanIssue.setAdvNum(finalResult.getHeightProductionQty());
            monthPlanIssue.setMonthAvgNum(finalResult.getAverageQty());
            monthPlanIssue.setStockSaleRatio(finalResult.getInventorySalesRatio());
            monthPlanIssue.setDayVulQty(finalResult.getDayVulcanizationQty());
            /*monthPlanIssue.setAdjustQty1();
            monthPlanIssue.setAdjustQty2();
            monthPlanIssue.setAdjustQty3();
            monthPlanIssue.setAdjustQty4();*/
            for (int i = 1; i <= 31; i++) {
                Object fieldValue = ReflectUtils.getFieldValue(finalResult, "day" + i);
                ReflectUtils.setFieldValue(monthPlanIssue, "day" + i, fieldValue);
            }
            String dataVersion = String.valueOf(System.currentTimeMillis());
            monthPlanIssue.setDataVersion(dataVersion);
            monthPlanIssue.setCompanyCode(finalResult.getFactoryCode());
            monthPlanIssue.setFactoryCode(finalResult.getFactoryCode());
        }
        baseDao.saveBatch(monthPlanIssues);
        // 发送MQ
        AjaxResult ajaxResult = null;
        // 获取下发接口版本号
        String dataVersion = syncDataHandle.getDataVersion(ItfSyncKeyEnum.SYNC_MONTH_PLAN.getCode());
        String factoryCode = monthPlanIssueList.get(0).getFactoryCode();
        String monthPlanVersion = monthPlanIssueList.get(0).getMonthPlanVersion();
        String productionVersion = monthPlanIssueList.get(0).getProductionVersion();
        try {
            // 数据同步到中间库后，往 mq中发送消息通知 MES去取数据
            SyncParamsVO syncParamsVO = new SyncParamsVO();
            syncParamsVO.setSyncKey(ItfSyncKeyEnum.SYNC_MONTH_PLAN.getCode());
            syncParamsVO.setDataVersion(dataVersion);
            // 请求参数
            JSONObject params = new JSONObject();
            params.put("monthPlanVersion", monthPlanVersion);
            params.put("productionVersion", productionVersion);
            params.put("rowCount", monthPlanIssues.size());
            syncParamsVO.setParams(params);
            syncParamsVO.setDockSys(ApsConstant.DOCK_SYS_MES);
            syncParamsVO.setFactoryCode(factoryCode);
            syncParamsVO.setCompanyCode(factoryCode);
            //往消息队列发送消息
            syncDataHandle.syncNotice(syncParamsVO);
            // 取回mes的反馈结果
            SyncDataLogs logs = syncDataLogsService.getSyncDataResult(dataVersion);
            String status = logs.getStatus();
            if (ApsConstant.IS_RELEASE.equals(status)) {
                // 成功
                ajaxResult = AjaxResult.success(I18nUtil.getMessage("ui.data.column.scheduleResult.successPublish"));
            } else {
                // 失败，需要返回异常信息
                ajaxResult = AjaxResult.error(logs.getMsg());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.failedPublish"));
        }
        return ajaxResult;
    }
}
