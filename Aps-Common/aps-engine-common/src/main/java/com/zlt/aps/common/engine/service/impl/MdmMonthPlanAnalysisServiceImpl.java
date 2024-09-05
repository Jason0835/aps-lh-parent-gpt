package com.zlt.aps.common.engine.service.impl;

import com.zlt.aps.common.engine.domain.MdmMonthPlanAnalysis;
import com.zlt.aps.common.engine.mapper.MdmMonthPlanAnalysisMapper;
import com.zlt.aps.common.engine.service.MdmMonthPlanAnalysisService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Gim
 */
@Service
public class MdmMonthPlanAnalysisServiceImpl implements MdmMonthPlanAnalysisService {

    @Resource
    private MdmMonthPlanAnalysisMapper planAnalysisMapper;


    @Override
    public List<MdmMonthPlanAnalysis> getByParams(MdmMonthPlanAnalysis entity) {
        return planAnalysisMapper.getByParams(entity);
    }

    @Override
    public int add(MdmMonthPlanAnalysis entity) {
        return planAnalysisMapper.insert(entity);
    }

    @Override
    public void addBatch(List<MdmMonthPlanAnalysis> list) {
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        for (MdmMonthPlanAnalysis analysis : list) {
            analysis.setBaseVale(null);
        }
        planAnalysisMapper.insertBatch(list);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        return planAnalysisMapper.deleteByIds(ids);
    }

    @Override
    public int update(MdmMonthPlanAnalysis entity) {
        if (entity == null || entity.getId() == null) {
            return 0;
        }
        entity.setBaseVale(entity.getId());
        return planAnalysisMapper.updateByPrimaryKey(entity);
    }

    @Override
    public MdmMonthPlanAnalysis getById(Long id) {
        return planAnalysisMapper.selectByPrimaryKey(id);
    }

    @Override
    public void deleteByApsVersion(String apsVersion) {
        planAnalysisMapper.deleteByMonthPlanApsVersion(apsVersion);
    }
}
