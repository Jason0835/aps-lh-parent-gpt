package com.zlt.aps.cx.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.cx.mapper.entity.AutoScheduleLogEntityMapper;
import com.zlt.aps.cx.service.IAutoScheduleLogService;
import com.zlt.aps.cxlh.cx.api.domain.entity.AutoScheduleLog;
import com.zlt.common.utils.PubUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：AutoScheduleLogServiceImpl.java
 * 描    述：AutoScheduleLogServiceImpl成型自动排程日志业务层处理
 *@author zlt
 *@date 2025-03-07
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoScheduleLogServiceImpl implements IAutoScheduleLogService {


    private final AutoScheduleLogEntityMapper autoScheduleLogMapper;

    /**
     * 条件拼接
     */
    protected void builderCondition(QueryWrapper<AutoScheduleLog> queryWrapper, AutoScheduleLog queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("procedureCode")), "PROCEDURE_CODE", queryVO.getFieldValueByFieldName("procedureCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("batchNo")), "BATCH_NO", queryVO.getFieldValueByFieldName("batchNo"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("orderNo")), "ORDER_NO", queryVO.getFieldValueByFieldName("orderNo"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("title")), "TITLE", queryVO.getFieldValueByFieldName("title"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("logDetail")), "LOG_DETAIL", queryVO.getFieldValueByFieldName("logDetail"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("delFlag")), "DEL_FLAG", queryVO.getFieldValueByFieldName("delFlag"));
    }

    /**
     * 列表查询
     *
     * @param queryVO
     */
    @Override
    public List<AutoScheduleLog> selectList(AutoScheduleLog queryVO) {
        QueryWrapper<AutoScheduleLog> wrapper = new QueryWrapper<>();
        builderCondition(wrapper, queryVO);
        return autoScheduleLogMapper.selectList(wrapper);
    }

    /**
     * 新增自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param logDetail  日志内容
     */
    @Override
    public void insertLhScheduleLog(String procedureCode,String batchNo, String orderNo, String title, String logDetail){
        AutoScheduleLog autoScheduleLog = new AutoScheduleLog();
        autoScheduleLog.setProcedureCode(procedureCode);
        autoScheduleLog.setBatchNo(batchNo);
        autoScheduleLog.setOrderNo(orderNo);
        autoScheduleLog.setTitle(title);
        autoScheduleLog.setLogDetail(logDetail);
        autoScheduleLog.setIsDelete(0);
        autoScheduleLogMapper.insert(autoScheduleLog);
    }
}



