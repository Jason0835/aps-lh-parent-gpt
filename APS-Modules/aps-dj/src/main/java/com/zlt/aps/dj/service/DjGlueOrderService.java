package com.zlt.aps.dj.service;

import java.util.List;

import com.zlt.aps.dj.api.domain.dto.DjGlueOrderDto;
import com.zlt.aps.dj.api.domain.entity.DjGlueOrder;
import com.zlt.bill.common.service.IDocService;

/**
 * <p>
 * 垫胶胶料顺序维护 服务类
 * </p>
 *
 * @author zhangbinglin
 */
public interface DjGlueOrderService extends IDocService<DjGlueOrder> {
    /**
     * 根据条件查询胶料顺序列表
     *
     * @return
     */
    List<DjGlueOrderDto> listGlueOrder(DjGlueOrderDto dto);

    /**
     * 根据code判断胶料号是否已经存在
     */
    String checkGlueCodeUnique(DjGlueOrderDto dto);
}
