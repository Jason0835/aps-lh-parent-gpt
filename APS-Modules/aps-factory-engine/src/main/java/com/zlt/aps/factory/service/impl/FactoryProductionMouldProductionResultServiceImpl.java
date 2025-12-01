package com.zlt.aps.factory.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.factory.mapper.MouldingProductionResultHelperMapper;
import com.zlt.aps.factory.service.IFactoryProductionMouldProductionResultService;
import com.zlt.aps.monthplan.api.domain.entity.MouldingProductionResultHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 分厂月度计划模具辅助信息服务实现
 * 性能优化需要
 *
 * @author ZLT
 * 20250515
 */
@Slf4j
@Service
public class FactoryProductionMouldProductionResultServiceImpl extends ServiceImpl<MouldingProductionResultHelperMapper, MouldingProductionResultHelper> implements IFactoryProductionMouldProductionResultService {
}
