package itsdc.dolphin.do2.auditcage;
import java.util.Date;

import itsdc.dolphin.models.PayCode;
 

public class DueDrawnElement {
	private PayCode payCode;
	//private Date fromDate;
	//private Date toDate;
	private double due;
	private double drawn;
	//private Do2 do2;
	//private Boolean arrestPayment;
	
/*	
	public Do2 getDo2() {
		return do2;
	}
	public void setDo2(Do2 do2) {
		this.do2 = do2;
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
		public Boolean getArrestPayment() {
		return arrestPayment;
	}
	public void setArrestPayment(Boolean arrestPayment) {
		this.arrestPayment = arrestPayment;
	}
*/	
	public PayCode getPayCode() {
		return payCode;
	}
	public void setPayCode(PayCode payCode) {
		this.payCode = payCode;
	}
	public double getDue() {
		return due;
	}
	public void setDue(double due) {
		this.due = due;
	}
	public double getDrawn() {
		return drawn;
	}
	public void setDrawn(double drawn) {
		this.drawn = drawn;
	}

}
