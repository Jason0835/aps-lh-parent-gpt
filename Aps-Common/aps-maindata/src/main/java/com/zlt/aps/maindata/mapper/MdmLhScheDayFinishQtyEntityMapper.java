package com.zlt.aps.maindata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.mp.api.domain.entity.MdmLhScheDayFinishQty;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * APS硫化排程日完成量接口Mapper
 *
 * @author APS Team
 * @since 2026/03/27
 */
@Mapper
public interface MdmLhScheDayFinishQtyEntityMapper extends BaseMapper<MdmLhScheDayFinishQty> {

    /**
     * 根据唯一键查询已存在的数据
     * @param list 唯一键列表
     * @return 已存在的数据
     */
    List<MdmLhScheDayFinishQty> selectByUniqueKeyList(@Param("list") List<MdmLhScheDayFinishQty> list);

}
