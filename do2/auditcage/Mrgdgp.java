package itsdc.dolphin.do2.auditcage;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT & SDC, Secunderabad
 * 
 */
@RestController
public class Mrgdgp extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage,
			AuditCage baseAuditCage) {
		// TODO Auto-generated method stub
		return newAuditCage;
	}

	@Override
	public boolean isValid(Do2 do2) {
		// TODO Auto-generated method stub
		do2.setStatus(Do2Manager.VALID);
		do2Repo.save(do2);
		return true;
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
						
				/*		Utils.getEntityManager().createQuery(
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
		
		// TODO Auto-generated method stub
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		// TODO Auto-generated method stub
		return Do2Manager.SUCCESS;
	}


}