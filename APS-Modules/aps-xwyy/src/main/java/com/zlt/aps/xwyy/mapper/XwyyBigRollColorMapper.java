package com.zlt.aps.xwyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.xwyy.api.domain.dto.XwyyBigRollColorDto;
import com.zlt.aps.xwyy.entity.XwyyBigRollColor;

import java.util.List;

/**
 * <p>
 * 帘布大卷颜色提示信息表 Mapper 接口
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-07
 */
public interface XwyyBigRollColorMapper extends BaseMapper<XwyyBigRollColor> {
    /**
     * 根据条件大卷颜色信息维护表
     *
     * @param dto
     * @return
     */
    List<XwyyBigRollColorDto> listXwyyBigRollColor(XwyyBigRollColorDto dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<XwyyBigRollColor> list);

}
