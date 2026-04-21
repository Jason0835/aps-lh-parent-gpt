package com.zlt.aps.mp.adjust.service.impl;

import com.ruoyi.common.datasource.service.BaseService;
import com.zlt.aps.mp.adjust.mapper.BatchMpAdjustMaterialLogEntityMapper;
import com.zlt.aps.mp.adjust.service.IBatchMpAdjustMaterialLogService;
import com.zlt.aps.mp.api.domain.entity.MpAdjustMaterialLog;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 月计划调整日志Impl，仅用于支持批量操作
 */
@Service
public class BatchMpAdjustMaterialLogServiceImpl extends BaseService<MpAdjustMaterialLog> implements IBatchMpAdjustMaterialLogService {

    @Override
    public void insertBatchData(Collection<MpAdjustMaterialLog> collection) {
        this.insertBatchData(collection, BatchMpAdjustMaterialLogEntityMapper.class);
    }

    @Override
    public void updateBatchData(Collection<MpAdjustMaterialLog> collection) {
        this.updateBatchData(collection, BatchMpAdjustMaterialLogEntityMapper.class);
    }

    @Override
    public void mergerIntoBatchData(List<MpAdjustMaterialLog> list) {

    }
}