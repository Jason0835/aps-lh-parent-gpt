package com.zlt.aps.nc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.nc.api.domain.entity.NcAssistSpec;

import java.util.List;


/**
 * <p>
 * 内衬外协规格管理表 Mapper 接口
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface NcAssistSpecMapper extends BaseMapper<NcAssistSpec> {

    /**
     * 根据条件查询列表
     *
     * @param dto
     * @return
     */
    List<NcAssistSpec> listAssistSpec(NcAssistSpec dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<NcAssistSpec> list);
}
