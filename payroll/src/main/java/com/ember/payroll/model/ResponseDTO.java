package com.ember.payroll.model;

import java.util.ArrayList;
import java.util.List;

public class ResponseDTO {
    private double gross;
    private double ssi_employee;
    private double unemployment_employee;
    private List<Integer> brackets;
    private double income_tax;
    private double stamp_tax;
    private double min_wage_exempt_tax;
    private double net;
    private double ssi_employer;
    private double unemployment_employer;
    private double ssi_unemployment_payment;
    private double stamp_payment;
    private double income_tax_payment;
    private double cost;

    public ResponseDTO() {
        this.brackets = new ArrayList<>();
    }

    public void nullifyAll() {
        this.gross = 0;
        this.ssi_employee = 0;
        this.unemployment_employee = 0;
        this.income_tax = 0;
        this.stamp_tax = 0;
        this.min_wage_exempt_tax = 0;
        this.net = 0;
        this.ssi_employer = 0;
        this.unemployment_employer = 0;
        this.ssi_unemployment_payment = 0;
        this.stamp_payment = 0;
        this.income_tax_payment = 0;
        this.cost = 0;
    }

    public void addBracket(int bracket) {
        brackets.add(bracket);
    }

    public double getGross() {
        return gross;
    }

    public void setGross(double gross) {
        this.gross = gross;
    }

    public double getSsi_employee() {
        return ssi_employee;
    }

    public double setSsi_employee(double ssi_employee) {
        this.ssi_employee = ssi_employee;
        return ssi_employee;
    }

    public double getUnemployment_employee() {
        return unemployment_employee;
    }

    public double setUnemployment_employee(double unemployment_employee) {
        this.unemployment_employee = unemployment_employee;
        return unemployment_employee;
    }

    public List<Integer> getBrackets() {
        return brackets;
    }

    public void setBrackets(List<Integer> brackets) {
        this.brackets = brackets;
    }

    public double getIncome_tax() {
        return income_tax;
    }

    public double setIncome_tax(double income_tax) {
        this.income_tax = income_tax;
        return income_tax;
    }

    public double getStamp_tax() {
        return stamp_tax;
    }

    public double setStamp_tax(double stamp_tax) {
        this.stamp_tax = stamp_tax;
        return stamp_tax;
    }

    public double getMin_wage_exempt_tax() {
        return min_wage_exempt_tax;
    }

    public double setMin_wage_exempt_tax(double min_wage_exempt_tax) {
        this.min_wage_exempt_tax = min_wage_exempt_tax;
        return min_wage_exempt_tax;
    }

    public double getNet() {
        return net;
    }

    public void setNet(double net) {
        this.net = net;
    }

    public double getSsi_employer() {
        return ssi_employer;
    }

    public double setSsi_employer(double ssi_employer) {
        this.ssi_employer = ssi_employer;
        return ssi_employer;
    }

    public double getUnemployment_employer() {
        return unemployment_employer;
    }

    public double setUnemployment_employer(double unemployment_employer) {
        this.unemployment_employer = unemployment_employer;
        return unemployment_employer;
    }

    public double getSsi_unemployment_payment() {
        return ssi_unemployment_payment;
    }

    public void setSsi_unemployment_payment(double ssi_unemployment_payment) {
        this.ssi_unemployment_payment = ssi_unemployment_payment;
    }

    public double getStamp_payment() {
        return stamp_payment;
    }

    public void setStamp_payment(double stamp_payment) {
        this.stamp_payment = stamp_payment;
    }

    public double getIncome_tax_payment() {
        return income_tax_payment;
    }

    public void setIncome_tax_payment(double income_tax_payment) {
        this.income_tax_payment = income_tax_payment;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }
}
