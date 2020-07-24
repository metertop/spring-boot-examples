package com.hyx.Thread.demo;

import org.apache.ibatis.session.SqlSession;

import java.util.LinkedList;
import java.util.List;

/**
 * @Description:
 * @Author: haoyuexun
 * @Date: 2020/7/24 14:06
 */
public interface CheckPositionService {

    SqlSession getSqlSession();

    TableDto getOldTableDetail(CheckTableInfo checkTableInfo);

    void checkData(CheckTableInfo checkTableInfo);
}
