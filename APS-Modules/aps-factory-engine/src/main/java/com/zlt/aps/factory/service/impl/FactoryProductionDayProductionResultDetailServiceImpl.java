package com.zlt.aps.factory.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.factory.mapper.FactoryProductionResultDetailMapper;
import com.zlt.aps.factory.service.IFactoryProductionDayProductionResultDetailService;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanProductionResultDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 分厂月度计划日排产结果明细服务实现
 * 性能优化需要
 *
 * @author ZLT
 * 20250515
 */
@Slf4j
@Service
public class FactoryProductionDayProductionResultDetailServiceImpl extends ServiceImpl<FactoryProductionResultDetailMapper, MonthPlanProductionResultDetail> implements IFactoryProductionDayProductionResultDetailService {
}
