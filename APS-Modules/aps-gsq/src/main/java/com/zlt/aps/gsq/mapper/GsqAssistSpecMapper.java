package com.zlt.aps.gsq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gsq.api.domain.entity.GsqAssistSpec;

import java.util.List;


/**
 * <p>
 * 钢丝圈外协规格管理表 Mapper 接口
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface GsqAssistSpecMapper extends BaseMapper<GsqAssistSpec> {

    /**
     * 根据条件查询列表
     *
     * @param dto
     * @return
     */
    List<GsqAssistSpec> listAssistSpec(GsqAssistSpec dto);
}
