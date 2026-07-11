package com.axtria.salesdata.controller;

import com.axtria.salesdata.dto.DashboardStatsDto;
import com.axtria.salesdata.entity.SalesRecord;
import com.axtria.salesdata.entity.UploadHistory;
import com.axtria.salesdata.service.AiReportService;
import com.axtria.salesdata.service.SalesDataService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SalesController {

    private static final Logger log = LoggerFactory.getLogger(SalesController.class);

    private final SalesDataService salesDataService;
    private final AiReportService aiReportService;

    public SalesController(SalesDataService salesDataService, AiReportService aiReportService) {
        this.salesDataService = salesDataService;
        this.aiReportService = aiReportService;
    }

    /**
     * POST /api/upload
     * Uploads and processes a CSV file through the ETL pipeline.
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = salesDataService.processUpload(file);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Header validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Internal processing error during upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process upload: " + e.getMessage()));
        }
    }

    /**
     * GET /api/dashboard/stats
     * Returns core dashboard metrics
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        return ResponseEntity.ok(salesDataService.getDashboardStats());
    }

    /**
     * GET /api/analytics/region
     * Returns total and average sales by region
     */
    @GetMapping("/analytics/region")
    public ResponseEntity<List<Map<String, Object>>> getRegionAnalytics() {
        return ResponseEntity.ok(salesDataService.getRegionAnalytics());
    }

    /**
     * GET /api/analytics/product
     * Returns top 5 and bottom 5 selling products
     */
    @GetMapping("/analytics/product")
    public ResponseEntity<Map<String, List<Map<String, Object>>>> getProductAnalytics() {
        return ResponseEntity.ok(salesDataService.getProductAnalytics());
    }

    /**
     * GET /api/upload-history
     * Returns last 10 uploaded CSV histories
     */
    @GetMapping("/upload-history")
    public ResponseEntity<List<UploadHistory>> getUploadHistory() {
        return ResponseEntity.ok(salesDataService.getLast10Uploads());
    }

    /**
     * GET /api/search
     * Performs a paginated search for sales records
     */
    @GetMapping("/search")
    public ResponseEntity<Page<SalesRecord>> searchRecords(
            @RequestParam(value = "product", required = false) String product,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(value = "dateFrom", required = false) String dateFrom,
            @RequestParam(value = "dateTo", required = false) String dateTo,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        
        Page<SalesRecord> records = salesDataService.searchRecords(product, region, dateFrom, dateTo, page, size);
        return ResponseEntity.ok(records);
    }

    /**
     * GET /api/report
     * Triggers Groq LLM or Template fall-back summary generation
     */
    @GetMapping("/report")
    public ResponseEntity<Map<String, String>> getAiReport() {
        DashboardStatsDto stats = salesDataService.getDashboardStats();
        List<Map<String, Object>> regions = salesDataService.getRegionAnalytics();
        Map<String, List<Map<String, Object>>> products = salesDataService.getProductAnalytics();

        if (stats.getTotalRecords() == 0) {
            return ResponseEntity.ok(Map.of("report", "No sales records are loaded. Please upload a valid CSV first."));
        }

        String report = aiReportService.generateReport(stats, regions, products);
        return ResponseEntity.ok(Map.of("report", report));
    }

    /**
     * GET /api/export
     * Exports dashboard summary stats as a downloadable CSV
     */
    @GetMapping("/export")
    public void exportSummary(HttpServletResponse response) {
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dashboard_summary.csv\"");

        DashboardStatsDto stats = salesDataService.getDashboardStats();

        try (PrintWriter writer = response.getWriter()) {
            // Write CSV headers
            writer.println("Metric,Value");
            
            // Write core KPI rows
            writer.printf("Total Sales,$%.2f\n", stats.getTotalSales());
            writer.printf("Average Sales,$%.2f\n", stats.getAverageSales());
            writer.printf("Highest Sale,$%.2f\n", stats.getHighestSale());
            writer.printf("Lowest Sale,$%.2f\n", stats.getLowestSale());
            writer.printf("Total Quantity Sold,%d\n", stats.getTotalQuantitySold());
            writer.printf("Total Records Loaded,%d\n", stats.getTotalRecords());
            writer.printf("Data Quality Index Score,%.2f%%\n", stats.getDataQualityScore());
            
            writer.flush();
        } catch (Exception e) {
            log.error("Failed to generate export file", e);
        }
    }

    /**
     * POST /api/reset
     * Resets the system by deleting all records and upload logs (useful for testing/demo).
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetSystem() {
        try {
            salesDataService.resetSystem();
            return ResponseEntity.ok(Map.of("message", "System reset successfully"));
        } catch (Exception e) {
            log.error("Reset system failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Reset failed: " + e.getMessage()));
        }
    }
}
