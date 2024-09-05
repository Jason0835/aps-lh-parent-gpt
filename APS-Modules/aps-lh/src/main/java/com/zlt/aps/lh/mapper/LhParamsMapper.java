package com.zlt.aps.lh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.lh.api.domain.dto.LhParamsDto;
import com.zlt.aps.lh.entity.LhParams;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 硫化参数信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-25
 */
public interface LhParamsMapper extends BaseMapper<LhParams> {
    /**
     * 查询参数集合
     *
     * @param params 查询条件
     * @return 查询到的结果
     */
    public List<LhParamsDto> listParams(LhParams params);

    /**
     * 检查参数代码唯一
     *
     * @param paramCode 参数代码
     * @param id        参数信息 id
     * @return 查询到的结果
     */
    public LhParams checkParamsCodeUnique(@Param("paramCode") String paramCode, @Param("id") Long id);
}
