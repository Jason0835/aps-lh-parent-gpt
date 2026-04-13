package com.zlt.aps.cx.mapper;

import com.zlt.aps.cx.api.domain.entity.CxScheFinishQty;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 成型排程完成量回报Mapper
 *
 * @author APS Team
 * @since 2026/04/09
 */
@Mapper
public interface CxScheFinishQtyMapper extends CommBaseMapper<CxScheFinishQty> {

    /**
     * 根据唯一键查询已存在的数据
     *
     * @param list 唯一键列表
     * @return 已存在的数据
     */
    List<CxScheFinishQty> selectByUniqueKeyList(@Param("list") List<CxScheFinishQty> list);

}
