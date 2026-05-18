package com.zlt.aps.mp.adjust.service.impl;

import com.ruoyi.common.datasource.service.BaseService;
import com.zlt.aps.mp.adjust.mapper.BatchMpAdjustResultEntityMapper;
import com.zlt.aps.mp.adjust.mapper.BatchMpProductionFinalResultEntityMapper;
import com.zlt.aps.mp.adjust.service.IBatchMpProductionFinalResultService;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.entity.MpAdjustResult;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 月计划定稿Impl，仅用于支持批量操作
 */
@Service
public class BatchMpProductionFinalResultServiceImpl extends BaseService<FactoryMonthPlanProductionFinalResult> implements IBatchMpProductionFinalResultService {

    @Override
    public void insertBatchData(Collection<FactoryMonthPlanProductionFinalResult> collection) {
        this.insertBatchData(collection, BatchMpProductionFinalResultEntityMapper.class);
    }

    @Override
    public void updateBatchData(Collection<FactoryMonthPlanProductionFinalResult> collection) {
        this.updateBatchData(collection, BatchMpProductionFinalResultEntityMapper.class);
    }

    @Override
    public void mergerIntoBatchData(List<FactoryMonthPlanProductionFinalResult> list) {

    }
}