package com.zlt.mix.setting.mapper;

import java.util.List;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import org.apache.ibatis.annotations.Param;
import com.zlt.mix.setting.api.domain.entity.GlueSpanSetting;

/**
 * 终炼母炼胶料跨区设置Mapper接口
 * 
 * @author chen
 * @date 2022-08-12
 */
public interface GlueSpanSettingMapper extends BaseMapper<GlueSpanSetting> {

    /**
     * 查询终炼母炼胶料跨区设置列表
     * 
     * @param glueSpanSetting 终炼母炼胶料跨区设置
     * @return 终炼母炼胶料跨区设置集合
     */
    List<GlueSpanSetting> selectGlueSpanSettingList(GlueSpanSetting glueSpanSetting);

    /**
     * 批量删除终炼母炼胶料跨区设置
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteGlueSpanSettingByIds(Long[] ids);

    /**
     * 查询出导入的数据中，在系统中已经存在的数据
     * @param list 导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listGlueSpanSettingNotUnique(@Param("importList") List<GlueSpanSetting> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);

    /**
     * 批量新增
     * @param list
     */
    void batchInsertGlueSpanSettingInfo(@Param("list") List<GlueSpanSetting> list);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    void mergeSql(List<GlueSpanSetting> list);
}
