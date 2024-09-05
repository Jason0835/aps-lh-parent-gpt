package com.zlt.aps.cx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.api.gateway.system.domain.Ztree;
import com.zlt.aps.cx.api.domain.dto.BomInfoDto;
import com.zlt.aps.cx.entity.BomInfo;

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
