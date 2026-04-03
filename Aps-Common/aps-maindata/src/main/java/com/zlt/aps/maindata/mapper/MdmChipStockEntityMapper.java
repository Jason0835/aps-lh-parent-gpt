package com.zlt.aps.maindata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.mdm.api.domain.entity.MdmChipStock;
import org.apache.ibatis.annotations.Mapper;

/**
 * 芯片库存 Mapper
 *
 * @author APS Team
 * @date 2026-04-02
 */
@Mapper
public interface MdmChipStockEntityMapper extends BaseMapper<MdmChipStock> {
}
