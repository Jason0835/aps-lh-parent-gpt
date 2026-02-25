package com.zlt.aps.job.task;

import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.mp.api.domain.entity.MdmProductStock;
import com.zlt.aps.mp.api.domain.entity.MdmUnqualifiedStock;
import com.zlt.aps.mp.api.domain.entity.RawSpecialMaterialStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * MES接口定时任务
 *
 * @author Chen
 * @since 2025/12/22
 */
@Component("mesTask")
public class MesTask {

    @Autowired
    private IMesItfService iMesItfService;

    /**
     * 同步成品库存
     */
    @ApiOperation("同步成品库存-默认当前年月")
    public void syncProductStock() {
        iMesItfService.syncProductStock(new MdmProductStock());
    }

    /**
     * 同步不合格库存
     */
    @ApiOperation("同步不合格库存-默认当前日期")
    public void syncUnqualifiedStock() {
        iMesItfService.syncUnqualifiedStock(new MdmUnqualifiedStock());
    }

    /**
     * 同步特殊材料库存
     */
    @ApiOperation("同步特殊材料库存-默认当前日期")
    public void syncRawSpecialMaterialStock() {
        iMesItfService.syncRawSpecialMaterialStock(new RawSpecialMaterialStock());
    }
}
