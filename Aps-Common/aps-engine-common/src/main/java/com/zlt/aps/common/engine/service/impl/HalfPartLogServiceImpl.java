package com.zlt.aps.common.engine.service.impl;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.domain.CxMonthPlanSurplusLog;
import com.zlt.aps.common.engine.domain.ProcedureSurplusLog;
import com.zlt.aps.common.engine.mapper.CxMonthPlanSurplusLogMapper;
import com.zlt.aps.common.engine.mapper.ProcedureSurplusLogMapper;
import com.zlt.aps.common.engine.service.HalfPartLogService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Gim
 */
@Service
public class HalfPartLogServiceImpl implements HalfPartLogService {
    @Resource
    private ProcedureSurplusLogMapper halfPartLogMapper;

    @Resource
    private CxMonthPlanSurplusLogMapper lhLogMapper;


    @Override
    public void addCxHalfPartLog(List<ProcedureSurplusLog> list) {
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        halfPartLogMapper.insertBatch(list);
    }

    @Override
    public void addLhLog(List<CxMonthPlanSurplusLog> list) {
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        lhLogMapper.insertBatch(list);
    }

    /**
     * 删除历史版本数据
     * @param toDeleteVersion
     */
    @Override
    public void removeHistoryVersion(String toDeleteVersion) {
        if (StringUtils.isEmpty(toDeleteVersion)) {
            return;
        }
        halfPartLogMapper.deleteByApsMonthVersion(toDeleteVersion);
    }
}
