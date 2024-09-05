package com.zlt.aps.common.engine.service.impl;

import com.zlt.aps.common.engine.domain.TLbcdMonthPlanSurplus;
import com.zlt.aps.common.engine.mapper.TLbcdMonthPlanSurplusMapper;
import com.zlt.aps.common.engine.service.TLbcdMonthPlanSurplusService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Gim
 */
@Service
public class TLbcdMonthPlanSurplusServiceImpl implements TLbcdMonthPlanSurplusService {
    @Resource
    private TLbcdMonthPlanSurplusMapper mapper;


    @Override
    public List<TLbcdMonthPlanSurplus> getByParams(TLbcdMonthPlanSurplus entity) {
        return mapper.getByParams(entity);
    }

    @Override
    public List<TLbcdMonthPlanSurplus> getByApsVersion(String apsVersion) {
        return mapper.selectAllByMonthPlanApsVersionAndDelFlag(apsVersion, "0");
    }

    @Override
    public List<TLbcdMonthPlanSurplus> getByCodeList(String apsVersion, List<String> codeList) {
        if (CollectionUtil.isEmpty(codeList)) {
            return new ArrayList<>();
        }
        return mapper.selectAllByMonthPlanApsVersionAndMaterialCodeInAndDelFlag(apsVersion, codeList, "0");
    }

    @Override
    public void deleteByApsVersionAndCodeList(String apsVersion, List<String> codeList) {
        if (CollectionUtil.isEmpty(codeList)) {
            return;
        }
        mapper.deleteByMonthPlanApsVersionAndMaterialCodeIn(apsVersion, codeList);
    }

    @Override
    public int add(TLbcdMonthPlanSurplus entity) {
        return mapper.insert(entity);
    }

    @Override
    public void addBatch(List<TLbcdMonthPlanSurplus> list) {
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        for (TLbcdMonthPlanSurplus entity : list) {
            try {
                entity.setBaseVale(null);
            } catch (Exception e) {
                entity.setDelFlag("0");
                entity.setCreateBy("system");
                entity.setUpdateBy("system");
                entity.setCreateTime(new Date());
                entity.setUpdateTime(new Date());
            }
        }
        mapper.insertBatch(list);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        return mapper.deleteByIds(ids);
    }

    @Override
    public int deleteByApsVersion(String apsVersion) {
        return mapper.deleteByApsVersion(apsVersion);
    }

    @Override
    public int update(TLbcdMonthPlanSurplus entity) {
        entity.setBaseVale(entity.getId());
        return mapper.updateByPrimaryKey(entity);
    }

    @Override
    public TLbcdMonthPlanSurplus getById(Long id) {
        return mapper.selectByPrimaryKey(id);
    }
}
