package itsdc.dolphin.do2.auditcage;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
 
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 
 
@RestController
// Territorial Army Training

public class Tatrng extends AuditCageBase implements  Do2AuditCageProcessor {
	
	Employee employee;
	 
 	Calendar pmhaFromDateCal = new GregorianCalendar();

	@Override
	public boolean isValid(Do2 do2) {
		do2.setStatus(Do2Manager.VALID);
		do2Repo.save(do2);

	 	return true;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		newAuditCage.setPmha(AuditCageHelper.getPmhaRate(newAuditCage.getDo2(),newAuditCage));
		newAuditCage.setLra((int)AuditCageHelper.getLraRate(newAuditCage));
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {

				
		return Do2Manager.SUCCESS;
	}
	
	
 }