package com.zlt.aps.tq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.tq.api.domain.entity.TqRollingLog;
import com.zlt.aps.tq.mapper.TqRollingLogMapper;
import com.zlt.aps.tq.service.ITqRollingLogService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 胎圈排程滚动更新日志Service业务层处理
 *
 * @author APS
 */
@Service
public class TqRollingLogServiceImpl extends AbstractDocService<TqRollingLog> implements ITqRollingLogService {

    @Resource
    private TqRollingLogMapper tqRollingLogMapper;

    @Override
    protected String getDocTypeCode() {
        return "TQ_ROLLING_LOG";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TQ_ROLLING_LOG");
        return sysDocType;
    }

    /**
     * 根据批次号查询滚动更新日志
     *
     * @param batchNo 滚动批次号
     * @return 滚动更新日志
     */
    @Override
    public TqRollingLog selectByBatchNo(String batchNo) {
        LambdaQueryWrapper<TqRollingLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TqRollingLog::getBatchNo, batchNo)
               .eq(TqRollingLog::getIsDelete, 0);
        return tqRollingLogMapper.selectOne(wrapper);
    }
}
