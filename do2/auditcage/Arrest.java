package itsdc.dolphin.do2.auditcage;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
 
import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 
 

@RestController
public class Arrest extends AuditCageBase implements
		 Do2AuditCageProcessor, java.io.Serializable {
 
	private StringBuffer buffer = new StringBuffer();
	String message;
	static public String INVALID_COLUMN7A = "VL:Invalid percentage in Col. 7A.\n ";
	static public String INVALID_DATE_IN_COLUMN_B = "VL:Invalid Date in Column 7B. \n ";
	static public String INVALID_DATE_IN_COLUMN_C = "VL:Invalid Date in Column 7C. \n ";
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	String withheldPercentage;
	double do2Days;
	float paymentMultiplier;
	Date fromDate, toDate;

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if (do2.getColumnA() != null) {
			if (!isValid7a(do2)) {
				validity = false;
				buffer.append(INVALID_COLUMN7A + NOT_IN_APPENDIX_J_FORMAT);
			}
		}

		if (do2.getColumnB() != null) {
			if (!isValidDate(do2.getColumnB())) {
				validity = false;
				buffer.append(INVALID_DATE_IN_COLUMN_B + NOT_IN_APPENDIX_J_FORMAT);
			} else {
				if (do2.getColumnC() != null) {
					if (!isValidDate(do2.getColumnC())) {
						validity = false;
						buffer.append(INVALID_DATE_IN_COLUMN_C + NOT_IN_APPENDIX_J_FORMAT);
					}
				}

			}
		}
		if (validity == false) {
			do2.setStatus("MR");
			message = buffer.toString();
			do2.setReason(message);

		} else {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
		}
	do2Repo.save(do2);
	return validity;
	}

	public boolean isValidDate(String dateStr) {
		int year = Integer.valueOf(dateStr.substring(4, 8));
		int month = Integer.valueOf(dateStr.substring(2, 4));
		int day = Integer.valueOf(dateStr.substring(0, 2));
		try {
			DateManager.getDate(year, month, day);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean isValid7a(Do2 do2) {
		
		// The folloiwng method commented as the record office is only publishing number i.e 25 instead of 25%  and intimated 
		// that there is no facility to add % in column A in there HRMS)
		
		/*if(do2.getColumnA()!=null && !do2.getColumnA().endsWith("%")){
			return false;
		} */
		
		paymentMultiplier= 0;
		withheldPercentage = do2.getColumnA();
		
		
		if (withheldPercentage != null && withheldPercentage.length() != 0
				&& withheldPercentage.charAt(withheldPercentage.length() - 1) == '%') {
			withheldPercentage = do2.getColumnA().substring(0,withheldPercentage.length() - 1);
		}
			
			try {
				paymentMultiplier = (100 - Integer.parseInt(withheldPercentage));

			} catch (Exception e) {
				return false;
			}
		
			
		
		if(paymentMultiplier <0)
			return false;
		
		return paymentMultiplier <= 100;

	}

	public Date getDate(String date) {
		Date date1 = null;
		int year = Integer.valueOf(date.substring(4, 8));
		int month = Integer.valueOf(date.substring(2, 4));
		int day = Integer.valueOf(date.substring(0, 2));
		try {
			date1 = DateManager.getDate(year, month, day);
		} catch (Exception e) {
			Utils.handleException(e);
		}
		return date1;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
     // CGDA letter No. AT/I/3510/MPS/XII dated 23.07.2014 - not to stop pay and allowances
	//	newAuditCage.setAbsentFlag(true);
		newAuditCage.setTransport(0);
		newAuditCage.setPmha(0);
		newAuditCage.setLra(0);
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		removeAllowances(newAuditCage);

		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PUNISHMENT+"");
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		List<EmployeeServiceDetail> esdList=esdRepo.findByDo2(refDo2.getId());
		
		if(esdList!=null && esdList.size()>0) {
			for(EmployeeServiceDetail esd:esdList) {
				esdRepo.delete(esd);
			}
		}
		 
		return Do2Manager.SUCCESS;
	}

	public void removeAllowances(AuditCage auditCage) {
		List<AuditCageAllowance> newAuditCageAllowanceList = acaRepo.findAllByAuditCageId(newAuditCage.getId());
		 

		for (AuditCageAllowance aca : newAuditCageAllowanceList) {

			if (!aca.getPayCode().getPayCode().equals(Do2Manager.STRINC)) {
				acaRepo.delete(aca);

			}
		}
	}
}