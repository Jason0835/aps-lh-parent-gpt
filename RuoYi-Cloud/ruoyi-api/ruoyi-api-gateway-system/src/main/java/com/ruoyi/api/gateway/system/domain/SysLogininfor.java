package com.ruoyi.api.gateway.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.annotation.Excel.ColumnType;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 系统访问记录表 sys_logininfor
 * 
 * @author ruoyi
 */
public class SysLogininfor extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    @Excel(name = "api.logininfo.columnname.orderno", cellType = ColumnType.NUMERIC)
    private Long infoId;

    /** 用户账号 */
    @Excel(name = "api.logininfo.columnname.loginName")
    private String userName;

    /** 状态 0成功 1失败 */
    @Excel(name = "api.logininfo.columnname.status", readConverterExp = "api.logininfo.status.desc")
    private String status;

    /** 地址 */
    @Excel(name = "api.logininfo.columnname.addr")
    private String ipaddr;

    /** 描述 */
    @Excel(name = "api.logininfo.columnname.detail")
    private String msg;

    /** 访问时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "api.logininfo.columnname.accessTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date accessTime;

    //查询条件
    private String  beginTime;
    private String  endTime;

    public String getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(String beginTime) {
        this.beginTime = beginTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Long getInfoId()
    {
        return infoId;
    }

    public void setInfoId(Long infoId)
    {
        this.infoId = infoId;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getIpaddr()
    {
        return ipaddr;
    }

    public void setIpaddr(String ipaddr)
    {
        this.ipaddr = ipaddr;
    }

    public String getMsg()
    {
        return msg;
    }

    public void setMsg(String msg)
    {
        this.msg = msg;
    }

    public Date getAccessTime()
    {
        return accessTime;
    }

    public void setAccessTime(Date accessTime)
    {
        this.accessTime = accessTime;
    }
}