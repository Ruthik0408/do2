package itsdc.dolphin.do2.accountingcage.independent;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Unit;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;
 

/**
 * 
 * @author Y Raja Reddy
 *  Date: 8-07-2009
 */

@RestController
public class Ishra extends Nra {
	public static String NOT_INTER_SERVICE = "AU:Unit not is an Inter Service Unit.\n";
	@Override
	public String process(Do2 do2) {
		super.process(do2);
		return Do2Manager.SUCCESS; 
	}

@Override
public boolean isValid(Do2 do2) {
		List<Unit> unitList = unitRepo.findByUnitCodeAndInterService(do2.getUnitCode(), true);
				
			/*	Utils
				.getEntityManager()
				.createQuery(
						"SELECT u FROM Unit u WHERE u.unitCode=:unitCode AND u.interService=:interService")
				.setParameter("unitCode", do2.getUnitCode()).setParameter(
						"interService", true).getResultList();*/
		 if(unitList.size() ==0){
			   do2.setStatus(Do2Manager.INVALID);
			   do2.setReason(NOT_INTER_SERVICE);
			 	do2Repo.save(do2);
				   return false;
		}else{
			if (super.isValid(do2)) {
				if(do2.getAcceptedQe()==null){
					do2.setAcceptedQe(Utils.getCurrentQuarter());	
				}else{
					if(!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
						do2.setReviewQe(Utils.getCurrentQuarter());
					}
				}
				do2Repo.save(do2);
				return true;
			}
			return false;
		}
	}
}
