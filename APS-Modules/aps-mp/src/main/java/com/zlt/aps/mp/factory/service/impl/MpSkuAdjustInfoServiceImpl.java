package com.zlt.aps.mp.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Maps;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.adjust.mapper.MpAdjustStructureInEntityMapper;
import com.zlt.aps.mp.adjust.mapper.MpAdjustStructureOutEntityMapper;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.entity.MpAdjustStructureIn;
import com.zlt.aps.mp.api.domain.entity.MpAdjustStructureOut;
import com.zlt.aps.mp.factory.dto.MpSkuAdjustInfoVo;
import com.zlt.aps.mp.factory.service.MpSkuAdjustInfoService;
import com.zlt.aps.utils.BeanCopyUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 月计划调整：计划待调整量业务实现
 *
 * @author ZLT
 * @date 20260606
 */
@Service
@RequiredArgsConstructor
public class MpSkuAdjustInfoServiceImpl implements MpSkuAdjustInfoService {

    private final MpAdjustStructureInEntityMapper mpAdjustStructureInEntityMapper;

    private final MpAdjustStructureOutEntityMapper mpAdjustStructureOutEntityMapper;

    @Override
    public Map<String, MpSkuAdjustInfoVo> getPendingQtyInfo(FactoryMonthPlanProductionFinalResult condition, String matchVersion) {
        if (null == condition || StringUtils.isBlank(condition.getProductionVersion())) {
            return Collections.emptyMap();
        }
        Map<String, MpSkuAdjustInfoVo> allAdjustInfoMap = Maps.newHashMap();
        addAdjustStructureIn(allAdjustInfoMap, condition, matchVersion);
        addAdjustStructureOut(allAdjustInfoMap, condition, matchVersion);
        if (CollectionUtils.isEmpty(allAdjustInfoMap)) {
            return Collections.emptyMap();
        }
        return allAdjustInfoMap;
    }


    /**
     * 增加：对应版本的结构内调整的待调整信息
     *
     * @param allAdjustInfoMap 所有待调整信息
     * @param condition        条件
     * @param matchVersion     匹配版本
     */
    private void addAdjustStructureIn(Map<String, MpSkuAdjustInfoVo> allAdjustInfoMap, FactoryMonthPlanProductionFinalResult condition, String matchVersion) {
        if (null == allAdjustInfoMap) {
            return;
        }
        LambdaQueryWrapper<MpAdjustStructureIn> structureInQuery = new LambdaQueryWrapper<>();
        structureInQuery.eq(MpAdjustStructureIn::getFactoryCode, condition.getFactoryCode());
        structureInQuery.eq(MpAdjustStructureIn::getYear, condition.getYear());
        structureInQuery.eq(MpAdjustStructureIn::getMonth, condition.getMonth());
        // 前端传入 version 时表示调整版本号，对应调整结果表 VERSION 字段。
        structureInQuery.eq(StringUtils.isNotBlank(matchVersion), MpAdjustStructureIn::getLastMonthPlanVersion, matchVersion);
        structureInQuery.eq(MpAdjustStructureIn::getIsDelete, YesOrNoEnum.NO.getCode());
        List<MpAdjustStructureIn> structureInList = mpAdjustStructureInEntityMapper.selectList(structureInQuery);
        if (CollectionUtils.isEmpty(structureInList)) {
            return;
        }
        structureInList.forEach(singleSku -> {
            String groupKey = singleSku.getPendingQtyKey();
            if (StringUtils.isBlank(groupKey)) {
                return;
            }
            if (allAdjustInfoMap.containsKey(groupKey)) {
                return;
            }
            MpSkuAdjustInfoVo info = BeanCopyUtils.copyBean(singleSku, MpSkuAdjustInfoVo.class);
            allAdjustInfoMap.put(groupKey, info);
        });
    }

    /**
     * 增加：对应版本的结构外调整的待调整信息
     *
     * @param allAdjustInfoMap 所有待调整信息
     * @param condition        条件
     * @param matchVersion     匹配版本
     */
    private void addAdjustStructureOut(Map<String, MpSkuAdjustInfoVo> allAdjustInfoMap, FactoryMonthPlanProductionFinalResult condition, String matchVersion) {
        if (null == allAdjustInfoMap) {
            return;
        }
        LambdaQueryWrapper<MpAdjustStructureOut> structureOutQuery = new LambdaQueryWrapper<>();
        structureOutQuery.eq(MpAdjustStructureOut::getFactoryCode, condition.getFactoryCode());
        structureOutQuery.eq(MpAdjustStructureOut::getYear, condition.getYear());
        structureOutQuery.eq(MpAdjustStructureOut::getMonth, condition.getMonth());
        // 前端传入 version 时表示调整版本号，对应调整结果表 VERSION 字段。
        structureOutQuery.eq(StringUtils.isNotBlank(matchVersion), MpAdjustStructureOut::getLastMonthPlanVersion, matchVersion);
        structureOutQuery.eq(MpAdjustStructureOut::getIsDelete, YesOrNoEnum.NO.getCode());
        List<MpAdjustStructureOut> structureOutList = mpAdjustStructureOutEntityMapper.selectList(structureOutQuery);
        if (CollectionUtils.isEmpty(structureOutList)) {
            return;
        }
        structureOutList.forEach(singleSku -> {
            String groupKey = singleSku.getPendingQtyKey();
            if (StringUtils.isBlank(groupKey)) {
                return;
            }
            if (allAdjustInfoMap.containsKey(groupKey)) {
                return;
            }
            MpSkuAdjustInfoVo info = BeanCopyUtils.copyBean(singleSku, MpSkuAdjustInfoVo.class);
            allAdjustInfoMap.put(groupKey, info);
        });
    }
}
