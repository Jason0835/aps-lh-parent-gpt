package com.zlt.aps.nc.engine.service.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.nc.api.domain.entity.NcScheduleResult;
import com.zlt.aps.nc.engine.constant.NcEngineConstants;
import com.zlt.aps.nc.engine.mapper.NcEngineScheduleResultMapper;
import com.zlt.aps.nc.engine.service.INcOrderGeneratorService;

import cn.hutool.core.date.DateUtil;

/**
 * 垫胶排程批次号与工单号生成服务实现
 */
@Service
public class NcOrderGeneratorServiceImpl implements INcOrderGeneratorService {

    @Resource
    private NcEngineScheduleResultMapper djEngineScheduleResultMapper;

    @Override
    public String generateBatchNo(String factoryCode, Date scheduleDate) {
        String dateStr = DateUtil.format(scheduleDate, NcEngineConstants.BATCH_NO_DATE_FORMAT);
        String prefix = NcEngineConstants.BATCH_NO_PREFIX + dateStr;

        // 查询当天已使用的最大批次号序号
        List<NcScheduleResult> records = djEngineScheduleResultMapper.selectList(
                new LambdaQueryWrapper<NcScheduleResult>()
                        .eq(NcScheduleResult::getScheduleDate, scheduleDate)
                        .isNotNull(NcScheduleResult::getBatchNo));
        String maxBatchNo = records.stream()
                .map(NcScheduleResult::getBatchNo)
                .max(String::compareTo)
                .orElse(null);
        int seq = 1; // 默认从 001 开始
        if (maxBatchNo != null && maxBatchNo.startsWith(prefix)) {
            try {
                String seqStr = maxBatchNo.substring(prefix.length());
                seq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }
        return prefix + String.format(NcEngineConstants.BATCH_NO_SEQ_FORMAT, seq);
    }

    @Override
    public String generateOrderNo(String batchNo, int maxSeq) {
        return batchNo + "-" + String.format("%04d", maxSeq + 1);
    }

    @Override
    public String fillOrderInfo(List<NcScheduleResult> results, String factoryCode, Date scheduleDate) {
        if (results == null || results.isEmpty()) {
            return null;
        }
        String batchNo = this.generateBatchNo(factoryCode, scheduleDate);
        int orderSeq = 0;
        for (NcScheduleResult result : results) {
            result.setBatchNo(batchNo);
            orderSeq++;
            result.setOrderNo(batchNo + String.format(NcEngineConstants.ORDER_NO_SEQ_FORMAT, orderSeq));
        }
        return batchNo;
    }
}
