package com.zlt.aps.common.engine.mapper;
import com.zlt.aps.common.engine.domain.EmbryoVersionVo;
import org.apache.ibatis.annotations.Param;

import com.zlt.aps.common.engine.domain.TProductConstructionInfo;

import java.util.List;

/**
 * @Entity com.zlt.aps.common.engine.domain.TProductConstructionInfo
 */
public interface TProductConstructionInfoMapper {

    int deleteByPrimaryKey(Long id);

    int insert(TProductConstructionInfo record);

    int insertSelective(TProductConstructionInfo record);

    TProductConstructionInfo selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TProductConstructionInfo record);

    int updateByPrimaryKey(TProductConstructionInfo record);

    List<TProductConstructionInfo> selectAllByEmbryoCodeAndEmbryoVersion(@Param("embryoCode") String embryoCode, @Param("embryoVersion") String embryoVersion);

    List<TProductConstructionInfo> selectInfoByEmbryoCodeAndVersion(@Param("list") List<EmbryoVersionVo> list);
}




