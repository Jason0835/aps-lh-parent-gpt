package com.zlt.aps.mps.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存回报
 * @author Gim
 */
public interface MesHalfPartService {
    // 胎面库存
    @Transactional
    public AjaxResult mergeTm(String dataVersion);

    // 胎侧库存
    @Transactional
    public AjaxResult mergeTc(String dataVersion);

    // 内衬库存
    @Transactional
    public AjaxResult mergeNc(String dataVersion);

    // 胎圈库存
    @Transactional
    public AjaxResult mergeTq(String dataVersion);

    // 钢丝圈库存
    @Transactional
    public AjaxResult mergeGsq(String dataVersion);

    // 钢带压延库存
    @Transactional
    public AjaxResult mergeGdyy(String dataVersion);

    // 纤维压延库存
    @Transactional
    public AjaxResult mergeXwyy(String dataVersion);

    // 15度裁断库存
    @Transactional
    public AjaxResult mergeCd15(String dataVersion);
    
    // 15度裁断线边库库存
    @Transactional
    public AjaxResult mergeCd15LineSide(String dataVersion);

    // 90度裁断库存
    @Transactional
    public AjaxResult mergeCd90(String dataVersion);
    
    // 90度裁断线边库库存
    @Transactional
    public AjaxResult mergeCd90LineSide(String dataVersion);
}
