package itsdc.dolphin.do2.auditcage;

import java.util.ArrayList;
import java.util.Date;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
 

 
@RestController
public class Bail extends AuditCageBase implements 
		Do2AuditCageProcessor, java.io.Serializable {

	 
	String message;
	Do2 refDo2,baildo2;
	double do2Days, rateAmount;
	Date fromDate, toDate;
	String withheldPercentage;
	double paymentPercentage;
	static public String INVALID_COLUMN7A = "VL:Invalid percentage in Col. 7A.\n ";
	static public String INVALID_REFDO2 = "AU:Incorrect Ref Do2.\n ";
	static public String INVALID_DATE_IN_COLUMN_B = "VL:Invalid Date in Column 7B. \n ";
	static public String INVALID_DATE_IN_COLUMN_C = "VL:Invalid Date in Column 7C. \n ";
	static public String DATE_IN_COLUMN_B_NOT_MATCHING = "AU:Date in column B not matching with arrest date. \n";
	static public String DATE_IN_COLUMN_C_NOT_MATCHING = "VL:Do2 from_date is not next date of column C date. \n";
	static public String CONVICT_EXISTS = "AU: PBOR already convicted. \n";
	static public String BAIL_EXISTS = "VL:BAIL do2 already exists with same reference do2.\n";
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	private StringBuffer buffer = new StringBuffer();

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		fromDate = null;
		toDate = null;
		if (do2.getColumnA() != null) {
			if (!isValid7a(do2)) {
				validity = false;
				buffer.append(INVALID_COLUMN7A + NOT_IN_APPENDIX_J_FORMAT);
				do2.setStatus("MR");
			}
		}
	//	if (newDo2.getId() == do2.getId()){
			if (!isValidRefDo2(do2)) {
				validity = false;
				buffer.append(INVALID_REFDO2);
			}	
			if (do2.getColumnB() != null) {
				if (!isValidDate(do2.getColumnB())) {
					validity = false;
					buffer.append(INVALID_DATE_IN_COLUMN_B + NOT_IN_APPENDIX_J_FORMAT);
					do2.setStatus("MR");
				} else {
					if (do2.getColumnC() != null) {
						if (!isValidDate(do2.getColumnC())) {
							validity = false;
							buffer.append(INVALID_DATE_IN_COLUMN_C + NOT_IN_APPENDIX_J_FORMAT);
							do2.setStatus("MR");
						} else {
							if (!isValidDateCombination(do2)) {
								validity = false;
							}
						}
					}

				}
			}
	//	}
		if (isConvictExists(do2)) {
			buffer.append(CONVICT_EXISTS);
			validity = false;
		}
		if (validity == false) {
			 if(do2.getStatus() != null)
				 do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
		}
		do2Repo.save(do2);
		message = buffer.toString();
		do2.setReason(message);
		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		Employee employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
		
	/*	if(employee.getStatusCode().getStatusName().equals("EFFECTIVE") 
				|| employee.getStatusCode().getStatusName().equals("NEW")
				|| employee.getStatusCode().getStatusName().equals("RECRUIT")){ */
			newAuditCage.setAbsentFlag(false);
			newAuditCage.setPmha(AuditCageHelper.getPmhaRate(newAuditCage.getDo2(),newAuditCage));
			newAuditCage.setLra((int) Math.round(AuditCageHelper.getLraRate(newAuditCage)));
	//	}
		
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);

		if (refDo2 == null) {
			refDo2 = Utils.getReferenceDo2(do2);
		//	System.out.println("refdo2 rejosl=========="+refDo2.getDescription()+"  "+refDo2.getFromDate());
		}
	
		List<EmployeeServiceDetail> esdList = esdRepo.findByDo2(refDo2.getId());
				
		 
		if (esdList.size() > 0) {
			if(esdList.get(0).getFromDate().equals(do2.getFromDate())){
				esdRepo.delete(esdList.get(0));
				 
			}else{
				esdList.get(0).setToDate(
						DateManager.getPreviousDate(do2.getFromDate()));
				esdRepo.save(esdList.get(0));
				 
			}
			
		}

		return Do2Manager.SUCCESS;

	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		Do2 arrestRefDo2 = Utils.getReferenceDo2(refDo2);
		if(arrestRefDo2 != null){
			AuditCage arrestAuditCage = Utils.getAuditCage(arrestRefDo2);
			arrestAuditCage.setToDate(null);
			arrestAuditCage.setAbsentFlag(true);
			acRepo.save(arrestAuditCage);
			 
			List<EmployeeServiceDetail> esdList =esdRepo.findByDo2(arrestRefDo2.getId());
			 
			if (esdList.size() > 0) {
				esdList.get(0).setToDate(null);
				esdRepo.save(esdList.get(0));
			 
			}
	
		}
		
		return Do2Manager.SUCCESS;
	}

	

	public boolean isValid7a(Do2 do2) {
		withheldPercentage = do2.getColumnA();
		if (withheldPercentage != null
				&& withheldPercentage.charAt(withheldPercentage.length() - 1) == '%') {
			withheldPercentage = do2.getColumnA().substring(0,
					withheldPercentage.length() - 1);
		}
		try {
			paymentPercentage = (100 - Integer.parseInt(withheldPercentage));

		} catch (Exception e) {
			return false;
		}
		return paymentPercentage <= 100;

	}

	public boolean isValidRefDo2(Do2 do2) {
		refDo2 = null;
		List<Do2> refDo2List = Utils.getReferenceDo2List(do2);
		for (Do2 arrestDo2 : refDo2List) {
		//	if (arrestDo2.getDescription().equals(Do2Manager.ARREST)) {
			//Updated on 21042021
			if (arrestDo2.getDescription().equals(Do2Manager.ARREST) || arrestDo2.getDescription().equals(Do2Manager.CLOSAR)) {
				refDo2 = arrestDo2;
			}
		}
		return refDo2 != null;
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

	public boolean isValidDateCombination(Do2 do2) {
		fromDate = getDate(do2.getColumnB());
		toDate = getDate(do2.getColumnC());
	//	System.out.println("to date------" + toDate + "do2 fromdate"
	//			+ do2.getFromDate());
      
		if (fromDate.equals(refDo2.getFromDate())) {
			if (!do2.getFromDate().equals(DateManager.getNextDate(toDate))) {
				buffer.append(DATE_IN_COLUMN_C_NOT_MATCHING);
				return false;
			}
		} else {
			buffer.append(DATE_IN_COLUMN_B_NOT_MATCHING);
			return false;
		}
		return true;
	}
	
	/*
	public boolean isConvictExists(Do2 do2) {
	 		Employee emp = Utils.getCurrentEmployee(do2.getArmyNo());
			List<Do2> do2List = do2Repo.findByDescriptionStatusNotEqualAndNotThisId(do2.getEmployee().getId(),Do2Manager.CANCEL,"CONVCT", do2.getId());
				 
			if (do2List.size()>0 && !do2List.get(0).getFromDate().after(do2.getFromDate())) {
	 			return true;
			}   
			return false;
	}
	*/
	public boolean isConvictExists(Do2 do2) {
 		Employee emp = Utils.getCurrentEmployee(do2.getArmyNo());
 		List<Do2>convctDo2List = do2Repo.findByDescription(do2.getEmployee().getId(), "CONVCT", Do2Manager.VALID); 
 	//	List<Do2>convctCancelDo2List = do2Repo.findByDescription(do2.getEmployee().getId(), "CONVCT", Do2Manager.CANCEL);
 		
 		if (convctDo2List.size() > 0) {
 			if (!do2.getFromDate().before(convctDo2List.get(0).getFromDate())) {
 				return true;
 			}
 		}
 				
 		return false;
 		
}	
	

}