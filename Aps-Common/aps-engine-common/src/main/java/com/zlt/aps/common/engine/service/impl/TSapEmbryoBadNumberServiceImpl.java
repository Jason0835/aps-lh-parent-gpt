package com.zlt.aps.common.engine.service.impl;

import com.zlt.aps.common.engine.domain.EmbryoVersionVo;
import com.zlt.aps.common.engine.domain.TSapEmbryoBadNumber;
import com.zlt.aps.common.engine.mapper.TSapEmbryoBadNumberMapper;
import com.zlt.aps.common.engine.service.TSapEmbryoBadNumberService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Gim
 */
@Service
public class TSapEmbryoBadNumberServiceImpl implements TSapEmbryoBadNumberService {
    
    @Resource
    private TSapEmbryoBadNumberMapper mapper;


    @Override
    public List<TSapEmbryoBadNumber> getByParams(TSapEmbryoBadNumber entity) {
        return mapper.getByParams(entity);
    }

    @Override
    public void mergeSql(List<TSapEmbryoBadNumber> list) {
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        mapper.mergeSql(list);
    }

    @Override
    public List<TSapEmbryoBadNumber> getSapByParams(String badDate, List<String> sapCodeList) {
        return mapper.getSapByParams(badDate, sapCodeList);
    }

    @Override
    public List<TSapEmbryoBadNumber> getEmbryoByParams(String badDate, List<String> embryoCodeList) {
        return mapper.getEmbryoByParams(badDate, embryoCodeList);
    }

    @Override
    public List<TSapEmbryoBadNumber> getByEmbryoVersionList(String badDate, List<EmbryoVersionVo> list) {
        return mapper.getByEmbryoVersionList(badDate, list);
    }

}
