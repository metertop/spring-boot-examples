package com.hyx.Thread.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description:
 * @Author: haoyuexun
 * @Date: 2020-07-25 11:43
 */

@RestController
@RequestMapping("/data")
public class CheckDataController {

    private Logger logger = LoggerFactory.getLogger(CheckDataController.class);

    @Autowired
    private CheckPositionService checkPositionService;

    @RequestMapping("/check")
    public void checkData() throws Exception{

        CheckTableInfo checkTableInfo = new CheckTableInfo();
        checkTableInfo.setNewTable("user_assets");
        checkTableInfo.setOldTable("stu_point");
        checkTableInfo.setQueryNewTableFileds("stu_id,count,sku_type");
        checkTableInfo.setQueryOldTableFileds("stu_id,content,type");
        checkTableInfo.setQueryOldTableWhereCondition("1=1");
        checkTableInfo.setOldTableRelationField("id");   // 两个表的关联字段，最好是旧表的主键
        checkTableInfo.setNewTableRelationField("stu_point_id");
        checkPositionService.checkData4(checkTableInfo);

    }
}

