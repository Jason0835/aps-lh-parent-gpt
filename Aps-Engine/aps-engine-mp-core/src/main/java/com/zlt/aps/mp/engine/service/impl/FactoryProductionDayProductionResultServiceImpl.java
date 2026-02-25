package com.zlt.aps.mp.engine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.mp.engine.mapper.FactoryMouldingDayResultMapper;
import com.zlt.aps.mp.engine.service.IFactoryProductionDayProductionResultService;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 工厂月度计划日排产结果服务实现
 * 性能优化需要
 *
 * @author ZLT
 * 20251225
 */
@Slf4j
@Service
public class FactoryProductionDayProductionResultServiceImpl extends ServiceImpl<FactoryMouldingDayResultMapper, FactoryMonthPlanMouldDayResult> implements IFactoryProductionDayProductionResultService {
}
