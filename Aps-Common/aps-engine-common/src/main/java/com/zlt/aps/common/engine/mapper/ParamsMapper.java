package com.zlt.aps.common.engine.mapper;

import java.util.List;

import com.zlt.aps.common.engine.domain.ParamsVo;
import com.zlt.aps.cx.api.domain.entity.CxCloseOutRange;

/**
 * 各工序系统参数查询
 * @Description
 */
public interface ParamsMapper {
    List<ParamsVo> listCxParams();
    List<ParamsVo> listLhParams();
    List<ParamsVo> listNcParams();
    List<ParamsVo> listTqParams();
    List<ParamsVo> listTcParams();
    List<ParamsVo> listGsqParams();
    List<ParamsVo> listTmParams();
    List<ParamsVo> listCd15Params();
    List<ParamsVo> listCd90Params();
    List<ParamsVo> listGdyyParams();
    List<ParamsVo> listXwyyParams();
    List<CxCloseOutRange> listCloseRangeParams();
}
