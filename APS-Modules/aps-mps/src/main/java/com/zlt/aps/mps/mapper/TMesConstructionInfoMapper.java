package com.zlt.aps.mps.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.aps.mps.domain.LhTireConstructionInfo;
import com.zlt.aps.mps.domain.TMesConstructionInfo;
import com.zlt.aps.mps.domain.TMesConstructionMapping;
import com.zlt.aps.mps.domain.TMesConstructionParam;
import com.zlt.aps.mps.domain.TMesPlmBomInfo;

/**
 * 施工标参数数据库操作接口
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-9-13 14:17:52
 */
public interface TMesConstructionInfoMapper {
	/**
	 * 查询施工表字段映射配置列表
	 * 
	 * @return
	 */
	List<TMesConstructionMapping> selectTPlmBomConstructionMapping();
	
	/**
	 * 取出所有BOM记录
	 * @return
	 */
	List<TMesPlmBomInfo> selectBomInfo();
    
    /**
     * 检查是否已存在施工记录
     * @param param
     * @return
     */
    Integer checkConstructionInfo(TMesConstructionParam param);
    /**
     * 更新施工记录
     * @param param
     * @return
     */
    int updateConstructionInfo(TMesConstructionParam param);
    /**
     * 新增施工记录
     * @param param
     * @return
     */
    int insertConstructionInfo(TMesConstructionParam param);

	/**
	 * 删除bom已废止的胎胚对应的施工版本记录
	 * 
	 * @param dataVersion bom数据版本
	 * @return
	 */
	int deleteConstructionVersionInfo(@Param("dataVersion") String dataVersion);

	/**
	 * 将中间库数据合并至施工表
	 * 
	 * @param constructionList 合并参数对象
	 * @param columnList       施工表待更新的字段名列表
	 * @return
	 */
	int mergeConstructionInfo(@Param("constructionList") List<TMesConstructionParam> constructionList,
			@Param("columnList") List<String> columnList);

	/**
	 * 从bom和Plm中获取本次更新版本需要处理的硫化施工表信息
	 * 
	 * @param bomDataVersion bom版本
	 * @param plmDataVersion plm版本
	 * @return
	 */
	List<TMesConstructionInfo> selectLhConstructionInfo(@Param("bomDataVersion") String bomDataVersion,
			@Param("plmDataVersion") String plmDataVersion);

	/**
	 * 合并硫化施工表
	 * 
	 * @param constructionList
	 * @return
	 */
	int mergeLhConstructionInfo(@Param("constructionList") List<LhTireConstructionInfo> constructionList);

	/**
	 * 将指定胎胚bom的施工信息同步到投产施工表
	 * 
	 * @param bomIdList
	 */
//	void mergeProductConstructionInfo(@Param("currentDate") Date currentDate);
//	void mergeProductConstructionInfo(@Param("bomIdList") List<Long> bomIdList);
	void updateProductConstructionInfo();
    void addNewProductConstructionInfo();

	/**
	 * 清除掉已废止的投产施工记录
	 * 
	 * @param embryoCodeList
	 */
	void cleanDisableConstructionInfo(@Param("embryoCodeList") List<String> embryoCodeList);
}
