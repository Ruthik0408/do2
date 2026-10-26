package itsdc.dolphin.do2.accountingcage.independent;

 
import itsdc.dolphin.models.Do2;

import javax.transaction.Transactional;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;

/*
 * @author N RAMACHANDRAN && K RAMADEVI   DATE: 27.11.2009
 */
@RestController
public class Pararc extends AccountingCageBase implements java.io.Serializable {

	 
	 
	@Transactional
	@Override
	public boolean isValid(Do2 do2) {
	
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
	}
	
	@Override
	public String process(Do2 do2) {
		return Do2Manager.SUCCESS;
	}
}		