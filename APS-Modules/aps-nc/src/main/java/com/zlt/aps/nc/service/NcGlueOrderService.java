package com.zlt.aps.nc.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.nc.api.domain.dto.NcGlueOrderDto;
import com.zlt.aps.nc.entity.NcGlueOrder;

import java.util.List;

/**
 * <p>
 * 内衬胶料顺序维护 服务类
 * </p>
 *
 * @author zhangbinglin
 */
public interface NcGlueOrderService extends IService<NcGlueOrder> {
    /**
     * 根据条件查询胶料顺序列表
     *
     * @return
     */
    List<NcGlueOrderDto> listGlueOrder(NcGlueOrderDto dto);

    /**
     * 保存胶料顺序信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    void saveGlueOrder(NcGlueOrder entity);

    /**
     * 根据code判断胶料号是否已经存在
     */
    String checkGlueCodeUnique(NcGlueOrderDto dto);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids
     */
    void deleteGlueOrder(Long[] ids);

    /**
     * 导入数据，并保存记录
     * @param list 要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId 导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<NcGlueOrderDto> list, boolean updateSupport, Long importLogId);
}
