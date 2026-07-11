// Global chart variables to prevent hover glitches by destroying old instances
let regionChartInstance = null;
let productChartInstance = null;

document.addEventListener("DOMContentLoaded", () => {
    // Initialize Dashboard Data
    refreshAllData();

    // Set up Drag and Drop Events for Ingestion
    initDragAndDrop();
});

/**
 * Re-fetches all backend resources to update the UI
 */
function refreshAllData() {
    loadDashboardStats();
    loadRegionAnalytics();
    loadProductAnalytics();
    loadUploadHistory();
    performSearch(0);
}

/**
 * Loads KPI summary stats cards
 */
function loadDashboardStats() {
    fetch("/api/dashboard/stats")
        .then(response => response.json())
        .then(stats => {
            document.getElementById("kpiTotalSales").innerText = formatCurrency(stats.totalSales);
            document.getElementById("kpiAvgSales").innerText = formatCurrency(stats.averageSales);
            document.getElementById("kpiMaxSale").innerText = formatCurrency(stats.highestSale);
            document.getElementById("kpiMinSale").innerText = formatCurrency(stats.lowestSale);
            document.getElementById("kpiTotalQuantity").innerText = stats.totalQuantitySold.toLocaleString();
            document.getElementById("kpiTotalRecords").innerText = stats.totalRecords.toLocaleString();
            
            const dqScore = stats.dataQualityScore;
            document.getElementById("kpiDqScore").innerText = dqScore.toFixed(2) + "%";

            // Update DQ KPI Card styling based on score
            const dqCard = document.getElementById("dqCard");
            const dqLabel = document.getElementById("dqLabel");
            const dqIcon = document.getElementById("dqIconWrapper");

            dqCard.className = "metric-card"; // reset
            if (stats.totalRecords === 0) {
                dqLabel.innerText = "No data uploaded yet";
                dqIcon.innerHTML = '<i class="bi bi-shield-slash fs-2 text-secondary"></i>';
            } else if (dqScore >= 95.0) {
                dqCard.classList.add("success-card");
                dqLabel.innerText = "Excellent data health. Safe for forecasting.";
                dqIcon.innerHTML = '<i class="bi bi-shield-check fs-2 text-success"></i>';
            } else if (dqScore >= 80.0) {
                dqCard.style.borderLeft = "6px solid #f59e0b";
                dqLabel.innerText = "Moderate anomalies. Review validation reports.";
                dqIcon.innerHTML = '<i class="bi bi-shield-exclamation fs-2 text-warning"></i>';
            } else {
                dqCard.style.borderLeft = "6px solid #ef4444";
                dqLabel.innerText = "Critical issues. Large amount of corrupted data skipped.";
                dqIcon.innerHTML = '<i class="bi bi-shield-x fs-2 text-danger"></i>';
            }
        })
        .catch(err => console.error("Error loading stats:", err));
}

/**
 * Loads Region Statistics and renders Bar Chart
 */
