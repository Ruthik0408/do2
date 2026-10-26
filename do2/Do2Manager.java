//Experimental
package itsdc.dolphin.do2;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

 import itsdc.dolphin.models.Do2;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 
 
public class Do2Manager {
	

	static public final String SIX_PM = "1800";
	static public final String SIX_AM = "0600";

	static public final String SUCCESS = "success";
	static public final String FAILURE = "failure";
	static public final int LOWER_RTF_RATE = 40;
	static public final int HIGHER_RTF_RATE = 50;
	
	public static String[] descValues =  new String[] { "BASE" , "CANCEL" , "ENROLL" , "ATCORR" , "ARREST" , "CLOSAR" , "ENRMST" , "MUSTER" , 
		"POSOUT" , "REVDEP" , "TFRDEP" , "TFROUT" , "DISEMB" , "TATRNG" , "EMBODI" , "AWL" , "OSL" , "DESN" , "ACTGA CEASED" , 
		"AETECH CEASED" , "AIRWCA CEASED" , "ANAL1 CEASED" , "ANAL2 CEASED" , "AVLPAY CEASED" , "BHUTAN CEASED" , 
		"CCA CEASED" , "CCA1 CEASED" , "CCB1 CEASED" , "CCB2 CEASED" , "CFAA CEASED" , "CHAFAA CEASED" , "CILQ CEASED" , 
		"FAA CEASED" , "HRA CEASED" , "HRAFAM CEASED" , "CMFAA CEASED" , "COMNDO CEASED" , "DAA CEASED" , "DEPA CEASED" , 
		"DEPSAG CEASED" , "SFASFF CEASED" , "DEPSRG CEASED" , "FLCHCA CEASED" , "HAA CEASED" , "HAUCA CEASED" , "HAUCH CEASED" , 
		"HAUCL CEASED" , "HAUCS CEASED" , "HCA CEASED" , "HCLA CEASED" , "HCTA CEASED" , "HCWA CEASED" , "ISLNDA CEASED" , 
		"LKSHDP CEASED" , "NSGA CEASED" , "PARA CEASED" , "PARAR CEASED" , "PMHAH CEASED" , "RLCA1 CEASED" , "RLCA2 CEASED" , 
		"RLCA3 CEASED" , "RLCA4 CEASED" , "SCA CEASED" , "SCAGN CEASED" , "SCCIA CEASED" , "SIACHN CEASED" , "SFA CEASED" , 
		"SSA CEASED" , "TPTH CEASED" , "TPTL CEASED" , "WCA CEASED" , "LRA" , "REMUST" , "INCR" , "SIXCPC" , "REINST" , 
		"REJAWL" , "BAIL" , "REJOSL" , "SPLLVE" , "ACP NKSCALE" , "ACPFIX NKSCALE" , "ACPFIX NKGRADE" , "MACPS NKGRADE" , 
		"MSTHAV" , "REDESN" , "REGAL" , "REGOSL" , "RTPGP" , "TFRIN" , "ACP HAVSCALE" , "MACPS HAVGRADE" , "ACPFIX HAVSCALE" , 
		"ACPFIX HAVGRADE" , "MSTJCO" , "OWNMST" , "PMHAL" , "POSIN" , "PROMOT" , "LATENR" , "RANKS" , "UPCLAS" , "SCCIA GRANTED" , 
		"ACTGA GRANTED" , "AETECH GRANTED" , "AIRWCA GRANTED" , "ANAL1 GRANTED" , "ANAL2 GRANTED" , "AVLPAY GRANTED" , 
		"BHUTAN GRANTED" , "CCA GRANTED" , "CCA1 GRANTED" , "CCB1 GRANTED" , "CCB2 GRANTED" , "CFAA GRANTED" , "CILQ GRANTED" , 
		"FAA GRANTED" , "HRA GRANTED" , "HRAFAM GRANTED" , "CMFAA GRANTED" , "DAA GRANTED" , "DEPA GRANTED" , "DEPSAG GRANTED" , 
		"SFASFF GRANTED" , "DEPSRG GRANTED" , "FLCHCA GRANTED" , "HAA GRANTED" , "HAUCA GRANTED" , "HAUCH GRANTED" , 
		"HAUCL GRANTED" , "HAUCS GRANTED" , "HCA GRANTED" , "HCLA GRANTED" , "HCTA GRANTED" , "HCWA GRANTED" , "ISLNDA GRANTED" , 
		"LKSHDP GRANTED" , "NSGA GRANTED" , "PARA GRANTED" , "PARAR GRANTED" , "PMHAH GRANTED" , "RLCA1 GRANTED" , "RLCA2 GRANTED" , 
		"RLCA3 GRANTED" , "RLCA4 GRANTED" , "SCA GRANTED" , "SCAGN GRANTED" , "SIACHN GRANTED" , "SFA GRANTED" , "SSA GRANTED" , 
		"TPTH GRANTED" , "TPTL GRANTED" , "WCA GRANTED" , "MACPS NSUBGRADE" , "ACPFIX NSUBGRADE" , "CHAFAA GRANTED" , 
		"COMNDO GRANTED" , "MILRI" , "REAPPT" , "MACPS SUBGRADE" , "ACPFIX SUBGRADE" , "APPT" , "DEATH" , "DISCH" , "INVALD" , 
		"LORANK" , "PAYFIX" , "REDUCE" , "REFIXP" , "SETASD" , "ALWREV" , "PROFIX" , "HAVFIX" , "MACPS SMAJGRADE" , 
	//	"ACPFIX SMAJGRADE" , "RMILRI" , "TYOJ" , "TYRJ" , "DAILY" , "DA" , "STEPUP" , "SPINC" , "TDMERG" , "FSDESN" ,
		"ACPFIX SMAJGRADE" , "RMILRI" , "TYOJ" , "TYRJ" , "DAILY" , "OPTCPC", "AICTGP", "DA" , "STEPUP" , "SPINC" , "TDMERG" , "FSDESN" , 
		"FSCOMN" , "FSDETH" , "FSDSCH" , "FSDSMS" , "FSINVL" , "FSKILL" , "FSTFOT" , "FSBTTL" , "FIX7PC", "HRDALL GRANTED" , "HRDALL CEASED",
		"ALWREV AWARD","ALWREV PMHAREV","ALWREV 6CPC","ALWREV 7CPC","ALWREV 25PC","ALWREV 50PC" , 
		"TLA1 CEASED", "TLA2 CEASED", "TLA3 CEASED", "TLA1 GRANTED", "TLA2 GRANTED", "TLA3 GRANTED", "SFACOM GRANTED" , "SFACOM CEASED",
		 "FORLAG GRANTED", "FORLAG CEASED", "ADPOST GRANTED", "ADPOST CEASED", "RUMCIG GRANTED", "RUMCIG CEASED",
		 "MACPS HONYGRADE",	"ACPFIX HONYGRADE",  "SPIC GRANTED", "SPIC CEASED", "PROALL GRANTED", "PROALL CEASED", "EXTWK GRANTED", "EXTWK CEASED",
		 "TRGALL GRANTED", "TRGALL CEASED", "ACQUIT", "BUNCH", "REJEOL", "RISK GRANTED", "RISK CEASED", "HAUCA GRANTED", "HAUCA CEASED", "FSRMVL", "STOPIN", "NEDA", "NELRA" };
	
	public static String[] priorityValues =  new String[] { "0", "1", "1", "2", "10", "10", "10", "10", "13", "12", "11", "10", "10", "10", 
			"12", "14", "15", "18", "30", "30", "30", "30", "30", "30", "30", "30", "30", "30", "30", "30", "30", "30", "30", 
			"30", "30", "30", "30", "30", "30", "30", "30", "30", "30", "30", "30", "30", "30", "30", "30", "30", "30", "30", 
			"30", "30", "30", "30", "30", "30", "30", "30", "30", "30", "30", "30", "30", "30", "30", "30", "30", "30", "30", 
			"19", "51", "31", "62", "40", "17", "17", "17", "40", "49", "49", "49", "49", "50", "19", "18", "50", "50", "11", 
			"60", "60", "60", "60", "60", "60", "60", "60", "60", "60", "60", "60", "70", "70", "70", "70", "70", "70", "70", 
			"70", "70", "70", "70", "70", "69", "70", "70", "70", "70", "70", "70", "70", "70", "70", "70", "70", "70", "70", 
			"70", "70", "70", "70", "70", "70", "70", "70", "70", "70", "70", "70", "70", "70", "70", "70", "70", "70", "70", 
			"70", "70", "71", "69", "68", "70", "70", "70", "70", "70", "80", "80", "80", "80", "90", "90", "90", "90", "90", 
	//		"48", "90", "90", "90", "90", "90", "91", "90", "90", "18", "91", "93", "92", "110", "108", "109", "110", "111",
			"48", "90", "90", "90", "90", "90", "91", "90", "90", "17", "91", "93", "92", "94", "95", "110", "108", "109", "110", "111",
			"111", "111", "111", "111", "111", "111", "111", "111", "9", "70", "30",
			 "90", "90", "90", "90", "90", "90", "30", "30", "30", "70", "70", "70", "70", "30", "70", "30", "96", "30", "70", "30",
	//		 "90", "90", "70", "30", "70", "30", "70", "30", "70", "30", "21", "97", "17", "70", "30", "70", "30" };
			 "90", "90", "70", "30", "70", "30", "70", "30", "70", "30", "21", "32", "17", "70", "30", "70", "30", "111", "32", "110", "19" };  //Signals - Bunch modified on 25.09.2025
	
	static public int getPriority(String do2Description) {
		int priority = 20;
	    for (int i = 0; i < descValues.length; i++){
	    	if (descValues[i].equals(do2Description)){
	    		priority = Integer.parseInt(priorityValues[i]);
	    	}
	    }
	    return priority;
	}
	
	public static List<String>priorityDescList=Arrays.asList("ALWREV", "APPT", "ARREST", "ATCORR", "AWL", "BAIL", "BASE", "CANCEL", "CLOSAR", 
			  "DA", "DAILY", "DEATH", "DESN", "DISCH", "DISEMB", "EMBODI", "ENRMST", "ENROLL", "FSBTTL", "FSCOMN", 
			  "FSDESN", "FSDETH", "FSDSCH", "FSDSMS", "FSINVL", "FSKILL", "FSTFOT", "HAVFIX", "INCR", "INVALD", 
			  "LATENR", "LORANK", "LRA", "MILRI", "MSTHAV", "MSTJCO", "MUSTER", "OSL", "OWNMST", "PAYFIX", "FIX7PC", 
			  "PMHAL", "POSIN", "POSOUT", "PROFIX", "PROMOT", "RANKS", "REAPPT", "REDESN", "REDUCE", "REFIXP", "REGAL", 
		//	  "REGOSL", "REINST", "REJAWL", "REJOSL", "REMUST", "REVDEP", "RMILRI", "RTPGP", "SETASD", "SIXCPC",
			  "REGOSL", "REINST", "REJAWL", "REJOSL", "REMUST", "REVDEP", "RMILRI", "RTPGP", "SETASD", "SIXCPC", "OPTCPC",
			  "SPINC", "SPLLVE", "STEPUP", "TATRNG", "TDMERG", "TFRDEP", "TFRIN", "TFROUT", "TYOJ", "TYRJ", "UPCLAS", "AICTGP", "FSRMVL");
	
