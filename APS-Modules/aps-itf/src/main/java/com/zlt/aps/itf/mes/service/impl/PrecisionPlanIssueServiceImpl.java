package com.zlt.aps.itf.mes.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.aps.itf.constant.SysCode;
import com.zlt.aps.itf.mes.enums.ItfSyncKeyEnum;
import com.zlt.aps.itf.mes.mapper.PrecisionPlanIssueMapper;
import com.zlt.aps.itf.mes.service.IPrecisionPlanIssueService;
import com.zlt.aps.itf.vo.MesPrecisionPlan;
import com.zlt.aps.lh.api.domain.entity.LhPrecisionPlanIssue;
import com.zlt.sync.handle.SyncDataHandle;
import com.zlt.sync.povo.SyncParamsVO;
import com.zlt.sync.service.SyncDataLogsService;
import com.zlt.aps.itf.vo.SyncDataLogs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 精度计划下发服务实现
 * 统一处理成型精度计划和硫化精度计划的下发到MES中间表MES_PRECISION_PLAN
 * PRECISION_TYPE值直接存"硫化精度"或"成型精度"，不做字典值存储
 *
 * @author APS Team
 */
@Slf4j
@Service
public class PrecisionPlanIssueServiceImpl implements IPrecisionPlanIssueService {

    @Autowired
    private SyncDataHandle syncDataHandle;

    @Autowired
    private SyncDataLogsService syncDataLogsService;

    @Autowired
    private PrecisionPlanIssueMapper precisionPlanIssueMapper;

    @Override
    public AjaxResult issueLhPrecisionPlan(List<LhPrecisionPlanIssue> lhPrecisionPlanIssueList, String factoryCode, String companyCode) {
        if (CollectionUtils.isEmpty(lhPrecisionPlanIssueList)) {
            return AjaxResult.success("没有需要下发的精度计划数据");
        }

        String dataVersion = syncDataHandle.getDataVersion(ItfSyncKeyEnum.SYNC_PRECISION_PLAN.getCode());

        List<MesPrecisionPlan> mesList = convertToMesList(lhPrecisionPlanIssueList, dataVersion, companyCode, factoryCode);

        if (CollectionUtils.isEmpty(mesList)) {
            return AjaxResult.success("没有需要下发的精度计划数据");
        }

        List<MesPrecisionPlan> insertList = new ArrayList<>();
        try {
            // 中间表MES_PRECISION_PLAN建在jy_aps_mid主库，需切换到master数据源
            DynamicDataSourceContextHolder.push(DataSource.MASTER);
            for (MesPrecisionPlan mesItem : mesList) {
                int updateCount = precisionPlanIssueMapper.updateByMachineCodeAndPrecisionType(mesItem);
                if (updateCount == 0) {
                    insertList.add(mesItem);
                }
            }
            if (CollectionUtils.isNotEmpty(insertList)) {
                precisionPlanIssueMapper.batchInsertPrecisionPlan(insertList);
            }
        } finally {
            DynamicDataSourceContextHolder.poll();
        }

        return sendMqNotice(mesList, dataVersion, factoryCode, companyCode);
    }

    private List<MesPrecisionPlan> convertToMesList(List<LhPrecisionPlanIssue> list, String dataVersion,
                                                     String companyCode, String factoryCode) {
        List<MesPrecisionPlan> result = new ArrayList<>();
        for (LhPrecisionPlanIssue item : list) {
            MesPrecisionPlan mesItem = new MesPrecisionPlan();
            mesItem.setId(item.getId());
            mesItem.setMachineCode(item.getMachineCode());
            mesItem.setPrecisionType("硫化精度");
            mesItem.setScheduleDate(item.getScheduleDate());
            mesItem.setPlanDate(item.getPlanDate());
            mesItem.setDataVersion(dataVersion);
            mesItem.setCompanyCode(companyCode);
            mesItem.setFactoryCode(factoryCode);
            result.add(mesItem);
        }
        return result;
    }

    private AjaxResult sendMqNotice(List<MesPrecisionPlan> mesList, String dataVersion,
                                    String factoryCode, String companyCode) {
        try {
            SyncParamsVO syncParamsVO = new SyncParamsVO();
            syncParamsVO.setSyncKey(ItfSyncKeyEnum.SYNC_PRECISION_PLAN.getCode());
            syncParamsVO.setDataVersion(dataVersion);

            JSONObject params = new JSONObject();
            params.put("rowCount", mesList.size());
            syncParamsVO.setParams(params);
            syncParamsVO.setDataSys(SysCode.APS);
            syncParamsVO.setDockSys(ApsConstant.DOCK_SYS_MES);
            syncParamsVO.setFactoryCode(factoryCode);
            syncParamsVO.setCompanyCode(companyCode);

            syncDataHandle.syncNotice(syncParamsVO);

            SyncDataLogs logs = syncDataLogsService.getSyncDataResult(dataVersion);
            String status = logs.getStatus();
            if (ApsConstant.IS_RELEASE.equals(status)) {
                return AjaxResult.success("精度计划下发成功");
            } else {
                return AjaxResult.error(logs.getMsg());
            }
        } catch (Exception e) {
            log.error("精度计划下发MES失败", e);
            return AjaxResult.error("精度计划下发MES失败：" + e.getMessage());
        }
    }
}
