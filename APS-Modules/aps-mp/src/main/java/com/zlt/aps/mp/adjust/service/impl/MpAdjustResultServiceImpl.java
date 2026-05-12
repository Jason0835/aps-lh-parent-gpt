package com.zlt.aps.mp.adjust.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.mp.adjust.mapper.MpAdjustResultEntityMapper;
import com.zlt.aps.mp.adjust.service.IMpAdjustResultService;
import com.zlt.aps.mp.adjust.service.IMpWeekAdjustService;
import com.zlt.aps.mp.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.mp.api.domain.entity.MpAdjustResult;
import com.zlt.aps.mp.api.enums.WeekAdjustTypeEnum;
import com.zlt.aps.mp.common.utils.StringUtil;
import com.zlt.aps.mp.engine.adjust.MpWeekRollAdjustEngine;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpAdjustResultServiceImpl.java
 * 描    述：MpAdjustResultServiceImpl调整-调整结果记录业务层处理
 *@author zlt
 *@date 2025-12-19
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
public class MpAdjustResultServiceImpl extends AbstractDocService<MpAdjustResult>  implements IMpAdjustResultService {

    @Autowired
    protected MpAdjustResultEntityMapper mpAdjustResultEntityMapper;

    @Autowired
    private MpWeekAdjustFactory mpWeekAdjustFactory;

    @Override
    protected String getDocTypeCode() {
        return "MP0804";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MP0804");
        return sysDocType;
    }

    @Override
    public String checkUnique(MpAdjustResult docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mpAdjustResult.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public void forceUpdateById(MpAdjustResult entity) {
        // 根据版本号+物料编号+施工阶段查询，如果没有，则新增，否则更新
        String adjVersion = StrUtil.isNotBlank(entity.getVersion()) ? entity.getProductionVersion() : entity.getVersion();

        LambdaQueryWrapper<MpAdjustResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MpAdjustResult::getFactoryCode, entity.getFactoryCode());
        queryWrapper.eq(MpAdjustResult::getVersion, adjVersion);
        queryWrapper.eq(MpAdjustResult::getMaterialCode, entity.getMaterialCode());
        queryWrapper.eq(MpAdjustResult::getConstructionStage, entity.getConstructionStage());
        List<MpAdjustResult> mpAdjustResultList = mpAdjustResultEntityMapper.selectList(queryWrapper);

        //1、更新开始和结束日期
        String dayField;
        int realBeginDay = FactoryConstant.MONTH_MAX_DAY+1;
        int realEndDay = 0;
        int totalQty = 0;
        for (int i = FactoryConstant.MONTH_START_DAY; i <= FactoryConstant.MONTH_MAX_DAY; i++){
            dayField = FactoryConstant.DAY_FIELD + i;
            if (entity.getFieldValueByFieldName(dayField) != null &&
                    (Integer) entity.getFieldValueByFieldName(dayField) != 0){
                if (realBeginDay > i){
                    realBeginDay = i;
                }
                if (realEndDay < i){
                    realEndDay = i;
                }

                totalQty += (Integer) entity.getFieldValueByFieldName(dayField);
            }
        }
        entity.setBeginDay(realBeginDay==FactoryConstant.MONTH_MAX_DAY+1 ? 0:realBeginDay);
        entity.setEndDay(realEndDay);
        entity.setTotalQty(totalQty);
        // 如果版本号没有值，更新调整类型=人工调整
        if (StrUtil.isNotBlank(entity.getVersion())) {
            entity.setAdjustType(ApsConstant.APS_ZERO_3);
            entity.setVersion(adjVersion);
        }
        // 计算各排产量
        MpWeekRollAdjustEngine weekRollAdjustEngine = new MpWeekRollAdjustEngine();
        weekRollAdjustEngine.allocateProductionByPriority(entity);
        // 没有数据，需新增
        if (CollUtil.isEmpty(mpAdjustResultList)) {
            entity.setId(null);
            mpAdjustResultEntityMapper.insert(entity);
        } else {
            //2、更新每日调整值
            mpAdjustResultEntityMapper.forceUpdateById(entity);
        }
    }

    @Override
    public void deleteAdjustResultByVersion(String factoryCode, String year, String month, String version) {
        mpAdjustResultEntityMapper.deleteAdjustResultByVersion(factoryCode,year,month,version);
    }
}
