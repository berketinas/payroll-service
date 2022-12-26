package com.ember.payroll.controller;

import com.ember.payroll.model.PayloadDTO;
import com.ember.payroll.model.ResponseDTO;
import com.ember.payroll.service.ConversionService;
import com.ember.payroll.service.SalaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

@RestController
public class SalaryController {
    private final SalaryService salaryService;
    private final ConversionService conversionService;

    public SalaryController(SalaryService salaryService, ConversionService conversionService) {
        this.salaryService = salaryService;
        this.conversionService = conversionService;
    }

    // ROUTE TO TEST WHETHER LOADED PROPERTIES ARE CORRECT
    @GetMapping("/")
    public String testProperties() {
        salaryService.testProperties();

        return "ok";
    }

    // NET SALARY TO EMPLOYER COST CONVERSION
    @PostMapping("/tr/net")
    public List<ResponseDTO> trNetToCost(@RequestBody PayloadDTO payload) {
        payload.setYearlyReport(payload.getYearlyReport().stream().map(conversionService::USDtoTRY).collect(Collectors.toList()));

        List<ResponseDTO> yearlyReport = salaryService.trNetToGross_STANDARD(payload.getYearlyReport());
        List<Double> yearlyGross = yearlyReport.stream().flatMapToDouble(element -> DoubleStream.of(element.getGross())).boxed().collect(Collectors.toList());
        payload.setYearlyReport(yearlyGross);

        return salaryService.trGrossToCost_STANDARD(payload);
    }

    // GROSS SALARY TO EMPLOYER COST CONVERSION
    @PostMapping("/tr/gross")
    public List<ResponseDTO> trGrossToCost(@RequestBody PayloadDTO payload) {
        payload.setYearlyReport(payload.getYearlyReport().stream().map(conversionService::USDtoTRY).collect(Collectors.toList()));

        return salaryService.trGrossToCost_STANDARD(payload);
    }
}
