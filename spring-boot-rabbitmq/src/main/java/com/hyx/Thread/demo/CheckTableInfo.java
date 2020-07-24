package com.hyx.Thread.demo;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * @Description:
 * /**
 *      * @param oldTable  旧表名称
 *      * @param newTable  新表名称
 *      * @param oldTableRelationField  旧表-新表关联字段
 *      * @param newTableRelationField  新表-旧表关联字段
 *      * @param queryOldTableFileds    得到旧表的字段名称
 *      * @param
 *
 * @Author: haoyuexun
 * @Date: 2020/7/24 14:37
 */
public class CheckTableInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String oldTable;
    private String newTable;
    private String oldTableRelationField;
    private String newTableRelationField;
    private String queryOldTableFileds;   // ,分割字段
    private String queryNewTableFileds;
//    private String queryOldTableSqlString;  // 原表查询语句

    private String queryOldTableWhereCondition;   // 查询条件

    public String getQueryOldTableWhereCondition() {
        return queryOldTableWhereCondition;
    }

    public void setQueryOldTableWhereCondition(String queryOldTableWhereCondition) {
        this.queryOldTableWhereCondition = queryOldTableWhereCondition;
    }





//    public String getQueryOldTableSqlString() {
//        return queryOldTableSqlString;
//    }
//
//    public void setQueryOldTableSqlString(String queryOldTableSqlString) {
//        this.queryOldTableSqlString = queryOldTableSqlString;
//    }

    public String getOldTable() {
        return oldTable;
    }

    public void setOldTable(String oldTable) {
        this.oldTable = oldTable;
    }

    public String getNewTable() {
        return newTable;
    }

    public void setNewTable(String newTable) {
        this.newTable = newTable;
    }

    public String getOldTableRelationField() {
        return oldTableRelationField;
    }

    public void setOldTableRelationField(String oldTableRelationField) {
        this.oldTableRelationField = oldTableRelationField;
    }

    public String getNewTableRelationField() {
        return newTableRelationField;
    }

    public void setNewTableRelationField(String newTableRelationField) {
        this.newTableRelationField = newTableRelationField;
    }

    public String getQueryOldTableFileds() {
        return queryOldTableFileds;
    }

    public void setQueryOldTableFileds(String queryOldTableFileds) {
        this.queryOldTableFileds = queryOldTableFileds;
    }

    public String getQueryNewTableFileds() {
        return queryNewTableFileds;
    }

    public void setQueryNewTableFileds(String queryNewTableFileds) {
        this.queryNewTableFileds = queryNewTableFileds;
    }



}
