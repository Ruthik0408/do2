package itsdc.dolphin.do2.auditcage;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
 
 
@RestController
public class AuditCageDischargeProcessor extends SpecialProcessor implements java.io.Serializable{
	
	EntityManager entityManager;
	String message;
	Employee employee;
	static public String INVALID_REFDO2 = "Col. 8 Reference Do2 No. is blank or Invalid.\n ";
	private StringBuffer buffer = new StringBuffer();
	private double refDo2Days = 0.0;
	private String rdDescription;
	private String paoNo;
	Do2 refDo2;
	boolean do2GenerationRequired;

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;

		if (do2.getRefDo2No() != null && !isValidRefDo2(do2)) {
			validity = false;
			buffer.append(INVALID_REFDO2);
		}
		
		message = buffer.toString();
		do2.setReason(message);
		do2Repo.save(do2);
		
		return validity;
	}
	@Override
	public String process(Do2 do2) {
		if(!isValid(do2))
			return Do2Manager.FAILURE;
		  
		Date fromDate = null;
		Date toDate = null;
		if (refDo2 != null
				&& Do2Manager.getLeaveTypes().contains(refDo2.getDescription())) {
			refDo2Days = DateManager.getNoOfDays(do2.getFromDate(), do2
					.getToDate());
		}
		if(refDo2 != null ){
			fromDate = refDo2.getFromDate();
			toDate = DateManager.getPreviousDate(do2.getFromDate());
			refDo2Days = DateManager.getNoOfDays(fromDate,toDate);
		   	generateDischDo2(refDo2,do2);	
		}
		return Do2Manager.SUCCESS;
	}

	
	/*private boolean isDo2GenerationRequired(Do2 do2) {
	   if(refDo2Days > Do2Manager.AL_CEASE_UPPER_LIMIT) {
			do2GenerationRequired = true;
		}else if (refDo2Days > Do2Manager.AL_CEASE_LOWER_LIMIT
					&& refDo2Days <= Do2Manager.AL_CEASE_UPPER_LIMIT) {
				do2GenerationRequired =true;
		}
	    System.out.println("do2 generated  "+do2GenerationRequired);
	    return do2GenerationRequired;
	}*/

	public boolean isValidRefDo2(Do2 do2) {
		List<Do2> refDo2List = Utils.getReferenceDo2List(do2);
	for(Do2 admitDo2 : refDo2List) {
			if(admitDo2.getDescription().equals(Do2Manager.HOSPITAL) && admitDo2.getColumnA().equals(Do2Manager.ADMITTED)){
				refDo2 = admitDo2;
			}
		}
		return refDo2 != null;
	}
	private void generateDischDo2(Do2 refDo2,Do2 do2){
		Do2 dischDo2 = new Do2();
        employee = Utils.getCurrentEmployee(do2.getArmyNo());
		dischDo2.setEmployee(employee);
		dischDo2.setArmyNo(employee.getArmyNo());
		dischDo2.setCheckDigit(employee.getCheckDigit());
		dischDo2.setUnitCode(dischDo2.getUnitCode());
		dischDo2.setUnit(do2.getUnit());
		dischDo2.setDo2Group(do2.getDo2Group());
		dischDo2.setDo2Type(do2.getDo2Type());
		dischDo2.setDo2No(do2.getDo2No());
		dischDo2.setDo2ItemNo(do2.getDo2ItemNo());
		dischDo2.setDo2Date(new Date());
		dischDo2.setDescription("HOSPTL");
		dischDo2.setFromDate(refDo2.getFromDate());
		dischDo2.setColumnA(Do2Manager.DISCH);
		dischDo2.setColumnB(refDo2.getColumnB());
		dischDo2.setRefDo2No(Utils.getReferenceDo2No(do2));
		dischDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		dischDo2.setAuditCage(true);
		dischDo2.setStatus(Do2Manager.PENDING);
		dischDo2.setQe(Utils.getCurrentQuarter());
		dischDo2.setAcceptedQe(Utils.getCurrentQuarter());
		dischDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		do2Repo.save(dischDo2);
	}	
}
