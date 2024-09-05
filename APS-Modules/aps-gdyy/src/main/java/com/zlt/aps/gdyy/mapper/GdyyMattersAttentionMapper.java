package com.zlt.aps.gdyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gdyy.api.domain.dto.GdyyMattersAttentionDto;
import com.zlt.aps.gdyy.entity.GdyyMattersAttention;

import java.util.List;

/**
 * <p>
 * 帘布大卷注意事项信息表 Mapper 接口
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-07
 */
public interface GdyyMattersAttentionMapper extends BaseMapper<GdyyMattersAttention> {
    /**
     * 根据条件查询注意事项列表
     *
     * @param dto
     * @return
     */
    List<GdyyMattersAttentionDto> listGwyyMattersAttention(GdyyMattersAttentionDto dto);


    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<GdyyMattersAttention> list);
}
