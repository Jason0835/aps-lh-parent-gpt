package com.zlt.aps.gsq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gsq.api.domain.dto.GsqTwiningDiscDto;
import com.zlt.aps.gsq.entity.GsqTwiningDisc;

import java.util.List;

/**
 * <p>
 * 钢丝圈缠绕盘表 Mapper 接口
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface GsqTwiningDiscMapper extends BaseMapper<GsqTwiningDisc> {

    /**
     * 根据条件查询缠绕盘顺序列表
     *
     * @param dto
     * @return
     */
    List<GsqTwiningDiscDto> listTwiningDisc(GsqTwiningDiscDto dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<GsqTwiningDiscDto> list);


    public int updateTGsqTwiningDisc(GsqTwiningDisc dto);

    void deleteAll();
}
