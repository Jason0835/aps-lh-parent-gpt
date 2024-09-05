package com.zlt.aps.common.engine.service.impl;

import com.zlt.aps.common.engine.domain.EmbryoVersionVo;
import com.zlt.aps.common.engine.domain.TCxEmbryoMonthPlanSurplus;
import com.zlt.aps.common.engine.mapper.TCxEmbryoMonthPlanSurplusMapper;
import com.zlt.aps.common.engine.service.TCxEmbryoMonthPlanSurplusService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Gim
 */
@Service
public class TCxEmbryoMonthPlanSurplusServiceImpl implements TCxEmbryoMonthPlanSurplusService {
    @Resource
    private TCxEmbryoMonthPlanSurplusMapper mapper;


    @Override
    public List<TCxEmbryoMonthPlanSurplus> getByParams(TCxEmbryoMonthPlanSurplus entity) {
        return mapper.getByParams(entity);
    }

    @Override
    public List<EmbryoVersionVo> getEmbryoInsertVo(String apsVersion) {
        return mapper.getInsertByApsVersion(apsVersion);
    }

    @Override
    public int add(TCxEmbryoMonthPlanSurplus entity) {
        return mapper.insert(entity);
    }

    @Override
    public void addBatch(List<TCxEmbryoMonthPlanSurplus> list) {
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        for (TCxEmbryoMonthPlanSurplus entity : list) {
            entity.setBaseVale(null);
        }
        mapper.insertBatch(list);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        return mapper.deleteByIds(ids);
    }

    @Override
    public int update(TCxEmbryoMonthPlanSurplus entity) {
        entity.setBaseVale(entity.getId());
        return mapper.updateByPrimaryKey(entity);
    }

    @Override
    public TCxEmbryoMonthPlanSurplus getById(Long id) {
        return mapper.selectByPrimaryKey(id);
    }

    @Override
    public int deleteByApsVersion(String apsVersion) {
        return mapper.deleteByApsVersion(apsVersion);
    }

    @Override
    public List<TCxEmbryoMonthPlanSurplus> getByEmbryoListAndYearAndMonth(List<String> embryoList, String year, String month) {
        if (CollectionUtil.isEmpty(embryoList)) {
            return new ArrayList<>();
        }
        return mapper.selectAllByMaterialCodeInAndYearAndMonth(embryoList, year, month);
    }

    @Override
    public List<TCxEmbryoMonthPlanSurplus> getByEmbryoListAndApsVersion(List<String> embryoList, String apsVersion) {
        if (CollectionUtil.isEmpty(embryoList)) {
            return new ArrayList<>();
        }
        return mapper.selectAllByMaterialCodeInAndMonthPlanApsVersionAndDelFlag(embryoList, apsVersion);
    }

    @Override
    public void mergeSql(List<TCxEmbryoMonthPlanSurplus> list) {
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        mapper.mergeSql(list);
    }

    /**
     * 根据年月查询所有胎胚对应的月度剩余量
     *
     * @param year  年
     * @param month 月
     * @return 结果
     */
    @Override
    public Map<String, BigDecimal> selectMonthRemainQtyByYearAndMonthGroupByMaterialCode(String year, String month) {
        List<TCxEmbryoMonthPlanSurplus> list = mapper.selectMonthRemainQtyByYearAndMonthGroupByMaterialCode(year, month);
        Map<String, BigDecimal> map = new HashMap<>();
        for (TCxEmbryoMonthPlanSurplus surplus : list) {
            map.put(surplus.getMaterialCode(), surplus.getMonthRemainQty());
        }
        return map;
    }
}
