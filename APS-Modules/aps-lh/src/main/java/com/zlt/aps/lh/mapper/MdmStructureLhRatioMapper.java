package com.zlt.aps.lh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio;
import org.apache.ibatis.annotations.Mapper;

/**
 * 成型结构硫化配比Mapper（表 T_MDM_STRUCTURE_LH_RATIO）。
 *
 * <p>供硫化侧「按最新成型余量重算结构可收尾时间」读取结构硫化配比（LH_MACHINE_MAX_QTY）使用。
 *
 * @author APS
 */
@Mapper
public interface MdmStructureLhRatioMapper extends BaseMapper<MdmStructureLhRatio> {
}
