package com.zlt.aps.maindata.mapper;

import com.zlt.aps.monthplan.api.domain.entity.RawMaterialMonthDiff;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


import java.util.List;

/**
 * 原材料需求差异Mapper接口
 * @author nick
 */
@Mapper
public interface RawMaterialMonthDiffMapper extends CommBaseMapper<RawMaterialMonthDiff> {


    /**
     * 批量插入原材料月差异数据
     * @param list 差异数据列表
     * @return 插入条数
     */
    int batchInsert(@Param("list") List<RawMaterialMonthDiff> list);

}
