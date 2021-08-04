package com.changgou.search.controller;

import com.changgou.pojo.SkuInfo;
import com.changgou.search.feign.SkuFeign;
import entity.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequestMapping(value = "/search")
public class SkuController {

    @Autowired
    private SkuFeign skuFeign;

    /**
     * 搜索
     * @param searchMap
     * @return
     */
    @GetMapping(value = "/list")
    public String search(@RequestParam(required = false) Map searchMap, Model model){
        //调用changgou-service-search微服务
        Map resultMap = skuFeign.search(searchMap);
        model.addAttribute("result",resultMap);

        //计算分页
        Page<SkuInfo> pageInfo =new Page<SkuInfo>(
            Long.parseLong(resultMap.get("total").toString()),
        Integer.parseInt(resultMap.get("pageNumber").toString())+1,
            Integer.parseInt(resultMap.get("pageSize").toString())
        );
        model.addAttribute("pageInfo",pageInfo);


        //将条件存储用于页面回显数据
        model.addAttribute("searchMap",searchMap);

        //获取上次请求的地址
        String[] urls = url(searchMap);
        model.addAttribute("url",urls[0]);
        model.addAttribute("sorturl",urls[1]);
        return "search";
    }

    /**
     * 页面需要再这次请求的基础上拼接额外的条件
     * @param searchMap
     * @return
     */
    public String[] url(Map<String,String> searchMap){
        String url="/search/list"; //初始化地址
        String sorturl="/search/list"; //排序地址

        //拼接搜索条件
        if(searchMap!=null&&searchMap.size()>0){
            url+="?";
            sorturl+="?";
            for (Map.Entry<String, String> entry : searchMap.entrySet()) {
                //key是搜索条件对象
                String key = entry.getKey();
                //跳过分页参数
                if(key.equalsIgnoreCase("pageNum")){
                    continue;
                }


                //value是搜索的值
                String value = entry.getValue();
                url += key+"="+value+"&";





                //排序参数，跳过
                if(key.equalsIgnoreCase("sortField")||key.equalsIgnoreCase("sortRule")){
                    continue;
                }
                sorturl +=key+"="+value+"&";
            }


            //去电最后一个&
            url=url.substring(0,url.length()-1);
            sorturl=sorturl.substring(0,sorturl.length()-1);


        }
        return new String[]{url,sorturl};
    }
}
