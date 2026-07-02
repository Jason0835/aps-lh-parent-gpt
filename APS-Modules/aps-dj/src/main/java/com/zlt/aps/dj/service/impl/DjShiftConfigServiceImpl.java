package com.zlt.aps.dj.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.dj.api.domain.entity.DjShiftConfig;
import com.zlt.aps.dj.mapper.DjShiftConfigMapper;
import com.zlt.aps.dj.service.IDjShiftConfigService;
import com.zlt.bill.common.service.AbstractDocService;

/**
 * 垫胶班制配置Service实现
 *
 * @author zlt
 */
@Service
public class DjShiftConfigServiceImpl extends AbstractDocService<DjShiftConfig> implements IDjShiftConfigService {
    @Autowired
    private DjShiftConfigMapper djShiftConfigMapper;
    
    @Override
    public List<DjShiftConfig> listActiveShifts() {
        LambdaQueryWrapper<DjShiftConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DjShiftConfig::getOpenFlag, "1")
               .orderByAsc(DjShiftConfig::getShiftOrder);
        return djShiftConfigMapper.selectList(wrapper);
    }

    @Override
    protected String getDocTypeCode() {
        return "";
    }
}
