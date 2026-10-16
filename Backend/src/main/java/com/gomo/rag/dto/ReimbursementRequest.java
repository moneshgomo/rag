package com.gomo.rag.dto;


import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

public class ReimbursementRequest {

    private MultipartFile bill;
    private String reason;
    private String employeeName;
    private String tlName;
    private String teamName;
    private String currency;
    private BigDecimal amount;

    public MultipartFile getBill() {
        return bill;
    }

    public void setBill(MultipartFile bill) {
        this.bill = bill;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getTlName() {
        return tlName;
    }

    public void setTlName(String tlName) {
        this.tlName = tlName;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}