	/*static java.util.HashMap<String, Integer> PRIORITY_TABLE;

	static public HashMap<String, Integer> getPriorityTable() {
		if (PRIORITY_TABLE != null)
			return PRIORITY_TABLE;
	
		PRIORITY_TABLE = new HashMap<String, Integer>();
		PRIORITY_TABLE.put("ATCORR", 2);
		PRIORITY_TABLE.put("CANCEL", 1);
		PRIORITY_TABLE.put("ENROLL", 1);
		PRIORITY_TABLE.put("BASE", 0);
		PRIORITY_TABLE.put("ARREST", 10);
		PRIORITY_TABLE.put("CLOSAR", 10);
		PRIORITY_TABLE.put("ENRMST", 10);
		PRIORITY_TABLE.put("MUSTER", 10);
		PRIORITY_TABLE.put("POSOUT", 13);
		PRIORITY_TABLE.put("REVDEP", 12);
		PRIORITY_TABLE.put("TFRDEP", 11);
		PRIORITY_TABLE.put("TFROUT", 10);
		PRIORITY_TABLE.put("DISEMB", 10);
		PRIORITY_TABLE.put("TATRNG", 10);
		PRIORITY_TABLE.put("EMBODI", 11);
		PRIORITY_TABLE.put("AWL", 14);
		PRIORITY_TABLE.put("DEFAULT", 20);
		PRIORITY_TABLE.put("OSL", 15);
		PRIORITY_TABLE.put("DESN", 18);
		PRIORITY_TABLE.put("ACTGA CEASED", 30);
		PRIORITY_TABLE.put("AETECH CEASED", 30);
		PRIORITY_TABLE.put("AIRWCA CEASED", 30);
		PRIORITY_TABLE.put("ANAL1 CEASED", 30);
		PRIORITY_TABLE.put("ANAL2 CEASED", 30);
		PRIORITY_TABLE.put("AVLPAY CEASED", 30);
		PRIORITY_TABLE.put("BHUTAN CEASED", 30);
		PRIORITY_TABLE.put("CCA CEASED", 30);TRGALL 
		PRIORITY_TABLE.put("CCA1 CEASED", 30);
		PRIORITY_TABLE.put("CCB1 CEASED", 30);
		PRIORITY_TABLE.put("CCB2 CEASED", 30);
		PRIORITY_TABLE.put("CFAA CEASED", 30);
		PRIORITY_TABLE.put("CHAFAA CEASED", 30);
		PRIORITY_TABLE.put("CILQ CEASED", 30);
		PRIORITY_TABLE.put("FAA CEASED", 30);
		PRIORITY_TABLE.put("HRA CEASED", 30);
		PRIORITY_TABLE.put("HRAFAM CEASED", 30);
		PRIORITY_TABLE.put("CMFAA CEASED", 30);
		PRIORITY_TABLE.put("COMNDO CEASED", 30);
		PRIORITY_TABLE.put("DAA CEASED", 30);
		PRIORITY_TABLE.put("DEPA CEASED", 30);
		PRIORITY_TABLE.put("DEPSAG CEASED", 30);
		PRIORITY_TABLE.put("SFASFF CEASED", 30);
		PRIORITY_TABLE.put("DEPSRG CEASED", 30);
		PRIORITY_TABLE.put("FLCHCA CEASED", 30);
		PRIORITY_TABLE.put("HAA CEASED", 30);
		PRIORITY_TABLE.put("HAUCA CEASED", 30);
		PRIORITY_TABLE.put("HAUCH CEASED", 30);
		PRIORITY_TABLE.put("HAUCL CEASED", 30);
		PRIORITY_TABLE.put("HAUCS CEASED", 30);
		PRIORITY_TABLE.put("HCA CEASED", 30);
		PRIORITY_TABLE.put("HCLA CEASED", 30);
		PRIORITY_TABLE.put("HCTA CEASED", 30);
		PRIORITY_TABLE.put("HCWA CEASED", 30);
		PRIORITY_TABLE.put("ISLNDA CEASED", 30);
		PRIORITY_TABLE.put("LKSHDP CEASED", 30);
		PRIORITY_TABLE.put("NSGA CEASED", 30);
		PRIORITY_TABLE.put("PARA CEASED", 30);
		PRIORITY_TABLE.put("PARAR CEASED", 30);
		PRIORITY_TABLE.put("PMHAH CEASED", 30);
		PRIORITY_TABLE.put("RLCA1 CEASED", 30);
		PRIORITY_TABLE.put("RLCA2 CEASED", 30);
		PRIORITY_TABLE.put("RLCA3 CEASED", 30);
		PRIORITY_TABLE.put("RLCA4 CEASED", 30);
		PRIORITY_TABLE.put("SCA CEASED", 30);
		PRIORITY_TABLE.put("SCAGN CEASED", 30);
		PRIORITY_TABLE.put("SCCIA CEASED", 30);
		PRIORITY_TABLE.put("SIACHN CEASED", 30);
		PRIORITY_TABLE.put("SFA CEASED", 30);
		PRIORITY_TABLE.put("SSA CEASED", 30);
		PRIORITY_TABLE.put("TPTH CEASED", 30);
		PRIORITY_TABLE.put("TPTL CEASED", 30);
		PRIORITY_TABLE.put("WCA CEASED", 30);
		PRIORITY_TABLE.put("LRA", 19);
		PRIORITY_TABLE.put("REMUST",51);
		PRIORITY_TABLE.put("INCR", 31);
		PRIORITY_TABLE.put("SIXCPC", 62);
		PRIORITY_TABLE.put("REINST", 40);
		PRIORITY_TABLE.put("REJAWL", 17);
		PRIORITY_TABLE.put("BAIL", 17);
		PRIORITY_TABLE.put("REJOSL", 17);
		PRIORITY_TABLE.put("SPLLVE", 40);
		PRIORITY_TABLE.put("ACP NKSCALE", 49);
		PRIORITY_TABLE.put("ACPFIX NKSCALE", 49);
		PRIORITY_TABLE.put("ACPFIX NKGRADE", 49);
		PRIORITY_TABLE.put("MACPS NKGRADE", 49);
		PRIORITY_TABLE.put("MSTHAV", 50);
		PRIORITY_TABLE.put("REDESN", 19);
		PRIORITY_TABLE.put("REGAL", 18); // setting the priority as DEFAULT (2)
		PRIORITY_TABLE.put("REGOSL", 50);
		PRIORITY_TABLE.put("RTPGP", 50);
		PRIORITY_TABLE.put("TFRIN", 11);
		PRIORITY_TABLE.put("ACP HAVSCALE", 60);
		PRIORITY_TABLE.put("MACPS HAVGRADE", 60);
		PRIORITY_TABLE.put("ACPFIX HAVSCALE", 60);
		PRIORITY_TABLE.put("ACPFIX HAVGRADE", 60);
		PRIORITY_TABLE.put("MSTJCO", 60);
		PRIORITY_TABLE.put("OWNMST", 60);
		PRIORITY_TABLE.put("PMHAL", 60);
		PRIORITY_TABLE.put("POSIN", 60);
		PRIORITY_TABLE.put("PROMOT", 60);
		PRIORITY_TABLE.put("LATENR", 60);
		PRIORITY_TABLE.put("RANKS", 60);
		PRIORITY_TABLE.put("UPCLAS", 60);
		PRIORITY_TABLE.put("SCCIA GRANTED", 70);
		PRIORITY_TABLE.put("ACTGA GRANTED", 70);
		PRIORITY_TABLE.put("AETECH GRANTED", 70);
		PRIORITY_TABLE.put("AIRWCA GRANTED", 70);
		PRIORITY_TABLE.put("ANAL1 GRANTED", 70);
		PRIORITY_TABLE.put("ANAL2 GRANTED", 70);
		PRIORITY_TABLE.put("AVLPAY GRANTED", 70);
		PRIORITY_TABLE.put("BHUTAN GRANTED", 70);
		PRIORITY_TABLE.put("CCA GRANTED", 70);
		PRIORITY_TABLE.put("CCA1 GRANTED", 70);
		PRIORITY_TABLE.put("CCB1 GRANTED", 70);
		PRIORITY_TABLE.put("CCB2 GRANTED", 70);
		PRIORITY_TABLE.put("CFAA GRANTED", 69);
		PRIORITY_TABLE.put("CILQ GRANTED", 70);
		PRIORITY_TABLE.put("FAA GRANTED", 70);
		PRIORITY_TABLE.put("HRA GRANTED", 70);
		PRIORITY_TABLE.put("HRAFAM GRANTED", 70);
		PRIORITY_TABLE.put("CMFAA GRANTED", 70);
		PRIORITY_TABLE.put("DAA GRANTED", 70);
		PRIORITY_TABLE.put("DEPA GRANTED", 70);
		PRIORITY_TABLE.put("DEPSAG GRANTED", 70);
		PRIORITY_TABLE.put("SFASFF GRANTED", 70);
		PRIORITY_TABLE.put("DEPSRG GRANTED", 70);
		PRIORITY_TABLE.put("FLCHCA GRANTED", 70);
		PRIORITY_TABLE.put("HAA GRANTED", 70);
		PRIORITY_TABLE.put("HAUCA GRANTED", 70);
		PRIORITY_TABLE.put("HAUCH GRANTED", 70);
		PRIORITY_TABLE.put("HAUCL GRANTED", 70);
		PRIORITY_TABLE.put("HAUCS GRANTED", 70);
		PRIORITY_TABLE.put("HCA GRANTED", 70);
		PRIORITY_TABLE.put("HCLA GRANTED", 70);
		PRIORITY_TABLE.put("HCTA GRANTED", 70);
		PRIORITY_TABLE.put("HCWA GRANTED", 70);
		PRIORITY_TABLE.put("ISLNDA GRANTED", 70);
		PRIORITY_TABLE.put("LKSHDP GRANTED", 70);
		PRIORITY_TABLE.put("NSGA GRANTED", 70);
		PRIORITY_TABLE.put("PARA GRANTED", 70);
		PRIORITY_TABLE.put("PARAR GRANTED", 70);
		PRIORITY_TABLE.put("PMHAH GRANTED", 70);
		PRIORITY_TABLE.put("RLCA1 GRANTED", 70);
		PRIORITY_TABLE.put("RLCA2 GRANTED", 70);
		PRIORITY_TABLE.put("RLCA3 GRANTED", 70);
		PRIORITY_TABLE.put("RLCA4 GRANTED", 70);
		PRIORITY_TABLE.put("SCA GRANTED", 70);
		PRIORITY_TABLE.put("SCAGN GRANTED", 70);
		PRIORITY_TABLE.put("SIACHN GRANTED", 70);
		PRIORITY_TABLE.put("SFA GRANTED", 70);
		PRIORITY_TABLE.put("SSA GRANTED", 71);
		PRIORITY_TABLE.put("TPTH GRANTED", 69);
		PRIORITY_TABLE.put("TPTL GRANTED", 68);
		PRIORITY_TABLE.put("WCA GRANTED", 70);
		PRIORITY_TABLE.put("MACPS NSUBGRADE", 70);
		PRIORITY_TABLE.put("ACPFIX NSUBGRADE", 70);
		PRIORITY_TABLE.put("CHAFAA GRANTED", 70);
		PRIORITY_TABLE.put("COMNDO GRANTED", 70);
		PRIORITY_TABLE.put("MILRI", 80);
		PRIORITY_TABLE.put("REAPPT", 80);
		PRIORITY_TABLE.put("MACPS SUBGRADE", 80);
		PRIORITY_TABLE.put("ACPFIX SUBGRADE", 80);
		PRIORITY_TABLE.put("APPT", 90);
		PRIORITY_TABLE.put("DEATH", 90);
		PRIORITY_TABLE.put("DISCH", 90);
		PRIORITY_TABLE.put("INVALD", 90);
		PRIORITY_TABLE.put("LORANK", 90);
		PRIORITY_TABLE.put("PAYFIX", 48);
		PRIORITY_TABLE.put("REDUCE", 90);
		PRIORITY_TABLE.put("REFIXP", 90);
		PRIORITY_TABLE.put("SETASD", 90);
		PRIORITY_TABLE.put("ALWREV", 90);
		PRIORITY_TABLE.put("PROFIX", 90);
		PRIORITY_TABLE.put("HAVFIX", 91);
		PRIORITY_TABLE.put("MACPS SMAJGRADE", 90);
		PRIORITY_TABLE.put("ACPFIX SMAJGRADE", 90);
		PRIORITY_TABLE.put("RMILRI", 17);
		PRIORITY_TABLE.put("TYOJ", 91);
		PRIORITY_TABLE.put("TYRJ", 93);
		PRIORITY_TABLE.put("DAILY", 92);
		PRIORITY_TABLE.put("OPTCPC", 94);
		PRIORITY_TABLE.put("AICTGP", 95);		
		PRIORITY_TABLE.put("DA", 110);
		PRIORITY_TABLE.put("STEPUP",108);
		PRIORITY_TABLE.put("SPINC",109);
		PRIORITY_TABLE.put("TDMERG", 110);
		PRIORITY_TABLE.put("FSDESN", 111);
		PRIORITY_TABLE.put("FSCOMN", 111);
		PRIORITY_TABLE.put("FSDETH", 111);
		PRIORITY_TABLE.put("FSDSCH", 111);
		PRIORITY_TABLE.put("FSDSMS", 111);
		PRIORITY_TABLE.put("FSINVL", 111);
		PRIORITY_TABLE.put("FSKILL", 111);
		PRIORITY_TABLE.put("FSTFOT", 111);
		PRIORITY_TABLE.put("FSBTTL", 111);
		PRIORITY_TABLE.put("ADPOST GRANTED", 96);
		PRIORITY_TABLE.put("ADPOST CEASED", 30);
		PRIORITY_TABLE.put("EXTWK GRANTED", 70);
		PRIORITY_TABLE.put("EXTWK CEASED", 30);
		PRIORITY_TABLE.put("TRGALL GRANTED", 70);
		PRIORITY_TABLE.put("TRGALL CEASED", 30);
		PRIORITY_TABLE.put("ACQUIT", 21);   -- TO HANDLE CONVCT, CVLRI AND ACQUIT WHICH ARE PUBLISHED WITH SAME FROMDATE  ---   12.08.2020 
		PRIORITY_TABLE.put("PROALL GRANTED", 70);
		PRIORITY_TABLE.put("PROALL CEASED", 30);
	//	PRIORITY_TABLE.put("BUNCH", 97);
	  	PRIORITY_TABLE.put("BUNCH", 32);
		PRIORITY_TABLE.put("RISK GRANTED", 70);
		PRIORITY_TABLE.put("RISK CEASED", 30);
		PRIORITY_TABLE.put("HAUCA GRANTED", 70);
		PRIORITY_TABLE.put("HAUCA CEASED", 30);
		PRIORITY_TABLE.put("FSRMVL", 111);
		PRIORITY_TABLE.put("STOPIN", 32);
		PRIORITY_TABLE.put("NEDA", 110);
		PRIORITY_TABLE.put("NELRA", 19);
		return PRIORITY_TABLE;
	}


	static public int getPriority(String do2Description) {
		PRIORITY_TABLE = getPriorityTable();
		int DEFAULT_PRIORITY = PRIORITY_TABLE.get("DEFAULT");
		if (PRIORITY_TABLE.get(do2Description) != null) {
			return PRIORITY_TABLE.get(do2Description);
		}
		return DEFAULT_PRIORITY;
	}
*/
	static String[] affectPayOnly = { "ADINC", "ATTSAM", "ATTSBM", "DNCLAS",
			"HCOMMN", "LOCLAS", "MSTJCO", "MSTHAV", "MUSTER", "OWNMST",
			"PMHAH", "REMUST", "TPTL", "TPTH", "UPCLAS" };
	
	static public final List<String> AFFECT_PAY_ONLY = Arrays.asList(affectPayOnly);

	static String[] affectPayAndAllowances = { "ACP", "MACPS","ACPFIX", "BHUTAN",
			"CHAFAA", "EMBODI", "LATENR", "LORANK", "INCR", "PROMOT", "PROFIX",
			"RANKS", "REVERT", "SCCIA", "STEPUP", "TAINCR" };
	static public final List<String> AFFECT_PAY_AND_ALLOWANCES = Arrays.asList(affectPayAndAllowances);

	static String[] affectAllowancesOnly = { "ACCMAL", "ADVAL", "BAL", "CL", "EXTNLV",
			"EXTNAL", "EXTNCL", "EXTNSL", "HOSPTL", "PAL", "SL" };

	static public List<String> LEAVE_TYPES;

	static public List<String> getLeaveTypes() {
		if (LEAVE_TYPES != null)
			return LEAVE_TYPES;
		LEAVE_TYPES = Arrays.asList(new String[] { "ACCMAL", "ADVAL", "BAL",
				"CL", "EXTNAL", "EXTNCL", "PAL", "SL", "EXTNSL" , "EXTNLV"});
		return LEAVE_TYPES;
	}
	
	static public List<String> ALL_LEAVE_TYPES;

	static public List<String> getAllLeaveTypes() {
		if (ALL_LEAVE_TYPES != null)
			return ALL_LEAVE_TYPES;
		ALL_LEAVE_TYPES = Arrays.asList(new String[] { "ACCMAL", "ADVAL", "BAL","AL",
				"CL", "EXTNAL", "EXTNCL", "PAL", "SL", "EXTNSL", "EXTNLV", "PATLVE" });
		return ALL_LEAVE_TYPES;
	}

	static public List<String> LEAVE_EXCLUDE;

	static public List<String> getLeaveExclude() {
		if (LEAVE_EXCLUDE != null)
			return LEAVE_EXCLUDE;
		LEAVE_EXCLUDE = Arrays.asList(new String[] { "EXTNAL", "EXTNCL", "EXTNSL", "EXTNLV", "CCLSMP" });
		return LEAVE_EXCLUDE;
	}
	static public List<String> TO_DATE_BEYOND_CURRRENT_QUARTER;

	static public List<String> getToDateBeyondCurrentQuarter() {
		if (TO_DATE_BEYOND_CURRRENT_QUARTER != null)
			return TO_DATE_BEYOND_CURRRENT_QUARTER;
		TO_DATE_BEYOND_CURRRENT_QUARTER = Arrays.asList(new String[] { "AL", "SL","CL","BAL","PAL","ADVAL","PRPLVE",
				"SPLLVE","RL","EXTNAL","EXTNSL","EXTNCL","RECALL","STOPIN","RETN","TFRDEP","TATRNG","MILRI","EXTNLV",
				"HCOMMN", "CONSER", "MEDCAT", "PFWDL", "MATLVE" });   //MEDCAT added on 24.02.2023 (PAO/AOC)
		return TO_DATE_BEYOND_CURRRENT_QUARTER;
	}
	static public List<String> TO_DATE_BEFORE_DO2_DATE;

	static public List<String> getToDateBeforeDo2Date() {
		if (TO_DATE_BEFORE_DO2_DATE != null)
			return TO_DATE_BEFORE_DO2_DATE;
		TO_DATE_BEFORE_DO2_DATE = Arrays.asList(new String[] { "FRA", "NRA","MRA","SRA","GSRA","ISNRA","ISFRA","DRDSRA",
				"ISMRA","TYOJ","TYRJ","DAILY","RTYOJ","RTYRJ","RDAILY" });
		return TO_DATE_BEFORE_DO2_DATE;
	}
	
	static public List<String> JOURNEY_OVERLAP_DESCRIPTIONS;

	static public List<String> getJourneyOverlapDescriptions() {
		if ( JOURNEY_OVERLAP_DESCRIPTIONS != null)
			return  JOURNEY_OVERLAP_DESCRIPTIONS;
		JOURNEY_OVERLAP_DESCRIPTIONS = Arrays.asList(new String[] { "TYOJ", "TYRJ","NRA","RTYOJ","RTYRJ","RDAILY",
				"DAILY", "LDAILY" });
		return  JOURNEY_OVERLAP_DESCRIPTIONS;
	}
	
