package com.zlt.aps.cd90.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.dto.Cd90MachineRollMappingDto;
import com.zlt.aps.cd90.entity.Cd90MachineRollMapping;

import java.util.List;

/**
 * <p>
 * 90度裁断帘布大卷与机台的映射表 服务类
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-15
 */
public interface Cd90MachineRollMappingService extends IService<Cd90MachineRollMapping> {
    /**
     * 根据条件查询帘布大卷与机台的映射表
     *
     * @return
     */
    List<Cd90MachineRollMappingDto> listMachineRollMapping(Cd90MachineRollMappingDto dto);

    /**
     * 保存帘布大卷与机台的映射表（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    void saveMachineRollMapping(Cd90MachineRollMapping entity);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids
     */
    void deleteMachineRollMapping(Long[] ids);

    /**
     * 根据大卷编号判断纤维压延帘布大卷与机台的映射表
     */
    String checkMachineRollMapping(Cd90MachineRollMappingDto dto);

    /**
     * 导入数据
     */
    AjaxResult importData(List<Cd90MachineRollMappingDto> list, boolean updateSupport, Long importLogId);

    void deleteAll();
}
