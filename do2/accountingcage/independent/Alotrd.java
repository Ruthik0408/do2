package itsdc.dolphin.do2.accountingcage.independent;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 
 
@RestController
public class Alotrd extends AccountingCageBase implements java.io.Serializable {
	Employee employee;
	private static final String INVALID_GROUP = "AU:Invalid group on From date. \n";
	private static final String INVALID_CATEGORY = "AU:Invalid category on From date. \n";
	private static final String INVALID_FROM_DATE = "VL:From date is before enrolment date. \n";
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	StringBuffer buffer = new StringBuffer();
	String message;

	@Override
	public boolean isValid(Do2 do2) {
	boolean validity = true;
	employee = Utils.getCurrentEmployee(do2.getArmyNo());

		if (employee.getEnrolmentDate()!=null && do2.getFromDate().before(employee.getEnrolmentDate())) {
			buffer.append(INVALID_FROM_DATE);
			do2.setStatus(Do2Manager.INVALID);
			validity = false;
		}

		if (!isValidGroup(do2)) {
			validity = false;
			//do2.setStatus("MR");
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(INVALID_GROUP + NOT_IN_APPENDIX_J_FORMAT);
		}

		if (!(isValidCategory(do2))) {
			//do2.setStatus("MR");
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(INVALID_CATEGORY + NOT_IN_APPENDIX_J_FORMAT);
			validity = false;
		}
		
		if (validity==false) {
			//if ((do2.getStatus() == null)||(do2.getStatus() != null && !do2.getStatus().equals(Do2Manager.PENDING))) {
				do2.setStatus(Do2Manager.INVALID);
				do2.setReason(buffer.toString());
		//	}   
		} else {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
			if (do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			} else {
				if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}			
		}	
		do2Repo.save(do2);
		return validity;

	}
	

	@Override
	public String process(Do2 do2) {
		updateEmployee(do2);
		return Do2Manager.SUCCESS;
	}
	
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		reverseEmployeeStatus(currentDo2, refDo2); 
		return Do2Manager.SUCCESS;
	}

	public boolean isValidGroup(Do2 do2) {
		if (!(do2.getFromDate().before(DateManager.getSixthCpcDate())
				&& (do2.getColumnA().charAt(3) == 'X' || 
					do2.getColumnA().charAt(3) == 'Y')))
			return true;
		else {
			if (do2.getColumnA().charAt(3) == 'X'
				|| do2.getColumnA().charAt(3) == 'Y'
				|| do2.getColumnA().charAt(3) == 'Z')
				return true;
		}
		return false;
	}

	public boolean isValidCategory(Do2 do2) {
		List<GroupCategory> grpCatList = gcRepo.findBySoldierGroupAndSoldierCategory(do2.getColumnA().charAt(3)+"", do2.getColumnC().trim());
				
		 
		if ((grpCatList.size() > 0)	&& (do2.getColumnC().trim().equals(grpCatList.get(0).getSoldierCategory()))) {
			return true;
		}
		return false;
	}
	
    public void updateEmployee(Do2 do2) { 
		String category = do2.getColumnC().trim();
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<GroupCategory> grpCatList =gcRepo.findBySoldierGroupAndSoldierCategory(do2.getColumnA().charAt(3)+"".toUpperCase(), category.toUpperCase()); 
				 System.out.println(".....grpcatlist " +grpCatList.size());
		if (grpCatList!=null && grpCatList.size()>0	&& category.toUpperCase().equals(grpCatList.get(0).getSoldierCategory().trim().toUpperCase())) {
			if (grpCatList.get(0).getRedesignatedAbbr() != null){
				category=grpCatList.get(0).getRedesignatedAbbr().trim();
			} else if (grpCatList.get(0).getMergedAbbr() != null){
				category=grpCatList.get(0).getMergedAbbr().trim();
			}
			grpCatList=null;
			grpCatList=gcRepo.findBySoldierCategory(category.toUpperCase());
			if (grpCatList!=null && grpCatList.size()>0) {
				if (employee.getGroupCategory()!=null)
					employee.setPreviousGroupCategory(employee.getGroupCategory());
			
				employee.setGroupCategory(gcRepo.findBySoldierCategory(category).get(0)); 
				empRepo.save(employee); 
			}
		}
		
/*		if (employee.getInitialMusteringDate()==null && employee.getAttestationDate() == null) {		 
			employee.setGroupCategory(gcRepo.findBySoldierCategory(category).get(0));	
			empRepo.save(employee); 
		} */
	}
    
    public void reverseEmployeeStatus(Do2 currentDo2, Do2 refDo2) {

		employee = Utils.getCurrentEmployee(currentDo2.getArmyNo());

		if (employee.getInitialMusteringDate()==null && employee.getAttestationDate()==null) {
			employee.setGroupCategory(null);
		}
		empRepo.save(employee);
	}


}
