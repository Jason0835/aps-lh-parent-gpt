package com.zlt.aps.gdyy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gdyy.api.domain.dto.GdyySpecifyMachineDto;
import com.zlt.aps.gdyy.entity.GdyySpecifyMachine;

import java.util.List;

/**
 * <p>
 * 钢带压延定点机台表 服务类
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface GdyySpecifyMachineService extends IService<GdyySpecifyMachine> {

    /**
     * 根据条件查询定点机台列表
     *
     * @return
     */
    List<GdyySpecifyMachineDto> listSpecifyMachine(GdyySpecifyMachineDto dto);

    /**
     * 保存定点机台信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    void saveSpecifyMachine(GdyySpecifyMachine entity);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    void deleteSpecifyMachine(Long[] ids);

    /**
     * 导入数据
     */
    AjaxResult importData(List<GdyySpecifyMachineDto> list, boolean updateSupport, Long importLogId);

    /**
     * 删除全部定点机台数据
     */
    void deleteAllSpecifyMachine();
}
