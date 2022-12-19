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

    @PostMapping("/tr/planned-format")
    public void plannedFormatTest(@RequestBody PayloadDTO payloadDTO) {
        payloadDTO.getYearlyReport().forEach(System.out::println);
        System.out.println(payloadDTO.getEmployeeType());
    }

    // GROSS SALARY TO NET SALARY CONVERSION
    @PostMapping("/tr/gross-to-net")
    public List<ResponseDTO> trGrossToNet(@RequestBody PayloadDTO payload) {
        return salaryService.trGrossToNet_INNER(payload);
    }

    // NET SALARY TO GROSS SALARY CONVERSION
    @PostMapping("/tr/net-to-gross")
    public List<ResponseDTO> trNetToGross(@RequestBody PayloadDTO payload) {
        return salaryService.trNetToGross_INNER(payload);
    }

    // NET SALARY TO EMPLOYER COST CONVERSION
    @PostMapping("/tr/net-to-cost")
    public List<ResponseDTO> trNetToCost(@RequestBody PayloadDTO payload) {
        List<ResponseDTO> yearlyReport = salaryService.trNetToGross_INNER(payload);
        payload.setYearlyReport(yearlyReport.stream().flatMapToDouble(element -> DoubleStream.of(element.getGross())).boxed().collect(Collectors.toList()));
        return salaryService.trGrossToCost_INNER(payload);
    }

    // GROSS SALARY TO EMPLOYER COST CONVERSION
    @PostMapping("/tr/gross-to-cost")
    public List<ResponseDTO> trGrossToCost(@RequestBody PayloadDTO payload) {
        return salaryService.trGrossToCost_INNER(payload);
    }
}
