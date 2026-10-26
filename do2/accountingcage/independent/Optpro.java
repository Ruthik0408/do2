package itsdc.dolphin.do2.accountingcage.independent;
 
import java.util.Arrays; 
import java.util.List;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RestController;
import itsdc.dolphin.models.Do2; 
import itsdc.dolphin.models.Usr; 
import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

@RestController
public class Optpro extends AccountingCageBase implements java.io.Serializable {
 
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	static public String INVALID_COLA  = "VL:Relationship not mentioned in Col.7A.\n";
	static public String INVALID_COLB  = "VL:Name not mentioned in Col.5B.\n";
	static public String INVALID_COLC  = "VL:UID No. not mentioned in Col.5C.\n";
 	static public String DUPLICATE = "DU: Valid DOII Already Exists With Same Values. \n";
	static public String REFDO_REQUIRED = "VL:Reference DOII Number Of Earlier Unwilling Required For Willing. \n";
	static public String INCORRECT_REFDO = "VL:Incorrect Reference DOII Number. \n";
	static public String BEFORE_DATE = "VL:DOII FromDate Is Before Description Introduction Date(01-06-2025). \n";

	final static public List<String> col7AList = Arrays.asList("WILLING", "UNWILLING");
	final static public List<String> col7bList = Arrays.asList("PROMOTION", "CADRE");
	final static public List<String> col7cList = Arrays.asList("NK", "LD", "HAV", "DFR", "NBSUB", "NBRIS", "SUB", "RIS", "SUBMAJ", "RISMAJ");

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		StringBuffer buffer = new StringBuffer();
		if (do2.getColumnA()==null || (do2.getColumnA()!=null && do2.getColumnA().length()==0)
				|| (do2.getColumnA()!=null && !col7AList.contains(do2.getColumnA().toUpperCase()))) {
			validity = false;
			buffer.append(INVALID_COLA + NOT_IN_APPENDIX_J_FORMAT);
		}

		if (do2.getColumnB()==null || (do2.getColumnB()!=null && do2.getColumnB().length()==0)
				|| (do2.getColumnB()!=null && !col7bList.contains(do2.getColumnB().toUpperCase()))) {
			validity = false;
			buffer.append(INVALID_COLB + NOT_IN_APPENDIX_J_FORMAT);		
		}
		
		if (do2.getColumnC()==null || (do2.getColumnC()!=null && do2.getColumnC().length()==0) 
				|| (do2.getColumnC()!=null && !col7cList.contains(do2.getColumnC().toUpperCase()))) {
			validity = false;
			buffer.append(INVALID_COLC + NOT_IN_APPENDIX_J_FORMAT);		
		}  			
		 
		if (isDuplicate(do2)) {
			validity = false;
			buffer.append(DUPLICATE);	
		}
		
		if (do2.getFromDate().before(DateManager.getOptproDescriptionEffectiveDate())) {
			validity = false;
			buffer.append(BEFORE_DATE);
		}
	 	
		if (do2.getColumnA()!=null && do2.getColumnA().trim().equals("WILLING")) {
			if (do2.getRefDo2No()==null) {
				validity = false;
				buffer.append(REFDO_REQUIRED);
			} else {
				List<Do2> refDo2List=Utils.getReferenceDo2List(do2);
				if (refDo2List!=null && refDo2List.size()==1) {
					if (refDo2List.get(0).getDescription().trim().equals("OPTPRO") 
							&& refDo2List.get(0).getColumnA().trim().equals("UNWILLING")
							&& refDo2List.get(0).getColumnC().trim().toUpperCase().equals(do2.getColumnC().trim().toUpperCase())) {
						;
					} else {
						validity = false;
						buffer.append(INCORRECT_REFDO);
					}
				} else {
					validity = false;
					buffer.append(INCORRECT_REFDO);
				}				 
			}			
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
		
	public boolean isDuplicate(Do2 do2) {
		List<Do2> do2List=do2Repo.findAllByEmployeeIdAndDescriptionAndColumnAAndColumnBAndColumnC(do2.getEmployee().getId(), do2.getId(), do2.getDescription(), 
															do2.getColumnA().toUpperCase(), do2.getColumnB().toUpperCase(), do2.getColumnC().toUpperCase());		
		return (do2List!=null && do2List.size()>0);
	}
	
	@Override
	public String process(Do2 do2) {		 
		return Do2Manager.SUCCESS;
	}
}		
	