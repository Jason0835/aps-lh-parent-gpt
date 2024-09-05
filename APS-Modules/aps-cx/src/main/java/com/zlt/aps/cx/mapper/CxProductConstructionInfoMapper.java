package com.zlt.aps.cx.mapper;

import com.zlt.aps.cx.api.domain.dto.CxProductConstructionInfoDto;
import com.zlt.aps.cx.api.domain.entity.CxHalfPartConversion;
import com.zlt.aps.cx.api.domain.entity.CxProductConstructionInfo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 投产施工信息Mapper接口
 * 
 * @author zlt
 * @date 2021-12-02
 */
public interface CxProductConstructionInfoMapper 
{
    /**
     * 查询投产施工信息
     * 
     * @param id 投产施工信息ID
     * @return 投产施工信息
     */
    public CxProductConstructionInfo selectCxProductConstructionInfoById(Long id);

    /**
     * 查询投产施工信息列表
     * 
     * @param cxProductConstructionInfo 投产施工信息
     * @return 投产施工信息集合
     */
    public List<CxProductConstructionInfo> selectCxProductConstructionInfoList(CxProductConstructionInfo cxProductConstructionInfo);

    public List<CxProductConstructionInfo> selectCxScheduleMongthPlan(Long[] ids);

    public List<CxProductConstructionInfo> checkCxProductConstructionInfoUnique(CxProductConstructionInfo cxProductConstructionInfo);

    //获取胎胚版本列表
    public List<CxProductConstructionInfo> getEmbryoVersions(CxProductConstructionInfo cxProductConstructionInfo);

    /**
     * 新增投产施工信息
     * 
     * @param cxProductConstructionInfo 投产施工信息
     * @return 结果
     */
    public int insertCxProductConstructionInfo(CxProductConstructionInfo cxProductConstructionInfo);

    /**
     * 修改投产施工信息
     * 
     * @param cxProductConstructionInfo 投产施工信息
     * @return 结果
     */
    public int updateCxProductConstructionInfo(CxProductConstructionInfo cxProductConstructionInfo);
    public int updateCxProductConstructionInfo2(CxProductConstructionInfo cxProductConstructionInfo);

    public int updateProductionStage(CxProductConstructionInfo cxProductConstructionInfo);


    /**
     * 删除投产施工信息
     * 
     * @param id 投产施工信息ID
     * @return 结果
     */
    public int deleteCxProductConstructionInfoById(Long id);

    /**
     * 批量删除投产施工信息
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxProductConstructionInfoByIds(Long[] ids);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    public void mergeSql(List<CxProductConstructionInfo> list);
    
	/**
	 * 查询指定工序指定物料的施工信息
	 * 
	 * @param procedureType    工序类型
	 * @param materialCodeList 物料编号列表
	 * @return
	 */
	List<CxProductConstructionInfoDto> selectProcedureConstructionList(@Param("procedureType") String procedureType,
			@Param("materialCodeList") List<String> materialCodeList);
    
	/**
	 * 查询指定工序指定物料的施工信息
	 * 
	 * @param materialCodeList 物料编号列表
	 * @return
	 */
	List<CxProductConstructionInfoDto> selectLhConstructionList(@Param("materialCodeList") List<String> materialCodeList);
	
	/**
	 * 半部件规则换算，将胎胚数换算成各半部件数量
	 * 
	 * @param embryoCode     胎胚号
	 * @param bomDataVersion 施工版本
	 * @param queryPlan      胎胚数量
	 * @param scheduleDate   排产日期
	 * @return
	 */
	List<CxHalfPartConversion> conversionHalfPartPlan(@Param("embryoCode") String embryoCode,
			@Param("bomDataVersion") String bomDataVersion, @Param("queryPlan") Long queryPlan,
			@Param("scheduleDate") Date scheduleDate);
	
	/**
	 * 查询所有半部件工序机台的机台信息
	 * @return
	 */
	List<CxHalfPartConversion> listAllHalfPartMachine();

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
