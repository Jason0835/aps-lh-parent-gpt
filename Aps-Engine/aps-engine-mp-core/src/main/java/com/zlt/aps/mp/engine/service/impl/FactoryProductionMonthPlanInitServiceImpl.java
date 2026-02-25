package com.zlt.aps.mp.engine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.mp.engine.mapper.FactoryProductionInitMapper;
import com.zlt.aps.mp.engine.service.IFactoryProductionMonthPlanInitService;
import com.zlt.aps.mp.api.domain.entity.ProductionMonthPlanInit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 分厂月度计划日排产初始化服务接口
 * 性能优化需要
 *
 * @author ZLT
 * 20250515
 */
@Slf4j
@Service
public class FactoryProductionMonthPlanInitServiceImpl extends ServiceImpl<FactoryProductionInitMapper, ProductionMonthPlanInit> implements IFactoryProductionMonthPlanInitService {
}
