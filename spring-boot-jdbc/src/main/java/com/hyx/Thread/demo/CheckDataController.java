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
        checkTableInfo.setNewTableRelationField("stu_point_id");
        checkTableInfo.setQueryNewTableFileds("stu_id,count,sku_type");
        checkTableInfo.setQueryOldTableFileds("stu_id,content,type");
        checkTableInfo.setQueryOldTableWhereCondition("id>1679000 and id<1779000");
        checkTableInfo.setOldTableRelationField("id");
        long start = System.currentTimeMillis();
        checkPositionService.checkData(checkTableInfo);
        long end = System.currentTimeMillis();

        long useTime = (end-start)/1000;
        logger.info("对比数据使用的时间为：{} 秒", useTime );
    }
}

