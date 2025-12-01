package com.zlt.mix.setting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.setting.api.domain.entity.SettingFormulaInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Liam
 * @date 2022-03-22
 */
public interface SettingFormulaInfoMapper extends BaseMapper<SettingFormulaInfo> {
    /**
     * 合并操作，有则更新，无则插入
     *
     * @param importList 配方信息列表
     */
    void mergeSql(List<SettingFormulaInfo> importList);

    /**
     * 查询已经存在的数据
     *
     * @param list        配方信息列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错误日志明细
     * @param createBy    创建人
     * @return 错误日志记录列表
     */
    List<ImportErrorLog> listFormulaInfoNotUnique(@Param("importList") List<SettingFormulaInfo> list,
                                                  @Param("importLogId") Long importLogId,
                                                  @Param("errorDetail") String errorDetail,
                                                  @Param("createBy") String createBy);

    /**
     * 批量插入
     *
     * @param importList 配方信息列表
     */
    void batchInsertFormulaInfo(List<SettingFormulaInfo> importList);

    /**
     * 查询配方信息列表
     *
     * @param entity 配方信息
     * @return 配方信息列表
     */
    List<SettingFormulaInfo> selectSettingFormulaInfoList(SettingFormulaInfo entity);

    int deleteBatchByIds(List<Long> list);
}