function loadRegionAnalytics() {
    fetch("/api/analytics/region")
        .then(response => response.json())
        .then(data => {
            const tableBody = document.getElementById("regionStatsTableBody");
            tableBody.innerHTML = "";

            if (data.length === 0) {
                tableBody.innerHTML = '<tr><td colspan="3" class="text-center text-muted py-3">No regional data.</td></tr>';
                renderRegionChart([], []);
                return;
            }

            const regions = [];
            const salesValues = [];

            data.forEach(row => {
                regions.push(row.region);
                salesValues.push(row.totalSales);

                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td class="fw-bold">${row.region}</td>
                    <td class="text-end fw-semibold">${formatCurrency(row.totalSales)}</td>
                    <td class="text-end text-muted">${formatCurrency(row.averageSales)}</td>
                `;
                tableBody.appendChild(tr);
            });

            renderRegionChart(regions, salesValues);
        })
        .catch(err => console.error("Error loading region analytics:", err));
}

/**
 * Loads Product statistics and renders Pie Chart
 */
function loadProductAnalytics() {
    fetch("/api/analytics/product")
        .then(response => response.json())
        .then(data => {
            const topList = document.getElementById("topProductsList");
            const bottomList = document.getElementById("bottomProductsList");

            topList.innerHTML = "";
            bottomList.innerHTML = "";

            const topProducts = data.top || [];
            const bottomProducts = data.bottom || [];

            if (topProducts.length === 0) {
                topList.innerHTML = '<li class="text-muted">No data available</li>';
                bottomList.innerHTML = '<li class="text-muted">No data available</li>';
                renderProductChart([], []);
                return;
            }

            const pieLabels = [];
            const pieData = [];

            topProducts.forEach(p => {
                pieLabels.push(p.product);
                pieData.push(p.totalSales);

                const li = document.createElement("li");
                li.className = "mb-1 fw-medium";
                li.innerHTML = `${p.product} <span class="text-success float-end">${formatCurrency(p.totalSales)}</span>`;
                topList.appendChild(li);
            });

            bottomProducts.forEach(p => {
                const li = document.createElement("li");
                li.className = "mb-1 text-muted";
                li.innerHTML = `${p.product} <span class="text-danger float-end">${formatCurrency(p.totalSales)}</span>`;
                bottomList.appendChild(li);
            });

            renderProductChart(pieLabels, pieData);
        })
        .catch(err => console.error("Error loading product rankings:", err));
}

/**
 * Loads the last 10 CSV Ingestions history log
 */
function loadUploadHistory() {
    fetch("/api/upload-history")
        .then(response => response.json())
        .then(history => {
            const tableBody = document.getElementById("historyTableBody");
            tableBody.innerHTML = "";

            if (history.length === 0) {
                tableBody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">No uploads registered yet.</td></tr>';
                return;
            }

            history.forEach(item => {
                const score = item.dataQualityScore;
                let badgeClass = "badge-success";
                if (score < 80.0) badgeClass = "badge-danger";
                else if (score < 95.0) badgeClass = "badge-warning";

                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td class="fw-semibold text-truncate" style="max-width: 180px;" title="${item.fileName}">${item.fileName}</td>
                    <td class="text-muted">${formatDateTime(item.uploadTime)}</td>
                    <td class="text-center fw-medium">${item.totalRecords}</td>
                    <td class="text-center text-success fw-medium">${item.validRecords}</td>
                    <td class="text-center text-danger fw-medium">${item.invalidRecords}</td>
                    <td class="text-center"><span class="badge ${badgeClass} px-2 py-1">${score.toFixed(2)}%</span></td>
                `;
                tableBody.appendChild(tr);
            });
        })
        .catch(err => console.error("Error loading upload history:", err));
}

/**
 * Handles search filtering with server-side pagination
 */
