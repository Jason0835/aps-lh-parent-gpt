package com.zlt.aps.maindata.service.impl;

import com.ruoyi.common.datasource.service.BaseService;
import com.zlt.aps.maindata.mapper.BatchMpMonthPlanStatisticsEntityMapper;
import com.zlt.aps.maindata.service.IBatchMpMonthPlanStatisticsService;
import com.zlt.aps.mp.api.domain.entity.MpMonthPlanStatistics;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 月计划调整日志Impl，仅用于支持批量操作
 */
@Service
public class BatchMpMonthPlanStatisticsServiceImpl extends BaseService<MpMonthPlanStatistics> implements IBatchMpMonthPlanStatisticsService {

    @Override
    public void insertBatchData(Collection<MpMonthPlanStatistics> collection) {
        this.insertBatchData(collection, BatchMpMonthPlanStatisticsEntityMapper.class);
    }

    @Override
    public void updateBatchData(Collection<MpMonthPlanStatistics> collection) {
        this.updateBatchData(collection, BatchMpMonthPlanStatisticsEntityMapper.class);
    }

    @Override
    public void mergerIntoBatchData(List<MpMonthPlanStatistics> list) {

    }
}