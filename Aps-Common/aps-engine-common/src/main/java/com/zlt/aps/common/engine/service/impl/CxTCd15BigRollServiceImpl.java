package com.zlt.aps.common.engine.service.impl;

import com.zlt.aps.common.engine.domain.CxTCd15BigRoll;
import com.zlt.aps.common.engine.mapper.CxTCd15BigRollMapper;
import com.zlt.aps.common.engine.service.CxTCd15BigRollService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gim
 */
@Service
public class CxTCd15BigRollServiceImpl implements CxTCd15BigRollService {
    @Resource
    private CxTCd15BigRollMapper mapper;

    @Override
    public List<CxTCd15BigRoll> getByParams(CxTCd15BigRoll entity) {
        return mapper.getByParams(entity);
    }

    @Override
    public int add(CxTCd15BigRoll entity) {
        return mapper.insert(entity);
    }

//    @Override
//    public void addBatch(List<CxTCd15BigRoll> list) {
//        mapper.insertBatch(list);
//    }

    @Override
    public int deleteByIds(Long[] ids) {
        return mapper.deleteByIds(ids);
    }

    @Override
    public int update(CxTCd15BigRoll timeLimit) {
        return mapper.updateByPrimaryKey(timeLimit);
    }

    @Override
    public CxTCd15BigRoll getById(Long id) {
        return mapper.selectByPrimaryKey(id);
    }

    @Override
    public List<CxTCd15BigRoll> getByBeltSpecList(List<String> list) {
        if (CollectionUtil.isEmpty(list)) {
            return new ArrayList<>();
        }
        return mapper.getByBeltSpecList(list);
    }
}
