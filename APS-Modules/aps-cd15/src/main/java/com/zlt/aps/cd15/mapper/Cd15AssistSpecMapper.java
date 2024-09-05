package com.zlt.aps.cd15.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15AssistSpec;

import java.util.List;


/**
 * <p>
 * 15度裁断钢压大卷信息表 Mapper 接口
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface Cd15AssistSpecMapper extends BaseMapper<Cd15AssistSpec> {

    /**
     * 根据条件查询列表
     *
     * @param dto
     * @return
     */
    List<Cd15AssistSpec> listAssistSpec(Cd15AssistSpec dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<Cd15AssistSpec> list);
}
