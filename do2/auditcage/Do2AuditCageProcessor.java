package itsdc.dolphin.do2.auditcage;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;

public interface Do2AuditCageProcessor {
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage);
	public boolean isValid(Do2 do2);
	public String process(Do2 do2, AuditCage currentAuditCage); 
	public void setNewAuditCage(AuditCage newAuditCage);
	public String processCancel(Do2 currentDo2, Do2 refDo2);
	
}
