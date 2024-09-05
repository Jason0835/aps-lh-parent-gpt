package com.zlt.aps.tq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tq.api.domain.entity.TqAssistSpec;

import java.util.List;


/**
 * <p>
 * 胎圈外协规格管理表 Mapper 接口
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface TqAssistSpecMapper extends BaseMapper<TqAssistSpec> {

    /**
     * 根据条件查询列表
     *
     * @param dto
     * @return
     */
    List<TqAssistSpec> listAssistSpec(TqAssistSpec dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<TqAssistSpec> list);
}
