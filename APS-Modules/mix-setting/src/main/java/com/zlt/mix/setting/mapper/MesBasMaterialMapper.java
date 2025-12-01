package com.zlt.mix.setting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.setting.api.domain.entity.MesBasMaterial;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 物料Mapper接口
 *
 * @author Joran.zhang
 * @date 2022-05-30
 */
public interface MesBasMaterialMapper extends BaseMapper<MesBasMaterial> {

    /**
     * 查询物料列表
     * 
     * @param mesBasMaterial 物料
     * @return 物料集合
     */
    List<MesBasMaterial> selectMesBasMaterialList(MesBasMaterial mesBasMaterial);

    /**
     * 批量删除物料
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteMesBasMaterialByIds(Long[] ids);

    /**
     * 查询出导入的数据中，在系统中已经存在的数据
     * @param list 导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listMesBasMaterialNotUnique(@Param("importList") List<MesBasMaterial> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);

    /**
     * 批量新增
     *
     * @param list
     */
    void batchInsertMesBasMaterialInfo(@Param("list") List<MesBasMaterial> list);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    void mergeSql(List<MesBasMaterial> list);

    /**
     * 根据物料大类列表查询物料名称列表
     *
     * @param majorTypes 物料大类列表
     * @return 物料名称列表
     */
    List<String> listMesBasMaterial(@Param("majorTypes") List<Integer> majorTypes);
}
