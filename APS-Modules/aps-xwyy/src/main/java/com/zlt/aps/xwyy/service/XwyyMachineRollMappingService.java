package com.zlt.aps.xwyy.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.xwyy.api.domain.dto.XwyyMachineRollMappingDto;
import com.zlt.aps.xwyy.entity.XwyyMachineRollMapping;

import java.util.List;

/**
 * <p>
 * 纤维压延帘布大卷与机台的映射表 服务类
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-15
 */
public interface XwyyMachineRollMappingService extends IService<XwyyMachineRollMapping> {
    /**
     * 根据条件查询纤维压延帘布大卷与机台的映射表
     *
     * @return
     */
    List<XwyyMachineRollMappingDto> listXwyyMachineRollMapping(XwyyMachineRollMappingDto dto);

    /**
     * 保存纤维压延帘布大卷与机台的映射表（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    void saveXwyyMachineRollMapping(XwyyMachineRollMapping entity);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids
     */
    void deleteXwyyMachineRollMapping(Long[] ids);

    /**
     * 根据大卷编号判断纤维压延帘布大卷与机台的映射表
     */
    String checkXwyyMachineRollMapping(XwyyMachineRollMappingDto dto);

    /**
     * 导入数据
     */
    AjaxResult importData(List<XwyyMachineRollMappingDto> list, boolean updateSupport, Long importLogId);
}