	static public List<String> NEW_JOURNEY_OVERLAP_DESCRIPTIONS;

	static public List<String> getNewJourneyOverlapDescriptions() {
		if (NEW_JOURNEY_OVERLAP_DESCRIPTIONS != null)
			return  NEW_JOURNEY_OVERLAP_DESCRIPTIONS;
		NEW_JOURNEY_OVERLAP_DESCRIPTIONS = Arrays.asList(new String[] { "RTYOJ","RTYRJ","RDAILY" });
		return NEW_JOURNEY_OVERLAP_DESCRIPTIONS;
	}
	
	static public List<String> RO_EXEMPTED_PAO_LIST;

	static public List<String> getRoExemptedPaoList() {
		if ( RO_EXEMPTED_PAO_LIST != null)
			return  RO_EXEMPTED_PAO_LIST;
		RO_EXEMPTED_PAO_LIST = Arrays.asList(new String[] { "79", "96","73","76","70","62","59","78","51","74","71","61","72","60","95","93","97","65",
				                                            "58","75","64","69","68","98","99","67","66","53","83"});
		return  RO_EXEMPTED_PAO_LIST;
	}
	
	static public List<String> MUTEXNAMES;
	
	static public List<String> getMutexNames() {
		if (MUTEXNAMES != null)
			return MUTEXNAMES;
        MUTEXNAMES = Arrays.asList(new String[] { "TYOJ", "TYRJ", "DRDSRA",
		"AL", "CL", "SL", "EXTNAL", "EXTNCL", "EXTNSL", "TJRA", "RTYOJ", "EXTNLV",
		"RTYRJ", "RDAILY", "HATRIP", "MRA", "FRA", "SRA", "GSRA", "NRA",
		"ISNRA", "ISMRA", "ISFRA", "ISHRA", "SPLLVE", "PRPLVE", "PAL", 
		"BAL", "ADVAL", "ACCMAL", "RL", "DAILY", "POSDA", "LDAILY" });
        
	    return MUTEXNAMES;
	}
	
	List<String> STOP_ALLOWANCES = Arrays.asList(new String[] { "POSIN", "POSOUT", "PCTRNG" });
	

	static public final List<String> AFFECT_ALLOWANCES_ONLY = Arrays.asList(affectAllowancesOnly);

	static String[] stopAll = { "ARREST", "AWL", "BATTLE", "CLOSAR", "COMMN",
			"CONFIN", "CONVCT", "CVLRI", "DEATH", "DESN", "DETMIL", "DISCH",
			"DISEMB", "DISMIS", "INVALD", "KILLED", "MILRI", "MISING", "OSL",
			"POW", "TFRDEP", "TFROUT" };
	static public final List<String> STOP_ALL = Arrays.asList(stopAll);
	
	static String[] dischargeTypesForOptfix = { "BATTLE", "COMMN", "DEATH",  "DISCH", "DISMIS", "INVALD", "KILLED", "PRDEAD", "REMVAL" };  // tfrout/tfrdep not included.  Added for validation of optfix/todate.
	static public final List<String> DISCHARGE_TYPES_FOR_OPTFIX = Arrays.asList(dischargeTypesForOptfix);

	
	static String[] dischargeTypes = { "BATTLE",  "COMMN", "DEATH",  "DISCH", "DISMIS", "INVALD", "KILLED", "TFROUT", "TFRDEP", "PRDEAD" };
	static public final List<String> DISCHARGE_TYPES = Arrays.asList(dischargeTypes);

	static private List<String> RESTORE_PAY_DESCRIPTIONS;

	static public List<String> getRestorePayDescriptions() {
		if (RESTORE_PAY_DESCRIPTIONS != null)
			return RESTORE_PAY_DESCRIPTIONS;
		RESTORE_PAY_DESCRIPTIONS = Arrays.asList(new String[] { "BAIL",
				"EMBODI", "RECOVR", "REDESN", "REVDEP", "REJAWL", "REJMIS",
				"REJOSL", "REMIT", "REINST","SETASD", "TFRIN","RMILRI" });

		return RESTORE_PAY_DESCRIPTIONS;
	}
	static public List<String> RATION_RELAED_DESCRIPTIONS;
	static public List<String> getRationRelatedDescriptions(){
		if (RATION_RELAED_DESCRIPTIONS !=null)
			return RATION_RELAED_DESCRIPTIONS;
		RATION_RELAED_DESCRIPTIONS = Arrays.asList(new String []{
				"TYOJ", "TYRJ" ,"AL" ,"CL" ,"SL", "EXTNAL" ,"EXTNCL",
			    "EXTNSL", "TJRA" ,"HATRIP" ,"MRA", "FRA", "DRDSRA", "EXTNLV",
			    "SRA" ,"GSRA" ,"NRA", "ISNRA" ,"ISMRA", "ISFRA", "RTYOJ", "RTYRJ", "RDAILY",
			    "ISHRA", "SPLLVE" ,"PRPLVE", "PAL", "BAL", "ADVAL",
			    "ACCMAL" ,"RL", "DAILY" ,"POSDA" ,"LDAILY"	});
		return RATION_RELAED_DESCRIPTIONS;
	}
	static public List<String> RECORD_OFFICE_DESCRIPTIONS;
	static public List<String> getRecordOfficeDescriptions() {
		if (RECORD_OFFICE_DESCRIPTIONS != null)
			return RECORD_OFFICE_DESCRIPTIONS;
		RECORD_OFFICE_DESCRIPTIONS =Arrays.asList(record_office_descriptions);
		return RECORD_OFFICE_DESCRIPTIONS;
	}
	
	// "REDUCE" "SETASD" removed from record office descriptions 05-10-2012
	/*static String[] record_office_descriptions = { "ACP", "MACPS","AWARD", "COMMN", "DNCLAS", "FORGSP", 
		"FORSER", "GSPAY", "HRANK", "GAZTTE", "HCOMMN", "JCNO",	"MRGDGP", "MRGDTD", "MSTHAV", "MEDCAT", 
		"MSTJCO", "MUSTER", "OWNMST", "6PCOPT", "PROMOT", "REINST", "REMUST", "RESGSP", "RETN", "REVERT", 
		"STEPUP", "STRINC", "UPCLAS", "ENCFIN"};*/

	static String[] record_office_descriptions = {"CESRES", "COMEND", "OPTION", "RESLIA", "RESTOR", 
		"UPCLAS", "AWARD", "COMMN", "CONDON", "CONSER", "ENCFIN", "FRMSER", "GAZTTE", "GSPAY", 
		"HCOMMN", "HRANK", "JCNO", "LATENR", "LEP", "MRGDGP", "MRGDTD", "MSTHAV", "MSTJCO", "MUSTER", 
		"OWNMST", "REFIXP", "REMUST", "RESGSP", "SPINC", "STEPUP", "STRINC", "TAINCR", "TB", "TFRIN", "TFROUT"};
	
	static public List<String> REGULARISATION_DESCRIPTIONS;

	static public List<String> getRegularisationDescriptions() {
		if (REGULARISATION_DESCRIPTIONS != null)
			return REGULARISATION_DESCRIPTIONS;
		REGULARISATION_DESCRIPTIONS = Arrays.asList(new String[] { "REGAWL", "REGOSL",
				"REGSL", "ACQUIT" });

		return REGULARISATION_DESCRIPTIONS;
	}

	static public final String CANCEL = "CANCEL";
	// Status values - for Do2 Table status

	static public final String VALID = "V";
	static public final String INVALID = "I";
	static public final String DUMMY_DO2 = "D";
	static public final String PENDING = "P";
	static public final String BACK_DATED = "B";
	static public final String DO2_CANCEL_STATUS = "C";
	static public final char DO2_CANCEL_STATUS_CHAR = 'C';
	static public final char VALID_CHAR = 'V';
	static public final String NONEFFECTIVE = "N";
	static public final String MASTER_MISSING = "M";

	// upto this

	static public final String SERVICE_DETAIL_MISSING = "M";
	static public final String SYSTEM_GENERATED = "S";
	static public final String DO2_GENERATED = "D";
	static public final String MANNUAL_INSERTED = "T";
	// =============================================//
	static public final String BASIC_PAY = "BASIC";
	static public final String DP = "DP";
	static public final String GRADE_PAY = "GRPAY";
	static public final String MS_PAY = "MSPAY";
	static public final String X_GROUP_PAY = "XPAY";
	static public final String CLASS_PAY = "CLSPAY";
	static public final String DA = "DA";
	static public final String PMHAL = "PMHAL";
	static public final String PMHAH = "PMHAH";
	static public final String LRA = "LRA";

	// =============================================//
	static public final String ACTGA = "ACTGA";
	static public final String ADPAY = "ADPAY";
	static public final String AETECH = "AETECH";
	static public final String AVPAY = "AVPAY";
	static public final String CCA = "CCA";
	static public final String CCA1 = "CCA1";
	static public final String CCB1 = "CCB1";
	static public final String CCB2 = "CCB2";
	static public final String CFAA = "CFAA";
	static public final String CHAFAA = "CHAFAA";
	static public final String CILQ = "CILQ";
	static public final String SNLQ = "SNLQ";
	static public final String CMFAA = "CMFAA";
	static public final String COMNDO = "COMNDO";
	static public final String SFACOM = "SFACOM";
	static public final String CCOTH = "CCOTH";
	static public final String DEPA = "DEPA";
	static public final String DEPSAG = "DEPSAG";
	static public final String SFASFF = "SFASFF";
	static public final String DEPSRG = "DEPSRG";
	static public final String FLCHCA = "FLCHCA";
	static public final String FLYPAY = "FLYPAY";
	static public final String FORLAG = "FORLAG";
	static public final String FLAWD = "FLAWD";
	static public final String HAA = "HAA";
	static public final String HRALF = "HRALF";
	static public final String HAUCH = "HAUCH";
	static public final String HAUCL = "HAUCL";
	static public final String HAUCS = "HAUCS";
	static public final String HCA = "HCA";
	static public final String INSTAL = "INSTAL";
	static public final String ISLNDA = "ISLNDA";
	static public final String EXTWK = "EXTWK";
	static public final String ADPOST = "ADPOST";
	static public final String NSGA = "NSGA";
	static public final String RLCA1 = "RLCA1";
	static public final String RLCA2 = "RLCA2";
	static public final String RLCA3 = "RLCA3";
	static public final String RLCA4 = "RLCA4";
	static public final String TLA1 = "TLA1";
	static public final String SCCIA = "SCCIA";
	static public final String SIACHN = "SIACHN";
	static public final String SSA = "SSA";
	static public final String TCA = "TCA";
	static public final String TPTH = "TPTH";
	static public final String TPTL = "TPTL";
	static public final String APTPAY = "APTPAY";
	static public final String BOUNTY = "BOUNTY";
	static public final String TRANSFER_GRANT_HIGH = "TFRGTH";
	static public final String TRANSFER_GRANT_LOW = "TFRGTL";
	static public final String POSIN = "POSIN";
	static public final String RMA = "RMA";
	static public final String GRMA = "GRMA";
	static public final String ENROLL = "ENROLL";
	static public final String RECRUIT_STATUS_CODE = "R";
	static public final String STRINC = "STRINC";
	static public final String SPINC = "SPINC";
	static public final String KILLED = "KILLED";
	static public final String DIED = "DIED";
	static public final String WOUNDED = "WOUNDED";
	static public final String ACDT = "ACDT";
	static public final String CAS = "CAS";
	static public final String FAMLF = "FAMLF";
	static public final String BATTLE = "BATTLE";
	static public final String COMMN = "COMMN";
	static public final String REVDEP = "REVDEP";
	static public final String TFRDEP = "TFRDEP";
	static public final String HOSSUB = "HOSSUB";
	static public final String CONFIN = "CONFIN";
	static public final String ENCASH = "ENCASH";
	static public final String DEATH = "DEATH";
	static public final String INVALD = "INVALD";
	static public final String SCAGN = "SCAGN";
	static public final String DISCH = "DISCH";
	static public final String DEMISE = "DEMISE";
	static public final String REINST = "REINST";
	static public final String MSTHAV = "MSTHAV";
	static public final String MSTJCO = "MSTJCO";
	static public final String ACP = "ACP";
	static public final String MACPS = "MACPS";
	static public final String ACQUIT = "ACQUIT";
	static public final String ARREST = "ARREST";
	static public final String INVOKE = "INVOKE";
	static public final String REVOKE = "REVOKE";
	static public final String MISING = "MISING";
	static public final String PARAR = "PARAR";
	static public final String INITIAL = "INITIAL";
	static public final String HRA = "HRA";
	static public final String FAA = "FAA";
	static public final String HRAFAM = "HRAFAM";
	static public final String CVLRI = "CVLRI";
	static public final String REP = "REP";
	static public final String SEVREP = "SEVREP";
	static public final String DEPAPT = "DEPAPT";
	static public final String DISMIS = "DISMIS";
	static public final String POW = "POW";
	static public final String LINES = "LINES";
	static public final String DETMIL = "DETMIL";
	static public final String REFIXP= "REFIXP";
	static public final String MILRI= "MILRI";
	static public final String ADOPT= "ADOPT";
	static public final String BIRTH= "BIRTH";
	static public final String CHMADN= "CHMADN";
	static public final String CHNAME= "CHNAME";
	static public final String CLDDIS= "CLDDIS";
	static public final String DIVORC= "DIVORC";
	static public final String MARRY= "MARRY";
	static public final String ELOP= "ELOP";
	static public final String REDESN = "REDESN";
	static public final String DESN = "DESN";
	static public final String ATTACH = "ATTACH";
	static public final String ADVENT = "ADVENT";
	static public final String CEA = "CEA";
	static public final String AICTGP = "AICTGP";
	static public final String CLOSAR = "CLOSAR";
	static public final String RISK = "RISK";
	
	static String[] nonPersonal = { "ACTGA", "ADPAY", "AETECH", "CCA", "CCA1",
			"CCB1", "CCB2", "CFAA", "CHAFAA", "CILQ", "CMFAA", "COMNDO",
			"DEPA", "DEPSAG", "SFASFF", "DEPSRG", "FLCHCA", "FLYPAY", "FORLAG",
			"HAA", "HAUCH", "HAUCL", "HAUCS", "HCA", "INSTAL", "NSGA", "RLCA1",
			"RLCA2", "RLCA3", "RLCA4", "SCCIA", "SIACHN", "SSA", "TCA", "TPTH",
			"TPTL" , "TLA1", "TLA2", "TLA3" };

	static public final List<String> NON_PERSONAL_ALLOWANCES = Arrays.asList(nonPersonal);
	static public final List<String> PERSONAL_ALLOWANCES = Arrays
			.asList(new String[] { "GSPAY", "LRA", "PMHAH", "APTPAY", "PMHAL",	"STRINC" });
	static public final List<String> APPOINTMENT_EXCEPTION_PAOS = Arrays
			.asList(new String[] { "80", "81", "82", "84", "85", "87", "90", "94", "91" });
	
	static public final List<String> DCG_PAOS = Arrays.asList(new String[] { "57", "58", "65", "71", "88"});
	static public final List<String> BLADE_SERVER_PAOS = Arrays.asList(new String[] { "73", "88","94", "56","85"});
	static public final String NBSUB = "50";
	static public final int NBSUB_INT = 50;
	static public final String SUB = "60";
	static public final String SUBMAJ = "70";   
	static public final String HONYLT = "80";
	static public final String HONYCP = "90";
	static public final String NK = "30";
	static public final String HAV = "40";
	static public final List<String> JCO_RANKS = Arrays.asList(new String[] {
			NBSUB, SUB, SUBMAJ, HONYLT, HONYCP });
	static public final List<String> NCO_RANKS = Arrays.asList(new String[] { NK, HAV });

