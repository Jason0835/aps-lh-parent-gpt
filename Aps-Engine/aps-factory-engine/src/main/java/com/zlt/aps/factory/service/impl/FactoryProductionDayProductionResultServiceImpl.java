package com.zlt.aps.factory.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.factory.mapper.FactoryMouldingDayResultMapper;
import com.zlt.aps.factory.service.IFactoryProductionDayProductionResultService;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanMouldDayResult;
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
