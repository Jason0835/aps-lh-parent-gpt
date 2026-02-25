package com.zlt.aps.mp.engine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.mp.engine.mapper.FactoryNoProductionPlanMapper;
import com.zlt.aps.mp.engine.service.IFactoryProductionNoProductionPlanService;
import com.zlt.aps.mp.api.domain.entity.MonthPlanNoProductionPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 分厂月度计划未排计划服务实现
 * 性能优化需要
 *
 * @author ZLT
 * 20250515
 */
@Slf4j
@Service
public class FactoryProductionNoProductionPlanServiceImpl extends ServiceImpl<FactoryNoProductionPlanMapper, MonthPlanNoProductionPlan> implements IFactoryProductionNoProductionPlanService {
}
