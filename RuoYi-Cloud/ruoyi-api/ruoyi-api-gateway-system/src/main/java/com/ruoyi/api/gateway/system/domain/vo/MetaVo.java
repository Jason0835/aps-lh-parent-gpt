package com.ruoyi.api.gateway.system.domain.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 路由显示信息
 * 
 * @author ruoyi
 */
@Getter
@Setter
public class MetaVo
{
    /**
     * 设置该路由在侧边栏和面包屑中展示的名字
     */
    private String title;

    /**
     * 设置该路由的图标，对应路径src/assets/icons/svg
     */
    private String icon;

    private String btIcon;

    /**
     * 设置为true，则不会被 <keep-alive>缓存
     */
    private boolean noCache;

    public MetaVo()
    {
    }

    public MetaVo(String title, String icon, String btIcon)
    {
        this.title = title;
        this.icon = icon;
        this.btIcon = btIcon;
    }

    public MetaVo(String title, String icon, boolean noCache, String btIcon)
    {
        this.title = title;
        this.icon = icon;
        this.noCache = noCache;
        this.btIcon = btIcon;
    }

    public boolean isNoCache()
    {
        return noCache;
    }

    public void setNoCache(boolean noCache)
    {
        this.noCache = noCache;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getIcon()
    {
        return icon;
    }

    public void setIcon(String icon)
    {
        this.icon = icon;
    }
}
