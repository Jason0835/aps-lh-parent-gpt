package com.zlt.aps.nc.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.nc.api.domain.entity.NcShiftConfig;
import com.zlt.aps.nc.mapper.NcShiftConfigMapper;
import com.zlt.aps.nc.service.INcShiftConfigService;
import com.zlt.bill.common.service.AbstractDocService;

/**
 * 内衬班制配置Service实现
 *
 * @author zlt
 */
@Service
public class NcShiftConfigServiceImpl extends AbstractDocService<NcShiftConfig> implements INcShiftConfigService {
    @Autowired
    private NcShiftConfigMapper djShiftConfigMapper;
    
    @Override
    public List<NcShiftConfig> listActiveShifts() {
        LambdaQueryWrapper<NcShiftConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper
//        .eq(DjShiftConfig::getOpenFlag, "1")
               .orderByAsc(NcShiftConfig::getShiftOrder);
        return djShiftConfigMapper.selectList(wrapper);
    }

    @Override
    protected String getDocTypeCode() {
        return "";
    }
}
