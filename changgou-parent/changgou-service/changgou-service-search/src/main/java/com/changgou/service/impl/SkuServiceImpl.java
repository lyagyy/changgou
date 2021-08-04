package com.changgou.service.impl;

import com.alibaba.fastjson.JSON;
import com.changgou.dao.SkuEsMapper;
import com.changgou.goods.feign.SkuFeign;
import com.changgou.goods.pojo.Sku;
import com.changgou.pojo.SkuInfo;
import com.changgou.service.SkuService;

import entity.Result;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Stream;


@Service
public class SkuServiceImpl implements SkuService{

    @Autowired
    private SkuEsMapper skuEsMapper;

    @Autowired
    private SkuFeign skuFeign;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;
    /**
     * 导入sku数据到es
     */
    @Override
    public void importSku() {
        //调用changgou-service-goods微服务，获取索引库数据
        Result<List<Sku>> skuListResult = skuFeign.findByStatus("1");
        //将数据转成search.Sku
        //将List<Sku>转为List<SkuInfo>
        //List<Sku>-->[{skuJSON}]-->List<SkuInfo>
        List<SkuInfo> skuInfos=  JSON.parseArray(JSON.toJSONString(skuListResult.getData()),SkuInfo.class);

        for (SkuInfo skuInfo : skuInfos) {
        //获取spec-->Map(String)-->Map类型  {"电视音响效果":"立体声","电视屏幕尺寸":"20英寸","尺码":"165"}
            Map<String, Object> map = JSON.parseObject(skuInfo.getSpec(), Map.class);
            //如果需要生成动态的域，只需要将该域存入到一个Map<String,Object>对象中即可,该Map<String,Object>的key会生成一个域，域的名字为该Map的key
            //当前Map<String,Object>后面object的值会作为当前sku对象该域(key)对应的值
            skuInfo.setSpecMap(map);
        }
        skuEsMapper.saveAll(skuInfos);

    }


    /**
     * 关键词搜索
     * @param searchMap
     * @return
     */
    @Override
    public Map search(Map<String, String> searchMap) {
        //搜索条件
        NativeSearchQueryBuilder nativeSearchQueryBuilder = buildBasicQuery(searchMap);


        //集合搜索
        Map<String, Object> resultMap = searchList(nativeSearchQueryBuilder);



        //分组查询-->分类、品牌、规格
        Map<String, Object> groupMap = searchGroupList(nativeSearchQueryBuilder, searchMap);
        resultMap.putAll(groupMap);

        return resultMap;


    }


    /**
     * 搜索条件封装
     * @param searchMap
     * @return
     */
    private NativeSearchQueryBuilder buildBasicQuery(Map<String, String> searchMap) {
        /**
         * 1.创建查询对象的构建对象NativeSearchQueryBuilder，用于封装各种搜索条件
         */
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();

        /**
         * 2.BoolQuery must,must_not,should组合查询
         */
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        /**3.关键词搜索*/
        if(searchMap!=null&&searchMap.size()>0){
            //获取关键字的值
            String keywords = searchMap.get("keywords");
            if (!StringUtils.isEmpty(keywords)) {
                //搜索关键词数据
                //nativeSearchQueryBuilder.withQuery(QueryBuilders.queryStringQuery(keywords).field("name"));
                boolQueryBuilder.must(QueryBuilders.queryStringQuery(keywords).field("name"));
            }


            /*输入了分类-->category*/
            if (!StringUtils.isEmpty(searchMap.get("category"))) {
                //搜索关键词数据
                boolQueryBuilder.must(QueryBuilders.termQuery("categoryName",searchMap.get("category")));
            }

            /*输入了品牌-->brand*/
            if (!StringUtils.isEmpty(searchMap.get("brand"))) {
                //搜索关键词数据
                boolQueryBuilder.must(QueryBuilders.termQuery("brandName",searchMap.get("brand")));
            }


            /*规格过滤spec_网络=联通3G&spec_颜色=红*/
            for (Map.Entry<String, String> entry : searchMap.entrySet()) {
                String key = entry.getKey();
                //如果key以spec_开始，则表示规格筛选查询
                if(key.startsWith("spec_")){
                    //规格的值
                    String value = entry.getValue();
                    //spec_网络 spec_前五个要去掉
                    boolQueryBuilder.must(QueryBuilders.termQuery("specMap."+key.substring(5)+".keyword",value));
                }
            }



            /*价格区间条件过滤*/
            //price 0-500元 500-1000元 1000-1500元 1500-2000元 2000元以上
            String price = searchMap.get("price");
            if (!StringUtils.isEmpty(price)){
                //去掉中文 元以上 0-500 500-100....
                price=price.replace("元","").replace("以上","");
                //price[]根据-分隔 [0,500] [500,1000]..[2000]
                String[] prices = price.split("-");
                //x一定不为空，y可能为null
                if(prices!=null&&prices.length>0){
                    //price>prices[0]
                    boolQueryBuilder.must(QueryBuilders.rangeQuery("price").gt(Integer.parseInt(prices[0])));
                    //prices[0]!=null price<=prices[1]
                    if(prices.length==2){
                        //price<=prices[1]
                        boolQueryBuilder.must(QueryBuilders.rangeQuery("price").lte(Integer.parseInt(prices[1])));
                    }

                }

            }


            /*分页，用户如果不传分页参数，则默认第一页*/
            Integer pageNum=converterPate(searchMap); //默认
            Integer size=100; //默认每页显示的查询数据条数
            nativeSearchQueryBuilder.withPageable(PageRequest.of(pageNum-1,size));



            /*排序实现*/
            String sortField = searchMap.get("sortField");//指定排序的域
            String sortRule = searchMap.get("sortRule");//指定排序的规则
            if(!StringUtils.isEmpty(sortField) && !StringUtils.isEmpty(sortRule)){
               nativeSearchQueryBuilder.withSort(new FieldSortBuilder(sortField)//指定排序域
                       .order(SortOrder.valueOf(sortRule)));//指定排序规则
            }


            /**
             *4.将boolQueryBuilder填充给NativeSearchQueryBuilder
             */
            nativeSearchQueryBuilder.withQuery(boolQueryBuilder);

        }
        return nativeSearchQueryBuilder;
    }

