package com.zlt.aps.tm.mapper;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 胎面机台信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-28
 */
public interface TmMachineInfoMapper {
    /**
     * 查询胎面机台信息
     *
     * @param id 胎面机台信息ID
     * @return 胎面机台信息
     */
    public TmMachineInfo selectMachineInfoById(Long id);

    /**
     * 查询胎面机台信息列表
     *
     * @param machineInfo 胎面机台信息
     * @return 胎面机台信息集合
     */
    public List<TmMachineInfo> selectMachineInfoList(TmMachineInfo machineInfo);

    /**
     * 新增胎面机台信息
     *
     * @param machineInfo 胎面机台信息
     * @return 结果
     */
    public int insertMachineInfo(TmMachineInfo machineInfo);

    /**
     * 修改胎面机台信息
     *
     * @param machineInfo 胎面机台信息
     * @return 结果
     */
    public int updateMachineInfo(TmMachineInfo machineInfo);

    /**
     * 删除胎面机台信息
     *
     * @param id 胎面机台信息ID
     * @return 结果
     */
    public int deleteMachineInfoById(Long id);

    /**
     * 批量删除胎面机台信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMachineInfoByIds(Long[] ids);

    /**
     * 校验胎面机台唯一性
     */
    public List<TmMachineInfo> checkMachineCodeUnique(TmMachineInfo machineInfo);

    /**
     * 根据胎面和口型板获取对应机台信息
     *
     * @param machineInfo 胎面机台信息
     * @return 胎面机台信息集合
     */
    public List<TmMachineInfo> selectMachineInfoList2(TmMachineInfo machineInfo);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<TmMachineInfo> list);

    /**
     * 查询出导入的数据中，在系统中已经存在的数据
     * @param list 导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @return
     */
    List<ImportErrorLog> listMachineCodeNotUnique(@Param("importList") List<TmMachineInfo> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail,
                                                  @Param("createBy") String createBy);

    /**
     * 查询出导入的数据中，在系统中已经存在的数据
     * @param list 导入的数据列表
     * @param importLogId 导入错误日志id
     * @param errorDetail 导入错入日志明细
     * @param createBy
     * @param updateSupport 已存在是否更新
     * @return
     */
    List<ImportErrorLog> listMachineNameNotUnique(@Param("importList") List<TmMachineInfo> list, @Param("importLogId") Long importLogId, @Param("errorDetail") String errorDetail,
                                                  @Param("createBy") String createBy, @Param("updateSupport")  boolean updateSupport);

    /**
     * 批量导入
     * @param list
     */
    void batchInsertMachineInfo(@Param("list") List<TmMachineInfo> list);
}
