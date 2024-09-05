package com.zlt.aps.cd15.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd15.api.domain.dto.Cd15ParamsDto;
import com.zlt.aps.cd15.entity.Cd15Params;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 15度裁断参数信息Mapper接口
 *
 * @author zlt
 * @date 2021-06-07
 */
public interface Cd15ParamsMapper extends BaseMapper<Cd15Params> {
    /**
     * 查询参数集合
     *
     * @param params 查询条件
     * @return 查询到的结果
     */
    public List<Cd15ParamsDto> listParams(Cd15Params params);

    /**
     * 检查15度裁断参数代码唯一
     *
     * @param paramCode 参数代码
     * @param id        15度裁断参数信息 id
     * @return 查询到的结果
     */
    public Cd15Params checkParamsCodeUnique(@Param("paramCode") String paramCode, @Param("id") Long id);
}
