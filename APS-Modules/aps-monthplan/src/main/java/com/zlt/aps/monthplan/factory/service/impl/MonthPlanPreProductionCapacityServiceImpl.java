package com.zlt.aps.monthplan.factory.service.impl;

import com.tlt.aps.utils.BeanCopyUtils;
import com.zlt.aps.factory.domain.vo.MonthPlanManufacturingRequirementVo;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanPreProductionCapacity;
import com.tlt.aps.utils.JsonUtils;
import com.zlt.aps.monthplan.factory.mapper.MonthPlanPreProductionCapacityMapper;
import com.zlt.aps.monthplan.factory.service.IMonthPlanPreProductionCapacityService;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanPreProductionCapacityServiceImpl.java
 * 描    述：MonthPlanPreProductionCapacityServiceImpl分厂月生产计划产能预占
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-07-09
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class MonthPlanPreProductionCapacityServiceImpl implements IMonthPlanPreProductionCapacityService {

    private final MonthPlanPreProductionCapacityMapper monthPlanPreProductionCapacityMapper;

    private final BaseDao baseDao;

    @Override
    public void savePreProductionCapacity(List<MonthPlanManufacturingRequirementVo> preAllocationCapacityList) {
        if (CollectionUtils.isEmpty(preAllocationCapacityList)) {
            return;
        }
        List<MonthPlanPreProductionCapacity> capacityList = new ArrayList<>();
        preAllocationCapacityList.stream().forEach(preAllocation -> {
            //没有需求量的不存
            if (preAllocation.getFactProdReqQty() <= BigDecimal.ZERO.intValue()) {
                return;
            }
            MonthPlanPreProductionCapacity preProductionCapacity = BeanCopyUtils.copyBean(preAllocation, MonthPlanPreProductionCapacity.class);
            preProductionCapacity.setPreProductionQty(preAllocation.getProductionQty());
            preProductionCapacity.setId(null);
            if (StringUtils.isNotBlank(preAllocation.getNoProductionReason())) {
                String reason = JsonUtils.parseJsonRemark(preAllocation.getNoProductionReason(), Locale.SIMPLIFIED_CHINESE.toString());
                preProductionCapacity.setRemark(reason);
            }
            Set<String> mouldCodeSet = preAllocation.getPreemptMouldCodeSet();
            if (!CollectionUtils.isEmpty(mouldCodeSet)) {
                preProductionCapacity.setMouldCodeInfo(mouldCodeSet.stream().collect(Collectors.joining(",")));
            }
            capacityList.add(preProductionCapacity);
        });
        if (CollectionUtils.isEmpty(capacityList)) {
            return;
        }
        monthPlanPreProductionCapacityMapper.deleteOldData(capacityList.get(0));
        baseDao.insertBatch(capacityList);
    }
}
