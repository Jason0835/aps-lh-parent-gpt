package com.zlt.aps.nc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.nc.api.domain.dto.NcParamsDto;
import com.zlt.aps.nc.entity.NcParams;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 内衬参数信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-25
 */
public interface NcParamsMapper extends BaseMapper<NcParams> {
    /**
     * 查询参数集合
     *
     * @param params 查询条件
     * @return 查询到的结果
     */
    public List<NcParamsDto> listParams(NcParams params);

    /**
     * 检查内衬参数代码唯一
     *
     * @param paramCode 参数代码
     * @param id        内衬参数信息 id
     * @return 查询到的结果
     */
    public NcParams checkParamsCodeUnique(@Param("paramCode") String paramCode, @Param("id") Long id);
}
