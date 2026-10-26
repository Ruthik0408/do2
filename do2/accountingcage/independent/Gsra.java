package itsdc.dolphin.do2.accountingcage.independent;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;
 

/**
 * 
 * @author Y Raja Reddy
 *  Date: 8-07-2009
 */

@RestController
public class Gsra extends Nra {
	
	public static String OTHER_THAN_GURKHA="AU:Admissible Only To GorKha. \n";
	@Override
	public String process(Do2 do2){
		super.process(do2);
		return Do2Manager.SUCCESS;
	}  
	@Override
	public boolean isValid(Do2 do2) {
		Employee emp = Utils.getCurrentEmployee(do2.getArmyNo());
		/*List<Employee> empList = Utils.getEntityManager()
		.createQuery("SELECT e FROM Employee e WHERE e.armyNo=:armyNo AND e.gsoCode=:gsoCode ")
		.setParameter("gsoCode", Do2Manager.ENROLL_GURKHA)
		.setParameter("armyNo", do2.getArmyNo())
		.getResultList();*/
		if(emp != null && emp.getGsoCode() != null && emp.getGsoCode()!=3){
			
		
	//	if(empList.size() == 0){
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(OTHER_THAN_GURKHA);
			do2Repo.save(do2);
			return false;
		}
		if (super.isValid(do2)) {
			do2.setStatus(Do2Manager.VALID);
			if(do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			}else{
				if(!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
			do2Repo.save(do2);
			return true;
		} else {
			do2Repo.save(do2);
			return false; 
		}
	}	
}
