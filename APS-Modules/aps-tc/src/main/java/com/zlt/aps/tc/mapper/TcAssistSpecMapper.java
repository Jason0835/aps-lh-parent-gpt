package com.zlt.aps.tc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tc.api.domain.entity.TcAssistSpec;

import java.util.List;


/**
 * <p>
 * 钢丝圈外协规格管理表 Mapper 接口
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface TcAssistSpecMapper extends BaseMapper<TcAssistSpec> {

    /**
     * 根据条件查询列表
     *
     * @param dto
     * @return
     */
    List<TcAssistSpec> listAssistSpec(TcAssistSpec dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<TcAssistSpec> list);
}
