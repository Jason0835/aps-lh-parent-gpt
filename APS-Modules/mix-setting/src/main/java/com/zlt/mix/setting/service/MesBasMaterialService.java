package com.zlt.mix.setting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.setting.api.domain.entity.MesBasMaterial;

import java.util.List;

/**
 * 物料Service接口
 *
 * @author Joran.zhang
 * @date 2022-05-30
 */
public interface MesBasMaterialService extends IService<MesBasMaterial> {
    /**
     * 查询物料列表
     *
     * @param mesBasMaterial 物料
     * @return 物料集合
     */
    List<MesBasMaterial> selectMesBasMaterialList(MesBasMaterial mesBasMaterial);

    /**
     * 保存物料信息（id为空则新增，id不为空则修改）
     *
     * @param mesBasMaterial
     */
    void saveMesBasMaterial(MesBasMaterial mesBasMaterial);

    /**
     * 批量删除物料
     * 
     * @param ids 需要删除的物料ID
     * @return 结果
     */
    int deleteMesBasMaterialByIds(Long[] ids);

    /**
     * 校验物料唯一性
     */
    String checkMesBasMaterialUnique(MesBasMaterial mesBasMaterial);

    /**
     * 导入物料数据
     */
    AjaxResult importData(List<MesBasMaterial> list, boolean updateSupport, Long importLogId);

    /**
     * 根据物料大类列表查询物料名称列表
     *
     * @param majorTypes 物料大类列表
     * @return 物料名称列表
     */
    List<String> listMesBasMaterial(List<Integer> majorTypes);
}
