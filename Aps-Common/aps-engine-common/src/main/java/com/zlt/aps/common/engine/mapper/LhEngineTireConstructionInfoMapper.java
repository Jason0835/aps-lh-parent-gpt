package com.zlt.aps.common.engine.mapper;

import com.zlt.aps.common.engine.domain.LhEngineTireConstructionInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 硫化工序施工Mapper接口
 */
@Mapper
public interface LhEngineTireConstructionInfoMapper {

    /**
     * 根据条件获取硫化工序施工信息
     * @param lhEngineTireConstructionInfo
     * @return
     */
    List<LhEngineTireConstructionInfo> selectLhTireConstructionInfoList(LhEngineTireConstructionInfo lhEngineTireConstructionInfo);
}
