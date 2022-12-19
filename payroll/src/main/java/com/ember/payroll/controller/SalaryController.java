package com.ember.payroll.controller;

import com.ember.payroll.model.Payload;
import com.ember.payroll.service.SalaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;

@RestController
public class SalaryController {
    private final SalaryService salaryService;

    public SalaryController(SalaryService salaryService) {
        this.salaryService = salaryService;
    }

    // ROUTE TO TEST WHETHER LOADED PROPERTIES ARE CORRECT
    @GetMapping("/")
    public void testProperties() {
        salaryService.testProperties();
    }

    @GetMapping("/tr/planned-format")
    public void plannedFormatTest(@RequestBody Payload payload) {
        payload.getYearlyReport().forEach(System.out::println);
        System.out.println(payload.getEmployeeType());
    }

    // GROSS SALARY TO NET SALARY CONVERSION
    @GetMapping("/tr/gross-to-net")
    public LinkedHashMap<String, Double> trGrossToNet(@RequestBody LinkedHashMap<String, Double> yearlyReport) {
        return salaryService.trGrossToNet_INNER(yearlyReport);
    }

    // NET SALARY TO GROSS SALARY CONVERSION
    @GetMapping("/tr/net-to-gross")
    public LinkedHashMap<String, Double> trNetToGross(@RequestBody LinkedHashMap<String, Double> yearlyReport) {
        return salaryService.trNetToGross_INNER(yearlyReport);
    }

    // NET SALARY TO EMPLOYER COST CONVERSION
    @GetMapping("/tr/net-to-cost")
    public LinkedHashMap<String, Double> trNetToCost(@RequestBody LinkedHashMap<String, Double> yearlyReport) {
        return salaryService.trGrossToCost_INNER(salaryService.trNetToGross_INNER(yearlyReport));
    }

    // GROSS SALARY TO EMPLOYER COST CONVERSION
    @GetMapping("/tr/gross-to-cost")
    public LinkedHashMap<String, Double> trGrossToCost(@RequestBody LinkedHashMap<String, Double> yearlyReport) {
        return salaryService.trGrossToCost_INNER(yearlyReport);
    }
}
