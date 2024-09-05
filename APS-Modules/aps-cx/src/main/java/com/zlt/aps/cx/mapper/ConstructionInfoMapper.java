package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.api.domain.dto.ConstructionInfoDto;
import com.zlt.aps.cx.entity.ConstructionInfo;

import java.util.List;

/**
 * <p>
 * 施工信息信息表 Mapper 接口
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface ConstructionInfoMapper extends BaseMapper<ConstructionInfo> {

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @param dto
     * @return
     */
    List<ConstructionInfoDto> listConstructionInfo(ConstructionInfoDto dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<ConstructionInfo> list);

}