function performSearch(page) {
    const product = document.getElementById("searchProduct").value;
    const region = document.getElementById("searchRegion").value;
    const dateFrom = document.getElementById("searchDateFrom").value;
    const dateTo = document.getElementById("searchDateTo").value;

    const url = `/api/search?product=${encodeURIComponent(product)}&region=${encodeURIComponent(region)}&dateFrom=${dateFrom}&dateTo=${dateTo}&page=${page}&size=10`;

    fetch(url)
        .then(response => response.json())
        .then(resPage => {
            const tableBody = document.getElementById("searchResultsTableBody");
            tableBody.innerHTML = "";

            document.getElementById("searchTotalCount").innerText = `${resPage.totalElements} records found`;

            if (resPage.content.length === 0) {
                tableBody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">No records matching search filters.</td></tr>';
                renderPagination(0, 0);
                return;
            }

            resPage.content.forEach(rec => {
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td>${rec.id}</td>
                    <td class="fw-semibold">${rec.product}</td>
                    <td><span class="badge bg-secondary px-2 py-1 small">${rec.region}</span></td>
                    <td class="text-end fw-medium">${formatCurrency(rec.sales)}</td>
                    <td class="text-center">${rec.quantity}</td>
                    <td class="text-center text-muted">${rec.date}</td>
                    <td class="text-muted small">${formatDateTime(rec.createdAt)}</td>
                `;
                tableBody.appendChild(tr);
            });

            renderPagination(resPage.totalPages, resPage.number);
        })
        .catch(err => console.error("Error searching records:", err));
}

/**
 * Builds standard Bootstrap pagination controls
 */
function renderPagination(totalPages, currentPage) {
    const container = document.getElementById("searchPagination");
    container.innerHTML = "";

    if (totalPages <= 1) return;

    // Previous Button
    const prevLi = document.createElement("li");
    prevLi.className = `page-item ${currentPage === 0 ? 'disabled' : ''}`;
    prevLi.innerHTML = `<a class="page-link" href="#" onclick="event.preventDefault(); performSearch(${currentPage - 1})">Previous</a>`;
    container.appendChild(prevLi);

    // Page Numbers
    for (let i = 0; i < totalPages; i++) {
        const pageLi = document.createElement("li");
        pageLi.className = `page-item ${i === currentPage ? 'active' : ''}`;
        pageLi.innerHTML = `<a class="page-link" href="#" onclick="event.preventDefault(); performSearch(${i})">${i + 1}</a>`;
        container.appendChild(pageLi);
    }

    // Next Button
    const nextLi = document.createElement("li");
    nextLi.className = `page-item ${currentPage === totalPages - 1 ? 'disabled' : ''}`;
    nextLi.innerHTML = `<a class="page-link" href="#" onclick="event.preventDefault(); performSearch(${currentPage + 1})">Next</a>`;
    container.appendChild(nextLi);
}

/**
 * Triggers summary CSV download from backend
 */
function triggerExport() {
    window.location.href = "/api/export";
}

/**
 * Resets database tables
 */
function resetSystem() {
    if (confirm("Are you sure you want to delete all loaded sales records and upload logs? This cannot be undone.")) {
        fetch("/api/reset", { method: "POST" })
            .then(res => res.json())
            .then(data => {
                alert(data.message || "Database wiped");
                refreshAllData();
                // Clear AI summary box
                document.getElementById("aiReportPlaceholder").classList.remove("d-none");
                document.getElementById("aiReportContent").classList.add("d-none");
            })
            .catch(err => console.error("Reset failed:", err));
    }
}

/**
 * Calls AI summarizing generator, rendering markdown dynamically
 */
function generateAiReport() {
    const placeholder = document.getElementById("aiReportPlaceholder");
    const loader = document.getElementById("aiLoader");
    const content = document.getElementById("aiReportContent");

    placeholder.classList.add("d-none");
    loader.classList.remove("d-none");
    content.classList.add("d-none");

    fetch("/api/report")
        .then(response => response.json())
        .then(data => {
            loader.classList.add("d-none");
            content.classList.remove("d-none");
            
            // marked.parse handles conversion of Markdown string to HTML securely
            content.innerHTML = marked.parse(data.report);
            
            // Show the PDF download button
            const pdfBtn = document.getElementById("btnDownloadPdf");
            if(pdfBtn) pdfBtn.classList.remove("d-none");
        })
        .catch(err => {
            loader.classList.add("d-none");
            content.classList.remove("d-none");
            content.innerHTML = `<div class="alert alert-danger py-2 px-3"><strong>Error generating report:</strong> ${err.message}</div>`;
        });
}

/**
 * Exports the AI Report container to a downloadable PDF
 */
function downloadAiReportPdf() {
    const element = document.getElementById("aiReportContent");
    const opt = {
      margin:       0.5,
      filename:     'AI_Sales_Report.pdf',
      image:        { type: 'jpeg', quality: 0.98 },
      html2canvas:  { scale: 2 },
      jsPDF:        { unit: 'in', format: 'letter', orientation: 'portrait' }
    };
    html2pdf().set(opt).from(element).save();
}

/**
 * Handles Drag & Drop File Upload listeners
 */
function initDragAndDrop() {
    const dropzone = document.getElementById("dropzone");
    const fileInput = document.getElementById("csvFileInput");

    ["dragenter", "dragover"].forEach(eventName => {
        dropzone.addEventListener(eventName, (e) => {
            e.preventDefault();
            dropzone.classList.add("dragover");
        }, false);
    });

    ["dragleave", "drop"].forEach(eventName => {
        dropzone.addEventListener(eventName, (e) => {
            e.preventDefault();
            dropzone.classList.remove("dragover");
        }, false);
    });

    dropzone.addEventListener("drop", (e) => {
        const dt = e.dataTransfer;
        const files = dt.files;
        if (files.length > 0) {
            handleFileUpload(files[0]);
        }
    });

    fileInput.addEventListener("change", (e) => {
        if (fileInput.files.length > 0) {
            handleFileUpload(fileInput.files[0]);
            fileInput.value = ""; // clear input
        }
    });
}

/**
 * Uploads file to /api/upload using AJAX Multipart Request
 */
function handleFileUpload(file) {
    const feedback = document.getElementById("uploadFeedback");
    const summary = document.getElementById("feedbackSummary");
    const errorsContainer = document.getElementById("validationErrorsContainer");
    const errorsBody = document.getElementById("errorsTableBody");

    feedback.classList.remove("d-none");
    summary.className = "alert alert-secondary py-2 px-3 mb-2 small";
    summary.innerHTML = `<i class="spinner-border spinner-border-sm me-2 text-primary" role="status"></i>Uploading and transforming dataset '${file.name}'...`;
    errorsContainer.classList.add("d-none");
    errorsBody.innerHTML = "";

    const formData = new FormData();
    formData.append("file", file);

    fetch("/api/upload", {
        method: "POST",
        body: formData
    })
    .then(async response => {
        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.error || "Upload failed");
        }
        return data;
    })
    .then(res => {
        summary.className = "alert alert-success py-2 px-3 mb-0 small";
        summary.innerHTML = `
            <h6 class="mb-1 fw-bold text-success"><i class="bi bi-check-circle-fill me-1"></i>ETL Load Complete!</h6>
            Dataset: <strong>${res.fileName}</strong> | Total Lines: <strong>${res.totalRecords}</strong><br>
            Loaded Successfully: <strong class="text-success">${res.validRecords}</strong> | 
            Validation Errors (Skipped): <strong class="text-danger">${res.invalidRecords}</strong> | 
            Duplicates Removed: <strong class="text-warning">${res.duplicatesRemoved}</strong><br>
            Quality Integrity: <strong>${res.dataQualityScore.toFixed(2)}%</strong>
        `;

        if (res.errors && res.errors.length > 0) {
            errorsContainer.classList.remove("d-none");
            res.errors.forEach(err => {
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td class="text-center text-muted fw-medium">${err.rowNumber}</td>
                    <td><span class="badge bg-light text-dark border">${err.fieldName}</span></td>
                    <td class="text-truncate" style="max-width:120px;" title="${err.invalidValue || ''}">${err.invalidValue || '<em>NULL</em>'}</td>
                    <td class="text-danger">${err.errorMessage}</td>
                `;
                errorsBody.appendChild(tr);
            });
        }

        // Refresh dashboard content
        refreshAllData();
    })
    .catch(err => {
        summary.className = "alert alert-danger py-2 px-3 mb-0 small";
        summary.innerHTML = `<i class="bi bi-x-circle-fill me-1"></i><strong>Ingestion Rejected:</strong> ${err.message}`;
        errorsContainer.classList.add("d-none");
    });
}

