package com.zlt.aps.factory.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.factory.mapper.FactoryMonthPlanPreProductionCapacityMapper;
import com.zlt.aps.factory.service.IFactoryMonthPlanPreProductionCapacityService;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanPreProductionCapacity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 分厂月度计划模具预占产能服务实现
 * 性能优化需要
 *
 * @author ZLT
 * 20250718
 */
@Slf4j
@Service
public class FactoryMonthPlanPreProductionCapacityServiceImpl extends ServiceImpl<FactoryMonthPlanPreProductionCapacityMapper, MonthPlanPreProductionCapacity> implements IFactoryMonthPlanPreProductionCapacityService {
}
