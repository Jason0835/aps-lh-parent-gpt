package com.zlt.aps.common.engine.service.impl;

import com.zlt.aps.common.engine.domain.TNcMonthPlanSurplus;
import com.zlt.aps.common.engine.mapper.TNcMonthPlanSurplusMapper;
import com.zlt.aps.common.engine.service.TNcMonthPlanSurplusService;
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
public class TNcMonthPlanSurplusServiceImpl implements TNcMonthPlanSurplusService {
    @Resource
    private TNcMonthPlanSurplusMapper mapper;


    @Override
    public List<TNcMonthPlanSurplus> getByParams(TNcMonthPlanSurplus entity) {
        return mapper.getByParams(entity);
    }

    @Override
    public List<TNcMonthPlanSurplus> getByApsVersion(String apsVersion) {
        return mapper.selectAllByMonthPlanApsVersionAndDelFlag(apsVersion, "0");
    }

    @Override
    public List<TNcMonthPlanSurplus> getByCodeList(String apsVersion, List<String> codeList) {
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
    public int add(TNcMonthPlanSurplus entity) {
        return mapper.insert(entity);
    }

    @Override
    public void addBatch(List<TNcMonthPlanSurplus> list) {
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        for (TNcMonthPlanSurplus entity : list) {
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
    public int update(TNcMonthPlanSurplus entity) {
        entity.setBaseVale(entity.getId());
        return mapper.updateByPrimaryKey(entity);
    }

    @Override
    public TNcMonthPlanSurplus getById(Long id) {
        return mapper.selectByPrimaryKey(id);
    }
}
