package com.zlt.aps.cx.engine.mapper;


import com.zlt.aps.cx.api.domain.dto.CxParamsDto;

import java.util.List;

/**
 * 成型工序参数mapper接口
 */
public interface CxEngineParamsMapper {

    /**
     * 加载成型工序机台参数信息
     * @param cxParamsDto
     * @return
     */
    public List<CxParamsDto> listParams(CxParamsDto cxParamsDto);
}
