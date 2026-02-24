package com.zlt.aps.mdm.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mdm.api.domain.entity.MdmMoldingMachineStatus;
import com.zlt.aps.mdm.api.domain.vo.CopyParamVo;
import com.zlt.aps.mdm.api.domain.vo.MdmMoldingMachineStatusVo;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IMdmMoldingMachineStatusService {

    /**
     * 查询基础数据-成型机可用信息
     *
     * @param id 基础数据-成型机可用信息ID
     * @return 基础数据-成型机可用信息
     */
    public MdmMoldingMachineStatus selectDocMoldingMachineStatusById(Long id);

    /**
     * 查询基础数据-成型机可用信息列表
     *
     * @param docMoldingMachineStatus 基础数据-成型机可用信息
     * @return 基础数据-成型机可用信息集合
     */
    public List<MdmMoldingMachineStatusVo> selectDocMoldingMachineStatusList(MdmMoldingMachineStatus docMoldingMachineStatus);

    /**
     * 新增基础数据-成型机可用信息
     *
     * @param docMoldingMachineStatus 基础数据-成型机可用信息
     * @return 结果
     */
    @Transactional
    public int insertDocMoldingMachineStatus(MdmMoldingMachineStatus docMoldingMachineStatus);

    /**
     * 修改基础数据-成型机可用信息
     *
     * @param ids    要修改状态的id
     * @param status 修改的状态
     * @return 结果
     */
    @Transactional
    public int updateDocMoldingMachineStatus(Long[] ids, String status);

    /**
     * 批量删除基础数据-成型机可用信息
     *
     * @param ids 需要删除的基础数据-成型机可用信息ID
     * @return 结果
     */
    @Transactional
    public int deleteDocMoldingMachineStatusByIds(Long[] ids);

    /**
     * 校验基础数据-成型机可用信息唯一性
     */
    public String checkDocMoldingMachineStatusUnique(MdmMoldingMachineStatus docMoldingMachineStatus);

    /**
     * 导入基础数据-成型机可用信息数据
     */
    @Transactional
    public AjaxResult importData(List<MdmMoldingMachineStatusVo> list, boolean updateSupport, Long importLogId);

    /**
     * 拷贝指定年月的数据到指定年月
     *
     * @param params 指定的数据源年月和指定要拷贝到的年月
     * @return 结果
     */
    public int copyMoldingMachineStatus(CopyParamVo params);

    /**
     * 合并指定年月的数据到指定年月
     *
     * @param params 指定的数据源年月和指定要拷贝到的年月
     * @return 结果
     */
    public int mergeMoldingMachineStatus(CopyParamVo params);


    /**
     * 从档案表生产数据到指定年月
     *
     * @param params 指定年月
     * @return 结果
     */
    public int generateMoldingMachineStatus(CopyParamVo params);
}
