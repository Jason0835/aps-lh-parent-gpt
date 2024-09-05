package com.zlt.aps.cd15.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.dto.Cd15MachineRollMappingDto;
import com.zlt.aps.cd15.entity.Cd15MachineRollMapping;

import java.util.List;

/**
 * <p>
 * 钢带大卷与机台的映射表 服务类
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-15
 */
public interface Cd15MachineRollMappingService extends IService<Cd15MachineRollMapping> {
    /**
     * 根据条件查询钢带大卷与机台的映射表
     *
     * @return
     */
    List<Cd15MachineRollMappingDto> listMachineRollMapping(Cd15MachineRollMappingDto dto);

    /**
     * 保存钢带大卷与机台的映射表（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    void saveMachineRollMapping(Cd15MachineRollMapping entity);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids
     */
    void deleteMachineRollMapping(Long[] ids);

    /**
     * 根据大卷编号判断钢带大卷与机台的映射表
     */
    String checkMachineRollMapping(Cd15MachineRollMappingDto dto);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<Cd15MachineRollMappingDto> list, boolean updateSupport, Long importLogId);

    void deleteAll();
}
