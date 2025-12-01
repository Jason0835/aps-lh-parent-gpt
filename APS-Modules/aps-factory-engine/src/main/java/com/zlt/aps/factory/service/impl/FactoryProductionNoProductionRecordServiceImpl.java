package com.zlt.aps.factory.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.factory.mapper.FactoryNoProductionRecordMapper;
import com.zlt.aps.factory.service.IFactoryProductionNoProductionRecordService;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoProductionRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 分厂月度计划不排记录服务接口
 * 性能优化需要
 *
 * @author ZLT
 * 20250515
 */
@Slf4j
@Service
public class FactoryProductionNoProductionRecordServiceImpl extends ServiceImpl<FactoryNoProductionRecordMapper, MonthPlanNoProductionRecord> implements IFactoryProductionNoProductionRecordService {
}
