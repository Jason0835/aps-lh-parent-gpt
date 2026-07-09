package com.zlt.aps.nc.service;

import java.util.List;

import com.zlt.aps.nc.api.domain.entity.NcGlueGroupOrder;
import com.zlt.bill.common.service.IDocService;

/**
 * <p>
 * 内衬胶料组别顺序维护 服务类
 * </p>
 *
 * @author zhangbinglin
 */
public interface NcGlueGroupOrderService extends IDocService<NcGlueGroupOrder> {

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @return
     */
    List<NcGlueGroupOrder> listGlueGroupOrder(NcGlueGroupOrder dto);

    /**
     * 根据code判断胶料组号是否已经存在
     */
    String checkGlueGroupCodeUnique(NcGlueGroupOrder dto);

    /**
     * 查询出被使用了的胶料组别
     *
     * @param glueGroupIds
     * @return
     */
    List<String> listUserdGlueGroup(List<Long> glueGroupIds);
}
