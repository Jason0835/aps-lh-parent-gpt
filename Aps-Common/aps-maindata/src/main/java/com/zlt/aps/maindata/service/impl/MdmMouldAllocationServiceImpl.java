package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.maindata.mapper.MdmMouldAllocationEntityMapper;
import com.zlt.aps.maindata.service.IMdmMouldAllocationService;
import com.zlt.aps.mp.api.domain.entity.MdmMouldAllocation;
import com.zlt.aps.mp.api.domain.vo.PeriodInfo;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmMouldAllocationServiceImpl.java
 * 描    述：MdmMouldAllocationServiceImpl模具分配比例(同结构/不同结构)业务层处理
 *@author zlt
 *@date 2025-12-14
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MdmMouldAllocationServiceImpl extends AbstractDocService<MdmMouldAllocation>  implements IMdmMouldAllocationService {

    @Autowired
    private ISysDictDataCacheService sysDictDataCacheService;

    @Autowired
    private MdmMouldAllocationEntityMapper mdmMouldAllocationEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "MDM0118";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0118");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmMouldAllocation docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            List<SysDictData> dictDataList = sysDictDataCacheService.getType("biz_factory_name");
            List<SysDictData> dictData = dictDataList.stream().filter(item -> item.getDictValue().equals(docEntityVO.getFactoryCode())).collect(Collectors.toList());
            String dictLabel = docEntityVO.getFactoryCode();
            if (CollectionUtils.isNotEmpty(dictData)) {
                dictLabel = dictData.get(0).getDictLabel();
            }
            String message = StringUtils.format(I18nUtil.getMessage("ui.data.alert.mdmMouldAllocation.notUnique"),
                    docEntityVO.getSpecifications(), docEntityVO.getMainPattern(),
                    docEntityVO.getStructureName(), dictLabel, docEntityVO.getYear(), docEntityVO.getMonth());
            throw new ServiceException(message);
        }
        return unique;
    }


    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("factoryCode", "year", "month", "structureName", "specifications", "mainPattern"));
    }


    /**
     * 复制模具分配比例
     */
    @Override
    public AjaxResult copy(PeriodInfo vo) {
        check(vo);
        mergeByPeriod(vo);
        return AjaxResult.success();
    }

    private void check(PeriodInfo vo) {
        Integer fromyear = vo.getFromyear();
        Integer frommonth = vo.getFrommonth();
        Integer toyear = vo.getToyear();
        Integer tomonth = vo.getTomonth();
        if (fromyear.equals(toyear) && frommonth.equals(tomonth)) {
            String message = StringUtils.format(I18nUtil.getMessage("ui.data.alert.mdmMouldAllocation.sameYearMonth"));
            throw new BusinessException(message);
        }
    }


    /**
     * 复制指定年月、分厂数据，有则更新，无则插入
     */
    private void mergeByPeriod(PeriodInfo vo) {
        LambdaQueryWrapper<MdmMouldAllocation> fromWrapper = Wrappers.lambdaQuery();
        fromWrapper.eq(MdmMouldAllocation::getYear, vo.getFromyear());
        fromWrapper.eq(MdmMouldAllocation::getMonth, vo.getFrommonth());
        fromWrapper.eq(StringUtils.isNotBlank(vo.getFactoryCode()), MdmMouldAllocation::getFactoryCode, vo.getFactoryCode());
        List<MdmMouldAllocation> fromList = mdmMouldAllocationEntityMapper.selectList(fromWrapper);

        LambdaQueryWrapper<MdmMouldAllocation> copyWrapper = Wrappers.lambdaQuery();
        copyWrapper.eq(MdmMouldAllocation::getYear, vo.getToyear());
        copyWrapper.eq(MdmMouldAllocation::getMonth, vo.getTomonth());
        copyWrapper.eq(StringUtils.isNotBlank(vo.getFactoryCode()), MdmMouldAllocation::getFactoryCode, vo.getFactoryCode());
        List<MdmMouldAllocation> copyList = mdmMouldAllocationEntityMapper.selectList(copyWrapper);
        // 按照主花纹、结构名称、规格进行分组
        Map<String, Long> copyMap = convertToMap(copyList);

        List<MdmMouldAllocation> updateList = new ArrayList<>();
        List<MdmMouldAllocation> insertList = new ArrayList<>();
        for (MdmMouldAllocation mdmMouldAllocation : fromList) {
            mdmMouldAllocation.setYear(vo.getToyear());
            mdmMouldAllocation.setMonth(vo.getTomonth());
            mdmMouldAllocation.setId(null);
            mdmMouldAllocation.setBaseVale(null);
            String key = Objects.toString(mdmMouldAllocation.getMainPattern(), "")
                    + Objects.toString(mdmMouldAllocation.getStructureName(), "")
                    + Objects.toString(mdmMouldAllocation.getSpecifications(), "");
            if (copyMap.containsKey(key)) {
                Long copyId = copyMap.get(key);
                mdmMouldAllocation.setId(copyId);
                mdmMouldAllocation.setCreateBy(null);
                mdmMouldAllocation.setCreateTime(null);
                updateList.add(mdmMouldAllocation);
            } else {
                insertList.add(mdmMouldAllocation);
            }
        }

        baseDao.insertBatch(insertList);
        baseDao.updateBatch(updateList);

    }

    private Map<String, Long> convertToMap(List<MdmMouldAllocation> list) {
        if (PubUtil.isEmpty(list)) {
            return Collections.emptyMap();
        }
        return list.stream()
                .collect(Collectors.toMap(
                        item -> Objects.toString(item.getMainPattern(), "")
                                + Objects.toString(item.getStructureName(), "")
                                + Objects.toString(item.getSpecifications(), ""),
                        MdmMouldAllocation::getId,
                        (existingId, newId) -> existingId
                ));
    }


}
