package com.example.controller;

import com.alibaba.fastjson.JSON;
import com.example.common.ResponseBean;
import com.example.constant.Constant;
import com.example.model.BookDto;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryRewriteContext;
import org.elasticsearch.index.query.QueryShardContext;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HighLevelRestController
 *
 * @author wliduo[i@dolyw.com]
 * @date 2019/8/14 16:24
 */
@RestController
@RequestMapping("/high")
public class HighLevelRestController {

    /**
     * logger
     */
    private static final Logger logger = LoggerFactory.getLogger(HighLevelRestController.class);

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    /**
     * ??????ES??????
     *
     * @param
     * @return com.example.common.ResponseBean
     * @throws IOException
     * @author wliduo[i@dolyw.com]
     * @date 2019/8/14 17:11
     */
    @GetMapping("/es")
    public ResponseBean getEsInfo() throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // SearchRequest
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(searchSourceBuilder);
        // ??????ES
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        return new ResponseBean(HttpStatus.OK.value(), "????????????", searchResponse);
    }

    /**
     * ????????????
     *
     * @param page
	 * @param rows
	 * @param keyword
     * @return com.example.common.ResponseBean
     * @throws
     * @author wliduo[i@dolyw.com]
     * @date 2019/8/15 16:01
     */
    @GetMapping("/book")
    public ResponseBean list(@RequestParam(defaultValue = "1") Integer page,
                             @RequestParam(defaultValue = "10") Integer rows,
                             String keyword) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // ?????????????????????from + size??????????????????????????????????????????????????????????????????????????????
        searchSourceBuilder.from((page - 1) * rows);
        searchSourceBuilder.size(rows);
        // ???????????????????????????????????????????????????????????????
        if (StringUtils.isNoneBlank(keyword)) {
            QueryBuilder queryBuilder = QueryBuilders.multiMatchQuery(keyword, "name", "content", "describe");
            searchSourceBuilder.query(queryBuilder);
        }
        // ???????????????ID??????
        // searchSourceBuilder.sort("id", SortOrder.DESC);
        // ??????unmappedType????????????????????????all shards failed????????????????????????????????????
        FieldSortBuilder fieldSortBuilder = SortBuilders.fieldSort("id").order(SortOrder.DESC).unmappedType("text");
        searchSourceBuilder.sort(fieldSortBuilder);
        // SearchRequest
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(searchSourceBuilder);
        // ??????ES
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits hits = searchResponse.getHits();
        // ????????????
        Long total = hits.getTotalHits().value;
        // ????????????????????????
        List<BookDto> bookDtoList = new ArrayList<>();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit searchHit : searchHits) {
            bookDtoList.add(JSON.parseObject(searchHit.getSourceAsString(), BookDto.class));
        }
        // ??????Map????????????
        Map<String, Object> result = new HashMap<String, Object>(16);
        result.put("count", total);
        result.put("data", bookDtoList);
        return new ResponseBean(HttpStatus.OK.value(), "????????????", result);
    }

    /**
     * ??????ID??????
     *
     * @param id
     * @return com.example.common.ResponseBean
     * @throws IOException
     * @author wliduo[i@dolyw.com]
     * @date 2019/8/15 14:10
     */
    @GetMapping("/book/{id}")
    public ResponseBean getById(@PathVariable("id") String id) throws IOException {
        // GetRequest
        GetRequest getRequest = new GetRequest(Constant.INDEX, id);
        // ??????ES
        GetResponse getResponse = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);
        BookDto bookDto = JSON.parseObject(getResponse.getSourceAsString(), BookDto.class);
        return new ResponseBean(HttpStatus.OK.value(), "????????????", bookDto);
    }

    /**
     * ????????????
     *
     * @param bookDto
     * @return com.example.common.ResponseBean
     * @throws
     * @author wliduo[i@dolyw.com]
     * @date 2019/8/15 16:01
     */
    @PostMapping("/book")
    public ResponseBean add(@RequestBody BookDto bookDto) throws IOException {
        // IndexRequest
        IndexRequest indexRequest = new IndexRequest(Constant.INDEX);
        Long id = System.currentTimeMillis();
        bookDto.setId(id);
        String source = JSON.toJSONString(bookDto);
        indexRequest.id(id.toString()).source(source, XContentType.JSON);
        // ??????ES
        IndexResponse indexResponse = restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
        return new ResponseBean(HttpStatus.OK.value(), "????????????", indexResponse);
    }

    /**
     * ????????????
     *
     * @param bookDto
     * @return com.example.common.ResponseBean
     * @throws
     * @author wliduo[i@dolyw.com]
     * @date 2019/8/15 16:02
     */
    @PutMapping("/book")
    public ResponseBean update(@RequestBody BookDto bookDto) throws IOException {
        // UpdateRequest
        UpdateRequest updateRequest = new UpdateRequest(Constant.INDEX, bookDto.getId().toString());
        updateRequest.doc(JSON.toJSONString(bookDto), XContentType.JSON);
        // ??????ES
        UpdateResponse updateResponse = restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);
        return new ResponseBean(HttpStatus.OK.value(), "????????????", updateResponse);
    }

    /**
     * ????????????
     *
     * @param id
     * @return com.example.common.ResponseBean
     * @throws
     * @author wliduo[i@dolyw.com]
     * @date 2019/8/15 16:02
     */
    @DeleteMapping("/book/{id}")
    public ResponseBean deleteById(@PathVariable("id") String id) throws IOException {
        // DeleteRequest
        DeleteRequest deleteRequest = new DeleteRequest(Constant.INDEX, id);
        // ??????ES
        DeleteResponse deleteResponse = restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
        return new ResponseBean(HttpStatus.OK.value(), "????????????", deleteResponse);
    }

}
