package itsdc.dolphin.do2.accountingcage.independent;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
 
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT & SDC, Secunderabad
 * 
 */

@RestController
public class Option extends AccountingCageBase implements java.io.Serializable {
	public StringBuffer buffer = new StringBuffer();
	static public List<String> col5AList = Arrays.asList("DIS","WARINJ","LDE");
	static public List<String> col5BList = Arrays.asList("PENSION","LUMPSUM");
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if (do2.getColumnA() != null && col5AList.contains(do2.getColumnA())){
			validity=true;			
		} else {
			validity=false;
			buffer.append("Col.5A should contain DIS/WARINJ/LDE");
		}
		if (do2.getColumnB() != null && col5BList.contains(do2.getColumnB())){
			validity=true;			
		} else {
			validity=false;
			buffer.append("Col.5B should contain PENSION/LUMPSUM");
		}
		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
			if (do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			} else {
				if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
		}
		
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
	 
		return validity;

	}

	@Override
	public String process(Do2 do2) {
		return Do2Manager.SUCCESS;
	}
}
