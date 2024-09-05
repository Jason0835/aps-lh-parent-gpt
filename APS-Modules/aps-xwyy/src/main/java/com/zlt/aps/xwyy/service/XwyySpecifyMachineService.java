package com.zlt.aps.xwyy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.xwyy.api.domain.dto.XwyySpecifyMachineDto;
import com.zlt.aps.xwyy.entity.XwyySpecifyMachine;

import java.util.List;

/**
 * <p>
 * 纤维压延定点机台表 服务类
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface XwyySpecifyMachineService extends IService<XwyySpecifyMachine> {

    /**
     * 根据条件查询定点机台列表
     *
     * @return
     */
    List<XwyySpecifyMachineDto> listSpecifyMachine(XwyySpecifyMachineDto dto);

    /**
     * 保存定点机台信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    void saveSpecifyMachine(XwyySpecifyMachine entity);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    void deleteSpecifyMachine(Long[] ids);

    /**
     * 导入数据
     */
    AjaxResult importData(List<XwyySpecifyMachineDto> list, boolean updateSupport, Long importLogId);

    /**
     * 删除全部定点机台数据
     */
    void deleteAllSpecifyMachine();
}
