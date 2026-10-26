package itsdc.dolphin.do2;

import java.util.Date;

public class Allowance {
	private double rateAmount;
	private Date fromDate;
	private Date toDate;
	private double ratePercentage;

	private String allowanceName;
	private String subAllowanceName;
	private int payEndRange;	
	
	public Allowance(){	}
	public Allowance(Date fromDate, Date toDate, double rateAmount, double ratePercentage, int payEndRange){
		this.fromDate = fromDate;
		this.toDate = toDate;
		this.rateAmount = rateAmount;
		this.ratePercentage = ratePercentage;
		this.payEndRange = payEndRange;
	}	
	public double getRateAmount() {
		return rateAmount;
	}
	public void setRateAmount(double rateAmount) {
		this.rateAmount = rateAmount;
	}
	public Date getFromDate() {
		return fromDate;
	}
	public void setFromDate(Date fromDate) {
		this.fromDate = fromDate;
	}
	public Date getToDate() {
		return toDate;
	}
	public void setToDate(Date toDate) {
		this.toDate = toDate;
	}
	public double getRatePercentage() {
		return ratePercentage;
	}
	public void setRatePercentage(double ratePercentage) {
		this.ratePercentage = ratePercentage;
	}
	public String getAllowanceName() {
		return allowanceName;
	}
	public void setAllowanceName(String allowanceName) {
		this.allowanceName = allowanceName;
	}
	public String getSubAllowanceName() {
		return subAllowanceName;
	}
	public void setSubAllowanceName(String subAllowanceName) {
		this.subAllowanceName = subAllowanceName;
	}
	public int getPayEndRange() {
		return payEndRange;
	}
	public void setPayEndRange(int payEndRange) {
		this.payEndRange = payEndRange;
	}	
	
}
