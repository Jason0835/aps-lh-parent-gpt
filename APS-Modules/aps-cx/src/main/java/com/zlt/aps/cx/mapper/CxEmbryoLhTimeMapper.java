package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.entity.config.CxEmbryoLhTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 胎胚最早可供硫化时间Mapper
 *
 * @author APS Team
 */
@Mapper
public interface CxEmbryoLhTimeMapper extends BaseMapper<CxEmbryoLhTime> {

    /**
     * 批量插入
     *
     * @param list 待插入的数据列表
     * @return 影响行数
     */
    int insertBatch(@Param("list") List<CxEmbryoLhTime> list);

    /**
     * 批量更新
     *
     * @param list 待更新的数据列表
     * @return 影响行数
     */
    int updateBatch(@Param("list") List<CxEmbryoLhTime> list);
}
