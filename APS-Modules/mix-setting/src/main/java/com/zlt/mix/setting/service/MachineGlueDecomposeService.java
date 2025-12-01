package com.zlt.mix.setting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.setting.api.domain.dto.MachineGlueDecomposeDto;
import com.zlt.mix.setting.api.domain.entity.MachineGlueDecompose;

import java.util.List;

/**
 * 密炼机指定胶料分解Service接口
 *
 * @author Liam
 * @date 2022-03-29
 */
public interface MachineGlueDecomposeService extends IService<MachineGlueDecompose> {

    /**
     * 保存密炼机指定胶料分解信息（id为空则新增，id不为空则修改）
     *
     * @param machineGlueDecompose
     */
    void saveMachineGlueDecompose(MachineGlueDecompose machineGlueDecompose);

    /**
     * 批量删除密炼机指定胶料分解
     *
     * @param ids 需要删除的密炼机指定胶料分解ID
     * @return 结果
     */
    int deleteMachineGlueDecomposeByIds(Long[] ids);

    /**
     * 校验密炼机指定胶料分解唯一性
     */
    String checkMachineGlueDecomposeUnique(MachineGlueDecompose machineGlueDecompose);

    /**
     * 导入密炼机指定胶料分解数据
     */
    AjaxResult importData(List<MachineGlueDecomposeDto> list, boolean updateSupport, Long importLogId);

    /**
     * 查询密炼机指定胶料分解列表(级联查询机台名称)
     */
    List<MachineGlueDecomposeDto> selectMachineGlueDecomposeListCascade(MachineGlueDecompose machineGlueDecompose);

    /**
     * 获取密炼机指定胶料分解详细信息(级联查询机台名称)
     */
    MachineGlueDecomposeDto getByIdCascade(Long id);
}
