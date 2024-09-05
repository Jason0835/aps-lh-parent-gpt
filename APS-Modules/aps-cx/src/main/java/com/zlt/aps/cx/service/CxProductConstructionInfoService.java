package com.zlt.aps.cx.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.entity.CxHalfPartConversion;
import com.zlt.aps.cx.api.domain.entity.CxProductConstructionInfo;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 投产施工信息Service接口
 * 
 * @author zlt
 * @date 2021-12-02
 */
public interface CxProductConstructionInfoService
{
    /**
     * 查询投产施工信息
     * 
     * @param id 投产施工信息ID
     * @return 投产施工信息
     */
    public CxProductConstructionInfo selectCxProductConstructionInfoById(Long id);

    public List<CxProductConstructionInfo> selectCxScheduleMongthPlan(Long[] ids);

    /**
     * 查询投产施工信息列表
     * 
     * @param cxProductConstructionInfo 投产施工信息
     * @return 投产施工信息集合
     */
    public List<CxProductConstructionInfo> selectCxProductConstructionInfoList(CxProductConstructionInfo cxProductConstructionInfo);

    /**
     * 新增投产施工信息
     * 
     * @param cxProductConstructionInfo 投产施工信息
     * @return 结果
     */
    @Transactional
    public int insertCxProductConstructionInfo(CxProductConstructionInfo cxProductConstructionInfo);

    /**
     * 修改投产施工信息
     * 
     * @param cxProductConstructionInfo 投产施工信息
     * @return 结果
     */
    @Transactional
    public int updateCxProductConstructionInfo(CxProductConstructionInfo cxProductConstructionInfo);
    public int updateCxProductConstructionInfo2(CxProductConstructionInfo cxProductConstructionInfo);

    public int updateProductionStage(CxProductConstructionInfo cxProductConstructionInfo);

    /**
     * 批量删除投产施工信息
     * 
     * @param ids 需要删除的投产施工信息ID
     * @return 结果
     */
    @Transactional
    public int deleteCxProductConstructionInfoByIds(Long[] ids);

    /**
     * 删除投产施工信息信息
     * 
     * @param id 投产施工信息ID
     * @return 结果
     */
    @Transactional
    public int deleteCxProductConstructionInfoById(Long id);

    /**
     * 校验投产施工信息唯一性
     */
    public String checkCxProductConstructionInfoUnique(CxProductConstructionInfo cxProductConstructionInfo);

    /**
     * 导入投产施工信息数据
     */
    @Transactional
    public AjaxResult importData(List<CxProductConstructionInfo> list, boolean updateSupport, Long importLogId);

    /**
     * 获取胎胚版本列表
     * @param pc
     * @return
     */
    public List<CxProductConstructionInfo> getEmbryoVersions(CxProductConstructionInfo pc);

    /**
     * 投产施工调用月度汇总重算
     */
    void reCalculateCauseConstructionChange();

	/**
	 * 生成指定工序相关的施工信息excel的字节数组
	 * 
	 * @param procedureType    工序类型
	 * @param materialCodeList 物料编号列表
	 * @return
	 */
    byte[] createProcedureConstructionExcel(String procedureType, List<String> materialCodeList);

	/**
	 * 半部件规则换算，将胎胚数换算成各半部件数量
	 * 
	 * @param queryParams
	 * @return
	 */
	List<CxHalfPartConversion> conversionHalfPartPlan(CxHalfPartConversion queryParams);

    /**
     * 根据排程日期、半部件类型、半部件编码，查询排程表是否有对应排程，有则返回排程id
     * @param queryParams 查询参数
     * @return 查询到的排程id
     */
	Long getScheduleResultByParams(CxHalfPartConversion queryParams);

    /**
     * 根据半部件类型代号查询对应的机台信息
     * @param queryParams 半部件类型代号
     * @return 机台id和机台名称
     */
    List<CxHalfPartConversion> getMachineInfoListByHalfPartType(CxHalfPartConversion queryParams);
}
