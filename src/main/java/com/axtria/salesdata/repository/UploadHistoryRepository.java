package com.axtria.salesdata.repository;

import com.axtria.salesdata.entity.UploadHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface UploadHistoryRepository extends JpaRepository<UploadHistory, Long> {

    // Fetch the last 10 uploads ordered by upload time descending
    List<UploadHistory> findTop10ByOrderByUploadTimeDesc();

    // Average data quality score across all uploads
    @Query("SELECT AVG(u.dataQualityScore) FROM UploadHistory u")
    Double getAverageDataQualityScore();
}
