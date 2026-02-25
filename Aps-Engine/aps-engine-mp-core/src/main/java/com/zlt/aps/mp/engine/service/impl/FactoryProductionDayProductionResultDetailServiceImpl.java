package com.zlt.aps.mp.engine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.mp.engine.mapper.FactoryMouldingDayResultDetailMapper;
import com.zlt.aps.mp.engine.service.IFactoryProductionDayProductionResultDetailService;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 工厂月度计划-模具排产结果明细记录数据
 * 性能优化需要
 *
 * @author ZLT
 * 20251225
 */
@Slf4j
@Service
public class FactoryProductionDayProductionResultDetailServiceImpl extends ServiceImpl<FactoryMouldingDayResultDetailMapper, FactoryMonthPlanMouldDayDetail> implements IFactoryProductionDayProductionResultDetailService {
}