	static public final char REGULAR_ARMY = 'R';
	static public final char TERRITORIAL_ARMY = 'T'; 
	static public final char STOP_DA_TA_CATEGORY = 'D';
	static public final String RECRUIT = "10";
	// status values for employee_service_detail
	static public final char ESD_POSIN = 'I';
	static public final char ESD_MUSTER = 'M';
	static public final char ESD_PROMOT = 'R';
	static public final char ESD_GSPAY = 'G';
	static public final char ESD_APPT = 'A';
	static public final char ESD_AWARD = 'W';
	static public final char ESD_PUNISHMENT = 'P';
	static public final char ESD_NQS = 'N';
	static public final char ESD_REGULARIZATION = 'Q';// qualifying service.
	static public final char ESD_DEPUTATION = 'D';
	static public final char ESD_DISCHARGE = 'F';
	static public final char ESD_CLASS_PAY = 'C';
	static public final char ESD_ENROLL = 'E';
	static public final char ESD_TFROUT ='O';
	static public final char ESD_TFRIN ='T';
	static public final char ESD_HIGHER_XPAY ='X';
	static public final char ESD_BARRING ='B';
	// DEPUTATION TO 'S','O','Y','B','H', 'P','M','G','J','I','L','E','N','C','D',Q' - NO PAY TO BE DRAWN
	// DEPUTATION TO 'U' PAY FULL ENTITLEMENT
	// DEPUTATION TO 'T' NO INDIAN DA - ONLY BHUTAN DA
	static public final char DEPU_ASSAM_RIFLES = 'S';
	static public final char DEPU_COAST_GUARD = 'O';
	static public final char DEPU_EMBASSY = 'Y';
	static public final char DEPU_CRPF = 'H';
	static public final char DEPU_BDDS = 'B';// BDDS BOMB DETECTION AND DISPOAL SQUAD
	static public final char DEPU_UNMISSION = 'U';
	static public final char DEPU_IMTRAT_BHUTAN = 'T';
	static public final char DEPU_DANTAK_BHUTAN = 'K';
	
	static public final char DEPU_STATEPOL = 'P';
	static public final char DEPU_HMI = 'M';
	static public final char DEPU_MGTINST = 'G';

	static public final char DEPU_JIMWS = 'J';
	static public final char DEPU_IATT = 'I';
	static public final char DEPU_LBSNA = 'L';
	static public final char DEPU_MEA = 'E';
	
	static public final char DEPU_NTRO = 'N';
	static public final char DEPU_NSCS = 'C';// National Security Council Secretariat 
	static public final char DEPU_SIGTEAM = 'A'; 
	static public final char DEPU_FRNCOURSE = 'F';
	static public final char DEPU_NIMAS = 'D';
	static public final char DEPU_MHA = 'Q';


	// This value is to store RETN,CONSER,CONDON
	// RETENTION,CONTINUANCE IN SERVICE
	// CONDONATION OF SHORT FALL OF SERVICE FOR PENSION
	static public final char ESD_LPC_SERVICE = 'L';
	static public final String CEASED = "CEASED";
	static public final String GRANTED = "GRANTED";
	static public final String DISCHARGE = "DISCH";
	static public final String HOSPITAL = "HOSPTL";
	static public final String ADMITTED = "ADM";
	static public final String GSPAY = "GSPAY";
	static public final String AWL = "AWL";
	static public final String OSL = "OSL";
	static public final String REGAL = "REGAL";
	static public final String REGOSL = "REGOSL";
	static public final String REJOSL = "REJOSL";
	static public final String REJAWL = "REJAWL";
	static public final String OWN_ADMIT = "OWN";
	static public final String NRA = "NRA";
	static public final String SRA = "SRA";
	static public final String MISSING_SERVICE_DETAIL = "Service Details Not Available";

	static public final String RATE_NOT_AVAILABLE = "Rate is Not Available in the Data Base";

	static java.util.HashMap<String, String> CEASED_MAP;

	static public HashMap<String, String> getCeasedMap() {
		if (CEASED_MAP != null)
			return CEASED_MAP;

		CEASED_MAP = new HashMap<String, String>();
		
		CEASED_MAP.put("OSL", "REJOSL");
		CEASED_MAP.put("AWL", "REJAWL");
		CEASED_MAP.put("DESN", "REDESN"); 
		CEASED_MAP.put("MILRI", "RMILRI");
		CEASED_MAP.put("ACP", "ACPFIX");
		CEASED_MAP.put("CILQ", "CILQ");
		CEASED_MAP.put("ACTGA", "ACTGA");
		CEASED_MAP.put("CCA", "CCA");
		CEASED_MAP.put("CCA1", "CCA1");
		CEASED_MAP.put("CCB1", "CCB1");
		CEASED_MAP.put("CCB2", "CCB2");
		CEASED_MAP.put("HCA", "HCA");
		CEASED_MAP.put("HAUCA", "HAUCA");
		CEASED_MAP.put("AVLPAY", "AVLPAY");
		CEASED_MAP.put("WCA", "WCA");
		CEASED_MAP.put("SIACHN", "SIACHN");
		CEASED_MAP.put("BHUTAN", "BHUTAN");
		CEASED_MAP.put("COMNDO", "COMNDO");
		CEASED_MAP.put("DEPA", "DEPA");
		CEASED_MAP.put("CFAA", "CFAA");
		CEASED_MAP.put("PARA", "PARA");
		CEASED_MAP.put("PARAR", "PARAR");
		CEASED_MAP.put("CMFAA", "CMFAA");
		CEASED_MAP.put("SCCIA", "SCCIA");
		CEASED_MAP.put("CHAFAA", "CHAFAA");
		CEASED_MAP.put("SCAGN", "SCAGN");
		CEASED_MAP.put("PMHAH", "PMHAH");
		CEASED_MAP.put("HAUCL", "HAUCL");
		CEASED_MAP.put("HAUCH", "HAUCH");
		CEASED_MAP.put("HAUCS", "HAUCS");
		CEASED_MAP.put("TPTL", "TPTL");
		CEASED_MAP.put("RLCA1", "RLCA1");
		CEASED_MAP.put("RLCA2", "RLCA2");
		CEASED_MAP.put("RLCA3", "RLCA3");
		CEASED_MAP.put("RLCA4", "RLCA4");
		CEASED_MAP.put("HRA", "HRA");
		CEASED_MAP.put("HRAFAM", "HRAFAM");
		CEASED_MAP.put("FAA", "FAA");
		CEASED_MAP.put("TLA1", "TLA1");
		CEASED_MAP.put("TLA2", "TLA2");
		CEASED_MAP.put("TLA3", "TLA3");
		CEASED_MAP.put("SFACOM", "SFACOM");
		CEASED_MAP.put("ADPOST", "ADPOST");	
		CEASED_MAP.put("EXTWK", "EXTWK");	
		CEASED_MAP.put("SPIC", "SPIC");		

		return CEASED_MAP;
	}

	static java.util.HashMap<String, String> RANK_MAP;

	static public HashMap<String, String> getRankMap() {
		if (RANK_MAP != null)
			return RANK_MAP;

		RANK_MAP = new HashMap<String, String>();
		RANK_MAP.put("SEP","SEP");
		RANK_MAP.put("CFN","SEP");
		RANK_MAP.put("GDR","SEP");
		RANK_MAP.put("GDSM","SEP");
		RANK_MAP.put("GNR","SEP");
		RANK_MAP.put("RFN","SEP");
		RANK_MAP.put("SIGMN","SEP");
		RANK_MAP.put("SPR","SEP");
		RANK_MAP.put("TRN","SEP");
		RANK_MAP.put("SWR","SEP");
		RANK_MAP.put("SOWAR","SEP");
		RANK_MAP.put("TSNK", "TSNK");
		RANK_MAP.put("TSLD", "TSNK");
		RANK_MAP.put("TSSL", "TSNK");
		RANK_MAP.put("SBNK", "NK");
		RANK_MAP.put("NK", "NK");
		RANK_MAP.put("PANK", "NK");
		RANK_MAP.put("LD", "NK");
		RANK_MAP.put("SBLD", "NK");
		RANK_MAP.put("PALD", "NK");
		RANK_MAP.put("SL", "NK");
		RANK_MAP.put("SBSL", "NK");
		RANK_MAP.put("PASL", "NK");
		RANK_MAP.put("SBHAV", "HAV");
		RANK_MAP.put("HAV", "HAV");
		RANK_MAP.put("PAHAV", "HAV");
		RANK_MAP.put("DFR", "HAV");
		RANK_MAP.put("SBDFR", "HAV");
		RANK_MAP.put("PADFR", "HAV");
		RANK_MAP.put("AL", "HAV");
		RANK_MAP.put("SBAL", "HAV");
		RANK_MAP.put("PAAL", "HAV");
		RANK_MAP.put("SPHAV", "HAV");
		RANK_MAP.put("GRHAV", "HAV");
		RANK_MAP.put("SPDFR", "HAV");
		RANK_MAP.put("GRDFR", "HAV");
		RANK_MAP.put("GRAL", "HAV");
		RANK_MAP.put("SPAL", "HAV");		
		RANK_MAP.put("NBSUB", "NBSUB");
		RANK_MAP.put("NBRIS", "NBSUB");
		RANK_MAP.put("DL", "NBSUB");
		RANK_MAP.put("SBSUB", "SUB");
		RANK_MAP.put("PASUB", "SUB");
		RANK_MAP.put("SUB", "SUB");
		RANK_MAP.put("RIS", "SUB");
		RANK_MAP.put("PARIS", "SUB");
		RANK_MAP.put("SBRIS", "SUB");
		RANK_MAP.put("CL", "SUB");
		RANK_MAP.put("PACL", "SUB");
		RANK_MAP.put("SBCL", "SUB");
		RANK_MAP.put("SUBMAJ", "SUBMAJ");
		RANK_MAP.put("SBSUBMAJ", "SUBMAJ");
		RANK_MAP.put("PASUBMAJ", "SUBMAJ");
		RANK_MAP.put("RISMAJ", "SUBMAJ");
		RANK_MAP.put("SBRISMAJ", "SUBMAJ");
		RANK_MAP.put("PARISMAJ", "SUBMAJ");
		RANK_MAP.put("POL", "SUBMAJ");
		RANK_MAP.put("SBPOL", "SUBMAJ");
		RANK_MAP.put("PAPOL", "SUBMAJ");
		RANK_MAP.put("HONYLT", "HONYLT");
		RANK_MAP.put("HONYCAPT", "HONYCAPT");
		RANK_MAP.put("DAPON", "HONYCAPT");
		RANK_MAP.put("RUPON", "HONYLT");
		RANK_MAP.put("SPNBSUB", "NBSUB");
		RANK_MAP.put("SPNBRIS", "NBSUB");
		RANK_MAP.put("SPRIS", "SUB");
		RANK_MAP.put("SPSUB", "SUB");

		return RANK_MAP;
	}
	static public HashSet<String> NO_CEASED_SET;

	static public HashSet<String> getNoCeasedSet() {
		if (NO_CEASED_SET != null)
			return NO_CEASED_SET;
		NO_CEASED_SET = new HashSet<String>();
		NO_CEASED_SET.add("OSL");
		NO_CEASED_SET.add("AWL");
		NO_CEASED_SET.add("BAIL");
		NO_CEASED_SET.add("DESN");
		NO_CEASED_SET.add("MILRI");
		return NO_CEASED_SET;
	}
	static public final String ANDAMAN_ALLOWANCE = "ANAL";
	static public final String ANDAMAN_ALLOWANCE1 = "ANAL1";
	static public final String ANDAMAN_ALLOWANCE2 = "ANAL2";
	static public final String LAKSHADWEEP_ALLOWANCE = "LAKDWP";
	static public final String BAIL = "BAIL";
	static public final String DAILY = "DAILY";
	static public final String RDAILY = "RDAILY";
	static public final String RTYOJ = "RTYOJ";
	static public final String RTYRJ = "RTYRJ";
	static public final String TYOJ = "TYOJ";
	static public final String TYRJ = "TYRJ";
	static public final String LDAILY = "LDAILY";
	static public final int MCA_MINIMUM_SERVICE = 180;
	static public final String DOWN_CLASS = "DNCLAS";
	static public final String LOW_CLASS = "LOCLAS";
	// the appointment rate is different for BHM for pao Nasik.
	static public final String PAO_NASIK = "73";
	static public final String PAO_AEC = "93";
	static public final String PAO_NASIK_APPOINMENT = "BHM";
	static public final int LRA_ORS_LEAVE_DAYS = 75;
	static public final String PAO_DSC = "94";
	static public final String PAO_JKLI = "99";
	static public final String PAO_EME = "88";
	static public final String PAO_ACR = "51";
	static public final String PAO_PBG = "52";
	static public final String PAO_INT = "89";
	static public final String PAO_MIRC = "74";
	static public final String PAO_BEG = "55";
	static public final String PAO_RVC = "84";
	static public final String PAO_KAMPTEE = "97";
	static public final String PAO_LADAK_SCOUTS = "98";
	static public final String AO_ESTTNO22 = "40";
	static public final String APS_NAGPUR = "41";
	static public final String PAO_RRRC = "62";
	static public final String REMUSTER = "REMUST";
	static public final int LRA_DSC_LEAVE_DAYS = 49;
	static public final int NO_OF_MONTHS = 12;
	static public final int AL_CEASE_UPPER_LIMIT = 28;
	static public final int AL_CEASE_LOWER_LIMIT = 21;
	static public final int CL_CEASE_UPPER_LIMIT = 21;
	static public final int CL_CEASE_LOWER_LIMIT = 14;
	static public final int LEAVE_ZERO_LIMIT = 0;
	static public final String PARA = "PARA";
	static public final char ENROLL_AGE_INDICATOR = 'R';
	static public final char NON_EFFECTIVE = 'N';
	static public final String PAO_AOC = "87";


	static public HashSet<String> VALID_CITY_CODES;

	static public HashSet<String> getValidCityCodes() {
		if (VALID_CITY_CODES != null)
			return VALID_CITY_CODES;
		String[] cityCodes = { "CCA1", "CCA", "CCB1", "CCB2", "CCOTH", "CCC" };
		VALID_CITY_CODES = new HashSet<String>();
		for (String city : cityCodes) {
			VALID_CITY_CODES.add(city);
		}
		return VALID_CITY_CODES;
	}

	static public final int ENROLL_NORMAL = 1;
	static public final int ENROLL_SIKH = 2;
	static public final int ENROLL_GURKHA = 3;
	
	static public final String ENROLL_HAV_Y = "SPHV-Y";
	static public final String ENROLL_HAV_Z = "SPHV-Z";
	static public final String ENROLL_HAV_AEC = "HAVEDN";
	static public final String ENROLL_NSUB_Y = "NSUB-Y";
	static public final String ENROLL_NSUB_Z = "NSUB-Z";
	static public final String ENROLL_SUBE_Y = "SUBE-Y";
	static public final String ENROLL_SUBE_Z = "SUBE-Z";
	static public final String ENROLL_SMAJ_Y = "SMAJ-Y";
	static public final String ENROLL_SMAJ_Z = "SMAJ-Z";
	
	//Added for NEW ENROLL Do2
		static public final String ENROLL_NBSUB = "NBSUB";
		static public final String ENROLL_HAV = "HAV";
		static public final String ENROLL_RECT = "RECT";
		
	
	static public final String ENROLL_HAV_BEGRKE = "HAVSAC";
	
	static public final int JCO_RANK_CODE = 50;
	

	static public final String APPENDIX_J_SPECIAL = "SPECIAL";
	static public final String SIXCPC_ALLOWANCE_DATE = "2008-09-01";

