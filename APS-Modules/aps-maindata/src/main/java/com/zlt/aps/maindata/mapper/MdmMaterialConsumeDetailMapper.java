package com.zlt.aps.maindata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialConsumeDetail;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * BOM物料消耗明细Mapper接口
 * @author nick
 */
@Mapper
public interface MdmMaterialConsumeDetailMapper extends CommBaseMapper<MdmMaterialConsumeDetail> {


}
