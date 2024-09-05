package com.zlt.aps.xwyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.xwyy.api.domain.dto.XwyyParamsDto;
import com.zlt.aps.xwyy.entity.XwyyParams;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 纤维压延参数信息Mapper接口
 *
 * @author zlt
 * @date 2021-06-07
 */
public interface XwyyParamsMapper extends BaseMapper<XwyyParams> {
    /**
     * 查询参数集合
     *
     * @param params 查询条件
     * @return 查询到的结果
     */
    public List<XwyyParamsDto> listParams(XwyyParams params);

    /**
     * 检查纤维压延参数代码唯一
     *
     * @param paramCode 参数代码
     * @param id        纤维压延参数信息 id
     * @return 查询到的结果
     */
    public XwyyParams checkParamsCodeUnique(@Param("paramCode") String paramCode, @Param("id") Long id);
}
