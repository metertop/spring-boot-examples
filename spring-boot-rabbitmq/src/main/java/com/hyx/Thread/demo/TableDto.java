package com.hyx.Thread.demo;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;


/**
 * @Description:
 * @Author: haoyuexun
 * @Date: 2020/7/24 14:11
 */
public class TableDto {


    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Long getTableLength() {
        return tableLength;
    }

    public void setTableLength(Long tableLength) {
        this.tableLength = tableLength;
    }

    public LinkedList<String> getTableColumnsValues() {
        return tableColumnsValues;
    }

    public void setTableColumnsValues(LinkedList<String> tableColumnsValues) {
        this.tableColumnsValues = tableColumnsValues;
    }

    public String getTableColumns() {
        return tableColumns;
    }

    public void setTableColumns(String tableColumns) {
        this.tableColumns = tableColumns;
    }

    private String tableName;
    private Long tableLength;
    private String tableColumns;
    private LinkedList<String> tableColumnsValues;

    }
