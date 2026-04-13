package com.zlt.aps.lh.mapper;

import com.zlt.aps.lh.api.domain.entity.LhScheFinishQty;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 硫化排程完成量回报Mapper
 *
 * @author APS Team
 * @since 2026/04/09
 */
@Mapper
public interface LhScheFinishQtyMapper extends CommBaseMapper<LhScheFinishQty> {

    /**
     * 根据唯一键查询已存在的数据
     *
     * @param list 唯一键列表
     * @return 已存在的数据
     */
    List<LhScheFinishQty> selectByUniqueKeyList(@Param("list") List<LhScheFinishQty> list);

}
