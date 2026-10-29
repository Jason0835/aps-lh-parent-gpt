package com.zlt.aps.lh.mapper;

import com.zlt.aps.lh.api.domain.entity.LhEndingDiscard;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 硫化收尾舍弃计划记录 Mapper（MyBatis-Plus）。
 *
 * <p>仅提供基础增删改查和批量保存能力，供收尾不排产时落库舍弃计划量。
 * 查询条件统一在业务层使用 LambdaQueryWrapper 组装，本接口不额外定义 SQL。</p>
 *
 * @author APS
 * @date 2026/10/28
 */
@Mapper
public interface LhEndingDiscardMapper extends CommBaseMapper<LhEndingDiscard> {

}
