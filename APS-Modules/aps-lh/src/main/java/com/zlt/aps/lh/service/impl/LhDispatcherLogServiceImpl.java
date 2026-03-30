package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.lh.api.domain.entity.LhDispatcherLog;
import com.zlt.aps.lh.api.domain.vo.LhDispatcherLogVo;
import com.zlt.aps.lh.mapper.LhDispatcherLogEntityMapper;
import com.zlt.aps.lh.service.ILhDispatcherLogService;
import com.zlt.common.utils.PubUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhDispatcherLogServiceImpl.java
 * 描    述：LhDispatcherLogServiceImpl硫化调度员排程操作日志业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LhDispatcherLogServiceImpl implements ILhDispatcherLogService {

    private final LhDispatcherLogEntityMapper baseMapper;

    /**
     * 列表查询
     */
    @Override
    public List<LhDispatcherLog> selectList(LhDispatcherLog queryVO) {
        QueryWrapper<LhDispatcherLog> wrapper = new QueryWrapper<>();
        builderCondition(wrapper, queryVO);
        List<LhDispatcherLog> list = baseMapper.selectList(wrapper);
        return list;
    }

    /**
     * 插入
     * @param entity
     */
    @Override
    public void insert(LhDispatcherLog entity){
        baseMapper.insert(entity);
    }
    protected void builderCondition(QueryWrapper<LhDispatcherLog> queryWrapper, LhDispatcherLog queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleId")), "SCHEDULE_ID", queryVO.getFieldValueByFieldName("scheduleId"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("operType")), "OPER_TYPE", queryVO.getFieldValueByFieldName("operType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleDate")), "SCHEDULE_DATE", queryVO.getFieldValueByFieldName("scheduleDate"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specCode")), "SPEC_CODE", queryVO.getFieldValueByFieldName("specCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beforeMachineCode")), "BEFORE_MACHINE_CODE", queryVO.getFieldValueByFieldName("beforeMachineCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beforeClass1Plan")), "BEFORE_CLASS1_PLAN", queryVO.getFieldValueByFieldName("beforeClass1Plan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beforeClass2Plan")), "BEFORE_CLASS2_PLAN", queryVO.getFieldValueByFieldName("beforeClass2Plan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beforeClass3Plan")), "BEFORE_CLASS3_PLAN", queryVO.getFieldValueByFieldName("beforeClass3Plan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beforeClass4Plan")), "BEFORE_CLASS4_PLAN", queryVO.getFieldValueByFieldName("beforeClass4Plan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("afterMachineCode")), "AFTER_MACHINE_CODE", queryVO.getFieldValueByFieldName("afterMachineCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("afterClass1Plan")), "AFTER_CLASS1_PLAN", queryVO.getFieldValueByFieldName("afterClass1Plan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("afterClass2Plan")), "AFTER_CLASS2_PLAN", queryVO.getFieldValueByFieldName("afterClass2Plan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("afterClass3Plan")), "AFTER_CLASS3_PLAN", queryVO.getFieldValueByFieldName("afterClass3Plan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("afterClass4Plan")), "AFTER_CLASS4_PLAN", queryVO.getFieldValueByFieldName("afterClass4Plan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("createBy")), "CREATE_BY", queryVO.getFieldValueByFieldName("createBy"));

        // 开始时间和结束时间查询
        if (queryVO.getCreateTimeStart() != null) {
            queryWrapper.ge("CREATE_TIME", queryVO.getCreateTimeStart());
        }
        if (queryVO.getCreateTimeEnd() != null) {
            queryWrapper.lt("CREATE_TIME", DateUtils.addDays(queryVO.getCreateTimeEnd(), 1));
        }
    }

    /**
     * 查询是否有变更列表
     *
     * @param queryVO        查询参数
     * @param scheduleIdList 排程ID列表
     * @return 结果
     */
    @Override
    public List<LhDispatcherLogVo> selectIsChangeList(LhDispatcherLog queryVO, List<Long> scheduleIdList) {
        return baseMapper.selectIsChangeList(queryVO, scheduleIdList);
    }
}
