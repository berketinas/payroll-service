package com.ember.payroll.model;

import java.util.List;

// FOR EASIER FUTURE REQUEST FORMAT MODIFICATIONS
public class Payload {
    List<Double> yearlyReport;
    String employeeType;

    public Payload(List<Double> yearlyReport, String employeeType) {
        this.yearlyReport = yearlyReport;
        this.employeeType = employeeType;
    }

    public List<Double> getYearlyReport() {
        return yearlyReport;
    }

    public void setYearlyReport(List<Double> yearlyReport) {
        this.yearlyReport = yearlyReport;
    }

    public String getEmployeeType() {
        return employeeType;
    }

    public void setEmployeeType(String employeeType) {
        this.employeeType = employeeType;
    }
}
