package com.zlt.aps.maindata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.mp.api.domain.entity.MdmCxScheDayFinishQty;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * APS成型排程日完成量接口Mapper
 *
 * @author APS Team
 * @since 2026/03/27
 */
@Mapper
public interface MdmCxScheDayFinishQtyEntityMapper extends BaseMapper<MdmCxScheDayFinishQty> {

    /**
     * 根据唯一键查询已存在的数据
     * @param list 唯一键列表
     * @return 已存在的数据
     */
    List<MdmCxScheDayFinishQty> selectByUniqueKeyList(@Param("list") List<MdmCxScheDayFinishQty> list);

}
