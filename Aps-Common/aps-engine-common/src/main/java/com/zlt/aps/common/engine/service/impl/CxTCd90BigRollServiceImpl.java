package com.zlt.aps.common.engine.service.impl;

import com.zlt.aps.common.engine.domain.CxTCd90BigRoll;
import com.zlt.aps.common.engine.mapper.CxTCd90BigRollMapper;
import com.zlt.aps.common.engine.service.CxTCd90BigRollService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gim
 */
@Service
public class CxTCd90BigRollServiceImpl implements CxTCd90BigRollService {
    @Resource
    private CxTCd90BigRollMapper mapper;

    @Override
    public List<CxTCd90BigRoll> getByParams(CxTCd90BigRoll entity) {
        return mapper.getByParams(entity);
    }

    @Override
    public int add(CxTCd90BigRoll entity) {
        return mapper.insert(entity);
    }

//    @Override
//    public void addBatch(List<CxTCd90BigRoll> list) {
//        mapper.insertBatch(list);
//    }

    @Override
    public int deleteByIds(Long[] ids) {
        return mapper.deleteByIds(ids);
    }

    @Override
    public int update(CxTCd90BigRoll timeLimit) {
        return mapper.updateByPrimaryKey(timeLimit);
    }

    @Override
    public CxTCd90BigRoll getById(Long id) {
        return mapper.selectByPrimaryKey(id);
    }

    @Override
    public List<CxTCd90BigRoll> getByCrodSpecList(List<String> list) {
        if (CollectionUtil.isEmpty(list)) {
            return new ArrayList<>();
        }
        return mapper.getByCrodSpecList(list);
    }
}
