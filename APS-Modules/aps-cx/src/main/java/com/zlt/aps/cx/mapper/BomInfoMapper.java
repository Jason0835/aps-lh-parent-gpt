package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.api.domain.dto.BomInfoDto;
import com.zlt.aps.cx.entity.BomInfo;

import java.util.List;

/**
 * BOM信息Mapper接口
 *
 * @author Chen
 * @date 2021-06-11
 */
public interface BomInfoMapper extends BaseMapper<BomInfo> {

    /**
     * 查询BOM信息列表
     *
     * @param bomInfo BOM信息
     * @return BOM信息集合
     */
    public List<BomInfoDto> selectBomInfoList(BomInfo bomInfo);


}
