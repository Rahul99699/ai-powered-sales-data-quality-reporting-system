package com.axtria.salesdata.service;

import com.axtria.salesdata.dto.DashboardStatsDto;
import com.axtria.salesdata.dto.ValidationErrorDto;
import com.axtria.salesdata.entity.SalesRecord;
import com.axtria.salesdata.entity.UploadHistory;
import com.axtria.salesdata.repository.SalesRecordRepository;
import com.axtria.salesdata.repository.UploadHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SalesDataService {

    private static final Logger log = LoggerFactory.getLogger(SalesDataService.class);

    private final SalesRecordRepository salesRecordRepository;
    private final UploadHistoryRepository uploadHistoryRepository;

    public SalesDataService(SalesRecordRepository salesRecordRepository, UploadHistoryRepository uploadHistoryRepository) {
        this.salesRecordRepository = salesRecordRepository;
        this.uploadHistoryRepository = uploadHistoryRepository;
    }

    private static final List<String> EXPECTED_HEADERS = List.of("Product", "Region", "Sales", "Quantity", "Date");

    /**
     * Executes the ETL pipeline on an uploaded CSV file.
     * Extract -> Validate & Transform -> Load -> Save History
     */
    @Transactional
    public Map<String, Object> processUpload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        List<ValidationErrorDto> validationErrors = new ArrayList<>();
        List<SalesRecord> validRecords = new ArrayList<>();
        
        // Sets for tracking duplicates within the current upload
        Set<String> seenInUpload = new HashSet<>();

        int totalRecordsCount = 0;
        int duplicateCount = 0;

        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            // 1. Extract & Validate Headers
            Map<String, Integer> headerMap = csvParser.getHeaderMap();
            validateCsvHeaders(headerMap);

            Iterable<CSVRecord> csvRecords = csvParser.getRecords();
            for (CSVRecord record : csvRecords) {
                totalRecordsCount++;
                int rowNum = (int) record.getRecordNumber() + 1; // 1-indexed row number (header is row 1)

                String rawProduct = record.get("Product");
                String rawRegion = record.get("Region");
                String rawSales = record.get("Sales");
                String rawQuantity = record.get("Quantity");
                String rawDate = record.get("Date");

                // Validation Phase
                boolean isValid = true;

                // Check for empty values
                if (isEmpty(rawProduct)) {
                    validationErrors.add(new ValidationErrorDto(rowNum, "Product", rawProduct, "Product cannot be empty"));
                    isValid = false;
                }
                if (isEmpty(rawRegion)) {
                    validationErrors.add(new ValidationErrorDto(rowNum, "Region", rawRegion, "Region cannot be empty"));
                    isValid = false;
                }
                if (isEmpty(rawSales)) {
                    validationErrors.add(new ValidationErrorDto(rowNum, "Sales", rawSales, "Sales cannot be empty"));
                    isValid = false;
                }
                if (isEmpty(rawQuantity)) {
                    validationErrors.add(new ValidationErrorDto(rowNum, "Quantity", rawQuantity, "Quantity cannot be empty"));
                    isValid = false;
                }
                if (isEmpty(rawDate)) {
                    validationErrors.add(new ValidationErrorDto(rowNum, "Date", rawDate, "Date cannot be empty"));
                    isValid = false;
                }

                if (!isValid) {
                    continue; // Skip further numeric/date validations if required fields are missing
                }

                // Numeric validations
                Double salesValue = null;
                try {
                    salesValue = Double.parseDouble(rawSales.trim());
                    if (salesValue < 0) {
                        validationErrors.add(new ValidationErrorDto(rowNum, "Sales", rawSales, "Sales cannot be negative"));
                        isValid = false;
                    }
                } catch (NumberFormatException e) {
                    validationErrors.add(new ValidationErrorDto(rowNum, "Sales", rawSales, "Sales must be a valid number"));
                    isValid = false;
                }

                Integer quantityValue = null;
                try {
                    quantityValue = Integer.parseInt(rawQuantity.trim());
                    if (quantityValue < 0) {
                        validationErrors.add(new ValidationErrorDto(rowNum, "Quantity", rawQuantity, "Quantity cannot be negative"));
                        isValid = false;
                    }
                } catch (NumberFormatException e) {
                    validationErrors.add(new ValidationErrorDto(rowNum, "Quantity", rawQuantity, "Quantity must be an integer"));
                    isValid = false;
                }

                LocalDate dateValue = null;
                try {
                    dateValue = parseLocalDate(rawDate.trim());
                } catch (Exception e) {
                    validationErrors.add(new ValidationErrorDto(rowNum, "Date", rawDate, "Invalid date format. Use YYYY-MM-DD or MM/DD/YYYY"));
                    isValid = false;
                }

                if (!isValid) {
                    continue;
                }

                // 2. Transformation Rules
                // - Trim leading/trailing spaces
                // - Collapse multiple spaces into one
                // - Normalize product names
                String transformedProduct = rawProduct.trim().replaceAll("\\s+", " ");
                
                // - Convert Region to UPPERCASE
                String transformedRegion = rawRegion.trim().replaceAll("\\s+", " ").toUpperCase();
                
                // - Standardize Date format (already LocalDate)
                LocalDate transformedDate = dateValue;

                // 3. Duplicate Detection
                // Unique key representation of a record
                String uniqueKey = String.format("%s|%s|%.2f|%d|%s", 
                        transformedProduct.toLowerCase(), 
                        transformedRegion.toLowerCase(), 
                        salesValue, 
                        quantityValue, 
                        transformedDate.toString());

                // Rule 1: Check duplicate within the uploaded CSV itself
                if (seenInUpload.contains(uniqueKey)) {
                    duplicateCount++;
                    continue; // Skip this duplicate record
                }
                seenInUpload.add(uniqueKey);

                // Rule 2: Check duplicate against existing records in the database
                boolean existsInDb = salesRecordRepository.existsByProductAndRegionAndSalesAndQuantityAndDate(
                        transformedProduct, transformedRegion, salesValue, quantityValue, transformedDate);
                
                if (existsInDb) {
                    duplicateCount++;
                    continue; // Skip this duplicate record
                }

                // Load: Add to save queue
                SalesRecord recordEntity = SalesRecord.builder()
                        .product(transformedProduct)
                        .region(transformedRegion)
                        .sales(salesValue)
                        .quantity(quantityValue)
                        .date(transformedDate)
                        .build();

                validRecords.add(recordEntity);
            }

            // Save valid records
            if (!validRecords.isEmpty()) {
                salesRecordRepository.saveAll(validRecords);
            }

            // Calculate Data Quality Score
            int validCount = validRecords.size();
            int invalidCount = validationErrors.size();
            
            // Note: Total records processed (excluding duplicate filters) is validCount + invalidCount
            int totalProcessed = validCount + invalidCount;
            double qualityScore = 100.0;
            if (totalProcessed > 0) {
                qualityScore = ((double) validCount / totalProcessed) * 100.0;
            }
            // Round to 2 decimal places
            qualityScore = Math.round(qualityScore * 100.0) / 100.0;

            // Save Upload History
            UploadHistory history = UploadHistory.builder()
                    .fileName(file.getOriginalFilename())
                    .uploadTime(LocalDateTime.now())
                    .totalRecords(totalProcessed + duplicateCount) // Including filtered duplicates in total file line count
                    .validRecords(validCount)
                    .invalidRecords(invalidCount)
                    .dataQualityScore(qualityScore)
                    .build();

            uploadHistoryRepository.save(history);

            // Response summary
            Map<String, Object> result = new HashMap<>();
            result.put("fileName", file.getOriginalFilename());
            result.put("totalRecords", totalProcessed + duplicateCount);
            result.put("validRecords", validCount);
            result.put("invalidRecords", invalidCount);
            result.put("duplicatesRemoved", duplicateCount);
            result.put("dataQualityScore", qualityScore);
            result.put("errors", validationErrors);

            return result;

        } catch (IllegalArgumentException e) {
            throw e; // Reroute standard validation header issues
        } catch (Exception e) {
            log.error("Error reading CSV file", e);
            throw new RuntimeException("Failed to parse and process CSV file: " + e.getMessage());
        }
    }

    /**
     * Core KPIs for the Dashboard stats card
     */
    public DashboardStatsDto getDashboardStats() {
        Double totalSales = salesRecordRepository.getTotalSales();
        Double averageSales = salesRecordRepository.getAverageSales();
        Double highestSale = salesRecordRepository.getHighestSale();
        Double lowestSale = salesRecordRepository.getLowestSale();
        Long totalQuantitySold = salesRecordRepository.getTotalQuantitySold();
        Long totalRecords = salesRecordRepository.count();
        Double avgQualityScore = uploadHistoryRepository.getAverageDataQualityScore();

        return DashboardStatsDto.builder()
                .totalSales(totalSales != null ? Math.round(totalSales * 100.0) / 100.0 : 0.0)
                .averageSales(averageSales != null ? Math.round(averageSales * 100.0) / 100.0 : 0.0)
                .highestSale(highestSale != null ? highestSale : 0.0)
                .lowestSale(lowestSale != null ? lowestSale : 0.0)
                .totalQuantitySold(totalQuantitySold != null ? totalQuantitySold : 0L)
                .totalRecords(totalRecords)
                .dataQualityScore(avgQualityScore != null ? Math.round(avgQualityScore * 100.0) / 100.0 : 100.0)
                .build();
    }

    /**
     * Get Region Analytics
     */
    public List<Map<String, Object>> getRegionAnalytics() {
        List<Object[]> queryResult = salesRecordRepository.getRegionAnalytics();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : queryResult) {
            Map<String, Object> map = new HashMap<>();
            map.put("region", row[0]);
            map.put("totalSales", Math.round(((Double) row[1]) * 100.0) / 100.0);
            map.put("averageSales", Math.round(((Double) row[2]) * 100.0) / 100.0);
            result.add(map);
        }
        return result;
    }

    /**
     * Get Product Rankings
     */
    public Map<String, List<Map<String, Object>>> getProductAnalytics() {
        // Top 5 Products
        List<Object[]> topQuery = salesRecordRepository.getTopProducts(PageRequest.of(0, 5));
        List<Map<String, Object>> topProducts = new ArrayList<>();
        for (Object[] row : topQuery) {
            Map<String, Object> map = new HashMap<>();
            map.put("product", row[0]);
            map.put("totalSales", Math.round(((Double) row[1]) * 100.0) / 100.0);
            topProducts.add(map);
        }

        // Lowest 5 Selling Products
        List<Object[]> bottomQuery = salesRecordRepository.getLowestSellingProducts(PageRequest.of(0, 5));
        List<Map<String, Object>> bottomProducts = new ArrayList<>();
        for (Object[] row : bottomQuery) {
            Map<String, Object> map = new HashMap<>();
            map.put("product", row[0]);
            map.put("totalSales", Math.round(((Double) row[1]) * 100.0) / 100.0);
            bottomProducts.add(map);
        }

        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        result.put("top", topProducts);
        result.put("bottom", bottomProducts);
        return result;
    }

    /**
     * Get recent 10 uploads
     */
    public List<UploadHistory> getLast10Uploads() {
        return uploadHistoryRepository.findTop10ByOrderByUploadTimeDesc();
    }

    /**
     * Search sales records with pagination
     */
    public Page<SalesRecord> searchRecords(String product, String region, String dateFromStr, String dateToStr, int page, int size) {
        LocalDate dateFrom = null;
        LocalDate dateTo = null;

        try {
            if (!isEmpty(dateFromStr)) {
                dateFrom = LocalDate.parse(dateFromStr.trim());
            }
            if (!isEmpty(dateToStr)) {
                dateTo = LocalDate.parse(dateToStr.trim());
            }
        } catch (Exception e) {
            log.error("Failed to parse search date range", e);
        }

        Pageable pageable = PageRequest.of(page, size);
        return salesRecordRepository.searchRecords(
                isEmpty(product) ? null : product.trim(),
                isEmpty(region) ? null : region.trim(),
                dateFrom,
                dateTo,
                pageable
        );
    }

    /**
     * Clear all database tables (useful helper for resetting application)
     */
    @Transactional
    public void resetSystem() {
        salesRecordRepository.deleteAll();
        uploadHistoryRepository.deleteAll();
    }

    /* Private Helpers */

    private void validateCsvHeaders(Map<String, Integer> headerMap) {
        if (headerMap == null || headerMap.isEmpty()) {
            throw new IllegalArgumentException("Invalid CSV Template: No headers found");
        }
        for (String expected : EXPECTED_HEADERS) {
            // Check case-insensitive existence
            boolean found = false;
            for (String actual : headerMap.keySet()) {
                if (actual.equalsIgnoreCase(expected)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalArgumentException("Invalid CSV Template: Missing header '" + expected + "'");
            }
        }
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private LocalDate parseLocalDate(String dateStr) {
        String[] patterns = {
                "yyyy-MM-dd",
                "M/d/yyyy",
                "MM/dd/yyyy",
                "d/M/yyyy",
                "dd/MM/yyyy",
                "yyyy/MM/dd",
                "d-M-yyyy",
                "dd-MM-yyyy"
        };
        for (String pattern : patterns) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
            } catch (Exception e) {
                // Continue trying other patterns
            }
        }
        throw new IllegalArgumentException("Unable to parse date: " + dateStr);
    }
}