	static String[][] recruitCategory = { { ENROLL_NSUB_Y, "50" },
		{ ENROLL_NSUB_Z, "50" }, { ENROLL_SUBE_Y, "60" },
		{ ENROLL_SUBE_Z, "60" }, { ENROLL_SMAJ_Y, "70" },
		{ ENROLL_SMAJ_Z, "70" }, { ENROLL_HAV_Y,  "40"},
		{ ENROLL_HAV_Z,  "40"}, { ENROLL_HAV_AEC, "40"},
		{ ENROLL_NBSUB, "50"},  {ENROLL_HAV, "40"}, {ENROLL_RECT, "10"}};//Added for NEW ENROLL Do2};};
	static public HashMap<String, String> RECRUIT_CATEGORY_MAP;

	static public HashMap<String, String> getRecruitCategoryMap() {
		if (RECRUIT_CATEGORY_MAP != null)
			return RECRUIT_CATEGORY_MAP;
		RECRUIT_CATEGORY_MAP = new HashMap<String, String>();
		for (int i = 0; i < recruitCategory.length; i++) {
			String keyVal[] = recruitCategory[i];
			RECRUIT_CATEGORY_MAP.put(keyVal[0], keyVal[1]);
		}
		return RECRUIT_CATEGORY_MAP;
	}
	static public final String ENROLL_DSC_Y = "DSC-Y";
	static public final String ENROLL_DSC_Z = "DSC-Z";
	static public final String ENROLL_SEP_Y = "SEP-Y";
	static public final String ENROLL_SEP_Z = "SEP-Z";
	static public final String ENROLL_SER_B = "SER-B";
	static public final String ENROLL_SER_C = "SER-C";
	static public final String ENROLL_SER_D = "SER-D";
	static public final String ENROLL_SER_E = "SER-E";
	//Added for New  Enroll
		static public final String ENROLL_SUB_Y = "SUB-Y";
	
	static String[][] dscRecruitCategory = { {ENROLL_DSC_Y, "20"}, {ENROLL_DSC_Z, "20"}, {ENROLL_SEP_Y, "20"},
	  										 {ENROLL_SEP_Z, "20"}, {ENROLL_SER_B, "20"}, {ENROLL_SER_C, "20"},
											 {ENROLL_SER_D, "20"}, {ENROLL_SER_E, "20"},
											 {ENROLL_NSUB_Y, "50"},{ENROLL_SUB_Y, "60"}, {ENROLL_SMAJ_Y, "70"}};// Added for New Enroll};

	static public HashMap<String, String> DSC_RECRUIT_CATEGORY_MAP;

	static public HashMap<String, String> getDscRecruitCategoryMap() {
		if (DSC_RECRUIT_CATEGORY_MAP != null)
			return DSC_RECRUIT_CATEGORY_MAP;
		DSC_RECRUIT_CATEGORY_MAP = new HashMap<String, String>();
		for (int i = 0; i < dscRecruitCategory.length; i++) {
			String keyVal[] = dscRecruitCategory[i];
			DSC_RECRUIT_CATEGORY_MAP.put(keyVal[0], keyVal[1]);
	 	}
		return DSC_RECRUIT_CATEGORY_MAP;
	}
	
	static String[][] payCodeMapping = { { ENROLL, "DAILY" }, { "FORPAY", BASIC_PAY }, {"ENCFIN", "FSCASH"},
			{"GSRA", "SRA"}, {"DRDSRA", "SRA"}, {"FDAILY", "DAILY"}, { "CEAHC", "CEA" }, { "HSBTWN", "HOSSUB" },
			{ "FINE", BASIC_PAY }, { "RECALL", "DAILY" }, { "RL", "NRA" }, { "CEATWN", "CEA" }, 
			{"PRPLVE", "NRA"}, {"DMALLC", "DIPMNY"}, {"DIVALL", "DIVING"}, {"PEDEDN", BASIC_PAY }};
	static public HashMap<String, String> PAY_CODE_MAP;

	static public HashMap<String, String> getPayCodeMap() {
		if (PAY_CODE_MAP != null)
			return PAY_CODE_MAP;
		PAY_CODE_MAP = new HashMap<String, String>();
		for (int i = 0; i < payCodeMapping.length; i++) {
			String keyVal[] = payCodeMapping[i];
			PAY_CODE_MAP.put(keyVal[0], keyVal[1]);
		}
		return PAY_CODE_MAP;
	}
	static String[][] descriptionMapping = { { "REJPAL", "REJAL" }, { "REJRL", "REJAL" }, 
		{ "REJAAL", "REJAL" }, { "REJBAL", "REJAL" },{ "POSDA", "JRNYPD" },{ "IRSTAL", "INSTAL" },{ "TPAL", "TPTL" } };
	static public HashMap<String, String> DESCRIPTION_MAP;

	static public HashMap<String, String> getDescriptionMap() {
		if (DESCRIPTION_MAP != null)
		return DESCRIPTION_MAP;
		DESCRIPTION_MAP = new HashMap<String, String>();
		for (int i = 0; i < descriptionMapping.length; i++) {
		String keyVal[] = descriptionMapping[i];
		DESCRIPTION_MAP.put(keyVal[0], keyVal[1]);
		}
		return DESCRIPTION_MAP;
	}
	
	static public HashSet<String> VALID_SYSTEM_DO2S;

	static public HashSet<String> getValidSystemDo2s() {
		if (VALID_SYSTEM_DO2S != null)
			return VALID_SYSTEM_DO2S;
		String[] descriptions = { "LRA", "DA", "BONUS", "ALWREV" };
		VALID_SYSTEM_DO2S = new HashSet<String>();
		for (String description : descriptions) {
			VALID_SYSTEM_DO2S.add(description);
		}
		return VALID_SYSTEM_DO2S;
	}
	static String[] awardList = { 	"AC", "KC", "MVC","PVC", "SC", "VRC","SM", "BARSM", "BARSC", "ASSPP" };
	static public final List<String> AWARD_LIST = Arrays.asList(awardList);

	static public HashSet<String> VALID_NEXTDAY_DO2S;

	static public HashSet<String> getValidNextDayDo2s() {
		if (VALID_NEXTDAY_DO2S != null)
			return VALID_NEXTDAY_DO2S;
		String[] descriptions = { "DEATH", "KILLED", "BATTLE", "DISCH", "FSDSCH", "FSDETH", "FSINVL", "FSDSMS", "FSRMVL"};   // FSDETH, FSINVL, FSDSMS added on 22-11-2021 due to miscfscases
		VALID_NEXTDAY_DO2S = new HashSet<String>();
		for (String description : descriptions) {
			VALID_NEXTDAY_DO2S.add(description);
		}
		return VALID_NEXTDAY_DO2S;
	}

	static public HashSet<String> VALID_CANCEL_EXCEPTIONS;

	static public HashSet<String> getValidCancelExceptions() {
		if (VALID_CANCEL_EXCEPTIONS != null)
			return VALID_CANCEL_EXCEPTIONS;
		String[] descriptions = { "DEATH", "KILLED", "BATTLE", "DISCH", "DESN",
				"COMMN", "DISMIS", "INVALD", "TFROUT" };
		VALID_CANCEL_EXCEPTIONS = new HashSet<String>();
		for (String description : descriptions) {
			VALID_CANCEL_EXCEPTIONS.add(description);
		}
		return VALID_CANCEL_EXCEPTIONS;
	}
	
	static public HashSet<String> NOT_VALID_DO2_DESCRIPTIONS_FOR_DSC;

	static public HashSet<String> getNotValidDo2DescriptionsForDSC() {
		if (NOT_VALID_DO2_DESCRIPTIONS_FOR_DSC != null)
			return NOT_VALID_DO2_DESCRIPTIONS_FOR_DSC;
		String[] descriptions = {"MUSTER","MSTHAV","MSTJCO","UPCLAS","LOCLAS", "DNCLAS","GSPAY","FORGSP","RESGSP",
				 "ACP","OWNMST","DAA","INSTAL","AIRWCA","AETECH","AVPAY","FLCHCA","FLYPAY","PARA","PARAR","PARARC","PAJUMP","SPCLA","RL"};
		NOT_VALID_DO2_DESCRIPTIONS_FOR_DSC = new HashSet<String>();
		for (String description : descriptions) {
			NOT_VALID_DO2_DESCRIPTIONS_FOR_DSC.add(description);
		}
		return NOT_VALID_DO2_DESCRIPTIONS_FOR_DSC;
	}
	
	static public HashSet<String> NOT_VALID_DO2_DESCRIPTIONS_FOR_ESTT22;

	static public HashSet<String> getNotValidDo2DescriptionsForEstt22() {
		if (NOT_VALID_DO2_DESCRIPTIONS_FOR_ESTT22 != null)
			return NOT_VALID_DO2_DESCRIPTIONS_FOR_ESTT22;
		String[] descriptions = {"ATTSAM", "ATTSBM","REMUST","MSTHAV","MSTJCO","UPCLAS","LOCLAS","DNCLAS","GSPAY","FORGSP","RESGSP",
				"OWNMST","DAA","AIRWCA","AETECH","AVPAY","FLCHCA","FLYPAY","SPCLA","TFRDEP","REVDEP","ACP","BHUTAN"};
		NOT_VALID_DO2_DESCRIPTIONS_FOR_ESTT22 = new HashSet<String>();
		for (String description : descriptions) {
			NOT_VALID_DO2_DESCRIPTIONS_FOR_ESTT22.add(description);
		}
		return NOT_VALID_DO2_DESCRIPTIONS_FOR_ESTT22;
	}
	
	static public List<String> EXCEPTION_LADAK_UNITS;

	static public List<String> getExceptionLadakUnits() {
		if (EXCEPTION_LADAK_UNITS != null)
			return EXCEPTION_LADAK_UNITS;
		EXCEPTION_LADAK_UNITS = Arrays.asList(new String[] { "98B001",
				"98B002", "98B003", "98B004", "98B005", "98B006", "98E001" });
		return EXCEPTION_LADAK_UNITS;
	}

	static public HashSet<String> VALID_STOP_PAY_DEPUTATION;

	static public HashSet<String> getValidStopPayDeputation() {
		if (VALID_STOP_PAY_DEPUTATION != null)
			return VALID_STOP_PAY_DEPUTATION;
		
		// String[] deputationCodes = { "CGUARD", "ASSAM", "EMBASSY", "BDDS","CRPF", "STATEPOL", "HMI", "MGTINST", "JIMWS","IATT","LBSNA","MEA","NTRO","NSCS" };

// As per the telephonic directions of Hqrs. Office, NTRO is removed from the list below on 24.04.2020.  Letter from O/o. PCDA(R&D) - DRDO awaited. 
		String[] deputationCodes = { "CGUARD", "ASSAM", "EMBASSY", "BDDS", "CRPF", "STATEPOL", "HMI", "MGTINST", "JIMWS", "IATT", "LBSNA", "MEA", "NSCS", "NIMAS", "MHA"};

		
		/*// As per the orders of PCDA Bangalore CGUARD are also exempted.
		String[] deputationCodes = { "ASSAM", "EMBASSY", "BDDS","CRPF", "STATEPOL", "HMI", "MGTINST" };*/
		
		
				VALID_STOP_PAY_DEPUTATION = new HashSet<String>();
		for (String deputation : deputationCodes) {
			VALID_STOP_PAY_DEPUTATION.add(deputation);
		}
		return VALID_STOP_PAY_DEPUTATION;
	}

	static public HashSet<String> VALID_STOP_PAY_DEPUTATION_FROMAUG2016;
	
	static public HashSet<String> getValidStopPayDeputationFromAug2016() {
		if (VALID_STOP_PAY_DEPUTATION_FROMAUG2016 != null)
			return VALID_STOP_PAY_DEPUTATION_FROMAUG2016;
	//	String[] deputationCodes = {  "ASSAM", "EMBASSY", "BDDS","CRPF", "STATEPOL", "HMI", "MGTINST","JIMWS","IATT","LBSNA","MEA", "NTRO","NSCS" };
// As per the telephonic directions of Hqrs. Office, NTRO is removed from the list below on 24.04.2020.  Letter from O/o. PCDA(R&D) - DRDO awaited. 

		String[] deputationCodes = { "ASSAM", "EMBASSY", "BDDS", "CRPF", "STATEPOL", "HMI", "MGTINST","JIMWS","IATT","LBSNA","MEA", "NSCS", "NIMAS", "MHA" };
	/*// As per the orders of PCDA Bangalore CGUARD are also exempted.
		String[] deputationCodes = { "ASSAM", "EMBASSY", "BDDS","CRPF", "STATEPOL", "HMI", "MGTINST" };*/
		VALID_STOP_PAY_DEPUTATION_FROMAUG2016 = new HashSet<String>();
		for (String deputation : deputationCodes) {
			VALID_STOP_PAY_DEPUTATION_FROMAUG2016.add(deputation);
		}
		return VALID_STOP_PAY_DEPUTATION_FROMAUG2016;
	}
	
	static String[][] deputationStatusCodeMapping = { { "CGUARD", "O" },
			{ "ASSAM", "S" }, { "EMBASSY", "Y" }, { "UNMISN", "U" },
			{ "IMTRAT", "T" }, { "BDDS", "B" }, {"CRPF","H"}, {"STATEPOL", "P"}, 
			{ "HMI", "M" }, { "MGTINST", "G" }, { "JIMWS", "J" }, { "IATT", "I" },
			{ "LBSNA", "L" }, { "MEA", "E" }, { "NTRO", "N" }, { "NSCS", "C" }, 
			{ "SIGTEAM", "A" }, { "FRNCOURSE", "F" }, { "NIMAS", "D" }, { "MHA", "Q" }, { "DANTAK", "K" } };
			
	static public HashMap<String, String> DEPUTATION_STATUS_CODE_MAP;

	static public HashMap<String, String> getDeputationStatusCodeMap() {
		if (DEPUTATION_STATUS_CODE_MAP != null)
			return DEPUTATION_STATUS_CODE_MAP;
		DEPUTATION_STATUS_CODE_MAP = new HashMap<String, String>();
		for (int i = 0; i < deputationStatusCodeMapping.length; i++) {
			String keyVal[] = deputationStatusCodeMapping[i];
			DEPUTATION_STATUS_CODE_MAP.put(keyVal[0], keyVal[1]);
		}
		return DEPUTATION_STATUS_CODE_MAP;
	}
	static public HashSet<String> INCR_CANCEL_DESCRIPTIONS;

	static public HashSet<String> getIncrCancelDescription() {
		if (INCR_CANCEL_DESCRIPTIONS != null)
			return INCR_CANCEL_DESCRIPTIONS;
		String[] do2Description = { "PROMOT","ACP","MACPS","REVERT","RANKS","MSTHAV","MSTJCO","LATENR","STEPUP","HCOMMN","OPTFIX" };
		INCR_CANCEL_DESCRIPTIONS = new HashSet<String>();
		for (String description : do2Description) {
			INCR_CANCEL_DESCRIPTIONS.add(description);
		}
		return INCR_CANCEL_DESCRIPTIONS;
	}
	static public HashSet<String> ARREST_DESCRIPTIONS;

	static public HashSet<String> getArrestDescription() {
		if (ARREST_DESCRIPTIONS != null)
			return ARREST_DESCRIPTIONS;
		String[] do2Description = { "ARREST", "BAIL","ACQUIT" };
		ARREST_DESCRIPTIONS = new HashSet<String>();
		for (String description : do2Description) {
			ARREST_DESCRIPTIONS.add(description);
		}
		return ARREST_DESCRIPTIONS;
	}
	static public HashSet<String> MUSTER_ATTEST_DESCRIPTIONS;

