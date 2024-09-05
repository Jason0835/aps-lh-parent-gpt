package com.zlt.aps.nc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.nc.api.domain.dto.NcGlueGroupOrderDto;
import com.zlt.aps.nc.entity.NcGlueGroupOrder;

import java.util.List;

/**
 * <p>
 * 内衬胶料组别顺序维护 服务类
 * </p>
 *
 * @author zhangbinglin
 */
public interface NcGlueGroupOrderService extends IService<NcGlueGroupOrder> {

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @return
     */
    List<NcGlueGroupOrderDto> listGlueGroupOrder(NcGlueGroupOrderDto dto);

    /**
     * 保存胶料组别顺序信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    void saveGlueGroupOrder(NcGlueGroupOrder entity);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids
     */
    void deleteGlueGroupOrder(Long[] ids);

    /**
     * 根据code判断胶料组号是否已经存在
     */
    String checkGlueGroupCodeUnique(NcGlueGroupOrderDto dto);

    /**
     * 导入数据，并保存记录
     * @param list 要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId 导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<NcGlueGroupOrderDto> list, boolean updateSupport, Long importLogId);
}
