package com.zlt.aps.maindata.mapper;

import com.zlt.aps.monthplan.api.domain.entity.RawMaterialMonthDiff;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;


import java.util.List;

/**
 * 原材料需求差异Mapper接口
 * @author nick
 */
@Mapper
public interface RawMaterialMonthDiffMapper extends CommBaseMapper<RawMaterialMonthDiff> {


}
