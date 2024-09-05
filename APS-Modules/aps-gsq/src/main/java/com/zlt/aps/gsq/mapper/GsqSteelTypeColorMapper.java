package com.zlt.aps.gsq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gsq.api.domain.dto.GsqSteelTypeColorDto;
import com.zlt.aps.gsq.entity.GsqSteelTypeColor;

import java.util.List;

/**
 * @author Gim
 */
public interface GsqSteelTypeColorMapper extends BaseMapper<GsqSteelTypeColor> {
    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @param dto
     * @return
     */
    List<GsqSteelTypeColorDto> listGsqSteelTypeColor(GsqSteelTypeColorDto dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<GsqSteelTypeColorDto> list);
}
