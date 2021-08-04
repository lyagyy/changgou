package com.changgou.canal;


import com.alibaba.fastjson.JSON;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.changgou.content.feign.ContentFeign;
import com.changgou.content.pojo.Content;
import com.changgou.item.feign.PageFeign;
import com.xpand.starter.canal.annotation.*;
import entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

@CanalEventListener
public class CanalDataEventListener {

   /* *//**
     * @InsertListenPoint:增加监听 只有增加后的数据
     *  rowData.getAfterColumnsList() :增加、修改
     *  rowData.getBeforeColumnsList()：删除，修改
     * @param eventType 当前操作的类型 增加数据
     * @param rowData 发生变更的一行数据
     *//*
    @InsertListenPoint
    public void onEventInsert(CanalEntry.EventType eventType,CanalEntry.RowData rowData){
        for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
            System.out.println("列名:" + column.getName() + "-------变更的数据:" + column.getValue());
        }
    }

    *//**
     * @InsertListenPoint:修改监听 有修改前后的数据
     *  rowData.getAfterColumnsList() :增加、修改
     *  rowData.getBeforeColumnsList()：删除，修改
     * @param eventType 当前操作的类型 修改数据
     * @param rowData 发生变更的一行数据
     *//*
    @UpdateListenPoint
    public void onEventUpdate(CanalEntry.EventType eventType,CanalEntry.RowData rowData){
        for (CanalEntry.Column column : rowData.getBeforeColumnsList()) {
            System.out.println("列名:" + column.getName() + "-------修改前的数据:" + column.getValue());
        }


        for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
            System.out.println("列名:" + column.getName() + "-------修改后的数据:" + column.getValue());
        }
    }

    *//**
     * @InsertListenPoint:删除监听
     *  rowData.getAfterColumnsList() :增加、修改
     *  rowData.getBeforeColumnsList()：删除，修改
     * @param eventType 当前操作的类型 删除数据
     * @param rowData 发生变更的一行数据
     *//*
    @DeleteListenPoint
    public void onEventDelete(CanalEntry.EventType eventType,CanalEntry.RowData rowData){
        for (CanalEntry.Column column : rowData.getBeforeColumnsList()) {
            System.out.println("列名:" + column.getName() + "-------删除前的数据:" + column.getValue());
        }
    }

    *//**
     * @InsertListenPoint:自定义监听
     *  rowData.getAfterColumnsList() :增加、修改
     *  rowData.getBeforeColumnsList()：删除，修改
     * @param eventType 当前操作的类型 增加数据
     * @param rowData 发生变更的一行数据
     *//*

    @ListenPoint(destination = "example",schema = "changgou_content",table = {"tb_content_category","tb_content"},eventType = {CanalEntry.EventType.DELETE})
    public void onEventCustomUpdate(CanalEntry.EventType eventType,CanalEntry.RowData rowData){
        System.out.println("自定义delete");
        System.out.println("==========");
        for (CanalEntry.Column column : rowData.getBeforeColumnsList()) {
            System.out.println("列名:" + column.getName() + "-------删除前的数据:" + column.getValue());
        }

    }*/


    @Autowired
    private ContentFeign contentFeign;
    //字符串
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    //自定义数据库的 操作来监听
    //destination = "example"
    @ListenPoint(destination = "example",
            schema = "changgou_content",
            table = {"tb_content", "tb_content_category"},
            eventType = {
                    CanalEntry.EventType.UPDATE,
                    CanalEntry.EventType.DELETE,
                    CanalEntry.EventType.INSERT})
    public void onEventCustomUpdate(CanalEntry.EventType eventType, CanalEntry.RowData rowData) {
        //1.获取列名 为category_id的值
        String categoryId = getColumnValue(eventType, rowData);
        //2.调用feign 获取该分类下的所有的广告集合
        Result<List<Content>> categoryresut = contentFeign.findByCategory(Long.valueOf(categoryId));
        List<Content> data = categoryresut.getData();
        //3.使用redisTemplate存储到redis中
        stringRedisTemplate.boundValueOps("content_" + categoryId).set(JSON.toJSONString(data));
        //System.out.println(stringRedisTemplate.boundValueOps("content_" + categoryId).get());
    }

    private String getColumnValue(CanalEntry.EventType eventType, CanalEntry.RowData rowData) {
        String categoryId = "";
        //判断 如果是删除  则获取beforlist
        if (eventType == CanalEntry.EventType.DELETE) {
            for (CanalEntry.Column column : rowData.getBeforeColumnsList()) {
                if (column.getName().equalsIgnoreCase("category_id")) {
                    categoryId = column.getValue();
                    return categoryId;
                }
            }
        } else {
            //判断 如果是添加 或者是更新 获取afterlist
            for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
                if (column.getName().equalsIgnoreCase("category_id")) {
                    categoryId = column.getValue();
                    return categoryId;
                }
            }
        }
        return categoryId;
    }

    @Autowired
    private PageFeign pageFeign;

    @ListenPoint(destination = "example",
            schema = "changgou_goods",
            table = {"tb_spu"},
            eventType = {CanalEntry.EventType.UPDATE, CanalEntry.EventType.INSERT, CanalEntry.EventType.DELETE})
    public void onEventCustomSpu(CanalEntry.EventType eventType, CanalEntry.RowData rowData) {

        //判断操作类型
        if (eventType == CanalEntry.EventType.DELETE) {
            String spuId = "";
            List<CanalEntry.Column> beforeColumnsList = rowData.getBeforeColumnsList();
            for (CanalEntry.Column column : beforeColumnsList) {
                if (column.getName().equals("id")) {
                    spuId = column.getValue();//spuid
                    break;
                }
            }
            //todo 删除静态页

        }else{
            //新增 或者 更新
            List<CanalEntry.Column> afterColumnsList = rowData.getAfterColumnsList();
            String spuId = "";
            for (CanalEntry.Column column : afterColumnsList) {
                if (column.getName().equals("id")) {
                    spuId = column.getValue();
                    break;
                }
            }
            //更新 生成静态页
            pageFeign.createHtml(Long.valueOf(spuId));
        }
    }


}
