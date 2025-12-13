package com.zlt.aps.maindata.mapper;

import com.zlt.aps.monthplan.api.domain.entity.RawMaterialMonthDiff;
import com.zlt.aps.monthplan.api.domain.entity.RawWarningConfig;
import com.zlt.aps.monthplan.api.domain.entity.RawWeekUsage;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;


import java.util.List;

/**
 * @author nick
 */
@Mapper
public interface RawWarningConfigMapper extends CommBaseMapper<RawWarningConfig> {


}
