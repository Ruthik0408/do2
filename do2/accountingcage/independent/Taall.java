package itsdc.dolphin.do2.accountingcage.independent;

 

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
 
import javax.persistence.EntityManager;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.util.GregorianCalendar;

 
public class Taall extends AccountingCageBase implements java.io.Serializable {

	private boolean validity = true;
	StringBuffer buffer = new StringBuffer();

	Rank rank;
	Employee employee;
	public List<Allowance> alwList;

	static public final String NOT_ADMISSIBLE_RECRUIT = "AU:Not admissible to Recruit. \n";
	static public String ALLOWANCE_RATE_NOT_AVAILABLE = "AU:Allowance Rate is not available.\n";
	static public String TJRA_IS_NULL = "AU:Tjra column should not be null/ It should contain 100/75 i.e. admissible rate percentage \n";
	static public String LRA_IS_NULL = "AU: Lra column should not be null/It should contain 100/80 i.e.  completed training percentage \n";
	

	@Override
	public boolean isValid(Do2 do2) {

		

		if (!isRecruit(do2)) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(NOT_ADMISSIBLE_RECRUIT);
			validity = false;
		}
		
		if(!isValidAllowanceRate(do2)) {
			validity = false;
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(ALLOWANCE_RATE_NOT_AVAILABLE);
		}
		System.out.println ( " tjra " +do2.getTjra()   +" lra " +do2.getLra());
		
		if (do2.getTjra()==null || (do2.getTjra()!=null && ((!(do2.getTjra()!=100)) || (!(do2.getTjra()!=75))))) {
			do2.setStatus(Do2Manager.INVALD);
			buffer.append(TJRA_IS_NULL);
		}
		
		if (do2.getLra()==null || (do2.getLra()!=null && ((!(do2.getLra()!=100)) || (!(do2.getLra()!=80))))) {
			do2.setStatus(Do2Manager.INVALD);
			buffer.append(LRA_IS_NULL);
		}

		if (validity) {
			do2.setStatus(Do2Manager.VALID);
			
		}
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;
	}

	public boolean isRecruit(Do2 do2) {
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		return rank != null && !rank.getRankCode().equals(Do2Manager.RECRUIT);
	}
	
	public boolean isValidAllowanceRate(Do2 do2) {
		
		  alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), 
					 do2.getFromDate(), do2.getFromDate(), rank);	  
	
	
	return (alwList.size() > 0);
}

	@Override
	public String process(Do2 do2) {
		
		int taallow = 0;
		
		   rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
			if (rank!=null) {
				alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), 
						 do2.getFromDate(), do2.getFromDate(), rank);	  
			}
			
			if(alwList.size() > 0) {
				taallow = (int) alwList.get(0).getRateAmount();
				if (do2.getLra()==100) {
					taallow = taallow;
				}
				if (do2.getLra()==80) {
					taallow = taallow*75/100;
				}
			}
		
		Utils.addArrears(do2, Math.round(taallow));
		return Do2Manager.SUCCESS;
	}

	
}
