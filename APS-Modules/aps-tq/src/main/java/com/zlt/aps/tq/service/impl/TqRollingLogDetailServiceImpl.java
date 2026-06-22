package com.zlt.aps.tq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.tq.api.domain.entity.TqRollingLogDetail;
import com.zlt.aps.tq.mapper.TqRollingLogDetailMapper;
import com.zlt.aps.tq.service.ITqRollingLogDetailService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 胎圈排程滚动更新日志明细Service实现类
 *
 * @author APS
 */
@Service
public class TqRollingLogDetailServiceImpl extends AbstractDocService<TqRollingLogDetail> implements ITqRollingLogDetailService {

    @Resource
    private TqRollingLogDetailMapper tqRollingLogDetailMapper;

    @Override
    protected String getDocTypeCode() {
        return "TQ_ROLLING_LOG_DETAIL";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TQ_ROLLING_LOG_DETAIL");
        return sysDocType;
    }

    /**
     * 根据主表ID查询日志明细列表
     *
     * @param logId 主表ID
     * @return 日志明细列表
     */
    @Override
    public List<TqRollingLogDetail> selectByLogId(Long logId) {
        LambdaQueryWrapper<TqRollingLogDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TqRollingLogDetail::getLogId, logId)
               .eq(TqRollingLogDetail::getIsDelete, 0)
               .orderByAsc(TqRollingLogDetail::getScheduleId)
               .orderByAsc(TqRollingLogDetail::getShiftIndex)
               .orderByAsc(TqRollingLogDetail::getChangeType);
        return tqRollingLogDetailMapper.selectList(wrapper);
    }
}
