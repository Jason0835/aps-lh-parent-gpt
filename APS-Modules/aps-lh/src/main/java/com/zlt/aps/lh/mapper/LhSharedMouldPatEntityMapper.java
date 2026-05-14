package com.zlt.aps.lh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.lh.api.domain.entity.LhSharedMouldPat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 共用模具花纹配置Mapper接口
 *
 * @author zlt
 * @date 2026-05-14
 */
@Mapper
public interface LhSharedMouldPatEntityMapper extends BaseMapper<LhSharedMouldPat> {

    /**
     * 批量插入
     *
     * @param list 数据列表
     * @return 插入行数
     */
    int insertBatch(@Param("list") List<LhSharedMouldPat> list);

    /**
     * 批量更新
     *
     * @param list 数据列表
     * @return 更新行数
     */
    int updateBatch(@Param("list") List<LhSharedMouldPat> list);
}
