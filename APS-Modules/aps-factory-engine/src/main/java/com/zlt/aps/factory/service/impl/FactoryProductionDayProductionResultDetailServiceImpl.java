package com.zlt.aps.factory.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.factory.mapper.FactoryMouldingDayResultDetailMapper;
import com.zlt.aps.factory.service.IFactoryProductionDayProductionResultDetailService;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanMouldDayDetail;
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
