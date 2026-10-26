package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.BhutanSlab;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.Pao;
import itsdc.dolphin.models.PayCode;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

@RestController
public class Mrgdtd extends AuditCageBase implements
 Do2AuditCageProcessor, java.io.Serializable{

	@Override
	public boolean isValid(Do2 do2) {
		do2.setStatus(Do2Manager.VALID);
		do2Repo.save(do2);
	return true;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		
		return Do2Manager.SUCCESS;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage,
			AuditCage baseAuditCage) {
		 
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		if (do2.getFromDate().after(DateManager.getSixthCpcAllowanceDate())) {
			if (!AuditCageHelper.isTdmergDo2Exist(do2)){
				String presentTrade = null;
				String sldrClass = null;
				String previousTrade=do2.getColumnB().trim();
				String group ="";
				if(do2.getColumnA()!=null && do2.getColumnA().length()>3){
					group = do2.getColumnA().charAt(3)+"";
				}
				List<GroupCategory> grpCatList = gcRepo.findBySoldierGroupAndSoldierCategory(group, previousTrade);
						
						
			/*			Utils.getEntityManager().createQuery(
					"SELECT gc FROM GroupCategory gc WHERE gc.soldierGroup=:sgrp AND gc.soldierCategory=:scat ")
					.setParameter("sgrp", group ).setParameter("scat", previousTrade).getResultList();*/
				if ((grpCatList.size() > 0)	&& (previousTrade.equals(grpCatList.get(0).getSoldierCategory()))) {
					 if (grpCatList.get(0).getMergedAbbr() != null){				
							presentTrade = grpCatList.get(0).getMergedAbbr();
							AuditCageHelper.createMergedTradeDo2(do2, sldrClass, previousTrade, presentTrade);
					 }
				}				
			}
		}
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		return Do2Manager.SUCCESS;
	}
	
	
}
