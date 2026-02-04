package com.zlt.aps.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tlt.aps.utils.BeanCopyUtils;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.mapper.FactoryProductionInitMapper;
import com.zlt.aps.factory.mapper.FactoryProductionSchedulingMapper;
import com.zlt.aps.factory.service.IFactoryMouldUsedStatusLogService;
import com.zlt.aps.factory.service.IFactoryProductionMonthPlanInitService;
import com.zlt.aps.factory.service.MonthProductionDataService;
import com.zlt.aps.monthplan.api.domain.entity.MouldProductionLog;
import com.zlt.aps.monthplan.api.domain.entity.MpMouldUsedStatusLog;
import com.zlt.aps.monthplan.api.domain.entity.ProductionMonthPlanInit;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * 排产调用数据获取服务类
 * 月份排产模块-数据业务实现
 *
 * @author ZLT
 * @date 20251208
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonthProductionDataServiceImpl extends AbstractDataService implements MonthProductionDataService {

    private final BaseDao baseDao;

    private final FactoryProductionInitMapper factoryProductionInitMapper;

    private final FactoryProductionSchedulingMapper factoryProductionSchedulingMapper;

    private final IFactoryMouldUsedStatusLogService factoryMouldUsedStatusLogService;

    private final IFactoryProductionMonthPlanInitService factoryProductionMonthPlanInitService;

    @Override
    public void deletedInitData(Context context) {
        String productionVersion = context.getProductionVersion();
        if (StringUtils.isBlank(productionVersion)) {
            return;
        }
        factoryProductionSchedulingMapper.deleteProductionInitVersion(context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
    }

    @Override
    public void deletedMouldProductionData(Context context) {
        String productionVersion = context.getProductionVersion();
        if (StringUtils.isBlank(productionVersion)) {
            return;
        }
        factoryProductionSchedulingMapper.deleteProductionMouldVersion(context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
    }

    @Override
    public void saveMonthPlanInit(List<MonthPlanProductionRequirePlanVo> monthPlanInitList) {
        if (CollectionUtils.isEmpty(monthPlanInitList)) {
            return;
        }
        List<ProductionMonthPlanInit> saveMonthPlanInitList = BeanCopyUtils.copyBeanList(monthPlanInitList, ProductionMonthPlanInit.class);
        factoryProductionMonthPlanInitService.saveBatch(saveMonthPlanInitList);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveMouldProductionLog(MouldProductionLog productionLog) {
        if (null == productionLog) {
            return;
        }
        baseDao.insert(productionLog);
    }

    @Override
    public List<MonthPlanProductionRequirePlanVo> getFactoryMonthPlanManufacturing(Context context) {
        if (isEmptyFactoryAndProductionVersion(context)) {
            return Collections.emptyList();
        }
        QueryWrapper<ProductionMonthPlanInit> queryWrapper = new QueryWrapper();
        queryWrapper.eq("FACTORY_CODE", context.getFactoryCode());
        queryWrapper.eq("YEAR", context.getYear());
        queryWrapper.eq("MONTH", context.getMonth());
        queryWrapper.eq("MONTH_PLAN_VERSION", context.getMonthPlanVersion());
        queryWrapper.eq("PRODUCTION_VERSION", context.getProductionVersion());
        List<ProductionMonthPlanInit> dataList = factoryProductionInitMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(dataList)) {
            return Collections.emptyList();
        }
        return BeanCopyUtils.copyBeanList(dataList, MonthPlanProductionRequirePlanVo.class);
    }

    @Override
    public void saveMouldUsedLog(List<MpMouldUsedStatusLog> usedLogList) {
        if (CollectionUtils.isEmpty(usedLogList)) {
            return;
        }
        factoryMouldUsedStatusLogService.saveBatch(usedLogList);
    }
}
