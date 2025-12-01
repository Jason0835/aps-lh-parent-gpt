package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.cx.mapper.entity.CxDispatcherLogMapper;
import com.zlt.aps.cx.service.CxDispatcherLogService;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxDispatcherLog;
import com.zlt.common.utils.PubUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 成型调度员排程操作日志Service业务层处理
 *
 * @author Gim
 * @date 2022-02-25
 */
@Service
public class CxDispatcherLogServiceImpl implements CxDispatcherLogService {
    @Autowired
    private CxDispatcherLogMapper cxDispatcherLogMapper;

    /**
     * 查询成型调度员排程操作日志列表
     *
     * @param dispatcherLog 成型调度员排程操作日志
     * @return 成型调度员排程操作日志
     */
    @Override
    public List<CxDispatcherLog> selectCxDispatcherLogList(CxDispatcherLog dispatcherLog) {
        QueryWrapper<CxDispatcherLog> wrapper = new QueryWrapper<>();
        builderCondition(wrapper, dispatcherLog);
        return cxDispatcherLogMapper.selectList(wrapper);
    }

    @Override
    public int insertCxDispatcherLog(CxDispatcherLog cxDispatcherLog) {
        cxDispatcherLog.setBaseVale(null);
        return cxDispatcherLogMapper.insert(cxDispatcherLog);
    }

    protected void builderCondition(QueryWrapper<CxDispatcherLog> queryWrapper, CxDispatcherLog queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleId")), "SCHEDULE_ID", queryVO.getFieldValueByFieldName("scheduleId"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("operType")), "OPER_TYPE", queryVO.getFieldValueByFieldName("operType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleDate")), "SCHEDULE_DATE", queryVO.getFieldValueByFieldName("scheduleDate"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("sapCode")), "SAP_CODE", queryVO.getFieldValueByFieldName("sapCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("embryoCode")), "EMBRYO_CODE", queryVO.getFieldValueByFieldName("embryoCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("embryoVersion")), "EMBRYO_VERSION", queryVO.getFieldValueByFieldName("embryoVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beforeLhMachineCode")), "BEFORE_LH_MACHINE_CODE", queryVO.getFieldValueByFieldName("beforeLhMachineCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beforeCxMachineCode")), "BEFORE_CX_MACHINE_CODE", queryVO.getFieldValueByFieldName("beforeCxMachineCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beforeClass1Plan")), "BEFORE_CLASS1_PLAN", queryVO.getFieldValueByFieldName("beforeClass1Plan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beforeClass2Plan")), "BEFORE_CLASS2_PLAN", queryVO.getFieldValueByFieldName("beforeClass2Plan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beforeClass3Plan")), "BEFORE_CLASS3_PLAN", queryVO.getFieldValueByFieldName("beforeClass3Plan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beforeClass4Plan")), "BEFORE_CLASS4_PLAN", queryVO.getFieldValueByFieldName("beforeClass4Plan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beforeClass5Plan")), "BEFORE_CLASS5_PLAN", queryVO.getFieldValueByFieldName("beforeClass5Plan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("afterLhMachineCode")), "AFTER_LH_MACHINE_CODE", queryVO.getFieldValueByFieldName("afterLhMachineCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("afterCxMachineCode")), "AFTER_CX_MACHINE_CODE", queryVO.getFieldValueByFieldName("afterCxMachineCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("afterClass1Plan")), "AFTER_CLASS1_PLAN", queryVO.getFieldValueByFieldName("afterClass1Plan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("afterClass2Plan")), "AFTER_CLASS2_PLAN", queryVO.getFieldValueByFieldName("afterClass2Plan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("afterClass3Plan")), "AFTER_CLASS3_PLAN", queryVO.getFieldValueByFieldName("afterClass3Plan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("afterClass4Plan")), "AFTER_CLASS4_PLAN", queryVO.getFieldValueByFieldName("afterClass4Plan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("afterClass5Plan")), "AFTER_CLASS5_PLAN", queryVO.getFieldValueByFieldName("afterClass5Plan"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("createBy")), "CREATE_BY", queryVO.getFieldValueByFieldName("createBy"));
        // 开始时间和结束时间查询
        if (queryVO.getStartTime() != null) {
            queryWrapper.ge("CREATE_TIME", queryVO.getStartTime());
        }
        if (queryVO.getEndTime() != null) {
            queryWrapper.lt("CREATE_TIME", DateUtils.addDays(queryVO.getEndTime(), 1));
        }
    }



}
