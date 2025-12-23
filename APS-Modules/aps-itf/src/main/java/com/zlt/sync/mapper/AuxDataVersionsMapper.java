package com.zlt.sync.mapper;

import com.zlt.sync.domain.AuxDataVersions;
import com.zlt.sync.domain.vo.AuxDataVersionsVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 数据版本号接口
 */
public interface AuxDataVersionsMapper {

    int insert(@Param("auxDataVersions") AuxDataVersions auxDataVersions);

    /**
     * 查询数据版本
     * fromSys+toSys 维度来查询
     * @param params
     * @return
     */
    List<AuxDataVersionsVO> queryDataVersion(Map<String, Object> params);

    /**
     * 更新数据
     * @param auxDataVersions
     * @return
     */
    int update(@Param("auxDataVersions") AuxDataVersions auxDataVersions);

}
