package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.api.domain.dto.CxParamsDto;
import com.zlt.aps.cx.api.domain.dto.CxShowDeDto;
import com.zlt.aps.cx.api.domain.dto.LhShowDeDto;
import com.zlt.aps.cx.entity.CxParams;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 成型参数信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-25
 */
public interface CxParamsMapper extends BaseMapper<CxParams> {
    /**
     * 查询参数集合
     *
     * @param params 查询条件
     * @return 查询到的结果
     */
    public List<CxParamsDto> listParams(CxParams params);

    /**
     * 检查参数代码唯一
     *
     * @param paramCode 参数代码
     * @param id        参数信息 id
     * @return 查询到的结果
     */
    public CxParams checkParamsCodeUnique(@Param("paramCode") String paramCode, @Param("id") Long id);

    /**
     * 查询成型定额
     * @param params
     * @return
     */
    List<CxShowDeDto> selectCxShowDeDtoList(CxShowDeDto params);

    /**
     * 查询成型定额
     * @param params
     * @return
     */
    List<LhShowDeDto> selecLhShowDeDtoList(LhShowDeDto params);

}
