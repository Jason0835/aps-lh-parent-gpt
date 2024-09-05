package com.zlt.mdm.auth.api.domain;

import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

//TODO:系统数据实体
@Setter
@Getter
public class MdmSystemData extends BaseEntity {

    /**
     * '系统记录ID'
     */
    private String systemId;
    /**
     * 删除标志（0代表存在 2代表删除）
     */
    private String delFlag;
    /**
     * 系统状态（0正常 1停用）
     */
    private String status;
    /**
     * 系统代码
     */
    private String systemCode;
    /**
     * '语言包'
     */
    private String langJson;
    /**
     * '显示顺序'
     */
    private Integer orderNum;
    /**
     * '父级系统显示'
     */
    private Long parentId;
    /**
     * '系统首页URL'
     */
    private String url;
    /**
     * '系统图标'
     */
    private String icon;
    /**
     * '弹出新页面或跳转'
     */
    private String target;

    /**
     * 用来显示的名称，不在实体当中
     */
    private String showName;

}