/* Rendering Chart.js Visualizations */

function renderRegionChart(labels, dataValues) {
    const ctx = document.getElementById("regionBarChart").getContext("2d");

    if (regionChartInstance) {
        regionChartInstance.destroy();
    }

    regionChartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Total Revenue ($)',
                data: dataValues,
                backgroundColor: 'rgba(79, 70, 229, 0.75)',
                borderColor: 'rgba(79, 70, 229, 1)',
                borderWidth: 1.5,
                borderRadius: 4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: function(value) { return '$' + value.toLocaleString(); }
                    }
                }
            }
        }
    });
}

function renderProductChart(labels, dataValues) {
    const ctx = document.getElementById("productPieChart").getContext("2d");

    if (productChartInstance) {
        productChartInstance.destroy();
    }

    productChartInstance = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: labels,
            datasets: [{
                data: dataValues,
                backgroundColor: [
                    '#4f46e5',
                    '#10b981',
                    '#f59e0b',
                    '#3b82f6',
                    '#ec4899'
                ],
                borderWidth: 2,
                borderColor: '#ffffff'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: { boxWidth: 12, font: { size: 10 } }
                }
            }
        }
    });
}

/* Helper Utilities */

function formatCurrency(value) {
    if (value === undefined || value === null) return "$0.00";
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
}

function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return "";
    const date = new Date(dateTimeStr);
    return date.toLocaleDateString() + " " + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}
