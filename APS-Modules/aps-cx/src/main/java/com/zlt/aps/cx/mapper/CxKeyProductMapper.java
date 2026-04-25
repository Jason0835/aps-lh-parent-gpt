package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.entity.config.CxKeyProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 关键产品配置Mapper
 *
 * @author APS Team
 */
@Mapper
public interface CxKeyProductMapper extends BaseMapper<CxKeyProduct> {

    /**
     * 批量插入
     */
    int insertBatch(@Param("list") List<CxKeyProduct> list);

    /**
     * 批量更新
     */
    int updateBatch(@Param("list") List<CxKeyProduct> list);
}
