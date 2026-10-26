package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Arrears;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT&SDC Secunderabad
 * 
 */

@RestController
public class Serfit extends AuditCageBase implements
		 Do2AuditCageProcessor, java.io.Serializable {
	 
	String message;
	Do2 refDo2;

	static public String INVALID_REFDO2 = "AU:Incorrect Reference Do2.\n ";
	static public String INVALID_TJRA = "VL:No of months to be mentioned in TJRA column.\n";
	static public String INVALID_LRA = "VL:No of days to be mentioned or more than 30 days in LRA column .\n";
	static public String INVALID_COL7A = "VL:Column 7A should indicate PEN.\n";
	
	private StringBuffer buffer = new StringBuffer();
	private double do2Days = 0.0;
	Date fromDate, toDate;

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		do2.setStatus(Do2Manager.VALID);
		
		if (!isValidRefDo2(do2)) {
			validity = false;
			buffer.append(INVALID_REFDO2);
		}
		if (!isValidTjra(do2)) {
			validity = false;
			buffer.append(INVALID_TJRA);
		}
		if (!isValidLra(do2)) {
			validity = false;
			buffer.append(INVALID_LRA);
		}
		if (!isValidCol7A(do2)) {
			validity = false;
			buffer.append(INVALID_COL7A);
		}
		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
			message = buffer.toString();
			do2.setReason(message);
	 	}
		do2Repo.save(do2);
	
		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage,
			AuditCage baseAuditCage) {
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PUNISHMENT+"");
		if (do2.getColumnB() != null && do2.getColumnC() != null){
			recoveryPayAndAllowances(do2, baseAuditCage);
		}
		return Do2Manager.SUCCESS;
	}

	public boolean isValidCol7A(Do2 do2) {
		if (do2.getColumnA() != null && do2.getColumnA().equals("PEN")) {
			return true;
		}
		return false;
	}

	public boolean isValidLra(Do2 do2) {
		if (do2.getLra() == null || (do2.getLra() >= 0 && do2.getLra() <= 30)) {
			return true;
		}
		return false;
	}

	public boolean isValidTjra(Do2 do2) {
		if (do2.getTjra() == null || (do2.getTjra() >= 0 && do2.getTjra() <= 60)) {
			return true;
		} 
		return false;
	}

	public boolean isValidRefDo2(Do2 do2) {
		refDo2 = Utils.getReferenceDo2(do2);
		if (refDo2 != null	&& (refDo2.getDescription().equals(Do2Manager.AWL)
				|| refDo2.getDescription().equals(Do2Manager.OSL)
				|| refDo2.getDescription().equals(Do2Manager.REP)
				|| refDo2.getDescription().equals(Do2Manager.SEVREP))) {
			return true;
		} 
		return false;
	}
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_PUNISHMENT);
		return Do2Manager.SUCCESS;
	}
	
	public Date getDate(String date) {
		Date date1=null;
		int year = Integer.valueOf(date.substring(4, 8));
		int month = Integer.valueOf(date.substring(2, 4));
		int day = Integer.valueOf(date.substring(0, 2));
		try {
			date1 = (Date) DateManager.getDate(year, month, day);
		} catch (Exception e) {
			Utils.handleException(e);
		}
		return date1;
	}
   
	public void recoveryPayAndAllowances(Do2 do2, AuditCage baseAuditCage){
    	if (do2.getColumnB() != null && do2.getColumnC() != null) {
    	List<AuditCageAllowance> acaList = acaRepo.findAllByAuditCageId(newAuditCage.getId());
    			
    /*			Utils
		.getEntityManager()
		.createQuery(
				"SELECT aca FROM AuditCageAllowance aca WHERE aca.auditCage = :newAuditCage")
		.setParameter("newAuditCage", newAuditCage)
		.getResultList();*/

 			fromDate = (Date) getDate(do2.getColumnB());
			toDate = (Date) getDate(do2.getColumnC());
			try {
			do2Days = DateManager.getNoOfMonths(fromDate, toDate);
			} catch (Exception e){
				Utils.handleException(e);
			}
			
		Utils.addArrears(do2, Math.round((baseAuditCage.getBasicPay() == null ? 0 : baseAuditCage.getBasicPay()) * - do2Days), Do2Manager.BASIC_PAY, fromDate, toDate);
		Utils.addArrears(do2, Math.round((baseAuditCage.getDp() == null ? 0 : baseAuditCage.getDp()) * - do2Days), Do2Manager.DP, fromDate, toDate);
		Utils.addArrears(do2, Math.round((baseAuditCage.getGradePay() == null ? 0	: baseAuditCage.getGradePay()) * - do2Days), Do2Manager.GRADE_PAY, fromDate, toDate);
		Utils.addArrears(do2, Math.round((baseAuditCage.getMsPay() == null ? 0 : baseAuditCage.getMsPay() * - do2Days)),	Do2Manager.MS_PAY, fromDate, toDate);
		Utils.addArrears(do2, Math.round((baseAuditCage.getXGroupPay() == null ? 0	: baseAuditCage.getXGroupPay())	* - do2Days), Do2Manager.X_GROUP_PAY,	fromDate, toDate);
		Utils.addArrears(do2, Math.round((baseAuditCage.getLra() == null ? 0 : baseAuditCage.getLra() * - do2Days)), Do2Manager.LRA, fromDate, toDate);
		Utils.addArrears(do2, Math.round((baseAuditCage.getPmha() == null ? 0 : baseAuditCage.getPmha() * - do2Days)), Do2Manager.PMHAL, fromDate, toDate);
		Utils.addArrears(do2, Math.round((baseAuditCage.getDa() == null ? 0 : baseAuditCage.getDa()	* - do2Days)), Do2Manager.DA, fromDate,	toDate);
		Utils.addArrears(do2, Math.round((baseAuditCage.getClassPay() == null ? 0 : baseAuditCage.getClassPay() * - do2Days)), Do2Manager.CLASS_PAY, fromDate, toDate);
		Utils.addArrears(do2, Math.round((baseAuditCage.getTransport() == null ? 0	: baseAuditCage.getTransport() * - do2Days)), Do2Manager.TPTL, fromDate, toDate);
    	
			if (acaList.size() > 0) {
				for (int i = 0; i < acaList.size(); i++) {
					AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
					AuditCageHelper.removeAllowances(Arrays.asList(new String[] { acaList.get(i).getPayCode().getPayCode()}), newAuditCage);
				}
			}
    	}
    }
}