	static public HashSet<String> getMusterAttesttDescription() {
		if (MUSTER_ATTEST_DESCRIPTIONS != null)
			return MUSTER_ATTEST_DESCRIPTIONS;
		String[] do2Description = { "MUSTER", "ATTSBM","ATTSAM"  };
		MUSTER_ATTEST_DESCRIPTIONS = new HashSet<String>();
		for (String description : do2Description) {
			MUSTER_ATTEST_DESCRIPTIONS.add(description);
		}
		return MUSTER_ATTEST_DESCRIPTIONS;
	}
	static public HashSet<String> SYSTEM_GENERATED_DESCRIPTIONS;

	static public HashSet<String> getSystemGeneratedDescription() {
		if (SYSTEM_GENERATED_DESCRIPTIONS != null)
			return SYSTEM_GENERATED_DESCRIPTIONS;
		String[] do2Description = { "DA", "LRA","PLBON","ALWREV", "INCR", "PROFIX","ACPFIX","TDMERG","HAVFIX", "FIX7PC" };
		SYSTEM_GENERATED_DESCRIPTIONS = new HashSet<String>();
		for (String description : do2Description) {
			SYSTEM_GENERATED_DESCRIPTIONS.add(description);
		}
		return SYSTEM_GENERATED_DESCRIPTIONS;
	}
	
	static public HashSet<String> VALIDATION_REQUIRED_SYSTEM_GENERATED_DESCRIPTIONS;

	static public HashSet<String> getValidationRequiredSystemGeneratedDescription() {
		if (VALIDATION_REQUIRED_SYSTEM_GENERATED_DESCRIPTIONS != null)
			return VALIDATION_REQUIRED_SYSTEM_GENERATED_DESCRIPTIONS;
		String[] do2Description = { "DA", "LRA", "PLBON", "INCR","ALWREV"};
		VALIDATION_REQUIRED_SYSTEM_GENERATED_DESCRIPTIONS = new HashSet<String>();
		for (String description : do2Description) {
			VALIDATION_REQUIRED_SYSTEM_GENERATED_DESCRIPTIONS.add(description);
		}
		return VALIDATION_REQUIRED_SYSTEM_GENERATED_DESCRIPTIONS;
	}
	
	static public HashSet<String> REJOIN_LEAVE_DESCRIPTIONS;

	static public HashSet<String> getRejoinLeaveDescription() {
		if (REJOIN_LEAVE_DESCRIPTIONS != null)
			return REJOIN_LEAVE_DESCRIPTIONS;
		String[] do2Description = { "REJAL", "REJSL","REJCL" };
		REJOIN_LEAVE_DESCRIPTIONS = new HashSet<String>();
		for (String description : do2Description) {
			REJOIN_LEAVE_DESCRIPTIONS.add(description);
		}
		return REJOIN_LEAVE_DESCRIPTIONS;
	}
	static String[] prevPromotDo2= { "MSTHAV","MSTJCO","LATENR", "INCR" };
	static public final List<String> PREV_PROMOT_DO2 = Arrays.asList(prevPromotDo2);

	static public HashSet<String> PAID_ACTING_DESCRIPTIONS;

	static public HashSet<String> getPaidActingDescription() {
		if (PAID_ACTING_DESCRIPTIONS != null)
			return PAID_ACTING_DESCRIPTIONS;
		String[] do2ColumnA = { "PANK", "PAHAV","PASUB","PASUBMAJ","PALD","PADFR","PARIS","PARISMAJ"};
		PAID_ACTING_DESCRIPTIONS = new HashSet<String>();
		for (String description : do2ColumnA) {
			PAID_ACTING_DESCRIPTIONS.add(description);
		}
		return PAID_ACTING_DESCRIPTIONS;
	}
	static public HashSet<String> SUBSTANTIVE_RANK_DESCRIPTIONS;

	static public HashSet<String> getSubstantiveRankDescription() {
		if (SUBSTANTIVE_RANK_DESCRIPTIONS != null)
			return SUBSTANTIVE_RANK_DESCRIPTIONS;
		String[] do2ColumnA = { "SBNK", "SBHAV","SBSUB","SBSUBMAJ","SBLD","SBDFR","SBRIS","SBRISMAJ"};
		SUBSTANTIVE_RANK_DESCRIPTIONS = new HashSet<String>();
		for (String description : do2ColumnA) {
			SUBSTANTIVE_RANK_DESCRIPTIONS.add(description);
		}
		return SUBSTANTIVE_RANK_DESCRIPTIONS;
	}
	static public HashSet<String> JCO_RANK_DESCRIPTIONS;

	static public HashSet<String> getJcoRankDescription() {
		if (JCO_RANK_DESCRIPTIONS != null)
			return JCO_RANK_DESCRIPTIONS;
		String[] do2ColumnA = { "NBSUB", "NBRIS","PASUB","SBSUB","PARIS","SBRIS","PASUBMAJ","SBSUBMAJ","PARISMAJ","SBRISMAJ"};
		JCO_RANK_DESCRIPTIONS = new HashSet<String>();
		for (String description : do2ColumnA) {
			JCO_RANK_DESCRIPTIONS.add(description);
		}
		return JCO_RANK_DESCRIPTIONS;
	}
	
	static public List<String> STOP_FIELD_ALLOWANCE_DESCRIPTIONS_ON_TEMPORARY_DUTY_FOURTEEN_DAYS;

	static public List<String> getStopFieldAllowanceDescriptionsOnTemporaryDutyFourteenDays() {
		
		if (STOP_FIELD_ALLOWANCE_DESCRIPTIONS_ON_TEMPORARY_DUTY_FOURTEEN_DAYS != null)
			return STOP_FIELD_ALLOWANCE_DESCRIPTIONS_ON_TEMPORARY_DUTY_FOURTEEN_DAYS;
		
		STOP_FIELD_ALLOWANCE_DESCRIPTIONS_ON_TEMPORARY_DUTY_FOURTEEN_DAYS = 
			 	Arrays.asList(new String[] {"SIACHN", "HAUCH", "HAUCL", "HAUCS", "HAUCA"});
		
		return STOP_FIELD_ALLOWANCE_DESCRIPTIONS_ON_TEMPORARY_DUTY_FOURTEEN_DAYS;
	}
	
	
	static public List<String> STOP_FIELD_ALLOWANCE_DESCRIPTIONS_ON_TEMPORARY_DUTY_TWENTYONE_DAYS;

	static public List<String> getStopFieldAllowanceDescriptionsOnTemporaryDutyTwentyoneDays() {
		
		if (STOP_FIELD_ALLOWANCE_DESCRIPTIONS_ON_TEMPORARY_DUTY_TWENTYONE_DAYS != null)
			return STOP_FIELD_ALLOWANCE_DESCRIPTIONS_ON_TEMPORARY_DUTY_TWENTYONE_DAYS;
		
		STOP_FIELD_ALLOWANCE_DESCRIPTIONS_ON_TEMPORARY_DUTY_TWENTYONE_DAYS = 
			 	Arrays.asList(new String[] {"SIACHN", "HAUCH", "HAUCL", "HAUCS", "SCCIA", "CHAFAA", "HAA", "RISK"});
		
		return STOP_FIELD_ALLOWANCE_DESCRIPTIONS_ON_TEMPORARY_DUTY_TWENTYONE_DAYS;
	}
	
	static public HashSet<String> FS_DISCH_DESCRIPTIONS;
	
	static public HashSet<String> getFsDischDescriptions() {
		if (FS_DISCH_DESCRIPTIONS != null)
			return FS_DISCH_DESCRIPTIONS;
		String[] do2Description = { "FSDSCH","FSDESN", "FSTFOT","FSDETH","FSDSMS","FSINVL","FSKILL", "FSBTTL", "FSCOMN", "FSRMVL" };
		FS_DISCH_DESCRIPTIONS = new HashSet<String>();
		for (String description : do2Description) {
			FS_DISCH_DESCRIPTIONS.add(description);
		}
		return FS_DISCH_DESCRIPTIONS;
		
	}
	              
	
	  static public List<String> mutualExclusiveList = Arrays.asList(new String [] {"ANAL1", "ANAL2","ARUNA1",
			  "ARUNA2", "ASMEGH","ASSAM", "AVLPAY", "BHUTAN","CCA","CCA1", "CCB1", "CCB2", "CCOTH",
			  "CHAFAA",  "CMFAA", "CFAA", "DAA",  "EXPAT", "HAA", "HAUCA", "HAUCH", "HAUCL", "HAUCS",
			  "HCA",  "HIMA1", "HIMA2",  "HIMA3", "HIMA4", "ISLNDA", "JK1", "JK2",
			  "JK3",  "LAKDWP", "MANI", "MIZO1", "MIZO2", "MIZO3", "NAGLND", "RLCA1", "RLCA2",
			  "RLCA3",  "RLCA4", "SCA", "SCAGN", "SCCIA", "SFA", "SIACHN", "SIKKIM", "SIKMAL",
			  "TRIPU1", "TRIPU2", "UP","HRDALL", "TLA1", "TLA2", "TLA3", "ANAL", "ARUNA", "UK", "RISK" });
	  //RISK added on 16.03.2023
	  static public String[] outfitValidRanks = {"NBSUB","NBRIS","HONYLT"};
	  static public final List<String> outfitValidRanksList = Arrays.asList(outfitValidRanks);

	  static public HashMap<String, String> AHMEDNAGR_MAP;
		
	  static String[][] ahmedNagarMapping = { { "SWR", "SEP" },
			{ "LD", "NK" }, { "PALD", "PANK" },{ "SBLD", "SBNK" },{ "DFR", "HAV" }
			,{ "SBDFR", "SBHAV" },{ "PADFR", "PAHAV" },{"NBRIS", "NBSUB"},{"RIS","SUB"},{"PARIS","PASUB"},{"SBRIS","SBSUB"},
			{"PARISMAJ","PASUBMAJ"},{"SBRISMAJ","SBSUBMAJ"},{ "TSALD", "TSNK" },
			{ "ALD", "LNK" }, { "LDFR", "LHAV" },{ "SDM", "CHM" },{ "RDM", "RHM" }
			,{ "BDM", "BHM" },{"SQMD", "CQMH"},{"MQMD","RQMH"},{"RQMD","BQMH"},{"SBRIS","SBSUB"},
			{"PARISMAJ","PASUBMAJ"},{"SBRISMAJ","SBSUBMAJ"},{ "LDSCALE", "NKSCALE" },
			{ "DFRSCALE", "HAVSCALE" },{ "LDGRADE", "NKGRADE" },{ "DFRGRADE", "HAVGRADE" }
			,{ "NRISGRADE", "NSUBGRADE" },{ "RISGRADE", "SUBGRADE" },{ "RMAJGRADE", "SMAJGRADE" }};
		
	  static public HashMap<String, String> getAhmedNagarMap() {
			if (AHMEDNAGR_MAP != null)
			return AHMEDNAGR_MAP;
			AHMEDNAGR_MAP = new HashMap<String, String>();
			for (int i = 0; i < ahmedNagarMapping.length; i++) {
			String keyVal[] = ahmedNagarMapping[i];
			AHMEDNAGR_MAP.put(keyVal[0], keyVal[1]);
			}
			return AHMEDNAGR_MAP;
	  }
	  
	  static public HashMap<String, String> AHMEDNAGR_REVERSE_MAP;
		
	  static String[][] ahmedNagarReverseMapping = { {"SEP","SWR"},{"NK","LD" }, {"TSNK","TSALD" },
			{"PANK", "PALD" },{ "SBNK", "SBLD" },{ "HAV", "DFR"},
			{"SBHAV", "SBDFR"}, {"PAHAV","PADFR" }, {"NBSUB", "NBRIS"},{"SUB", "RIS"},
			{"PASUB", "PARIS"}, {"SBSUB", "SBRIS"}, {"PASUBMAJ", "PARISMAJ"}, {"SBSUBMAJ", "SBRISMAJ"},
			{"LNK", "ALD"}, {"LHAV","LDFR" },{"CHM","SDM"},{"RHM", "RDM"},
			{"BHM", "BDM"},{"CQMH", "SQMD"},{"RQMH","MQMD"},{"BQMH","RQMD"},{"SBSUB","SBRIS"},
			{"PASUBMAJ","PARISMAJ"},{"SBSUBMAJ","SBRISMAJ"},{ "NKSCALE","LDSCALE" },
			{"HAVSCALE","DFRSCALE"},{"NKGRADE", "LDGRADE"},{"HAVGRADE", "DFRGRADE"},
			{"NSUBGRADE","NRISGRADE"},{"SUBGRADE","RISGRADE"},{"SMAJGRADE", "RMAJGRADE"}};
		
	  static public HashMap<String, String> getAhmedNagarReverseMap() {
			if (AHMEDNAGR_REVERSE_MAP != null)
			return AHMEDNAGR_REVERSE_MAP;
			AHMEDNAGR_REVERSE_MAP = new HashMap<String, String>();
			for (int i = 0; i < ahmedNagarReverseMapping.length; i++) {
			String keyVal[] = ahmedNagarReverseMapping[i];
			AHMEDNAGR_REVERSE_MAP.put(keyVal[0], keyVal[1]);
			}
			return AHMEDNAGR_REVERSE_MAP;
	  }
	  
	  static public HashMap<String, String> ESTT22_MAP;
		
	  static String[][] estt22Mapping = { { "TRN", "SEP" },
			{ "SL", "NK" }, { "PASL", "PANK" },{ "SBSL", "SBNK" },{ "AL", "HAV" }, {"POL","SUBMAJ"},
			{ "SBAL", "SBHAV" },{ "PAAL", "PAHAV" },{"DL", "NBSUB"},{"CL","SUB"},{"PACL","PASUB"},{"SBCL","SBSUB"},
			{ "PAPOL","PASUBMAJ" },{"SBPOL","SBSUBMAJ"},{ "TSSL", "TSNK" }, {"DAPON", "HONYCAPT"}, {"RUPON", "HONYLT"},
			{ "CAL", "CHM" }, { "BAL", "BHM" }, {"CALQ", "CQMH"}, { "SLSCALE", "NKSCALE" },
			{ "ALSCALE", "HAVSCALE" },{ "SLGRADE", "NKGRADE" },{ "ALGRADE", "HAVGRADE" },
			{ "DLGRADE", "NSUBGRADE" },{ "CLGRADE", "SUBGRADE" },{ "POLGRADE", "SMAJGRADE" }};
		
	  static public HashMap<String, String> getEstt22Map() {
			if (ESTT22_MAP != null)
				return ESTT22_MAP;
			ESTT22_MAP = new HashMap<String, String>();
			for (int i = 0; i < estt22Mapping.length; i++) {
				String keyVal[] = estt22Mapping[i];
				ESTT22_MAP.put(keyVal[0], keyVal[1]);
			}
			return ESTT22_MAP;
	  }
	  
	  static public HashMap<String, String> ESTT22_REVERSE_MAP;
		
	  static String[][] estt22ReverseMapping = { {"SEP", "TRN"}, { "NK", "SL"}, 
		    { "PANK","PASL"},{ "SBNK","SBSL"},{"HAV", "AL"}, {"SUBMAJ","POL"},
			{ "SBHAV","SBAL"},{ "PAHAV","PAAL"},{"NBSUB","DL"},{"SUB","CL"},{"PASUB","PACL"},{"SBSUB","SBCL"},
			{ "PASUBMAJ", "PAPOL"},{"SBSUBMAJ","SBPOL"},{ "TSNK","TSSL"}, {"HONYCAPT","DAPON"}, {"HONYLT","RUPON"},
			{ "CHM","CAL"}, { "BHM","BAL"}, {"CQMH","CALQ"}, {"NKSCALE", "SLSCALE"},
			{ "HAVSCALE","ALSCALE"},{"NKGRADE","SLGRADE" },{ "HAVGRADE","ALGRADE" },
			{ "NSUBGRADE","DLGRADE"},{"SUBGRADE", "CLGRADE"},{ "SMAJGRADE","POLGRADE"}};
		
