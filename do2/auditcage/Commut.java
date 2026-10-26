package itsdc.dolphin.do2.auditcage;

import java.util.List;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.utils.Utils;
 
/**
 * 
 * @author IT&SDC Secunderabad
 * 
 */

@RestController
public class Commut extends AuditCageBase implements 
		Do2AuditCageProcessor, java.io.Serializable {
	 
	
	static public final String INVALID_REFDO2 = "AU:Incorrect Reference DO2.\n ";	
	static public final String INVALID_FROM_DATE="AU:From date is not same as from date of reference DO2.\n";
	static public final String INVALID_COLUMNA ="VL:Column 7A should be either SEVREP/REP.\n";
	
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
		if (!isValidColumnA(do2)) {
			validity = false;
			buffer.append(INVALID_COLUMNA);
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
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}
	
	public boolean isValidRefDo2(Do2 do2) {
		do2List = Utils.getReferenceDo2List(do2);
		if (do2List.size() > 0) {
			for (Do2 oldDo2 : do2List) {
				if (do2.getColumnB() != null && oldDo2.getDescription().equals(do2.getColumnB())) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean isValidFromDate(Do2 do2) {
		// As per the discussion with ASDC visiting team on 15.04.14
		do2List = Utils.getReferenceDo2List(do2);
		/*if (do2List.size() > 0) {
			for (Do2 oldDo2 : do2List) {
				if (oldDo2.getFromDate().equals(do2.getFromDate())) {
					return true;
				}
			}
		}
		return false;*/
		return true;
	}
	
	public boolean isValidColumnA(Do2 do2) {
		if (do2.getColumnA() == null){
			return false;
		} else if (do2.getColumnA().equals("SEVREP") || do2.getColumnA().equals("REP")){
			return true;
		}
	    return false;
	}
}