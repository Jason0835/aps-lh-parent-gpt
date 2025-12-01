package com.zlt.mix.setting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.setting.api.domain.entity.SettingGlueWorkmanship;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 分厂胶料工艺信息 Mapper接口
 *
 * @author Liam
 * @date 2022-03-18
 */
public interface SettingGlueWorkmanshipMapper extends BaseMapper<SettingGlueWorkmanship> {

    /**
     * 合并记录（有则更新，无则插入）
     *
     * @param importList 分厂胶料工艺信息列表
     */
    void mergeSql(List<SettingGlueWorkmanship> importList);

    /**
     * 查询已经存在的数据
     *
     * @param importList  分厂胶料工艺信息列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错误日志明细
     * @param createBy    创建人
     * @return 错误日志记录列表
     */
    List<ImportErrorLog> listFormulaInfoNotUnique(@Param("importList") List<SettingGlueWorkmanship> importList,
                                                  @Param("importLogId") Long importLogId,
                                                  @Param("errorDetail") String errorDetail,
                                                  @Param("createBy") String createBy);

    /**
     * 批量插入
     *
     * @param importList 分厂胶料工艺信息列表
     */
    void batchInsertFormulaInfo(List<SettingGlueWorkmanship> importList);

    /**
     * 获取分厂胶料工艺信息列表
     *
     * @param entity 分厂胶料工艺信息
     * @return 分厂胶料工艺信息列表
     */
    List<SettingGlueWorkmanship> selectSettingGlueWorkmanshipList(SettingGlueWorkmanship entity);
}
