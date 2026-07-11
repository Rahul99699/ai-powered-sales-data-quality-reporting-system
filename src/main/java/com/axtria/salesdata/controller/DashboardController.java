package com.axtria.salesdata.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    /**
     * Renders the main dashboard page
     */
    @GetMapping({"/", "/dashboard"})
    public String viewDashboard() {
        return "dashboard";
    }
}
