package com.zlt.aps.xwyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.xwyy.api.domain.entity.XwyyAssistSpec;

import java.util.List;


/**
 * <p>
 * 纤维压延外协规格管理表 Mapper 接口
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface XwyyAssistSpecMapper extends BaseMapper<XwyyAssistSpec> {

    /**
     * 根据条件查询列表
     *
     * @param dto
     * @return
     */
    List<XwyyAssistSpec> listAssistSpec(XwyyAssistSpec dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<XwyyAssistSpec> list);
}
