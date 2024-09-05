package com.zlt.aps.xwyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.xwyy.api.domain.entity.XwyyOriginalLineSpec;

import java.util.List;


/**
 * <p>
 * 纤维压延原线规格管理表 Mapper 接口
 * </p>
 *
 */
public interface XwyyOriginalLineSpecMapper extends BaseMapper<XwyyOriginalLineSpec> {

    /**
     * 根据条件查询列表
     *
     * @param dto
     * @return
     */
    List<XwyyOriginalLineSpec> listOriginalLineSpec(XwyyOriginalLineSpec dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<XwyyOriginalLineSpec> list);
}
