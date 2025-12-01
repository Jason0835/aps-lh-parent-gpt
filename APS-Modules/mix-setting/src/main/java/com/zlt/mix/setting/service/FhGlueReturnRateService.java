package com.zlt.mix.setting.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.setting.api.domain.entity.FhGlueReturnRate;

/**
 * 返回胶日返回率Service接口
 * 
 * @author zlt
 * @date 2022-11-28
 */
public interface FhGlueReturnRateService  extends IService<FhGlueReturnRate>
{
    /**
     * 查询返回胶日返回率列表
     * 
     * @param fhGlueReturnRate 返回胶日返回率
     * @return 返回胶日返回率集合
     */
    List<FhGlueReturnRate> selectFhGlueReturnRateList(FhGlueReturnRate fhGlueReturnRate);

    /**
     * 保存返回胶日返回率信息（id为空则新增，id不为空则修改）
     *
     * @param fhGlueReturnRate
     */
    void saveFhGlueReturnRate(FhGlueReturnRate fhGlueReturnRate);

    /**
     * 批量删除返回胶日返回率
     * 
     * @param ids 需要删除的返回胶日返回率ID
     * @return 结果
     */
    int deleteFhGlueReturnRateByIds(Long[] ids);

    /**
     * 校验返回胶日返回率唯一性
     */
    String checkFhGlueReturnRateUnique(FhGlueReturnRate fhGlueReturnRate);

    /**
     * 导入返回胶日返回率数据
     */
    AjaxResult importData(List<FhGlueReturnRate> list, boolean updateSupport, Long importLogId);
}
