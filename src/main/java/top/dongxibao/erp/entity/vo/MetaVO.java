package top.dongxibao.erp.entity.vo;

import lombok.Data;

/**
 * 路由显示信息
 *
 *@Author Dongxibao
 *@Date 2021/1/21
 */
@Data
public class MetaVO {
    /**
     * 设置该路由在侧边栏和面包屑中展示的名字
     */
    private String title;

    /**
     * 设置该路由的图标，对应路径下svg
     */
    private String icon;

    /**
     * 设置为true，则不会被 <keep-alive>缓存
     */
    private boolean noCache;

    public MetaVO() {
    }

    public MetaVO(String title, String icon) {
        this.title = title;
        this.icon = icon;
    }

    public MetaVO(String title, String icon, boolean noCache) {
        this.title = title;
        this.icon = icon;
        this.noCache = noCache;
    }
}
