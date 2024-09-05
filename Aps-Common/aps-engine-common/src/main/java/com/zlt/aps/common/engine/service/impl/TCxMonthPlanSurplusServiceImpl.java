package com.zlt.aps.common.engine.service.impl;

import com.zlt.aps.common.engine.domain.TCxMonthPlanSurplus;
import com.zlt.aps.common.engine.mapper.TCxMonthPlanSurplusMapper;
import com.zlt.aps.common.engine.service.TCxMonthPlanSurplusService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gim
 */
@Service
public class TCxMonthPlanSurplusServiceImpl implements TCxMonthPlanSurplusService {
    
    @Resource
    private TCxMonthPlanSurplusMapper mapper;


    @Override
    public List<TCxMonthPlanSurplus> getByParams(TCxMonthPlanSurplus entity) {
        return mapper.getByParams(entity);
    }

    @Override
    public int add(TCxMonthPlanSurplus entity) {
        return mapper.insert(entity);
    }

    @Override
    public void addBatch(List<TCxMonthPlanSurplus> list) {
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        for (TCxMonthPlanSurplus entity : list) {
            entity.setBaseVale(null);
        }
        mapper.insertBatch(list);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        return mapper.deleteByIds(ids);
    }

    @Override
    public int update(TCxMonthPlanSurplus entity) {
        entity.setBaseVale(entity.getId());
        return mapper.updateByPrimaryKey(entity);
    }

    @Override
    public TCxMonthPlanSurplus getById(Long id) {
        return mapper.selectByPrimaryKey(id);
    }

    @Override
    public int deleteByApsVersion(String apsVersion) {
        return mapper.deleteByApsVersion(apsVersion);
    }

    @Override
    public List<TCxMonthPlanSurplus> getBySapCodeAndYearAndMonth(List<String> sapCodeList, String year, String month) {
        if (CollectionUtil.isEmpty(sapCodeList)) {
            return new ArrayList<>();
        }
        return mapper.selectAllBySapCodeInAndYearAndMonth(sapCodeList, year, month);
    }

    @Override
    public List<TCxMonthPlanSurplus> getBySapCodeAndApsVersion(List<String> sapCodeList, String apsVersion) {
        if (CollectionUtil.isEmpty(sapCodeList)) {
            return new ArrayList<>();
        }
        return mapper.selectAllBySapCodeInAndMonthPlanApsVersionAndDelFlag(sapCodeList, apsVersion);
    }

    @Override
    public void mergeSql(List<TCxMonthPlanSurplus> list) {
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        mapper.mergeSql(list);
    }

}
