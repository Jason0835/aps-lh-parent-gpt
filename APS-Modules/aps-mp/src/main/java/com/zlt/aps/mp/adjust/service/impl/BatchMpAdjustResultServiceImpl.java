package com.zlt.aps.mp.adjust.service.impl;

import com.ruoyi.common.datasource.service.BaseService;
import com.zlt.aps.mp.adjust.mapper.BatchMpAdjustResultEntityMapper;
import com.zlt.aps.mp.adjust.service.IBatchMpAdjustResultService;
import com.zlt.aps.mp.api.domain.entity.MpAdjustResult;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 月计划调整结果Impl，仅用于支持批量操作
 */
@Service
public class BatchMpAdjustResultServiceImpl extends BaseService<MpAdjustResult> implements IBatchMpAdjustResultService {

    @Override
    public void insertBatchData(Collection<MpAdjustResult> collection) {
        this.insertBatchData(collection, BatchMpAdjustResultEntityMapper.class);
    }

    @Override
    public void updateBatchData(Collection<MpAdjustResult> collection) {
        this.updateBatchData(collection, BatchMpAdjustResultEntityMapper.class);
    }

    @Override
    public void mergerIntoBatchData(List<MpAdjustResult> list) {

    }
}