    /**
     * 接收前端传入的分页参数
     * @param searchMap
     * @return
     */
    public Integer converterPate(Map<String,String> searchMap){
        if (searchMap!=null&&!StringUtils.isEmpty(searchMap.get("pageNum"))){
                String pageNum = searchMap.get("pageNum");
                return Integer.parseInt(pageNum);
        }
        return 1;
    }


    /**
     * 结果集搜索
     * @param nativeSearchQueryBuilder
     * @return
     */
    private Map<String, Object> searchList(NativeSearchQueryBuilder nativeSearchQueryBuilder) {

        //设置高亮条件
        HighlightBuilder.Field field = new HighlightBuilder.Field("name");//指定高亮域
        field.preTags("<em style=\"color:red\">");//前缀
        field.postTags("</em>");//后缀
        field.fragmentSize(100);//碎片长度，关键词数据的长度
        //添加高亮
        nativeSearchQueryBuilder.withHighlightFields(field);


        //2.执行搜索,响应结果集
        /**
         * 第一个参数：搜索条件封装对象
         * 第二个参数：搜索结果集（集合的数据）需要转换的类型
         * AggregatedPage<SkuInfo>搜索结果集的封装
         */
        AggregatedPage<SkuInfo> page = elasticsearchTemplate.queryForPage(
                nativeSearchQueryBuilder.build(),   //搜索条件封装
                SkuInfo.class,                      //数据集合要转换的类型
                new SearchResultMapper() {          //执行搜索后，将数据结果集封装到这个对象中
                    @Override
                    public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> aClass, Pageable pageable) {
                        //存储所有转换后的高亮数据对象
                        List<T> list = new ArrayList<>();

                        //执行查询，获取所有数据->结果集[非高亮数据|高亮数据]
                        for (SearchHit hit : response.getHits()) {
                            //分析结果集数据，获取[非高亮数据]
                        SkuInfo skuInfo=JSON.parseObject(hit.getSourceAsString(),SkuInfo.class);

                            //分析结果集数据，获取[高亮]数据->只有某个域的高亮数据
                            HighlightField highlightField = hit.getHighlightFields().get("name");

                           if(highlightField!=null&&highlightField.getFragments()!=null){
                               //高亮数据读取出来
                               Text[] fragments = highlightField.getFragments();
                               StringBuffer buffer = new StringBuffer();
                               for (Text fragment : fragments) {
                                   buffer.append(fragment.toString());
                               }
                               //非高亮数据中指定的域替换成高亮数据
                               skuInfo.setName(buffer.toString());
                           }
                           //将高亮数据条件到集合中
                           list.add((T) skuInfo);

                        }

                        //将数据返回
                        /**
                         * 1.搜索的集合数据(携带高亮)List<T> content
                         * 2.分页对象信息Pageable pageable
                         * 3.搜索记录的总条数：long total
                         */

                        return new AggregatedPageImpl<T>(list,pageable,response.getHits().getTotalHits());
                    }
                }
        );