	  static public HashMap<String, String> getEstt22ReverseMap() {
			if (ESTT22_REVERSE_MAP != null)
				return ESTT22_REVERSE_MAP;
			ESTT22_REVERSE_MAP = new HashMap<String, String>();
			for (int i = 0; i < estt22ReverseMapping.length; i++) {
				String keyVal[] = estt22ReverseMapping[i];
				ESTT22_REVERSE_MAP.put(keyVal[0], keyVal[1]);
			}
			return ESTT22_REVERSE_MAP;
	  }
	  
//	  static public final List<String> HRMS_IMPLEMENTED_PAO_LIST = Arrays.asList(new String[] { "62", "88", "57"});
//	  static public final List<String> HRMS_IMPLEMENTED_PAO_LIST = Arrays.asList(new String[] { "62", "88", "59", "57", "81", "82", "73"});
//	  static public final List<String> HRMS_IMPLEMENTED_PAO_LIST = Arrays.asList(new String[] { "62", "88", "59", "57", "81", "82", "73", "56", "54", "55", "51","74"});

// added on 03.09.2015 based on CGDA letter No.AT/I/3500/Dolphin/V dated 24.08.2015
//	  static public final List<String> HRMS_IMPLEMENTED_PAO_LIST = Arrays.asList(new String[] { "62", "88", "59", "57", "81", "82", "73", "56", "54", "55", "51","74","87","79","85","97"});

// added on 03.12.2015
//	  static public final List<String> HRMS_IMPLEMENTED_PAO_LIST = Arrays.asList(new String[] { "62", "88", "59", "57", "81", "82", "73", "56",
//		  										"54", "55", "51","74","87","79","85","97","53","58","68","69", "63", "66"});	  

// added on 07.01.2016
		 /* static public final List<String> HRMS_IMPLEMENTED_PAO_LIST = Arrays.asList(new String[] { "62", "88", "59", "57", "81", "82", "73", "56",
				  									"54", "55", "51","74","87","79","85","97","53","58","68","69","64","60","61","72","95","65","66","67","63","78"});	  */
//added on 16-11-2016  -- 40 paos
		  static public final List<String> HRMS_IMPLEMENTED_PAO_LIST = Arrays.asList(new String[] 
				   { "51", "52", "53", "54", "55", "56", "57", "58", "59", "60", "61", "62", "63", "64", "65", "66", "67", "68", "69", "70",
					 "71", "72", "73", "74", "75", "76", "78", "79", "81", "82", "83", "84", "85", "86", "87", "88", "89", "90", "91", "92", 
					 "93", "94", "95", "96", "97", "98", "99", "41"
					});	  
		  
	  static public final List<String> dischTypes = Arrays.asList(new String[]{"BATTLE", "COMMN", "DEATH", "DISCH",
				"DISMIS", "INVALD", "KILLED", "TFROUT","MISING", "POW" });
	  
	  //For NE
	  static public List<String> NE_ADMISSIBLE_DESCRIPTIONS;

		static public List<String> getNeAdmissibleDescriptions() {
			if (NE_ADMISSIBLE_DESCRIPTIONS != null)
				return NE_ADMISSIBLE_DESCRIPTIONS;
			NE_ADMISSIBLE_DESCRIPTIONS = Arrays.asList(new String[] { "NEDA", "NEBON", "NEENC","NELRA" });

			return NE_ADMISSIBLE_DESCRIPTIONS;
		}
		
		static public HashSet<String> SYSTEM_GENERATED_NE_DESCRIPTIONS;
		static public HashSet<String> getSystemGeneratedNeDescription() {
			if (SYSTEM_GENERATED_NE_DESCRIPTIONS != null)
				return SYSTEM_GENERATED_NE_DESCRIPTIONS;
			String[] do2Description = { "NEDA", "NEENC","NEBON","NELRA" };
			SYSTEM_GENERATED_NE_DESCRIPTIONS = new HashSet<String>();
			for (String description : do2Description) {
				SYSTEM_GENERATED_NE_DESCRIPTIONS.add(description);
			}
			return SYSTEM_GENERATED_NE_DESCRIPTIONS;
		}
		
		static public HashSet<String> VALIDATION_REQUIRED_SYSTEM_GENERATED_NE_DESCRIPTIONS;

		static public HashSet<String> getValidationRequiredSystemGeneratedNeDescription() {
			if (VALIDATION_REQUIRED_SYSTEM_GENERATED_NE_DESCRIPTIONS != null)
				return VALIDATION_REQUIRED_SYSTEM_GENERATED_NE_DESCRIPTIONS;
			String[] do2Description = { "NEDA", "NEENC", "NEBON","NELRA" };
			VALIDATION_REQUIRED_SYSTEM_GENERATED_NE_DESCRIPTIONS = new HashSet<String>();
			for (String description : do2Description) {
				VALIDATION_REQUIRED_SYSTEM_GENERATED_NE_DESCRIPTIONS.add(description);
			}
			return VALIDATION_REQUIRED_SYSTEM_GENERATED_NE_DESCRIPTIONS;
		}
		static String[] msthavTrades= { "MTECGUN","MTECELE","MTECAVEH","MTECBVEH","MTECCVEH","MTECAVNAE", 
			                            "MTECAVNAF", "MTECAVNAR", "MTECAVNCS", "MTECAVNEL", "MTECAVNIP",
			                            "MTECNW", "MTECCOMN", "MTECGCE", "MTECO", "MTECRDR", "MTECSA"};
		static public final List<String> MSTHAV_TRADE_LIST = Arrays.asList(msthavTrades);
		
		
		
		// the following methods added due to seven cpc
		

		static java.util.HashMap<Integer,String> SEVEN_CPC_PAY_LEVEL;
		
		static public HashMap<Integer, String> getSevenCpcPayLevel() {
			if (SEVEN_CPC_PAY_LEVEL != null)
				return SEVEN_CPC_PAY_LEVEL;
		
			SEVEN_CPC_PAY_LEVEL = new HashMap<Integer, String>();
			SEVEN_CPC_PAY_LEVEL.put(2000,"3");
			SEVEN_CPC_PAY_LEVEL.put(2400,"4");
			SEVEN_CPC_PAY_LEVEL.put(2800,"5");
			SEVEN_CPC_PAY_LEVEL.put(4200,"6");
			SEVEN_CPC_PAY_LEVEL.put(4600,"7");
			SEVEN_CPC_PAY_LEVEL.put(4800,"8");
			SEVEN_CPC_PAY_LEVEL.put(5400,"10");
			SEVEN_CPC_PAY_LEVEL.put(6100,"10B");
		// Level 9 Is Not Added here for 5400 in PB2 as there is a possibility of picking two levels for same gradepay and needs separate handling.
			return SEVEN_CPC_PAY_LEVEL;
		}
		
		
		static public String getPayLevel(Integer gradePay) {
			SEVEN_CPC_PAY_LEVEL = getSevenCpcPayLevel();
			
			if (SEVEN_CPC_PAY_LEVEL.get(gradePay)!=null) {
				return SEVEN_CPC_PAY_LEVEL.get(gradePay);
			}
			return "";
		}

		
		
		static java.util.HashMap<String,Integer> SEVEN_CPC_GRADE_PAY;
		
		static public HashMap<String,Integer> getSevenCpcGradePay() {
			if (SEVEN_CPC_GRADE_PAY != null)
				return SEVEN_CPC_GRADE_PAY;
		
			SEVEN_CPC_GRADE_PAY = new HashMap<String,Integer>();
			SEVEN_CPC_GRADE_PAY.put("3",2000);
			SEVEN_CPC_GRADE_PAY.put("4",2400);
			SEVEN_CPC_GRADE_PAY.put("5",2800);
			SEVEN_CPC_GRADE_PAY.put("6",4200);
			SEVEN_CPC_GRADE_PAY.put("7",4600);
			SEVEN_CPC_GRADE_PAY.put("8",4800);
			SEVEN_CPC_GRADE_PAY.put("9",5400);			
			SEVEN_CPC_GRADE_PAY.put("10",5400);
			SEVEN_CPC_GRADE_PAY.put("10B",6100);			
			
			return SEVEN_CPC_GRADE_PAY;
		}
		
		
		static public Integer getSevenCpcGradepay(String payLevel) {
			SEVEN_CPC_GRADE_PAY = getSevenCpcGradePay();
			
			if (SEVEN_CPC_GRADE_PAY.get(payLevel)!=null) {
				return SEVEN_CPC_GRADE_PAY.get(payLevel);
			} else {
				return null;
			}
			
		}
		
		//added on 16.05.2025 -- Not used for now
		static java.util.HashMap<String, String> SEVEN_CPC_RANK_BASED_PAY_LEVEL;
		
		static public HashMap<String, String> getSevenCpcPayRankBasedLevel() {
			if (SEVEN_CPC_RANK_BASED_PAY_LEVEL != null)
				return SEVEN_CPC_RANK_BASED_PAY_LEVEL;
		
			SEVEN_CPC_RANK_BASED_PAY_LEVEL = new HashMap<String, String>();
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("SBNK", "4");
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("PANK", "4");
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("NK", "4");
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("LD", "4");
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("SBLD", "4");
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("PALD", "4");
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("ACPLD", "4");
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("NKGRADE", "4");
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("HAV", "5");
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("SBHAV", "5");
 			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("PAHAV", "5");
 			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("SPHAV", "5");
 			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("GRHAV", "5");
 			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("SBDFR", "5");
 			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("PADFR", "5"); 
 			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("DFR", "5");
 			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("ACPDFR", "5");
 			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("HAVGRADE", "5");
 			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("MSTHAV", "5");
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("NBSUB", "6");
 			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("NBRIS", "6");
 			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("NSUBGRADE", "6");
 			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("ACPNBRIS", "6");
 			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("SUB", "7");
 			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("SBSUB", "7");
 			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("PASUB", "7");
 			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("PARIS", "7");
 			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("SBRIS", "7");
 			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("RIS", "7");
 			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("SUBGRADE", "7");
 			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("ACPRIS", "7");
 			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("SBSUBMAJ", "8");
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("PASUBMAJ", "8");
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("SUBMAJ", "8");
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("RISMAJ", "8");			
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("SMAJGRADE", "8");
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("ACPRISMAJ", "8");
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("HONYGRADE", "9");
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("ACPHONYLT", "9");
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("HONYLT", "10");
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("SUBHONYLT", "10");
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("RISHONYLT", "10");
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("SUBMAJHONYLT", "10");
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("RISMAJHONYLT", "10");
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("HONYCAPT", "10B");
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("SUBHONYCAPT", "10B");
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("RISHONYCAPT", "10B");
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("SUBMAJHONYCAPT", "10B");
			SEVEN_CPC_RANK_BASED_PAY_LEVEL.put("RISMAJHONYCAPT", "10B");
	
			return SEVEN_CPC_RANK_BASED_PAY_LEVEL;
		}	  
		
		
static java.util.HashMap<String,String> PAY_LEVEL_PROMOT;
		
		static public HashMap<String,String> getPayLevelPromot() {
			if (PAY_LEVEL_PROMOT != null)
				return PAY_LEVEL_PROMOT;
		
			PAY_LEVEL_PROMOT = new HashMap<String,String>();
			PAY_LEVEL_PROMOT.put("10","3");
			PAY_LEVEL_PROMOT.put("20","3");
			PAY_LEVEL_PROMOT.put("25","3");
			
			PAY_LEVEL_PROMOT.put("29","4");
			PAY_LEVEL_PROMOT.put("30","4");
			
			PAY_LEVEL_PROMOT.put("39","5");
			PAY_LEVEL_PROMOT.put("40","5");
			
			PAY_LEVEL_PROMOT.put("49","6");
			PAY_LEVEL_PROMOT.put("50","6");
			
			PAY_LEVEL_PROMOT.put("59","7");
			PAY_LEVEL_PROMOT.put("60","7");

			PAY_LEVEL_PROMOT.put("69","8");
			PAY_LEVEL_PROMOT.put("70","8");
			
			PAY_LEVEL_PROMOT.put("79","9");
			PAY_LEVEL_PROMOT.put("80","10");
			PAY_LEVEL_PROMOT.put("90","10B");
			
			return PAY_LEVEL_PROMOT;
		}
		
		
		static public String getPayLevelPromot(String rankCode) {
			PAY_LEVEL_PROMOT = getPayLevelPromot();
			
			if (PAY_LEVEL_PROMOT.get(rankCode)!=null) {
				return PAY_LEVEL_PROMOT.get(rankCode);
			} else {
				return null;
			}
			
		}

		
		
static java.util.HashMap<String,String> ENTRY_PAY_LEVEL;
		
		static public HashMap<String,String> getEntryPayLevel() {
			if (ENTRY_PAY_LEVEL != null)
				return ENTRY_PAY_LEVEL;
		
			ENTRY_PAY_LEVEL = new HashMap<String,String>();
			ENTRY_PAY_LEVEL.put("DSC-Y","3");
			ENTRY_PAY_LEVEL.put("DSC-Z","3");
			ENTRY_PAY_LEVEL.put("MATRIC","3");
			ENTRY_PAY_LEVEL.put("MER","3");
			ENTRY_PAY_LEVEL.put("NER","3");
			ENTRY_PAY_LEVEL.put("NORMAL","3");
			ENTRY_PAY_LEVEL.put("SEP-Y","3");
			ENTRY_PAY_LEVEL.put("SEP-Z","3");
			ENTRY_PAY_LEVEL.put("NORMAL","3");
			ENTRY_PAY_LEVEL.put("SEP-Y","3");
			ENTRY_PAY_LEVEL.put("SEP-Z","3");
			ENTRY_PAY_LEVEL.put("SER-B","3");
			ENTRY_PAY_LEVEL.put("SER-C","3");
			ENTRY_PAY_LEVEL.put("SER-D","3");
			ENTRY_PAY_LEVEL.put("SER-E","3");
			ENTRY_PAY_LEVEL.put("SOLTECH","3");
			ENTRY_PAY_LEVEL.put("SOLGD","3");	
			ENTRY_PAY_LEVEL.put("SOLCLK","3");
			ENTRY_PAY_LEVEL.put("SOLTDN","3");
			ENTRY_PAY_LEVEL.put("SKT","3");
			
			ENTRY_PAY_LEVEL.put("SPHV-Y","5");
			ENTRY_PAY_LEVEL.put("SPHV-Z","5");
			ENTRY_PAY_LEVEL.put("HAVEDN","5");
			ENTRY_PAY_LEVEL.put("SPHAV","5");
			ENTRY_PAY_LEVEL.put("HAVSAC","5");
			ENTRY_PAY_LEVEL.put("HAVCLK","5");
			
			ENTRY_PAY_LEVEL.put("DEJCO","6");
			ENTRY_PAY_LEVEL.put("DERTJCO","6");
			ENTRY_PAY_LEVEL.put("NSUB-Y","6");
			ENTRY_PAY_LEVEL.put("NSUB-Z","6");
			
			ENTRY_PAY_LEVEL.put("SUBE-Y","7");
			ENTRY_PAY_LEVEL.put("SUBE-Z","7");
			
			ENTRY_PAY_LEVEL.put("SUB-Y","7");
			ENTRY_PAY_LEVEL.put("SUB-Z","7");
			
			ENTRY_PAY_LEVEL.put("SMAJ-Y","8");
			ENTRY_PAY_LEVEL.put("SMAJ-Z","8");
			
						
			return ENTRY_PAY_LEVEL;
		}
				
