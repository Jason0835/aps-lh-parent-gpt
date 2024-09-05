package com.zlt.aps.gdyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gdyy.api.domain.dto.GdyyParamsDto;
import com.zlt.aps.gdyy.entity.GdyyParams;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 钢丝圈参数信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-25
 */
public interface GdyyParamsMapper extends BaseMapper<GdyyParams> {
    /**
     * 查询参数集合
     *
     * @param params 查询条件
     * @return 查询到的结果
     */
    public List<GdyyParamsDto> listParams(GdyyParams params);

    /**
     * 检查钢丝圈参数代码唯一
     *
     * @param paramCode 参数代码
     * @param id        钢丝圈参数信息 id
     * @return 查询到的结果
     */
    public GdyyParams checkParamsCodeUnique(@Param("paramCode") String paramCode, @Param("id") Long id);
}
