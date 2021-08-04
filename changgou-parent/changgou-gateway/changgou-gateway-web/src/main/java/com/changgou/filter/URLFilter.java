package com.changgou.filter;

public class URLFilter {

    /**
     * 要放行的路径
     */
    private static final String noAuthorizeurls = "/api/user/add,/api/user/login,/api/weixin/pay/notify/url";


    /**
     * 判断 当前的请求的地址中是否在需要验证权限,如果需要验证true   不需要验证  false
     *
     * @param url 获取到的当前的请求的地址
     * @return
     */
    public static boolean hasAuthorize(String url) {
        String[] urls = noAuthorizeurls.split(",");

        for (String uri : urls) {
            if (url.equals(uri)) {
                return false;
            }
        }
        return true;
    }


}