		static public String getEntryPayLevel(String entryCategory) {
			ENTRY_PAY_LEVEL = getEntryPayLevel();
			
			if (ENTRY_PAY_LEVEL.get(entryCategory)!=null) {
				return ENTRY_PAY_LEVEL.get(entryCategory);
			} else {
				return null;
			}
			
		}
			
		static public HashSet<String> ABOLISHED_DESCRIPTIONS_SEVENCPC;

		static public HashSet<String> getAbolishedDescriptionsSevencpc() {
			if (ABOLISHED_DESCRIPTIONS_SEVENCPC != null)
				return ABOLISHED_DESCRIPTIONS_SEVENCPC;
			String[] do2Description = { "CILQ", "SNLQ", "FAA", "STRINC", "HCA", "FLCHCA", "ADALLC", "ACTGA",
					"COMNDO", "INSTAL", "OUTFIT", "HSINC", "HCWA", "CONDMT", "BOUNTY", "RLCA1", "RLCA2", "RLCA3", "RLCA4",
					"TYOJ", "TYRJ", "DAILY"};     // these three descriptions viz. TYOJ TYRJ DAILY added on 15.04.2019.                      -- Modified on 11.07.2025
	
	/*		String[] do2Description = { "CILQ", "SNLQ", "FAA", "STRINC", "HCA", "FLCHCA", "ADALLC", "ACTGA",
					"COMNDO", "INSTAL", "OUTFIT", "HSINC", "HCWA", "CONDMT", "BOUNTY", "RLCA1", "RLCA2", "RLCA3", "RLCA4",
					"TYOJ", "TYRJ", "DAILY", "APPT"};     // these three descriptions viz. TYOJ TYRJ DAILY added on 15.04.2019.  */
			// APPT added on 19-03-2020 as the same is merged in EXTWK (Extra Work Allowance)
			ABOLISHED_DESCRIPTIONS_SEVENCPC = new HashSet<String>();
			for (String description : do2Description) {
				ABOLISHED_DESCRIPTIONS_SEVENCPC.add(description);
			}
			return ABOLISHED_DESCRIPTIONS_SEVENCPC;
		}
		static public HashSet<String> OPTFIX_ALLOWED_DESCRIPTIONS;

		static public HashSet<String> getOptfixAllowedDescriptions() {
			if (OPTFIX_ALLOWED_DESCRIPTIONS != null)
				return OPTFIX_ALLOWED_DESCRIPTIONS;
			String[] do2Description = {"PROMOT", "MACPS", "HCOMMN", "MSTHAV", "MSTJCO"};     
			OPTFIX_ALLOWED_DESCRIPTIONS = new HashSet<String>();
			for (String description : do2Description) {
				OPTFIX_ALLOWED_DESCRIPTIONS.add(description);
			}
			return OPTFIX_ALLOWED_DESCRIPTIONS;
		}
		
		static public HashSet<String> OPTFIX_VALID_COLUMNB;

		static public HashSet<String> getOptfixValidColumnB() {
			if (OPTFIX_VALID_COLUMNB != null)
				return OPTFIX_VALID_COLUMNB;
			String[] do2Description = {"TSNK", "TSLD", "PANK", "PALD", "SBNK", "SBLD", "SBSL", "PAHAV", "PADFR", "SBHAV", "SBDFR", 
				"SBAL", "GRHAV", "GRDFR", "GRAL", "SPHAV", "SPDFR", "SPAL", "NBSUB", "NBRIS", "DL", "PASUB", "PARIS", "SBSUB", 
				"SBRIS", "SBCL", "PASUBMAJ", "PARISMAJ", "SBSUBMAJ", "SBRISMAJ", "SBPOL", "NK", "HAV", "SUB", "SUBMAJ", "LD", "DFR",  
				"HONYGRADE", "NSUBGRADE", "SUBGRADE", "NKGRADE", "HAVGRADE", "GP-X", "HONYLT", "HONYCAPT", "SMAJGRADE"};
			
			OPTFIX_VALID_COLUMNB = new HashSet<String>();
			for (String description : do2Description) {
				OPTFIX_VALID_COLUMNB.add(description);
			}
			return OPTFIX_VALID_COLUMNB;
		} 
		 static public List<String> MISC_FS_DESCRIPTIONS;

			static public List<String> getMiscFsDescriptions() {
				if (MISC_FS_DESCRIPTIONS != null)
					return MISC_FS_DESCRIPTIONS;
				MISC_FS_DESCRIPTIONS = Arrays.asList(new String[] {"LDISCH","DEATH","DISMIS","INVALID"});

				return MISC_FS_DESCRIPTIONS;
			}
			
		static public HashSet<String> SBICMPFASTPLUS_PAOCODES;
			
		static public HashSet<String> getSbicmpfastplusPaocodes() {
			if (SBICMPFASTPLUS_PAOCODES != null)
				return SBICMPFASTPLUS_PAOCODES;
							  //  AOC   EME   RVC   BEGR  JRC   KRC   Garhwal Rifles, Lansdowne, Subathu, Delhi Cantt.
			String[] paocodes = { "87", "88", "84", "56", "64", "69", "68", "59", "94", "75", "62", "58", "52", "65", "53", 
					"99", "70", "71", "76", "55", "51", "60", "74", "89", "86", "73", "83", "95", "66", "72", "98", "57", "63", "85", "67", "97",
					"54", "61", "80", "81", "90", "91", "96", "93", "79", "82", "78" };
			// MRC Wellington - paocode 59 added on 15.06.2021
			// DSC Kannur     - paocode 94 added on 15.06.2021
			// 14 GTC Subathu - paocode 75 added on 06.07.2021
			// RRRC Delhi   - paocode 62 added on 06.07.2021
			// PRC Ramgarh -  paocode 58 added on 06.07.2021
			// PBG Delhi   -  paocode 52 added on 07.07.2021
			// SRC Ramgarh -  paocode 65 added on 07.07.2021
			// AAD Gopalpur - paocode 53 added on 09.07.2021
			// JAKLI Srinagar paocode 99 added on 12.07.2021
			// ARC Shillong - paocode 70 added on 13.07.2021
			// 58gtc Shillong paocode 76 added on 13.07.2021
			// BRC Danapur    paocode 71 added on 13.07.2021
			// BEG Kirkee     paocode 55 added on 14.07.2021
			// ACR AhmedNagar paocode 51 added on 14.07.2021
			// Grenadiers JBR paocode 60 added on 14.07.2021
			// MIRC AhmedNgr  paocode 74 added on 15.07.2021
			// INT Pune       paocode 89 added on 15.07.2021
			// APTC Pune      paocode 86 added on 15.07.2021
			// Arty Nasik     paocode 73 added on 15.07.2021
			// Aviation Nasik paocode 83 added on 15.07.2021
			// JAKRifles JBP  paocode 95 added on 16.07.2021
			// SLI Fatehgarh  paocode 66 added on 16.07.2021
			// MRC Saugor     paocode 72 added on 16.07.2021
			// Ladakh S Leh   paocode 98 added on 16.07.2021
			// Signals JBP    paocode 57 added on 16.07.2021
			// RRC Fatehgarh  paocode 63 added on 19.07.2021
			// AMC Lucknow    paocode 85 added on 19.07.2021
			// DRC Faizabad   paocode 67 added on 19.07.2021
			// Guards Kamptee paocode 97 added on 20.07.2021
			// PAOs under PCDA Blr added on 20.07.2021, 54-MEG, 61-MLI, 80-ASC(S), 81-ASC-AT, 90-CMP, 91-PCTC and 96-Para
			// AEC Pachmarri  paocode 93 added on 20.07.2021
			// 11GRRC Lucknow paocode 79 added on 20.07.2021
			// ASC Bengaluru  paocode 82 added on 23.07.2021
			// 39GTC Varanasi paocode 78 added on 23.07.2021
			
			                 
			SBICMPFASTPLUS_PAOCODES = new HashSet<String>();
			for (String pc : paocodes) {
				SBICMPFASTPLUS_PAOCODES.add(pc);
			}
			return SBICMPFASTPLUS_PAOCODES;
		}
		
		
		static java.util.HashMap<String, Double> FUND_INTEREST_TABLE;

		static public HashMap<String, Double> getFundInterestTable() {
			if (FUND_INTEREST_TABLE != null)
				return FUND_INTEREST_TABLE;
		
			FUND_INTEREST_TABLE = new HashMap<String, Double>();
			FUND_INTEREST_TABLE.put("1985040119860331",10.5);
			FUND_INTEREST_TABLE.put("1986040119870331",12.0);
			FUND_INTEREST_TABLE.put("1987040119880331",12.0);
			FUND_INTEREST_TABLE.put("1988040119890331",12.0);
			FUND_INTEREST_TABLE.put("1989040119900331",12.0);
			FUND_INTEREST_TABLE.put("1990040119910331",12.0);
			FUND_INTEREST_TABLE.put("1991040119920331",12.0);
			FUND_INTEREST_TABLE.put("1992040119930331",12.0);
			FUND_INTEREST_TABLE.put("1993040119940331",12.0);
			FUND_INTEREST_TABLE.put("1994040119950331",12.0);
			FUND_INTEREST_TABLE.put("1995040119960331",12.0);
			FUND_INTEREST_TABLE.put("1996040119970331",12.0);
			FUND_INTEREST_TABLE.put("1997040119980331",12.0);
			FUND_INTEREST_TABLE.put("1998040119990331",12.0);
			FUND_INTEREST_TABLE.put("1999040120000331",12.0);
			FUND_INTEREST_TABLE.put("2000040120010331",11.0);
			FUND_INTEREST_TABLE.put("2001040120020331",9.5);
			FUND_INTEREST_TABLE.put("2002040120030331",9.0);
			FUND_INTEREST_TABLE.put("2003040120040331",8.0);
			FUND_INTEREST_TABLE.put("2004040120050331",8.0);
			FUND_INTEREST_TABLE.put("2005040120060331",8.0);
			FUND_INTEREST_TABLE.put("2006040120070331",8.0);
			FUND_INTEREST_TABLE.put("2007040120080331",8.0);
			FUND_INTEREST_TABLE.put("2008040120090331",8.0);
			FUND_INTEREST_TABLE.put("2009040120100331",8.0);
			FUND_INTEREST_TABLE.put("2010040120110331",8.0);
			FUND_INTEREST_TABLE.put("2011040120111130",8.0);
			FUND_INTEREST_TABLE.put("2011120120120331",8.6);
			FUND_INTEREST_TABLE.put("2012040120130331",8.8);
			FUND_INTEREST_TABLE.put("2013040120140331",8.7);
			FUND_INTEREST_TABLE.put("2014040120150331",8.7);
			FUND_INTEREST_TABLE.put("2015040120160331",8.7);
			FUND_INTEREST_TABLE.put("2016040120160930",8.1);
			FUND_INTEREST_TABLE.put("2016100120170331",8.0);
			FUND_INTEREST_TABLE.put("2017040120170630",7.9);
			FUND_INTEREST_TABLE.put("2017070120170930",7.8);
			FUND_INTEREST_TABLE.put("2017100120171231",7.8);
			FUND_INTEREST_TABLE.put("2018010120180331",7.6);
			FUND_INTEREST_TABLE.put("2018040120180630",7.6);
			FUND_INTEREST_TABLE.put("2018070120180930",7.6);
			FUND_INTEREST_TABLE.put("2018100120181231",8.0);
			FUND_INTEREST_TABLE.put("2019010120190331",8.0);
			FUND_INTEREST_TABLE.put("2019040120190630",8.0);
			FUND_INTEREST_TABLE.put("2019070120190930",7.9);
			FUND_INTEREST_TABLE.put("2019100120191231",7.9);
			FUND_INTEREST_TABLE.put("2020010120200331",7.9);
			FUND_INTEREST_TABLE.put("2020040120200630",7.1);
			FUND_INTEREST_TABLE.put("2020070120200930",7.1);
			FUND_INTEREST_TABLE.put("2020100120201231",7.1);
			FUND_INTEREST_TABLE.put("2021010120210331",7.1);
			FUND_INTEREST_TABLE.put("2021040120991231",7.1);
			return FUND_INTEREST_TABLE;
		}
		
		static public List<String> RISK_HAUCA_REPLACED_ALLOWANCES_LIST;

		/**
		 * First Four DOII Descriptions In The List, viz. CHAFAA, SCCIA, CFAA And CMFAA Replaced By RISK
		 * Rest Three DOII Descriptions In The List, viz. HAUCL, HAUCH And HAUCS Replaced By HAUCA
		 */
		static public List<String> getRiskHaucaReplacedAllowancesList() {
			if (RISK_HAUCA_REPLACED_ALLOWANCES_LIST != null)
				return  RISK_HAUCA_REPLACED_ALLOWANCES_LIST;
			RISK_HAUCA_REPLACED_ALLOWANCES_LIST = Arrays.asList(new String[] { "CHAFAA", "SCCIA", "CFAA", "CMFAA", "HAUCL", "HAUCH", "HAUCS" });
			return  RISK_HAUCA_REPLACED_ALLOWANCES_LIST;
		}
		
		static public List<String> VALID_COLUMNB_VALUES_FOR_RISK;

		static public List<String> getValidColumnBValuesForRisk() {
			if (VALID_COLUMNB_VALUES_FOR_RISK != null)
				return VALID_COLUMNB_VALUES_FOR_RISK;
			VALID_COLUMNB_VALUES_FOR_RISK = Arrays.asList(new String[] { "R1H1", "R1H2", "R2H2", "R3H2", "R2H1" });

			return VALID_COLUMNB_VALUES_FOR_RISK;
		}
		
		static public List<String> VALID_COLUMNB_VALUES_FOR_HAUCA;

		static public List<String> getValidColumnBValuesForHauca() {
			if (VALID_COLUMNB_VALUES_FOR_HAUCA != null)
				return VALID_COLUMNB_VALUES_FOR_HAUCA;
			VALID_COLUMNB_VALUES_FOR_HAUCA = Arrays.asList(new String[] { "R1H1", "R3H1", "R3H2" });

			return VALID_COLUMNB_VALUES_FOR_HAUCA;
		}
		/*
		static public boolean isRiskLocationValidationRequired(Do2 do2) {
			//String riskValidationStartMonth="10/2023";
			String riskValidationStartMonth="11/2023";
			
			if(do2.getInputQe()!=null && do2.getInputQe().length()==7) {
				
				int do2Me=Integer.parseInt(do2.getInputQe().substring(3))*100+Integer.parseInt(do2.getInputQe().substring(0,2));
				int liveMe=Integer.parseInt(riskValidationStartMonth.substring(3))*100+Integer.parseInt(riskValidationStartMonth.substring(0,2));
				if(do2Me>=liveMe)
					return true;
			}
			
			if((do2.getId()!=null && do2.getId()==0) || do2.getId()==null) {
				return true;
			}
			return false;
		}
		*/
		static public boolean isRiskLocationValidationRequired(Do2 do2) {
			 
			java.util.Date riskValidationStartDate=null;
			
			try {
				riskValidationStartDate = DateManager.getDate("2023-11-01");
			} catch (Exception e) {
				Utils.handleException(e);
			}
			
			if (do2.getDo2Date()!=null && riskValidationStartDate!=null && !do2.getDo2Date().before(riskValidationStartDate)) {			 
					return true;
			}
			if ((do2.getId()!=null && do2.getId()==0) || do2.getId()==null) {
				return true;
			}
			
			return false;
		}
		static public List<String> DEFAULT_NEW_TAX_REGIME_ASSESSMENT_YEARS = Arrays.asList(new String[] {"2024-2025", "2025-2026", "2026-2027", "2027-2028"});
		
}
