package com.zlt.aps.dj.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.dj.api.domain.dto.DjParamsDto;
import com.zlt.aps.dj.api.domain.entity.DjParams;

/**
 * 垫胶参数信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-25
 */
public interface DjParamsMapper extends BaseMapper<DjParams> {
    /**
     * 查询参数集合
     *
     * @param params 查询条件
     * @return 查询到的结果
     */
    public List<DjParamsDto> listParams(DjParams params);

    /**
     * 检查垫胶参数代码唯一
     *
     * @param paramCode 参数代码
     * @param id        垫胶参数信息 id
     * @return 查询到的结果
     */
    public DjParams checkParamsCodeUnique(@Param("paramCode") String paramCode, @Param("id") Long id);
}
