package com.zlt.aps.tq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tq.api.domain.dto.TqSpecifyMachineDto;
import com.zlt.aps.tq.entity.TqSpecifyMachine;

import java.util.List;

/**
 * <p>
 * 胎圈定点机台表 服务类
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface TqSpecifyMachineService extends IService<TqSpecifyMachine> {

    /**
     * 根据条件查询定点机台列表
     *
     * @return
     */
    List<TqSpecifyMachineDto> listSpecifyMachine(TqSpecifyMachineDto dto);

    /**
     * 保存定点机台信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    void saveSpecifyMachine(TqSpecifyMachine entity);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    void deleteSpecifyMachine(Long[] ids);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<TqSpecifyMachineDto> list, boolean updateSupport, Long importLogId);

    /**
     * 删除全部定点机台数据
     */
    void deleteAllSpecifyMachine();
}
