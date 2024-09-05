package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;

import java.util.List;

/**
 * 成型机台信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-25
 */
public interface CxMachineInfoMapper extends BaseMapper<CxMachineInfo> {
    /**
     * 查询成型机台信息
     *
     * @param id 成型机台信息ID
     * @return 成型机台信息
     */
    public CxMachineInfo selectCxMachineInfoById(Long id);

    /**
     * 查询成型机台信息列表
     *
     * @param cxMachineInfo 成型机台信息
     * @return 成型机台信息集合
     */
    public List<CxMachineInfo> selectCxMachineInfoList(CxMachineInfo cxMachineInfo);

    public List<CxMachineInfo> listOrderByName(CxMachineInfo cxMachineInfo);

    public List<CxMachineInfo> selectCxMachineInfoList2(CxMachineInfo cxMachineInfo);

    /**
     * 获取其他半部件机台列表
     * @param cxMachineInfo
     * @return
     */
    public List<CxMachineInfo> getOrtherMachineInfo(CxMachineInfo cxMachineInfo);

    /**
     * 新增成型机台信息
     *
     * @param cxMachineInfo 成型机台信息
     * @return 结果
     */
    public int insertCxMachineInfo(CxMachineInfo cxMachineInfo);

    /**
     * 修改成型机台信息
     *
     * @param cxMachineInfo 成型机台信息
     * @return 结果
     */
    public int updateCxMachineInfo(CxMachineInfo cxMachineInfo);

    /**
     * 删除成型机台信息
     *
     * @param id 成型机台信息ID
     * @return 结果
     */
    public int deleteCxMachineInfoById(Long id);

    /**
     * 批量删除成型机台信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxMachineInfoByIds(Long[] ids);

    /**
     * 校验胎面机台唯一性
     */
    public List<CxMachineInfo> checkMachineCodeUnique(CxMachineInfo cxMachineInfo);
    public List<CxMachineInfo> checkMachineNameUnique(CxMachineInfo cxMachineInfo);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<CxMachineInfo> list);

    /**
     * 通过硫化机编号查询其最大使用模式
     */
    public List<CxMachineInfo> getMaxMoldsByLhMachineCode(CxMachineInfo cxMachineInfo);

    /**
     * 成型排程硫化机台-不可作业/限制作业校验
     * @param cxMachineInfo
     * @return
     */
    public List<CxMachineInfo> getLhSpecimalMachine(CxMachineInfo cxMachineInfo);


}
