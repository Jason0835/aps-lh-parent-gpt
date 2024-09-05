package com.zlt.aps.cd90.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.dto.Cd90SpecifyMachineDto;
import com.zlt.aps.cd90.entity.Cd90SpecifyMachine;

import java.util.List;

/**
 * <p>
 * 90度裁断定点机台表 服务类
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface Cd90SpecifyMachineService extends IService<Cd90SpecifyMachine> {

    /**
     * 根据条件查询定点机台列表
     *
     * @return
     */
    List<Cd90SpecifyMachineDto> listSpecifyMachine(Cd90SpecifyMachineDto dto);

    /**
     * 保存定点机台信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    void saveSpecifyMachine(Cd90SpecifyMachine entity);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    void deleteSpecifyMachine(Long[] ids);

    /**
     * 导入数据
     */
    AjaxResult importData(List<Cd90SpecifyMachineDto> list, boolean updateSupport, Long importLogId);

    /**
     * 删除全部定点机台数据
     */
    void deleteAllSpecifyMachine();
}
