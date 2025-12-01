package com.zlt.mix.schedule.engine.vo;

import com.zlt.mix.schedule.api.domain.entity.MaterialSpanReceive;
import com.zlt.mix.schedule.api.domain.entity.MaterialSpanSend;
import lombok.Data;

import java.util.List;

/**
 * 硫磺辅料跨区发送和接收列表VO
 */
@Data
public class MaterialSpanVo {


    /**
     * 跨区发送记录列表
     */
    private List<MaterialSpanSend> spanSendList;

    /**
     * 跨区发送接收列表
     */
    private List<MaterialSpanReceive> spanReceiveList;
}
