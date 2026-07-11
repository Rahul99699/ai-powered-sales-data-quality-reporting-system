package com.axtria.salesdata.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "upload_history")
public class UploadHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "upload_time", nullable = false)
    private LocalDateTime uploadTime;

    @Column(name = "total_records", nullable = false)
    private Integer totalRecords;

    @Column(name = "valid_records", nullable = false)
    private Integer validRecords;

    @Column(name = "invalid_records", nullable = false)
    private Integer invalidRecords;

    @Column(name = "data_quality_score", nullable = false)
    private Double dataQualityScore;

    // Constructors
    public UploadHistory() {
    }

    public UploadHistory(Long id, String fileName, LocalDateTime uploadTime, Integer totalRecords, 
                         Integer validRecords, Integer invalidRecords, Double dataQualityScore) {
        this.id = id;
        this.fileName = fileName;
        this.uploadTime = uploadTime;
        this.totalRecords = totalRecords;
        this.validRecords = validRecords;
        this.invalidRecords = invalidRecords;
        this.dataQualityScore = dataQualityScore;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    public Integer getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }

    public Integer getValidRecords() {
        return validRecords;
    }

    public void setValidRecords(Integer validRecords) {
        this.validRecords = validRecords;
    }

    public Integer getInvalidRecords() {
        return invalidRecords;
    }

    public void setInvalidRecords(Integer invalidRecords) {
        this.invalidRecords = invalidRecords;
    }

    public Double getDataQualityScore() {
        return dataQualityScore;
    }

    public void setDataQualityScore(Double dataQualityScore) {
        this.dataQualityScore = dataQualityScore;
    }

    // Custom Builder Pattern
    public static class Builder {
        private Long id;
        private String fileName;
        private LocalDateTime uploadTime;
        private Integer totalRecords;
        private Integer validRecords;
        private Integer invalidRecords;
        private Double dataQualityScore;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder uploadTime(LocalDateTime uploadTime) {
            this.uploadTime = uploadTime;
            return this;
        }

        public Builder totalRecords(Integer totalRecords) {
            this.totalRecords = totalRecords;
            return this;
        }

        public Builder validRecords(Integer validRecords) {
            this.validRecords = validRecords;
            return this;
        }

        public Builder invalidRecords(Integer invalidRecords) {
            this.invalidRecords = invalidRecords;
            return this;
        }

        public Builder dataQualityScore(Double dataQualityScore) {
            this.dataQualityScore = dataQualityScore;
            return this;
        }

        public UploadHistory build() {
            return new UploadHistory(id, fileName, uploadTime, totalRecords, validRecords, invalidRecords, dataQualityScore);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
