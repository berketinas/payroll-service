package com.ember.payroll.controller;

import com.ember.payroll.model.PayloadDTO;
import com.ember.payroll.model.ResponseDTO;
import com.ember.payroll.service.SalaryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

@RestController
@CrossOrigin
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

    // NET SALARY TO EMPLOYER COST CONVERSION
    @PostMapping("/tr/net")
    public List<ResponseDTO> trNetToCost(@RequestBody PayloadDTO payload) {
        List<ResponseDTO> yearlyReport = salaryService.trNetToGross_INNER(payload);
        payload.setYearlyReport(yearlyReport.stream().flatMapToDouble(element -> DoubleStream.of(element.getGross())).boxed().collect(Collectors.toList()));
        return salaryService.trGrossToCost_INNER(payload);
    }

    // GROSS SALARY TO EMPLOYER COST CONVERSION
    @PostMapping("/tr/gross")
    public List<ResponseDTO> trGrossToCost(@RequestBody PayloadDTO payload) {
        return salaryService.trGrossToCost_INNER(payload);
    }
}
