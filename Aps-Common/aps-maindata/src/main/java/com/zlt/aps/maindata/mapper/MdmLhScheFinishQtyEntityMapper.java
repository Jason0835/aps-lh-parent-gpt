package com.zlt.aps.maindata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.mp.api.domain.entity.MdmLhScheFinishQty;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * APS硫化排程完成量回报接口Mapper
 *
 * @author APS Team
 * @since 2026/03/27
 */
@Mapper
public interface MdmLhScheFinishQtyEntityMapper extends BaseMapper<MdmLhScheFinishQty> {

    /**
     * 根据唯一键查询已存在的数据
     * @param list 唯一键列表
     * @return 已存在的数据
     */
    List<MdmLhScheFinishQty> selectByUniqueKeyList(@Param("list") List<MdmLhScheFinishQty> list);

}
