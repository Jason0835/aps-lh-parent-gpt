package com.zlt.aps.lh.mapper;

import com.zlt.aps.lh.api.domain.entity.LhMouldCleanWarn;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 模具清洗预警Mapper
 *
 * @author APS Team
 * @since 2026/04/10
 */
@Mapper
public interface LhMouldCleanWarnMapper extends CommBaseMapper<LhMouldCleanWarn> {

    /**
     * 根据唯一键查询已存在的数据
     *
     * @param list 唯一键列表
     * @return 已存在的数据
     */
    List<LhMouldCleanWarn> selectByUniqueKeyList(@Param("list") List<LhMouldCleanWarn> list);
}
