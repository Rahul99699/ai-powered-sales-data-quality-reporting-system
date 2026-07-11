package com.axtria.salesdata.dto;

public class SalesRecordDto {
    private int rowNumber;
    private String product;
    private String region;
    private String sales;
    private String quantity;
    private String date;

    // Constructors
    public SalesRecordDto() {
    }

    public SalesRecordDto(int rowNumber, String product, String region, String sales, String quantity, String date) {
        this.rowNumber = rowNumber;
        this.product = product;
        this.region = region;
        this.sales = sales;
        this.quantity = quantity;
        this.date = date;
    }

    // Getters and Setters
    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getSales() {
        return sales;
    }

    public void setSales(String sales) {
        this.sales = sales;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
