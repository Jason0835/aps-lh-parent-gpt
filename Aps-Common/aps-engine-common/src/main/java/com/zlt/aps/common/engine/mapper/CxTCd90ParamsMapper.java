package com.zlt.aps.common.engine.mapper;
import com.zlt.aps.common.engine.domain.XwyyParamsVo;
import org.apache.ibatis.annotations.Param;

import com.zlt.aps.common.engine.domain.CxTCd90Params;

import java.util.List;

/**
 * @Entity com.zlt.aps.common.engine.domain.CxTCd90Params
 */
public interface CxTCd90ParamsMapper {

    int deleteByPrimaryKey(Long id);

    int insert(CxTCd90Params record);

    int insertSelective(CxTCd90Params record);

    CxTCd90Params selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(CxTCd90Params record);

    int updateByPrimaryKey(CxTCd90Params record);

    CxTCd90Params selectOneByParamCodeAndDelFlag(@Param("paramCode") String paramCode, @Param("delFlag") String delFlag);

    List<XwyyParamsVo> listXwyyParams();
}




