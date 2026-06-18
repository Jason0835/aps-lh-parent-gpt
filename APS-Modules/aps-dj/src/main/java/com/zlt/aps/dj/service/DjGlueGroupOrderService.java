package com.zlt.aps.dj.service;

import java.util.List;

import com.zlt.aps.dj.api.domain.dto.DjGlueGroupOrderDto;
import com.zlt.aps.dj.api.domain.entity.DjGlueGroupOrder;
import com.zlt.bill.common.service.IDocService;

/**
 * <p>
 * 垫胶胶料组别顺序维护 服务类
 * </p>
 *
 * @author zhangbinglin
 */
public interface DjGlueGroupOrderService extends IDocService<DjGlueGroupOrder> {

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @return
     */
    List<DjGlueGroupOrderDto> listGlueGroupOrder(DjGlueGroupOrderDto dto);

    /**
     * 根据code判断胶料组号是否已经存在
     */
    String checkGlueGroupCodeUnique(DjGlueGroupOrderDto dto);

    /**
     * 查询出被使用了的胶料组别
     *
     * @param glueGroupIds
     * @return
     */
    List<String> listUserdGlueGroup(List<Long> glueGroupIds);
}
