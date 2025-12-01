package com.zlt.mix.setting.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import org.apache.ibatis.annotations.Param;
import com.zlt.mix.setting.api.domain.entity.RemindSetting;

/**
 * 提醒设备Mapper接口
 *
 * @author Gim
 * @date 2022-03-23
 */
public interface RemindSettingMapper extends BaseMapper<RemindSetting> {

    /**
     * 查询提醒设备列表
     *
     * @param remindSetting 提醒设备
     * @return 提醒设备集合
     */
    List<RemindSetting> selectRemindSettingList(RemindSetting remindSetting);

    /**
     * 批量删除提醒设备
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteRemindSettingByIds(Long[] ids);

    /**
     * 查询出导入的数据中，在系统中已经存在的数据
     *
     * @param list        导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listRemindSettingNotUnique(@Param("importList") List<RemindSetting> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);

    /**
     * 批量新增
     *
     * @param list
     */
    void batchInsertRemindSettingInfo(@Param("list") List<RemindSetting> list);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    void mergeSql(List<RemindSetting> list);
}
