package com.zlt.aps.nc.service;

import java.util.List;

import com.zlt.aps.nc.api.domain.entity.NcGlueOrder;
import com.zlt.bill.common.service.IDocService;

/**
 * <p>
 * 内衬胶料顺序维护 服务类
 * </p>
 *
 * @author zhangbinglin
 */
public interface NcGlueOrderService extends IDocService<NcGlueOrder> {
    /**
     * 根据条件查询胶料顺序列表
     *
     * @return
     */
    List<NcGlueOrder> listGlueOrder(NcGlueOrder dto);

    /**
     * 根据code判断胶料号是否已经存在
     */
    String checkGlueCodeUnique(NcGlueOrder dto);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids
     */
    void deleteGlueOrder(Long[] ids);
}
