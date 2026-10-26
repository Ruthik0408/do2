package itsdc.dolphin.do2.accountingcage.independent;

import java.util.Date;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
 
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Constants;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT & SDC, Secunderabad
 * 
 */

@RestController
public class Tdch extends AccountingCageBase implements java.io.Serializable {
	private static final String INVALID_COLUMNAB = "VL:Trade should be mentioned in Cols.7A and 7B.\n";
	private static final String INVALID_COLUMNBC = "VL:Trade should be mentioned in Cols.7B and 7C.\n";
	private static final String INVALID_COLUMNA  = "VL:Group should be mentioned in Col.7A.\n";
	private static final String INVALID_TRADE    = "AU:Invalid Trade.\n";
	StringBuffer buffer = new StringBuffer();
	static public Date revised_appxj_impl_date ;

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		revised_appxj_impl_date = DateManager.getRevisedAppendixJImplementationDate();
		if (do2.getDo2Date().before(revised_appxj_impl_date)){
			if (do2.getColumnA() == null || do2.getColumnB() == null) {
				validity = false;
				buffer.append(INVALID_COLUMNAB);
			} 
		} else {
			if (!(do2.getColumnA().equals("GP-X") || do2.getColumnA().equals("GP-Y"))){
				validity = false;
				buffer.append(INVALID_COLUMNA);
			}	
			if (do2.getColumnB() == null || do2.getColumnC() == null){
				validity = false;
				buffer.append(INVALID_COLUMNBC);
			}
		}
		if (!isValidTrade(do2)) {
			validity = false;
			buffer.append(INVALID_TRADE);
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
	

	public boolean isValidTrade(Do2 do2) {
		String trade1 = null;
		String trade2 = null;
		if (do2.getDo2Date().before(revised_appxj_impl_date)){
			trade1 = do2.getColumnA();
			trade2 = do2.getColumnB();
		} else {
			trade1 = do2.getColumnB();
			trade2 = do2.getColumnC();
		}
		List<GroupCategory> gcList = gcRepo.findAllBySoldierCategoryOrderByFromDate(trade1);
				
			//	Utils.getEntityManager().createQuery("SELECT grc FROM GroupCategory grc where grc.soldierCategory = :trade")
			//	.setParameter("trade", trade1).getResultList();
		if (gcList.size() > 0){
			List<GroupCategory> gc1List = gcRepo.findAllBySoldierCategoryOrderByFromDate(trade2);
					
		//			Utils.getEntityManager().createQuery("SELECT grc FROM GroupCategory grc where grc.soldierCategory = :trade")
		//	.setParameter("trade", trade2).getResultList();
			if (gc1List.size() > 0){
				return true;
			}
		}
		return false;
	}
}
