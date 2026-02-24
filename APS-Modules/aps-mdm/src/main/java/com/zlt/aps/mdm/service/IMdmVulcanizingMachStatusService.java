package com.zlt.aps.mdm.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mdm.api.domain.entity.MdmVulcanizingMachStatus;
import com.zlt.aps.mdm.api.domain.vo.CopyParamVo;
import com.zlt.aps.mdm.api.domain.vo.MdmVulcanizingMachStatusVo;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IMdmVulcanizingMachStatusService {

    /**
     * 查询基础数据-硫化机可用信息
     *
     * @param id 基础数据-硫化机可用信息ID
     * @return 基础数据-硫化机可用信息
     */
    public MdmVulcanizingMachStatus getDocVulcanizingMachStatusEntityById(Long id);

    /**
     * 查询基础数据-硫化机可用信息列表
     *
     * @param entity 基础数据-硫化机可用信息
     * @return 基础数据-硫化机可用信息集合
     */
    public List<MdmVulcanizingMachStatusVo> selectDocVulcanizingMachStatusEntityList(MdmVulcanizingMachStatusVo entity);

    /**
     * 新增基础数据-硫化机可用信息
     *
     * @param entity 基础数据-硫化机可用信息
     * @return 结果
     */
    public int insertDocVulcanizingMachStatusEntity(MdmVulcanizingMachStatusVo entity);

    /**
     * 修改基础数据-硫化机可用信息
     *
     * @return 结果
     */
    public int updateDocVulcanizingMachStatusEntity(Long[] ids, String status);

    /**
     * 批量删除基础数据-硫化机可用信息
     *
     * @param ids 需要删除的基础数据-硫化机可用信息ID
     * @return 结果
     */
    public int deleteDocVulcanizingMachStatusEntityByIds(Long[] ids);

    /**
     * 删除基础数据-硫化机可用信息信息
     *
     * @param id 基础数据-硫化机可用信息ID
     * @return 结果
     */
    public int deleteDocVulcanizingMachStatusEntityById(Long id);

    /**
     * 校验基础数据-硫化机可用信息唯一性
     */
    public String checkDocVulcanizingMachStatusEntityUnique(MdmVulcanizingMachStatusVo entity);

    /**
     * 复制可用台账信息
     */
    public int copyDocVulcanizingMachStatus(CopyParamVo copyParamVo);

    /**
     * 合并可用台账信息
     */
    public int mergeDocVulcanizingMachStatus(CopyParamVo copyParamVo);

    /**
     * 生成可用台账信息
     */
    public int generateDocVulcanizingMachStatus(CopyParamVo params);

    /**
     * 导入基础数据-硫化机可用信息数据
     *
     * @param list          导入集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult importData(List<MdmVulcanizingMachStatusVo> list, boolean updateSupport, Long importLogId);
}
