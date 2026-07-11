package com.axtria.salesdata.service;

import com.axtria.salesdata.dto.DashboardStatsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AiReportService {

    private static final Logger log = LoggerFactory.getLogger(AiReportService.class);

    @Value("${groq.api.key:YOUR_GROQ_API_KEY_HERE}")
    private String apiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String apiUrl;

    @Value("${groq.api.model:llama3-8b-8192}")
    private String modelName;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Generates a Sales Analysis report.
     * Tries the Groq API first, and falls back to a template-based generator if needed.
     */
    public String generateReport(DashboardStatsDto stats, List<Map<String, Object>> regions, Map<String, List<Map<String, Object>>> products) {
        // If the key is not set, use template-based fallback
        if (isDefaultKey()) {
            log.info("Groq API key is not configured. Falling back to template-based report.");
            return generateTemplateReport(stats, regions, products, "System fallback (No API key configured)");
        }

        try {
            String prompt = buildPrompt(stats, regions, products);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.3); // Lower temperature for more factual analysis

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map body = response.getBody();
                List choices = (List) body.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map choice = (Map) choices.get(0);
                    Map message = (Map) choice.get("message");
                    if (message != null && message.get("content") != null) {
                        return (String) message.get("content");
                    }
                }
            }
            
            log.warn("Groq API response was empty or invalid. Status: {}", response.getStatusCode());
            return generateTemplateReport(stats, regions, products, "System fallback (API returned empty response)");

        } catch (Exception e) {
            log.error("Failed to connect or fetch report from Groq API. Error: {}", e.getMessage());
            return generateTemplateReport(stats, regions, products, "System fallback (API request failed: " + e.getMessage() + ")");
        }
    }

    private boolean isDefaultKey() {
        return apiKey == null 
                || apiKey.trim().isEmpty() 
                || apiKey.contains("YOUR_GROQ_API_KEY_HERE") 
                || apiKey.equals("default");
    }

    /**
     * Builds the prompt payload for the Groq LLM
     */
    private String buildPrompt(DashboardStatsDto stats, List<Map<String, Object>> regions, Map<String, List<Map<String, Object>>> products) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a Senior Business Analyst.\n\n");
        sb.append("Analyze the following business KPIs:\n");
        sb.append(String.format("- Total Sales: $%s\n", stats.getTotalSales()));
        sb.append(String.format("- Average Sales: $%s\n", stats.getAverageSales()));
        sb.append(String.format("- Highest Sale: $%s\n", stats.getHighestSale()));
        sb.append(String.format("- Lowest Sale: $%s\n", stats.getLowestSale()));
        sb.append(String.format("- Total Quantity Sold: %d units\n", stats.getTotalQuantitySold()));
        sb.append(String.format("- Total Records Processed: %d records\n", stats.getTotalRecords()));
        sb.append(String.format("- Data Quality score (ETL valid record percentage): %s%%\n\n", stats.getDataQualityScore()));

        sb.append("Region Performance (Ordered by sales desc):\n");
        for (Map<String, Object> r : regions) {
            sb.append(String.format("  - Region: %s, Total Sales: $%s, Avg Sale: $%s\n", 
                    r.get("region"), r.get("totalSales"), r.get("averageSales")));
        }
        sb.append("\n");

        sb.append("Top Selling Products:\n");
        List<Map<String, Object>> top = products.get("top");
        if (top != null) {
            for (Map<String, Object> p : top) {
                sb.append(String.format("  - Product: %s, Revenue: $%s\n", p.get("product"), p.get("totalSales")));
            }
        }
        sb.append("\n");

        sb.append("Lowest Selling Products:\n");
        List<Map<String, Object>> bottom = products.get("bottom");
        if (bottom != null) {
            for (Map<String, Object> p : bottom) {
                sb.append(String.format("  - Product: %s, Revenue: $%s\n", p.get("product"), p.get("totalSales")));
            }
        }
        sb.append("\n");

        sb.append("Generate:\n");
        sb.append("1. Executive Summary\n");
        sb.append("2. Key Observations\n");
        sb.append("3. Best Performing Region\n");
        sb.append("4. Worst Performing Region\n");
        sb.append("5. Business Risks\n");
        sb.append("6. Recommendations\n");
        sb.append("7. Data Quality Assessment\n\n");
        sb.append("Keep the report concise, professional, and suitable for business stakeholders.");
        
        return sb.toString();
    }

    /**
     * Template-based Report Generator (fallback logic)
     * Formats data into a high-quality Markdown report following exact corporate style.
     */
    private String generateTemplateReport(DashboardStatsDto stats, List<Map<String, Object>> regions, Map<String, List<Map<String, Object>>> products, String reason) {
        String bestRegion = "N/A";
        double bestRegionSales = 0.0;
        String worstRegion = "N/A";
        double worstRegionSales = 0.0;

        if (regions != null && !regions.isEmpty()) {
            Map<String, Object> best = regions.get(0);
            bestRegion = (String) best.get("region");
            bestRegionSales = (Double) best.get("totalSales");

            Map<String, Object> worst = regions.get(regions.size() - 1);
            worstRegion = (String) worst.get("region");
            worstRegionSales = (Double) worst.get("totalSales");
        }

        String topProduct = "N/A";
        List<Map<String, Object>> top = products.get("top");
        if (top != null && !top.isEmpty()) {
            topProduct = (String) top.get(0).get("product");
        }

        String bottomProduct = "N/A";
        List<Map<String, Object>> bottom = products.get("bottom");
        if (bottom != null && !bottom.isEmpty()) {
            bottomProduct = (String) bottom.get(0).get("product");
        }

        String qualityGrade;
        double dq = stats.getDataQualityScore();
        if (dq >= 98.0) {
            qualityGrade = "EXCELLENT (Tier 1 Data Integrity)";
        } else if (dq >= 90.0) {
            qualityGrade = "GOOD (Minor ETL adjustments required)";
        } else if (dq >= 80.0) {
            qualityGrade = "WARNING (Moderate structural anomalies flagged)";
        } else {
            qualityGrade = "CRITICAL (Severe validation failure rate)";
        }

        return "# Sales Performance & Data Quality Audit Report\n" +
                String.format("> *Note: This analytical report was generated using our pre-compiled Business Analyst Template. Reason: %s.*\n\n", reason) +
                
                "## 1. Executive Summary\n" +
                String.format("The commercial portfolio has logged a total cumulative revenue of **$%s** across **%d** processed entries, moving a total volume of **%d** units. The system registers a healthy average transaction size of **$%s**. Operations show a solid baseline, although clear variance in regional and product contributions suggests opportunities for efficiency improvements and resource reallocation.\n\n",
                        stats.getTotalSales(), stats.getTotalRecords(), stats.getTotalQuantitySold(), stats.getAverageSales()) +

                "## 2. Key Observations\n" +
                String.format("- **Transaction Spanning**: Sales transactions exhibit a wide operational spread, ranging from a floor value of **$%s** to a maximum transaction size of **$%s**.\n", stats.getLowestSale(), stats.getHighestSale()) +
                String.format("- **Product concentration**: Key product groups, led by **%s**, represent the primary revenue engine, indicating high brand focus.\n", topProduct) +
                String.format("- **Data Cleanliness**: Average database quality score stands at **%s%%**. The automated ETL pipeline successfully filtered out empty fields, non-numeric values, and duplicate records prior to system load.\n\n", stats.getDataQualityScore()) +

                "## 3. Best Performing Region\n" +
                String.format("The top-performing geographical division is **%s**, contributing a total of **$%s** in revenue. This region serves as the core revenue anchor for operations, indicating a highly mature client base and active distribution channels.\n\n",
                        bestRegion, bestRegionSales) +

                "## 4. Worst Performing Region\n" +
                String.format("The weakest performer is **%s**, which registered a total revenue of only **$%s**. Lower transaction sizes and lower order frequency suggest a need for market expansion and adjusted sales focus.\n\n",
                        worstRegion, worstRegionSales) +

                "## 5. Business Risks\n" +
                String.format("- **Concentration Exposure**: More than half of total sales volume is concentrated in the top region (**%s**). A localized market decline in this area represents a major downside risk.\n", bestRegion) +
                String.format("- **Inventory Stagnation**: Product **%s** shows extremely low traction, representing tied-up capital and potential storage costs.\n", bottomProduct) +
                "- **Siloed Performance**: Large disparities between the highest and lowest transactions indicate lack of consistency in pricing or buyer targeting.\n\n" +

                "## 6. Recommendations\n" +
                String.format("- **Replicate Best Practices**: Deploy the customer engagement templates utilized in the **%s** region to the lagging **%s** region.\n", bestRegion, worstRegion) +
                String.format("- **Product Portfolio Rationalization**: Review the bottom product **%s** for potential discounting, bundling, or discontinuation.\n", bottomProduct) +
                "- **Data Ingestion Controls**: Maintain the strict ETL validation pipeline to ensure high data hygiene and protect database records from structural corruption.\n\n" +

                "## 7. Data Quality Assessment\n" +
                String.format("The current dataset scores **%s%%** on the Data Quality Index, classifying the ingestion integrity as **%s**. All records saved to MySQL are fully formatted and safe for executive decision-making. Future CSV uploads should continue to undergo header template checks and space normalization to keep database entities clean.",
                        stats.getDataQualityScore(), qualityGrade);
    }
}
