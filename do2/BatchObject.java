package itsdc.dolphin.do2;

import java.io.Serializable;

import org.springframework.web.bind.annotation.RestController;

 
@RestController
public class BatchObject implements Serializable {

	String batchNo, qe;
	int totalDo2s, validDo2s, invalidDo2s, pendingDo2s, cancelDo2s, backDatedDo2s, mrDo2s,
	 masterMissingDo2s, nonEffectiveDo2s;

	public String getBatchNo() {
		return this.batchNo;
	}

	public void setBatchNo(String batchNo) {
		this.batchNo = batchNo;
	}

	public String getQe() {
		return qe;
	}

	public void setQe(String qe) {
		this.qe = qe;
	}

	public int getTotalDo2s() {
		return totalDo2s;
	}

	public void setTotalDo2s(int totalDo2s) {
		this.totalDo2s = totalDo2s;
	}

	public int getValidDo2s() {
		return validDo2s;
	}

	public void setValidDo2s(int validDo2s) {
		this.validDo2s = validDo2s;
	}

	public int getInvalidDo2s() {
		return invalidDo2s;
	}

	public void setInvalidDo2s(int invalidDo2s) {
		this.invalidDo2s = invalidDo2s;
	}

	public int getPendingDo2s() {
		return pendingDo2s;
	}

	public void setPendingDo2s(int pendingDo2s) {
		this.pendingDo2s = pendingDo2s;
	}

	public int getCancelDo2s() {
		return cancelDo2s;
	}

	public void setCancelDo2s(int cancelDo2s) {
		this.cancelDo2s = cancelDo2s;
	}

	public int getBackDatedDo2s() {
		return backDatedDo2s;
	}

	public void setBackDatedDo2s(int backDatedDo2s) {
		this.backDatedDo2s = backDatedDo2s;
	}
	public int getNonEffectiveDo2s() {
		return nonEffectiveDo2s;
	}

	public void setNonEffectiveDo2s(int nonEffectiveDo2s) {
		this.nonEffectiveDo2s = nonEffectiveDo2s;
	}
	public int getMasterMissingDo2s() {
		return masterMissingDo2s;
	}

	public void setMasterMissingDo2s(int masterMissingDo2s) {
		this.masterMissingDo2s = masterMissingDo2s;
	}

	public int getMrDo2s() {
		return mrDo2s;
	}

	public void setMrDo2s(int mrDo2s) {
		this.mrDo2s = mrDo2s;
	}

}
