package de.sedzkitchens.dashboard;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

// Business figures for office, sales and management; installers have no access
@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'OFFICE')")
@RequiredArgsConstructor
public class DashboardController {

	private final DashboardService dashboardService;

	@GetMapping
	public DashboardResponse getDashboard() {
		return dashboardService.getDashboard();
	}

}
