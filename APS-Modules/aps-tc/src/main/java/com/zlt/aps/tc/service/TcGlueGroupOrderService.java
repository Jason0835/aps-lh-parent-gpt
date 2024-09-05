package com.zlt.aps.tc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tc.api.domain.dto.TcGlueGroupOrderDto;
import com.zlt.aps.tc.entity.TcGlueGroupOrder;

import java.util.List;

/**
 * <p>
 * 胎侧胶料组别顺序维护 服务类
 * </p>
 *
 * @author zhangbinglin
 */
public interface TcGlueGroupOrderService extends IService<TcGlueGroupOrder> {

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @return
     */
    List<TcGlueGroupOrderDto> listGlueGroupOrder(TcGlueGroupOrderDto dto);

    /**
     * 保存胶料组别顺序信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    void saveGlueGroupOrder(TcGlueGroupOrder entity);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids
     */
    void deleteGlueGroupOrder(Long[] ids);

    /**
     * 根据code判断胶料组号是否已经存在
     */
    String checkGlueGroupCodeUnique(TcGlueGroupOrderDto dto);

    /**
     * 导入数据
     */
    public AjaxResult importData(List<TcGlueGroupOrderDto> list, boolean updateSupport, Long importLogId);
}
