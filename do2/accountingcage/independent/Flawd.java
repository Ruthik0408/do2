package itsdc.dolphin.do2.accountingcage.independent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

@RestController
public class Flawd extends AccountingCageBase implements java.io.Serializable { 
	 
	String message;
	Employee employee;
	
	static public String RATE_NOT_AVAILABLE = "AU:Rate not available in the Allowance Rate table.\n ";
	
	
	private StringBuffer buffer = new StringBuffer();
	String courseName;
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		
		if (do2.getColumnA().equals(Do2Manager.GRANTED)) {
			if (!isRateAvailable(do2)) {
				validity = false;
				do2.setStatus(Do2Manager.PENDING);
				buffer.append(RATE_NOT_AVAILABLE);
				
		 	}
		}
		
		
		if (validity){
			do2.setStatus(Do2Manager.VALID);
	//		do2.setStatus(Do2Manager.PENDING);
			do2.setReason(null);
		}
		
		do2.setReason(buffer.toString());
		
		do2Repo.save(do2);
		
		
		
		return validity;
	}
	@Override
	public String process(Do2 do2) {
		if (isValid(do2)) {
			double flawdRate =0.0;
			courseName =do2.getColumnC().toUpperCase();
	
			
			List<Allowance> allowances = new ArrayList<Allowance>();
					allowances = Utils.getApplicableAllowanceRates(do2.getDescription(),courseName, do2.getFromDate(), do2.getFromDate(), do2.getColumnA());
					
					System.out.println ( " allowances  " +allowances.size()  +" frdt " +do2.getFromDate()  +" todt "+do2.getToDate()  +" course Name " +courseName  +" do2colmna " +do2.getColumnA());
					for (Allowance allow : allowances){
						flawdRate = allow.getRateAmount();
					}
					Utils.addArrears(do2, flawdRate);
	 
				}
		
		
		return Do2Manager.SUCCESS;
	}
	
	
	
	
	public boolean isRateAvailable(Do2 do2) {
		List<Allowance> allowances = new ArrayList<Allowance>();
		courseName =do2.getColumnC().toUpperCase();
		allowances = Utils.getApplicableAllowanceRates(do2.getDescription(),courseName,do2.getFromDate(), do2.getFromDate(), do2.getColumnA());
		if (allowances.size() > 0) {
			return true;
		}
		return false;
	}
}