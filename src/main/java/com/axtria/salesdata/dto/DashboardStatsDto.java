package com.axtria.salesdata.dto;

public class DashboardStatsDto {
    private Double totalSales;
    private Double averageSales;
    private Double highestSale;
    private Double lowestSale;
    private Long totalQuantitySold;
    private Long totalRecords;
    private Double dataQualityScore;

    // Constructors
    public DashboardStatsDto() {
    }

    public DashboardStatsDto(Double totalSales, Double averageSales, Double highestSale, Double lowestSale, 
                             Long totalQuantitySold, Long totalRecords, Double dataQualityScore) {
        this.totalSales = totalSales;
        this.averageSales = averageSales;
        this.highestSale = highestSale;
        this.lowestSale = lowestSale;
        this.totalQuantitySold = totalQuantitySold;
        this.totalRecords = totalRecords;
        this.dataQualityScore = dataQualityScore;
    }

    // Getters and Setters
    public Double getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(Double totalSales) {
        this.totalSales = totalSales;
    }

    public Double getAverageSales() {
        return averageSales;
    }

    public void setAverageSales(Double averageSales) {
        this.averageSales = averageSales;
    }

    public Double getHighestSale() {
        return highestSale;
    }

    public void setHighestSale(Double highestSale) {
        this.highestSale = highestSale;
    }

    public Double getLowestSale() {
        return lowestSale;
    }

    public void setLowestSale(Double lowestSale) {
        this.lowestSale = lowestSale;
    }

    public Long getTotalQuantitySold() {
        return totalQuantitySold;
    }

    public void setTotalQuantitySold(Long totalQuantitySold) {
        this.totalQuantitySold = totalQuantitySold;
    }

    public Long getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Long totalRecords) {
        this.totalRecords = totalRecords;
    }

    public Double getDataQualityScore() {
        return dataQualityScore;
    }

    public void setDataQualityScore(Double dataQualityScore) {
        this.dataQualityScore = dataQualityScore;
    }

    // Custom Builder Pattern
    public static class Builder {
        private Double totalSales = 0.0;
        private Double averageSales = 0.0;
        private Double highestSale = 0.0;
        private Double lowestSale = 0.0;
        private Long totalQuantitySold = 0L;
        private Long totalRecords = 0L;
        private Double dataQualityScore = 100.0;

        public Builder totalSales(Double totalSales) {
            this.totalSales = totalSales;
            return this;
        }

        public Builder averageSales(Double averageSales) {
            this.averageSales = averageSales;
            return this;
        }

        public Builder highestSale(Double highestSale) {
            this.highestSale = highestSale;
            return this;
        }

        public Builder lowestSale(Double lowestSale) {
            this.lowestSale = lowestSale;
            return this;
        }

        public Builder totalQuantitySold(Long totalQuantitySold) {
            this.totalQuantitySold = totalQuantitySold;
            return this;
        }

        public Builder totalRecords(Long totalRecords) {
            this.totalRecords = totalRecords;
            return this;
        }

        public Builder dataQualityScore(Double dataQualityScore) {
            this.dataQualityScore = dataQualityScore;
            return this;
        }

        public DashboardStatsDto build() {
            return new DashboardStatsDto(totalSales, averageSales, highestSale, lowestSale, totalQuantitySold, totalRecords, dataQualityScore);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
