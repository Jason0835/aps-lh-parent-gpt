package com.zlt.aps.mp.demand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.maindata.mapper.MdmOutbountOrdersNotScanEntityMapper;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.mp.api.domain.entity.DpDemandPlanSum;
import com.zlt.aps.mp.api.domain.entity.DpShippedNotScanVersion;
import com.zlt.aps.mp.api.domain.entity.MdmOutbountOrdersNotScan;
import com.zlt.aps.mp.common.utils.BatchInsertProcessor;
import com.zlt.aps.mp.demand.mapper.DpDemandPlanSumEntityMapper;
import com.zlt.aps.mp.demand.mapper.DpShippedNotScanVersionEntityMapper;
import com.zlt.aps.mp.demand.service.IDpShippedNotScanVersionService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class DpShippedNotScanVersionServiceImpl extends AbstractDocService<DpShippedNotScanVersion> implements IDpShippedNotScanVersionService {

    private final BatchInsertProcessor<DpShippedNotScanVersion> batchInsertProcessor;
    private final DpShippedNotScanVersionEntityMapper dpShippedNotScanVersionEntityMapper;

    private final MdmOutbountOrdersNotScanEntityMapper mdmOutbountOrdersNotScanEntityMapper;

    private final DpDemandPlanSumEntityMapper dpDemandPlanSumEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "2026033101";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("2026033101");
        return sysDocType;
    }

    @Override
    public String checkUnique(DpShippedNotScanVersion docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.dpShippedNotScanVersion.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Collections.emptyList();
    }

    @Override
    public void generateShippedNotScanVersion(DpShippedNotScanVersion queryCondition) {
        String factoryCode = queryCondition.getFactoryCode();
        Integer year = queryCondition.getYear();
        Integer month = queryCondition.getMonth();
        String requireVersion = queryCondition.getRequireVersion();

        LambdaQueryWrapper<DpDemandPlanSum> demandWrapper = Wrappers.lambdaQuery();
        demandWrapper.eq(DpDemandPlanSum::getFactoryCode, factoryCode);
        demandWrapper.eq(DpDemandPlanSum::getYear, year);
        demandWrapper.eq(DpDemandPlanSum::getMonth, month);
        demandWrapper.eq(DpDemandPlanSum::getMonthPlanVersion, requireVersion);
        demandWrapper.eq(DpDemandPlanSum::getIsDelete, YesOrNoEnum.NO.getValue());
        List<DpDemandPlanSum> demandPlanSumList = dpDemandPlanSumEntityMapper.selectList(demandWrapper);

        if (CollectionUtils.isEmpty(demandPlanSumList)) {
            return;
        }

        Set<String> materialCodeSet = demandPlanSumList.stream()
                .map(DpDemandPlanSum::getMaterialCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (CollectionUtils.isEmpty(materialCodeSet)) {
            return;
        }

        LambdaQueryWrapper<MdmOutbountOrdersNotScan> notScanWrapper = Wrappers.lambdaQuery();
        notScanWrapper.eq(MdmOutbountOrdersNotScan::getFactoryCode, factoryCode);
        notScanWrapper.in(MdmOutbountOrdersNotScan::getSapCode, materialCodeSet);
        List<MdmOutbountOrdersNotScan> notScanList = mdmOutbountOrdersNotScanEntityMapper.selectList(notScanWrapper);

        if (CollectionUtils.isEmpty(notScanList)) {
            return;
        }

        Map<String, Object> deleteMap = new HashMap<>();
        deleteMap.put("FACTORY_CODE", factoryCode);
        deleteMap.put("YEAR", year);
        deleteMap.put("MONTH", month);
        deleteMap.put("REQUIRE_VERSION", requireVersion);
        deleteMap.put("IS_DELETE", YesOrNoEnum.NO.getValue());
        baseDao.deleteByMap(DpShippedNotScanVersion.class, deleteMap);

        List<DpShippedNotScanVersion> insertList = Lists.newArrayList();
        for (MdmOutbountOrdersNotScan notScan : notScanList) {
            DpShippedNotScanVersion version = new DpShippedNotScanVersion();
            BeanUtils.copyProperties(notScan, version);
            version.setId(null);
            version.setYear(year);
            version.setMonth(month);
            version.setRequireVersion(requireVersion);
            version.setIsDelete(YesOrNoEnum.NO.getValue());
            insertList.add(version);
        }

        if (!CollectionUtils.isEmpty(insertList)) {
            List<List<DpShippedNotScanVersion>> splitList = ScmListUtils.getSplitList(insertList, 1000);
            for (List<DpShippedNotScanVersion> batchList : splitList) {
                batchInsertProcessor.batchInsert(batchList);
            }
        }
    }

    @Override
    public List<String> findMonthPlanVersion(DpShippedNotScanVersion queryCondition) {
        return dpShippedNotScanVersionEntityMapper.selectDistinctMonthPlanVersion(
                queryCondition.getFactoryCode(),
                queryCondition.getYear(),
                queryCondition.getMonth(),
                YesOrNoEnum.NO.getValue()
        );
    }
}
