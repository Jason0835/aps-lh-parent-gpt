package com.zlt.aps.cx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.cxlh.cx.api.domain.entity.BomInfo;
import com.zlt.aps.cxlh.cx.api.domain.vo.BomInfoDto;


import java.util.List;

/**
 * BOM信息Service接口
 *
 * @author Chen
 * @date 2021-06-11
 */
public interface BomInfoService extends IService<BomInfo> {

    /**
     * 查询BOM信息列表
     *
     * @param bomInfo BOM信息
     * @return BOM信息集合
     */
    public List<BomInfoDto> selectBomInfoList(BomInfo bomInfo);


}
