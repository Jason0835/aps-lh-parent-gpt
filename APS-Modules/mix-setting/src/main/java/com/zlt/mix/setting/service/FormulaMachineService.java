package com.zlt.mix.setting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.setting.api.domain.entity.FormulaMachine;

import java.util.ArrayList;
import java.util.List;

/**
 * 配方与机台对应Service接口
 * 
 * @author Gim
 * @date 2022-03-28
 */
public interface FormulaMachineService  extends IService<FormulaMachine>
{
    /**
     * 查询配方与机台对应列表
     * 
     * @param formulaMachine 配方与机台对应
     * @return 配方与机台对应集合
     */
    List<FormulaMachine> selectFormulaMachineList(FormulaMachine formulaMachine);

    /**
     * 保存配方与机台对应信息（id为空则新增，id不为空则修改）
     *
     * @param formulaMachine
     */
    void saveFormulaMachine(FormulaMachine formulaMachine);

    /**
     * 批量删除配方与机台对应
     * 
     * @param ids 需要删除的配方与机台对应ID
     * @return 结果
     */
    int deleteFormulaMachineByIds(Long[] ids);

    /**
     * 校验配方与机台对应唯一性
     */
    String checkFormulaMachineUnique(FormulaMachine formulaMachine);

    /**
     * 导入配方与机台对应数据
     */
    AjaxResult importData(List<FormulaMachine> list, boolean updateSupport, Long importLogId);

    /**
     * 根据机台名称和胶料名称进行精确查询
     *
     * @param machine
     * @return
     */
    List<FormulaMachine> selectExactFormulaMachineList(FormulaMachine machine);

    /**
     * 查询所有配方与机台对应关系
     * @param machine 参数
     * @return 查询到的集合
     */
    ArrayList<FormulaMachine> getFormulaMachineList(FormulaMachine machine);

    /**
     * 根据机台名称和胶料名称进行精确查询
     *
     * @param machine
     * @return
     */
    ArrayList<FormulaMachine> selectRecipeMachineList(FormulaMachine machine);
}
