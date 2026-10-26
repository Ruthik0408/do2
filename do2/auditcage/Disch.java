package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

@RestController
public class Disch extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {

	Employee emp;
	Do2 refDo2;
	private StringBuffer buffer = new StringBuffer();
	static final public String FS_DUE_DATE = "AU:Fs Due date already updated in Employee Table. Check the Correctness.\n";
	boolean validity = true;
	final static List<String> COL_7CLIST = Arrays.asList(new String[] { "EXEMPLARY", "VERY GOOD", "GOOD", "FAIR",
			"INDIFFERENT", "BAD", "VERY BAD", "NO ASSESSMENT" });
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";

	@Override
	public boolean isValid(Do2 do2) {
		Date revised_appxj_impl_date = DateManager.getRevisedAppendixJImplementationDate();

		emp = Utils.getCurrentEmployee(do2.getArmyNo());
//		if (newDo2.getId() == do2.getId() && emp.getFsDueDate() != null) {
		if (do2.getAcceptedQe() == null && emp.getFsDueDate() != null) {
			validity = false;
			buffer.append(FS_DUE_DATE);
		}
		if (do2.getDescription().equals("INVALD") && !do2.getDo2Date().before(revised_appxj_impl_date)) {
			//System.out.println("----do2 desc--:");
			if (do2.getRefDo2No() == null) {
				validity = false;
				buffer.append("Ref Do2 of Hosptl Adm is required");
			} else {
				// refDo2 = Utils.getReferenceDo2(do2);
				List<Do2> refDo2List = Utils.getReferenceDo2List(do2);
				//System.out.println("----do2 desc-111-:" + refDo2List);
				if (refDo2List != null && refDo2List.size() > 0) {
					boolean do2Found = false;
					for (Do2 refdo2 : refDo2List) {
						if (refdo2 != null && refdo2.getDescription().equals(Do2Manager.HOSPITAL)
								&& refdo2.getColumnA().equals("ADM")) {
							do2Found = true;
							
							break;
						}
					}

				} else {
					validity = false;
					buffer.append("Ref Do2 of Hosptl Adm is required");
				}
				/*if (refDo2 != null && refDo2.getDescription().equals(Do2Manager.HOSPITAL)
						&& refDo2.getColumnA().equals("ADM")) {
					;
				} else {
					validity = false;
					buffer.append("Ref Do2 of Hosptl Adm is required");
				}*/
			}
		}
		if (do2.getDo2Date().before(revised_appxj_impl_date)) {
			if (do2.getColumnA() != null && !do2.getColumnA().equals("ENCASH")) {
				do2.setStatus("MR");
				validity = false;
				buffer.append("Column-7A should have ENCASH" + NOT_IN_APPENDIX_J_FORMAT);
			}
			if (do2.getColumnA() != null && do2.getColumnA().equals("ENCASH")) {
				if (do2.getColumnB() == null) {
					do2.setStatus("MR");
					validity = false;
					buffer.append("Column-7B cannot be left blank" + NOT_IN_APPENDIX_J_FORMAT);
				}
			}
		} else {
		//	System.out.println("----XXXX---:"+do2.getColumnA()+"---"+do2.getColumnB());
			if (do2.getColumnA() != null && do2.getColumnA().trim().length()>0) {
				do2.setStatus("MR");
				validity = false;
				buffer.append("Column-7A should be blank" + NOT_IN_APPENDIX_J_FORMAT);
			}
			if (do2.getColumnB() != null && do2.getColumnB().trim().length()>0) {
				do2.setStatus("MR");
				validity = false;
				buffer.append("Column-7B should be blank" + NOT_IN_APPENDIX_J_FORMAT);
			}
			if (do2.getColumnC() == null || (do2.getColumnC() != null && !COL_7CLIST.contains(do2.getColumnC().trim()))) {
				do2.setStatus("MR");
				validity = false;
				buffer.append("Column-7C is blank or character abbreviation is not correct" + NOT_IN_APPENDIX_J_FORMAT);
			}
		}
		if (validity == false) {
			do2.setReason(buffer.toString());
			if (do2.getStatus() != null)
				do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
		}

		do2Repo.save(do2);
		return validity;

	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		emp = Utils.getCurrentEmployee(do2.getArmyNo());
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		if (do2.getDescription().equals("INVALD")) {
			emp.setFsStatus(scRepo.findByStatusCode("I"));
		} else if (do2.getDescription().equals("DISCH")) {
			emp.setFsStatus(scRepo.findByStatusCode("G"));
		}
		emp.setFsDueDate(do2.getFromDate());
		empRepo.save(emp);
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_DISCHARGE + "");
		return Do2Manager.SUCCESS;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		newAuditCage.setAbsentFlag(true);
		return newAuditCage;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		emp = Utils.getCurrentEmployee(refDo2.getArmyNo());
		emp.setFsDueDate(null);
		emp.setFsStatus(null);
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_DISCHARGE);
		return Do2Manager.SUCCESS;
	}

}