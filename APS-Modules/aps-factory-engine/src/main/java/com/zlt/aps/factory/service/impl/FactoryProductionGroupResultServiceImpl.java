package com.zlt.aps.factory.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.factory.mapper.ProductionGroupResultHelperMapper;
import com.zlt.aps.factory.service.IFactoryProductionGroupResultService;
import com.zlt.aps.monthplan.api.domain.entity.ProductionGroupResultHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 分厂月度计划分组排产结果辅助信息服务实现
 * 性能优化需要
 *
 * @author ZLT
 * 20250515
 */
@Slf4j
@Service
public class FactoryProductionGroupResultServiceImpl extends ServiceImpl<ProductionGroupResultHelperMapper, ProductionGroupResultHelper> implements IFactoryProductionGroupResultService {
}
