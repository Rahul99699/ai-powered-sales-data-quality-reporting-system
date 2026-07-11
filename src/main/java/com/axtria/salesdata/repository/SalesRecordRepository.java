package com.axtria.salesdata.repository;

import com.axtria.salesdata.entity.SalesRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface SalesRecordRepository extends JpaRepository<SalesRecord, Long> {

    // Main KPI Statistics
    @Query("SELECT SUM(s.sales) FROM SalesRecord s")
    Double getTotalSales();

    @Query("SELECT AVG(s.sales) FROM SalesRecord s")
    Double getAverageSales();

    @Query("SELECT MAX(s.sales) FROM SalesRecord s")
    Double getHighestSale();

    @Query("SELECT MIN(s.sales) FROM SalesRecord s")
    Double getLowestSale();

    @Query("SELECT SUM(s.quantity) FROM SalesRecord s")
    Long getTotalQuantitySold();

    @Query("SELECT COUNT(DISTINCT s.product) FROM SalesRecord s")
    Long getUniqueProductCount();

    // Duplicate detection: check if an identical record already exists in the database
    boolean existsByProductAndRegionAndSalesAndQuantityAndDate(
            String product, String region, Double sales, Integer quantity, LocalDate date);

    // Region Analytics (Region, Total Sales, Avg Sales) ordered by total sales descending
    @Query("SELECT s.region, SUM(s.sales), AVG(s.sales) FROM SalesRecord s GROUP BY s.region ORDER BY SUM(s.sales) DESC")
    List<Object[]> getRegionAnalytics();

    // Top Selling Products based on sum of sales (returns product name and total sales)
    @Query("SELECT s.product, SUM(s.sales) FROM SalesRecord s GROUP BY s.product ORDER BY SUM(s.sales) DESC")
    List<Object[]> getTopProducts(Pageable pageable);

    // Lowest Selling Products based on sum of sales
    @Query("SELECT s.product, SUM(s.sales) FROM SalesRecord s GROUP BY s.product ORDER BY SUM(s.sales) ASC")
    List<Object[]> getLowestSellingProducts(Pageable pageable);

    // Paginated dynamic search by product, region, and date range
    @Query("SELECT s FROM SalesRecord s WHERE " +
           "(:product IS NULL OR :product = '' OR LOWER(s.product) LIKE LOWER(CONCAT('%', :product, '%'))) AND " +
           "(:region IS NULL OR :region = '' OR LOWER(s.region) = LOWER(:region)) AND " +
           "(:dateFrom IS NULL OR s.date >= :dateFrom) AND " +
           "(:dateTo IS NULL OR s.date <= :dateTo)")
    Page<SalesRecord> searchRecords(
            @Param("product") String product,
            @Param("region") String region,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            Pageable pageable);
}
