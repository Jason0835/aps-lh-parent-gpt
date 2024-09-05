package com.zlt.aps.common.engine.mapper;
import com.zlt.aps.common.engine.domain.XwyyParamsVo;
import org.apache.ibatis.annotations.Param;

import com.zlt.aps.common.engine.domain.CxTCd15Params;

import java.util.List;

/**
 * @Entity com.zlt.aps.common.engine.domain.CxTCd15Params
 */
public interface CxTCd15ParamsMapper {

    int deleteByPrimaryKey(Long id);

    int insert(CxTCd15Params record);

    int insertSelective(CxTCd15Params record);

    CxTCd15Params selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(CxTCd15Params record);

    int updateByPrimaryKey(CxTCd15Params record);

    CxTCd15Params selectOneByParamCodeAndDelFlag(@Param("paramCode") String paramCode, @Param("delFlag") String delFlag);

    List<XwyyParamsVo> listGdyyParams();
}




