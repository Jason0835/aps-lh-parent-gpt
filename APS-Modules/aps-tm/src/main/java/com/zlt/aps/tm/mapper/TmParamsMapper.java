package com.zlt.aps.tm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tm.api.domain.dto.TmParamsDto;
import com.zlt.aps.tm.entity.TmParams;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 胎面参数信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-25
 */
public interface TmParamsMapper extends BaseMapper<TmParams> {
    /**
     * 查询参数集合
     *
     * @param tmParams 查询条件
     * @return 查询到的结果
     */
    public List<TmParamsDto> listParams(TmParams tmParams);

    /**
     * 检查胎面参数代码唯一
     *
     * @param paramCode 参数代码
     * @param id        胎面参数信息 id
     * @return 查询到的结果
     */
    public TmParams checkParamsCodeUnique(@Param("paramCode") String paramCode, @Param("id") Long id);
}
