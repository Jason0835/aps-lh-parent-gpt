package com.zlt.aps.lh.mapper;

import com.zlt.aps.lh.api.domain.entity.LhDayFinishQty;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 硫化排程日完成量Mapper
 *
 * @author APS Team
 * @since 2026/04/13
 */
@Mapper
public interface LhDayFinishQtyMapper extends CommBaseMapper<LhDayFinishQty> {

    /**
     * 根据唯一键查询已存在的数据
     *
     * @param list 唯一键列表
     * @return 已存在的数据
     */
    List<LhDayFinishQty> selectByUniqueKeyList(@Param("list") List<LhDayFinishQty> list);

}
