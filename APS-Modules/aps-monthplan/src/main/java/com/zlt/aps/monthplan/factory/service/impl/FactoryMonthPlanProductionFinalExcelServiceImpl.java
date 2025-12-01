package com.zlt.aps.monthplan.factory.service.impl;

import com.zlt.aps.monthplan.api.domain.entity.MonthPlanProductionFinalResult;
import com.zlt.aps.monthplan.factory.helper.ProductionPlanExcelImportHelper;
import com.zlt.aps.monthplan.factory.mapper.FactoryMonthPlanProductionFinalMapper;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProductionFinalExcelService;
import com.zlt.aps.monthplan.factory.service.IMonthPlanSurplusService;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 排产管理导入 数据保存业务接口，因要捕获异常，由需要事务，故而抽取数据存储动作到另外业务接口
 *
 * @author ZLT
 * @date 20251121
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FactoryMonthPlanProductionFinalExcelServiceImpl implements IFactoryMonthPlanProductionFinalExcelService {

    private final BaseDao baseDao;

    private final IMonthPlanSurplusService monthPlanSurplusService;

    private final FactoryMonthPlanProductionFinalMapper factoryMonthPlanProductionFinalMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveImportTrialProductionPlan(List<MonthPlanProductionFinalResult> importList) {
        baseDao.insertBatch(importList);
        //重新汇总对应月度外胎汇总
        monthPlanSurplusService.finalUpdatePlanSurplusList(importList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer saveImportAdjustPlan(ProductionPlanExcelImportHelper excelHelper,
                                        List<MonthPlanProductionFinalResult> insertList,
                                        Integer successNum) {
        // 无排产单号的新增
        if (CollectionUtils.isNotEmpty(insertList)) {
            baseDao.insertBatch(insertList);
            successNum += insertList.size();
        }
        List<MonthPlanProductionFinalResult> updateList = excelHelper.getUpdateList();
        if (CollectionUtils.isNotEmpty(updateList)) {
            successNum += updateList.size();
            // 根据排产单号更新
            factoryMonthPlanProductionFinalMapper.updateByProductionNo(updateList);
        }
        //重新汇总对应月度外胎汇总
        List<MonthPlanProductionFinalResult> importList = new ArrayList<>(insertList);
        importList.addAll(updateList);
        if (CollectionUtils.isNotEmpty(importList)) {
            monthPlanSurplusService.finalUpdatePlanSurplusList(importList);
        }
        return successNum;
    }
}