        //获取结果集的参数
        long totalElements = page.getTotalElements();
        int totalPages = page.getTotalPages();
        List<SkuInfo> contents = page.getContent();

        //3.封装一个map存储所有的数据，并返回
        Map<String,Object> resultMap = new HashMap<String, Object>();
        resultMap.put("rows",contents);
        resultMap.put("total",totalElements);
        resultMap.put("totalPages",totalPages);

        //获取搜索封装信息
       /* NativeSearchQuery query = nativeSearchQueryBuilder.build();
        Pageable pageable = query.getPageable();
        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();*/
        int pageNumber = page.getNumber();


        //封装分页数据
        resultMap.put("pageSize",100);
        resultMap.put("pageNumber",pageNumber);


        return resultMap;
    }






    /**
     * 分组查询-->分类分组、品牌分组、规格分组
     * @param nativeSearchQueryBuilder
     * @return
     */
    private Map<String, Object> searchGroupList(NativeSearchQueryBuilder nativeSearchQueryBuilder,Map<String, String> searchMap) {
        //分组查询分类集合
        //addAggregation():添加一个聚合操作
        //1)取别名
        //2)表示根据那个域进行分组查询
        if(searchMap==null || StringUtils.isEmpty(searchMap.get("category"))) {
            nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuCategory").field("categoryName"));
        }
        if(searchMap==null || StringUtils.isEmpty(searchMap.get("brand"))) {
            nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuBrand").field("brandName"));
        }
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuSpec").field("spec.keyword"));

        AggregatedPage<SkuInfo> aggregatedPage=elasticsearchTemplate.queryForPage(nativeSearchQueryBuilder.build(), SkuInfo.class);

        //定义一个map集合，存储所有分组结果
        Map<String,Object> groupMapResult = new HashMap<String,Object>();

        //获取分组数据
        //aggregatedPage.getAggregations()获取的是集合，可以根据多个域进行分组
        //get("skuCategory")指定哪个域的集合


        //获取分类分组集合数据
        if(searchMap==null || StringUtils.isEmpty(searchMap.get("category"))) {
            StringTerms categoryTerms = aggregatedPage.getAggregations().get("skuCategory");
            List<String> categoryList = getGroupList(categoryTerms);
            groupMapResult.put("categoryList", categoryList);
        }
        //获取品牌分组集合数据
        if(searchMap==null || StringUtils.isEmpty(searchMap.get("brand"))){
            StringTerms brandTerms = aggregatedPage.getAggregations().get("skuBrand");
            List<String> brandList = getGroupList(brandTerms);
            groupMapResult.put("brandList",brandList);
        }


        StringTerms specTerms = aggregatedPage.getAggregations().get("skuSpec");
        //获取规格分组集合数据->实现合并操作
        List<String> specList = getGroupList(specTerms);
        Map<String, Set<String>> specMap = putAllSpec(specList);
        groupMapResult.put("specList",specMap);


        return groupMapResult;
    }

    /**
     * 获取分组集合数据
     * @param stringTerms
     * @return
     */
    public List<String> getGroupList(StringTerms stringTerms){
        List<String> groupList=new ArrayList<String>();
        for (StringTerms.Bucket bucket : stringTerms.getBuckets()) {
            String filedName = bucket.getKeyAsString();//其中一个分类名字
            groupList.add(filedName);
        }
        return groupList;
    }


    /**
     * 规格汇总
     * @param specList
     * @return
     */
    private Map<String, Set<String>> putAllSpec(List<String> specList) {
        //合并后的Map对象
        Map<String,Set<String>> allSpec= new HashMap<String, Set<String>>();

        //1.循环SpecList {\"手机屏幕尺寸\":\"5.5寸\",\"网络\":\"联通2G\",\"颜色\":\"紫\",\"测试\":\"实施\",\"机身内存\":\"128G\",\"存储\":\"16G\",\"像素\":\"800万像素\"}
        for (String spec : specList) {
            //2.将每个json字符串转为Map
            Map<String,String> specMap = JSON.parseObject(spec, Map.class);

            //4.合并流程,循环所有Map
            for (Map.Entry<String, String> entry : specMap.entrySet()) {
                //4.2取出当前Map，并且获取对应的key以及对应的value
                String key = entry.getKey();
                String value = entry.getValue();
                //4.3将当前循环的数据合并到一个Map<String,Set<String>>中
                //
                Set<String> specSet = allSpec.get(key);
                if (specSet==null){
                    //之前allSpec中没有该规格
                    specSet = new HashSet<String>();
                }
                specSet.add(value);
                allSpec.put(key,specSet);

            }
        }
        return allSpec;
    }

}
