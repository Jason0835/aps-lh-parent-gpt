package com.zlt.aps.common.engine.service;


import com.zlt.aps.common.engine.domain.EmbryoVersionVo;
import com.zlt.aps.common.engine.domain.TSapEmbryoBadNumber;

import java.util.List;

/**
 * @author Gim
 */
public interface TSapEmbryoBadNumberService {

    List<TSapEmbryoBadNumber> getByParams(TSapEmbryoBadNumber entity);

    void mergeSql(List<TSapEmbryoBadNumber> list);

    List<TSapEmbryoBadNumber> getSapByParams(String badDate, List<String> sapCodeList);
    List<TSapEmbryoBadNumber> getEmbryoByParams(String badDate, List<String> embryoCodeList);
    List<TSapEmbryoBadNumber> getByEmbryoVersionList(String badDate, List<EmbryoVersionVo> list);

}
