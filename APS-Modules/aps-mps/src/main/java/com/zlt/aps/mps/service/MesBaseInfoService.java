package com.zlt.aps.mps.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Gim
 */
public interface MesBaseInfoService {

    /**
     * bom信息同步
     * 这是业务库表，非mes中间表
     */
    @Transactional
    AjaxResult mergeBomInfo(String dataVersion);
    
    /**
     * PLM参数同步，将中间表的PLM参数合并到业务表中
     * @param dataVersion	同步版本
     */
    AjaxResult mergePlmConstructionInfo(String dataVersion);
}
