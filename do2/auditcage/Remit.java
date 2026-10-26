package itsdc.dolphin.do2.auditcage;

import java.util.List;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.EmployeeServiceDetail;
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
public class Remit extends AuditCageBase implements 
		Do2AuditCageProcessor, java.io.Serializable {
	 
	
	static public final String INVALID_REFDO2 = "AU:Incorrect Reference DO2.\n ";
	static public final String INVALID_TODATE = "AU:To date in DO2 not matching with To date of punishment DOII. \n";
	static public final String INVALID_FROM_DATE="AU:From date of DOII is not after from date of punishment DO2.\n";
	
	private StringBuffer buffer = new StringBuffer();
	private List<Do2> do2List;
	private boolean validity = true;
	@Override
	public boolean isValid(Do2 do2) {
		
		if (!isValidRefDo2(do2)) {
			validity = false;
			buffer.append(INVALID_REFDO2);
		}
		
		if (!isValidFromDate(do2)) {
			validity = false;
			buffer.append(INVALID_FROM_DATE);
		}
		
		if (do2.getToDate()!=null) {
			if (!isValidToDate(do2)) {
				validity = false;
				buffer.append(INVALID_TODATE);
			}
		}
		
		if (!(validity)) {
			do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
		}
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage,	baseAuditCage);
		do2List = Utils.getReferenceDo2List(do2);
		for (Do2 oldDo2 : do2List) {
			if (oldDo2.getDescription().equals(do2.getColumnA())) {
		
				List<EmployeeServiceDetail> esdPunishList = Utils.getEmployeeServiceDetail(oldDo2, Do2Manager.ESD_PUNISHMENT+"");
				for (EmployeeServiceDetail esd : esdPunishList) {
					if (oldDo2.getFromDate().equals(esd.getDo2().getFromDate())) {
						esd.setToDate(DateManager.getPreviousDate(do2.getFromDate())); 
					}
				}
								
				List<EmployeeServiceDetail> esdNQSList = Utils.getEmployeeServiceDetail(oldDo2, Do2Manager.ESD_NQS+"");
				for (EmployeeServiceDetail esd : esdNQSList) {
					if (oldDo2.getFromDate().equals(esd.getDo2().getFromDate())) {
						esd.setToDate(DateManager.getPreviousDate(do2.getFromDate())); 
					}
				}
			}
			if ((oldDo2.getDescription().equals("RMILRI")) && 
					(do2.getToDate()!=null) && (oldDo2.getFromDate().equals(do2.getToDate()))) {
				oldDo2.setStatus(Do2Manager.CANCEL);
			}
		}
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		do2List = Utils.getReferenceDo2List(refDo2);
		
		for (Do2 oldDo2 : do2List) {
			if (oldDo2.getDescription().equals(refDo2.getColumnA())) {
		
				List<EmployeeServiceDetail> esdPunishList = Utils.getEmployeeServiceDetail(oldDo2, Do2Manager.ESD_PUNISHMENT+"");
				for (EmployeeServiceDetail esd : esdPunishList) {
					if (oldDo2.getFromDate().equals(esd.getDo2().getFromDate())) {
						esd.setToDate(DateManager.getPreviousDate(currentDo2.getFromDate())); 
					}
				}
								
				List<EmployeeServiceDetail> esdNQSList = Utils.getEmployeeServiceDetail(oldDo2, Do2Manager.ESD_NQS+"");
				for (EmployeeServiceDetail esd : esdNQSList) {
					if (oldDo2.getFromDate().equals(esd.getDo2().getFromDate())) {
						esd.setToDate(DateManager.getPreviousDate(currentDo2.getFromDate())); 
					}
				}
			}
		
			if ((oldDo2.getDescription().equals("RMILRI")) && 
					(refDo2.getToDate()!=null) && (oldDo2.getFromDate().equals(refDo2.getToDate()))) {
				oldDo2.setStatus(Do2Manager.PENDING);
			}
		
		}
		return Do2Manager.SUCCESS;
	}
	
	public boolean isValidRefDo2(Do2 do2) {
		do2List = Utils.getReferenceDo2List(do2);
		if (do2List.size() > 0) {
			for (Do2 oldDo2 : do2List) {
				if (oldDo2.getDescription().equals(do2.getColumnA())) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean isValidFromDate(Do2 do2) {
		do2List = Utils.getReferenceDo2List(do2);
		if (do2List.size() > 0) {
			for (Do2 oldDo2 : do2List) {
				if (oldDo2.getFromDate().before(do2.getFromDate())) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean isValidToDate(Do2 do2) {
		do2List = Utils.getReferenceDo2List(do2);
		if (do2List.size() > 0) {
			for (Do2 oldDo2 : do2List) {
				if (oldDo2.getToDate().equals(do2.getToDate())) {
					return true;
				}
			}
		}
		return false;
	}
}