package com.zlt.aps.tc.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tc.api.domain.dto.TcGlueOrderDto;
import com.zlt.aps.tc.entity.TcGlueOrder;

import java.util.List;

/**
 * <p>
 * 胎侧胶料顺序维护 服务类
 * </p>
 *
 * @author zhangbinglin
 */
public interface TcGlueOrderService extends IService<TcGlueOrder> {
    /**
     * 根据条件查询胶料顺序列表
     *
     * @return
     */
    List<TcGlueOrderDto> listGlueOrder(TcGlueOrderDto dto);

    /**
     * 保存胶料顺序信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    void saveGlueOrder(TcGlueOrder entity);

    /**
     * 根据code判断胶料号是否已经存在
     */
    String checkGlueCodeUnique(TcGlueOrderDto dto);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids
     */
    void deleteGlueOrder(Long[] ids);

    /**
     * 导入数据
     */
    public AjaxResult importData(List<TcGlueOrderDto> list, boolean updateSupport, Long importLogId);

}
