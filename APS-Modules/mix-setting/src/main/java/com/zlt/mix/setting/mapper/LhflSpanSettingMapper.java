package com.zlt.mix.setting.mapper;

import java.util.List;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import org.apache.ibatis.annotations.Param;
import com.zlt.mix.setting.api.domain.entity.LhflSpanSetting;

/**
 * 硫磺辅料跨区设置Mapper接口
 * 
 * @author chen
 * @date 2022-08-12
 */
public interface LhflSpanSettingMapper extends BaseMapper<LhflSpanSetting> {

    /**
     * 查询硫磺辅料跨区设置列表
     * 
     * @param lhflSpanSetting 硫磺辅料跨区设置
     * @return 硫磺辅料跨区设置集合
     */
    List<LhflSpanSetting> selectLhflSpanSettingList(LhflSpanSetting lhflSpanSetting);

    /**
     * 批量删除硫磺辅料跨区设置
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteLhflSpanSettingByIds(Long[] ids);

    /**
     * 查询出导入的数据中，在系统中已经存在的数据
     * @param list 导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listLhflSpanSettingNotUnique(@Param("importList") List<LhflSpanSetting> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);

    /**
     * 批量新增
     * @param list
     */
    void batchInsertLhflSpanSettingInfo(@Param("list") List<LhflSpanSetting> list);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    void mergeSql(List<LhflSpanSetting> list);
}
