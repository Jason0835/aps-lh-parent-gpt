package com.zlt.aps.mp.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zlt.aps.mp.api.domain.entity.MpAdjustPlanRequireInfo;
import com.zlt.aps.mp.factory.mapper.MpAdjustPlanRequireInfoEntityMapper;
import com.zlt.aps.mp.factory.service.IMpAdjustPlanRequireInfoService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 计划调整信息业务服务实现
 *
 * @author ZLT
 * 20260716
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MpAdjustPlanRequireInfoServiceImpl extends AbstractDocService<MpAdjustPlanRequireInfo> implements IMpAdjustPlanRequireInfoService {

    private final MpAdjustPlanRequireInfoEntityMapper adjustPlanInfoMapper;

    @Override
    public List<MpAdjustPlanRequireInfo> getListByCondition(QueryWrapper<MpAdjustPlanRequireInfo> wrapper) {
        if (null == wrapper) {
            return Collections.emptyList();
        }
        return adjustPlanInfoMapper.selectList(wrapper);
    }

    @Override
    protected String getDocTypeCode() {
        return "S2-0801";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode(getDocTypeCode());
        return sysDocType;
    }
}
