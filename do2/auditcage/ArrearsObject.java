package itsdc.dolphin.do2.auditcage;



 

import java.util.List;

import itsdc.dolphin.models.AuditCage;

public class ArrearsObject {
	AuditCage dueAuditCage, drawnAuditCage;
	List<DueDrawnElement> dueDrawnElementList;

	boolean dueEligibility;
	boolean drawnEligibility;
	boolean regularPay;

	public boolean isRegularPay() {
		return regularPay;
	}

	public void setRegularPay(boolean regularPay) {
		this.regularPay = regularPay;
	}

	public boolean isDueEligibility() {
		return dueEligibility;
	}

	public void setDueEligibility(boolean dueEligibility) {
		this.dueEligibility = dueEligibility;
	}

	public boolean isDrawnEligibility() {
		return drawnEligibility;
	}

	public void setDrawnEligibility(boolean drawnEligibility) {
		this.drawnEligibility = drawnEligibility;
	}

	public List<DueDrawnElement> getDueDrawnElementList() {
		return dueDrawnElementList;
	}

	public void setDueDrawnElementList(List<DueDrawnElement> dueDrawnElementList) {
		this.dueDrawnElementList = dueDrawnElementList;
	}

	public AuditCage getDueAuditCage() {
		return dueAuditCage;
	}

	public void setDueAuditCage(AuditCage dueAuditCage) {
		this.dueAuditCage = dueAuditCage;
	}
	public AuditCage getDrawnAuditCage() {
		return drawnAuditCage;
	}

	public void setDrawnAuditCage(AuditCage drawnAuditCage) {
		this.drawnAuditCage = drawnAuditCage;
	}
}