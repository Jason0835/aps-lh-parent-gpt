package com.zlt.aps.tc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tc.api.domain.dto.TcParamsDto;
import com.zlt.aps.tc.entity.TcParams;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 胎侧参数信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-25
 */
public interface TcParamsMapper extends BaseMapper<TcParams> {
    /**
     * 检查胎侧参数代码唯一
     *
     * @param paramCode 参数代码
     * @param id        胎侧参数信息 id
     * @return 查询到的结果
     */
    public TcParams checkParamsCodeUnique(@Param("paramCode") String paramCode, @Param("id") Long id);

    /**
     * 查询参数集合
     *
     * @param params 查询条件
     * @return 查询到的结果
     */
    public List<TcParamsDto> listParams(TcParams params);
}
