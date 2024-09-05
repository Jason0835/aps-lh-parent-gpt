package com.zlt.aps.common.engine.service.impl;

import com.zlt.aps.common.engine.domain.TTmMonthPlanSurplus;
import com.zlt.aps.common.engine.mapper.TTmMonthPlanSurplusMapper;
import com.zlt.aps.common.engine.service.TTmMonthPlanSurplusService;
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
public class TTmMonthPlanSurplusServiceImpl implements TTmMonthPlanSurplusService {
    @Resource
    private TTmMonthPlanSurplusMapper mapper;


    @Override
    public List<TTmMonthPlanSurplus> getByParams(TTmMonthPlanSurplus entity) {
        return mapper.getByParams(entity);
    }

    @Override
    public List<TTmMonthPlanSurplus> getByApsVersion(String apsVersion) {
        return mapper.selectAllByMonthPlanApsVersionAndDelFlag(apsVersion, "0");
    }

    @Override
    public List<TTmMonthPlanSurplus> getByCodeList(String apsVersion, List<String> codeList) {
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
    public int add(TTmMonthPlanSurplus entity) {
        return mapper.insert(entity);
    }

    @Override
    public void addBatch(List<TTmMonthPlanSurplus> list) {
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        for (TTmMonthPlanSurplus entity : list) {
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
    public int update(TTmMonthPlanSurplus entity) {
        entity.setBaseVale(entity.getId());
        return mapper.updateByPrimaryKey(entity);
    }

    @Override
    public TTmMonthPlanSurplus getById(Long id) {
        return mapper.selectByPrimaryKey(id);
    }
}
