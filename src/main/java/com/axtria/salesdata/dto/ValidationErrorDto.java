package com.axtria.salesdata.dto;

public class ValidationErrorDto {
    private int rowNumber;
    private String fieldName;
    private String invalidValue;
    private String errorMessage;

    // Constructors
    public ValidationErrorDto() {
    }

    public ValidationErrorDto(int rowNumber, String fieldName, String invalidValue, String errorMessage) {
        this.rowNumber = rowNumber;
        this.fieldName = fieldName;
        this.invalidValue = invalidValue;
        this.errorMessage = errorMessage;
    }

    // Getters and Setters
    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getInvalidValue() {
        return invalidValue;
    }

    public void setInvalidValue(String invalidValue) {
        this.invalidValue = invalidValue;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "ValidationErrorDto{" +
                "rowNumber=" + rowNumber +
                ", fieldName='" + fieldName + '\'' +
                ", invalidValue='" + invalidValue + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
