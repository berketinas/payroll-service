package com.ember.payroll.controller;

import com.ember.payroll.model.PayloadDTO;
import com.ember.payroll.model.ResponseDTO;
import com.ember.payroll.service.SalaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    public void plannedFormatTest(@RequestBody PayloadDTO payloadDTO) {
        payloadDTO.getYearlyReport().forEach(System.out::println);
        System.out.println(payloadDTO.getEmployeeType());
    }

    // GROSS SALARY TO NET SALARY CONVERSION
    @GetMapping("/tr/gross-to-net")
    public List<ResponseDTO> trGrossToNet(@RequestBody PayloadDTO payload) {
        return salaryService.trGrossToNet_INNER(payload);
    }

    // NET SALARY TO GROSS SALARY CONVERSION
    @GetMapping("/tr/net-to-gross")
    public List<ResponseDTO> trNetToGross(@RequestBody PayloadDTO payload) {
        return salaryService.trNetToGross_INNER(payload);
    }

    // NET SALARY TO EMPLOYER COST CONVERSION
//    @GetMapping("/tr/net-to-cost")
//    public LinkedHashMap<String, Double> trNetToCost(@RequestBody PayloadDTO payload) {
//        return salaryService.trGrossToCost_INNER(salaryService.trNetToGross_INNER(payload));
//    }

    // GROSS SALARY TO EMPLOYER COST CONVERSION
//    @GetMapping("/tr/gross-to-cost")
//    public LinkedHashMap<String, Double> trGrossToCost(@RequestBody LinkedHashMap<String, Double> yearlyReport) {
//        return salaryService.trGrossToCost_INNER(yearlyReport);
//    }
}
