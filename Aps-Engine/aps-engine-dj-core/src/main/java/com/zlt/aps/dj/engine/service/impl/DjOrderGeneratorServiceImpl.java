package com.zlt.aps.dj.engine.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.dj.api.domain.entity.DjScheduleResult;
import com.zlt.aps.dj.engine.constant.DjEngineConstants;
import com.zlt.aps.dj.engine.mapper.DjEngineScheduleResultMapper;
import com.zlt.aps.dj.engine.service.IDjOrderGeneratorService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 垫胶排程批次号与工单号生成服务实现
 */
@Service
public class DjOrderGeneratorServiceImpl implements IDjOrderGeneratorService {

    @Resource
    private DjEngineScheduleResultMapper djEngineScheduleResultMapper;

    @Override
    public String generateBatchNo(String factoryCode, Date scheduleDate) {
        String dateStr = DateUtil.format(scheduleDate, DjEngineConstants.BATCH_NO_DATE_FORMAT);
        String prefix = DjEngineConstants.BATCH_NO_PREFIX + dateStr;

        // 查询当天已使用的最大批次号序号
        List<DjScheduleResult> records = djEngineScheduleResultMapper.selectList(
                new LambdaQueryWrapper<DjScheduleResult>()
                        .eq(DjScheduleResult::getScheduleDate, scheduleDate)
                        .isNotNull(DjScheduleResult::getBatchNo));
        String maxBatchNo = records.stream()
                .map(DjScheduleResult::getBatchNo)
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
        return prefix + String.format(DjEngineConstants.BATCH_NO_SEQ_FORMAT, seq);
    }

    @Override
    public String generateOrderNo(String batchNo, int maxSeq) {
        return batchNo + "-" + String.format("%04d", maxSeq + 1);
    }

    @Override
    public String fillOrderInfo(List<DjScheduleResult> results, String factoryCode, Date scheduleDate) {
        if (results == null || results.isEmpty()) {
            return null;
        }
        String batchNo = this.generateBatchNo(factoryCode, scheduleDate);
        int orderSeq = 0;
        for (DjScheduleResult result : results) {
            result.setBatchNo(batchNo);
            orderSeq++;
            result.setOrderNo(batchNo + String.format(DjEngineConstants.ORDER_NO_SEQ_FORMAT, orderSeq));
        }
        return batchNo;
    }
}
