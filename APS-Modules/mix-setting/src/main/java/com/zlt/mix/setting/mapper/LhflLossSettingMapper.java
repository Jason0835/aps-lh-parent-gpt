package com.zlt.mix.setting.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.setting.api.domain.entity.LhflLossSetting;
import org.apache.ibatis.annotations.Param;

/**
 * 硫磺辅料耗损率设定Mapper接口
 * 
 * @author Joran.zhang
 * @date 2022-05-23
 */
public interface LhflLossSettingMapper extends BaseMapper<LhflLossSetting> {

    /**
     * 查询硫磺辅料耗损率设定列表
     * 
     * @param lhflLossrateSetting 硫磺辅料耗损率设定
     * @return 硫磺辅料耗损率设定集合
     */
    List<LhflLossSetting> selectLhflLossSettingList(LhflLossSetting lhflLossrateSetting);

    /**
     * 批量删除硫磺辅料耗损率设定
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteLhflLossSettingByIds(Long[] ids);

    /**
     * 查询出导入的数据中，在系统中已经存在的数据
     * @param list 导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listLhflLossSettingNotUnique(@Param("importList") List<LhflLossSetting> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail, @Param("createBy") String createBy);

    /**
     * 批量新增
     * @param list
     */
    void batchInsertLhflLossSettingInfo(@Param("list") List<LhflLossSetting> list);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    void mergeSql(List<LhflLossSetting> list);

    /**
     * 通过机台名称批量查询出机台编号
     * 为避免在使用右连接时，oracle优化引擎可能会导致顺序改变，指定了正确的顺序字段进行排序
     *
     * @param list 导入的数据列表
     * @return 机台名称列表
     */
    List<String> selectMachineCodeList(@Param("importList") List<LhflLossSetting> list);
}
