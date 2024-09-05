package com.zlt.aps.lh.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.lh.engine.domain.LhEngineParams;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 硫化参数信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-25
 */
public interface LhEngineParamsMapper extends BaseMapper<LhEngineParams> {
    /**
     * 查询参数集合
     *
     * @param params 查询条件
     * @return 查询到的结果
     */
    public List<LhEngineParams> listParams(LhEngineParams params);

    /**
     * 检查参数代码唯一
     *
     * @param paramCode 参数代码
     * @param id        参数信息 id
     * @return 查询到的结果
     */
    public LhEngineParams checkParamsCodeUnique(@Param("paramCode") String paramCode, @Param("id") Long id);
}
