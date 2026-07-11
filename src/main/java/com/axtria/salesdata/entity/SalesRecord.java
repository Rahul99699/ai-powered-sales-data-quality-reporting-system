package com.axtria.salesdata.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales_records")
public class SalesRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String product;

    @Column(nullable = false)
    private String region;

    @Column(nullable = false)
    private Double sales;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private LocalDate date;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public SalesRecord() {
    }

    public SalesRecord(Long id, String product, String region, Double sales, Integer quantity, LocalDate date, LocalDateTime createdAt) {
        this.id = id;
        this.product = product;
        this.region = region;
        this.sales = sales;
        this.quantity = quantity;
        this.date = date;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Double getSales() {
        return sales;
    }

    public void setSales(Double sales) {
        this.sales = sales;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Custom Builder Pattern
    public static class Builder {
        private Long id;
        private String product;
        private String region;
        private Double sales;
        private Integer quantity;
        private LocalDate date;
        private LocalDateTime createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder product(String product) {
            this.product = product;
            return this;
        }

        public Builder region(String region) {
            this.region = region;
            return this;
        }

        public Builder sales(Double sales) {
            this.sales = sales;
            return this;
        }

        public Builder quantity(Integer quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder date(LocalDate date) {
            this.date = date;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public SalesRecord build() {
            return new SalesRecord(id, product, region, sales, quantity, date, createdAt);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
