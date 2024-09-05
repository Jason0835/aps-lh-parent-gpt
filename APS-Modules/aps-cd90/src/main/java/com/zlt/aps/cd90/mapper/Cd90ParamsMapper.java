package com.zlt.aps.cd90.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd90.api.domain.dto.Cd90ParamsDto;
import com.zlt.aps.cd90.entity.Cd90Params;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 90度裁断参数信息Mapper接口
 *
 * @author zlt
 * @date 2021-06-07
 */
public interface Cd90ParamsMapper extends BaseMapper<Cd90Params> {
    /**
     * 查询参数集合
     *
     * @param params 查询条件
     * @return 查询到的结果
     */
    public List<Cd90ParamsDto> listParams(Cd90Params params);

    /**
     * 检查90度裁断参数代码唯一
     *
     * @param paramCode 参数代码
     * @param id        90度裁断参数信息 id
     * @return 查询到的结果
     */
    public Cd90Params checkParamsCodeUnique(@Param("paramCode") String paramCode, @Param("id") Long id);
}
