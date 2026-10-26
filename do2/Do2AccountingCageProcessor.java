package itsdc.dolphin.do2;

import itsdc.dolphin.models.Do2;
import itsdc.dolphin.repositories.AuditCageRepository;
import itsdc.dolphin.repositories.Do2Repository;

public interface Do2AccountingCageProcessor {
	public String process(Do2 do2);
	public boolean isValid(Do2 do2);
	public String processCancel(Do2 currentDo2,Do2 refDo2);

	
	 
	
}
