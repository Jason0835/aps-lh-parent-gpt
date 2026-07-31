package com.zlt.aps.gsq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gsq.api.domain.dto.GsqParamsDto;
import com.zlt.aps.gsq.entity.GsqParams;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 钢丝圈参数信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-25
 */
public interface GsqParamsMapper extends BaseMapper<GsqParams> {
    /**
     * 查询参数集合
     *
     * @param params 查询条件
     * @return 查询到的结果
     */
    public List<GsqParamsDto> listParams(GsqParams params);

    /**
     * 检查钢丝圈参数代码唯一
     *
     * @param paramCode 参数代码
     * @param id        钢丝圈参数信息 id
     * @return 查询到的结果
     */
    public GsqParams checkParamsCodeUnique(@Param("paramCode") String paramCode, @Param("id") Long id);
}
