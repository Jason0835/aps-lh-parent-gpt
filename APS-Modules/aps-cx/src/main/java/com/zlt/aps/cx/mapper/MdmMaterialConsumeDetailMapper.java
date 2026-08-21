package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialConsumeDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * BOM物料消耗明细Mapper接口
 * <p>用于成型导出胶种取值：按 工厂+胎胚代码+胎胚版本 查询 AQT 开头的原材料描述</p>
 *
 * @author APS Team
 */
@Mapper
public interface MdmMaterialConsumeDetailMapper extends BaseMapper<MdmMaterialConsumeDetail> {
}