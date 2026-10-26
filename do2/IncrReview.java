package itsdc.dolphin.do2; 

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Temporal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.RestController;
import itsdc.dolphin.controllers.Do2Controller;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.repositories.AuditCageRepository;
import itsdc.dolphin.repositories.Do2Repository;
import itsdc.dolphin.repositories.EmployeeRepository;
import itsdc.dolphin.repositories.MiscRepository;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

@RestController
public class IncrReview implements java.io.Serializable {
	
	JdbcTemplate jdbc;
	NamedParameterJdbcTemplate namedJdbc;
		
	@Autowired
	Do2Controller d2c;
	
	public Do2Controller getDo2Controller() {
		return d2c;
	}
	
	public void setDo2Controller(Do2Controller d2c) {
		this.d2c=d2c;
	}
	
	EmployeeRepository empRepo;	
	Do2Repository do2Repo;
	AuditCageRepository acRepo;
	MiscRepository miscRepo;
	
	List<Do2> generatedDo2=new ArrayList<Do2>();
		
	public AuditCageRepository getAcRepo() {
		return acRepo;
	}

	public void setAcRepo(AuditCageRepository acRepo) {
		this.acRepo = acRepo;
	}

	public Do2Repository getDo2Repo() {
		return do2Repo;
	}

	public void setDo2Repo(Do2Repository do2Repo) {
		this.do2Repo = do2Repo;
	}
	
	public EmployeeRepository getEmpRepo() {
		return empRepo;
	}

	public void setEmpRepo(EmployeeRepository empRepo) {
		this.empRepo = empRepo;
	}
	
	public MiscRepository getMiscRepo() {
		return miscRepo;
	}

	public void setMiscRepo(MiscRepository miscRepo) {
		this.miscRepo = miscRepo;
	}

	public JdbcTemplate getJdbc() {
		return jdbc;
	}

	public void setJdbc(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	static String url; 
	static String driver;
	static String user;
	static String password; 
	static String bladeserver = "";   
	static String armyNumber;	
	static Date cutOffDate;
	Employee employee;
	String monthEnding = null;
	Date sevencpcDate = DateManager.getSeventhCpcDate();
	boolean nqs180plus = false, nqsDuringCrucial6Months = false;
	boolean optfixExercised = false;
	String revtype;
	String inFile;
	String nqsFileDir;
	int fileSize;
	List<String> anoList = new ArrayList<String>();
	List<String> musterDateNullList = new ArrayList<String>();
	javax.transaction.UserTransaction userTransaction = null;
	List<Date> correctIncrementDateIn7cpcList = new ArrayList<Date>();	
	int batchNo=3600;
	int insertedDo2Count=0;
	
	public String getNqsFileDir() {
		return nqsFileDir;
	}

	public void setNqsFileDir(String nqsFileDir) {
		this.nqsFileDir = nqsFileDir;
	}
  
	public static Date getCutOffDate() {
		return cutOffDate;
	}

	public static void setCutOffDate() {
		cutOffDate = Utils.getLastDayOfCurrentQuarter();
		Calendar cal = new GregorianCalendar();
		cal.setTime(cutOffDate);
		if (cal.get(Calendar.MONTH)==0 || cal.get(Calendar.MONTH)==6) {  // January or July
			cutOffDate = Utils.getLastDayOfPreviousQuarter();
		}
	}

	public String getArmyNumber() {
		return armyNumber;
	}

	public void setArmyNumber(String armyNumber) {
		this.armyNumber = armyNumber;
	}
		
	public String getRevtype() {
		return revtype;
	}

	public void setRevtype(String revtype) {
		this.revtype = revtype;
	}
	
	public String startReview(String armyNumber) {    	 		
		monthEnding = Utils.getCurrentQuarter(); 
		nqs180plus = false;
		employee=null;	
	 
		if (checkArmyNumberLength(armyNumber)==false) {
			revtype=null;
			return "Not Done";
		}
		
		employee = Utils.getCurrentEmployee(armyNumber);
		
		if (employee==null) {
        	System.out.println("Entered Army Number - "+ armyNumber+" Not Found In The Database !!! ");
        	return  "Not Done"; 
		}
	 
		System.out.println("................  incr review for emp " +employee.getArmyNo());
		verifyIncrementDo2AndDate(employee); // Single
        
		if (correctIncrementDateIn7cpcList!=null && correctIncrementDateIn7cpcList.size()>0) {
			return "Valid";
		/*	List<Do2> do2List =  do2Repo.getLatestPendingAtcorrIncrDo2List(employee.getId(), new Date());
	 		if (do2List!=null && do2List.size()>0) {	
	 			new Do2Controller().persist(do2List.get(0));
    			d2c.persist(do2List.get(0));	   
    		}
	 		employee = null;
	 		System.out.println();
			System.out.println("Job Done !!!!!!!!!!!!!!!!!!");*/
	 	}
		nqs180plus=false;
       	System.out.println("Job Done !!!!!!!!!!!!!!!!!!");
	 	return  "Done";
    }
   
    public void verifyIncrementDo2AndDate(Employee employee) {
      
     	get7cpcOptionDate(employee);   	    	
     	
    	correctIncrementDateIn7cpcList = new ArrayList<Date>();
	  	Date presentIncrDate = DateManager.getSeventhCpcDate(), correctDNI = null,changedIncrDateDueToNqs = null, changedIncrDateDueToFix = null;
	  	try {
  			correctDNI = DateManager.getDate("2016-07-01");
  			if (Utils.getPao().getPaoCode().equals(Do2Manager.APS_NAGPUR)) {
  				presentIncrDate=correctDNI;
  				correctDNI=DateManager.getDate("2017-07-01");
  			}
  		} catch (Exception e1) {
  			Utils.handleException(e1);
  		}
	
	  	if (employee!=null) {
	  		if (employee.getRegularTaFlag().equals("T")) {
	  			System.out.println("Army Number ---> " + employee.getArmyNo() + ".  Individual Is NOT Eligible For Regular Increment, Being TA Personnel !!! \n");
	  			return;	
	  		}
	  		String paoCode = Utils.getPao().getPaoCode();
  			if (!paoCode.equals(Do2Manager.PAO_DSC) && employee.getInitialMusteringDate()==null && employee.getAttestationDate()==null) {
   				System.out.println("Army Number ---> " + employee.getArmyNo() + ".  Mustering And Attestation Dates Are Null !!! \n");
   				return;
  			}
  			if (employee.getStatusCode()==null || (employee.getStatusCode()!=null && employee.getStatusCode().getId()>4 
  					&& employee.getEnrolmentDate()!=null && !employee.getEnrolmentDate().before(DateManager.getSeventhCpcDate()))) { // added on 10.12.2024
	 			System.out.println("Army Number ---> " + employee.getArmyNo() + ". Employee Status Is Not Effective !!! \n");
   				return;
  			}
  			if (employee.getEnrolmentDate()==null) { // added on 10.12.2024
	 			System.out.println("Army Number ---> " + employee.getArmyNo() + ". Employee Enrollment Date Is NULL !!! \n");
   				return;
  			}
  			System.out.println(".................... enrol dt " +employee.getEnrolmentDate()+"    7cpcdt " +sevencpcDate);
	  	 	if (!paoCode.equals(Do2Manager.PAO_DSC) && employee.getEnrolmentDate()!=null && !employee.getEnrolmentDate().before(sevencpcDate)) {	  
	  			if (employee.getInitialMusteringDate()!=null || employee.getAttestationDate()!=null) {
	  			//	presentIncrDate = Utils.getNextIncrementDate(employee.getEnrolmentDate());
	  				presentIncrDate = Utils.getSevenPcNextIncrementDate(employee.getEnrolmentDate());  // changed on 22.09.2025
		  			correctDNI = DateManager.getNextYear(presentIncrDate);
		 			correctIncrementDateIn7cpcList.add(presentIncrDate); // Since increment date arrived here itself, it is added immediately.
		 			System.out.println("............. presnetincrdate  " +presentIncrDate+"  correctdni " +correctDNI +"  ano "+employee.getArmyNo());
				} else {
					if (employee.getGroupCategory()!=null && employee.getGroupCategory().getSoldierCategory().equals("RT")) {
						presentIncrDate = Utils.getNextIncrementDate(employee.getEnrolmentDate());
			  			correctDNI = DateManager.getNextYear(presentIncrDate);
			 			correctIncrementDateIn7cpcList.add(presentIncrDate); // Since increment date arrived here itself, it is added immediately.			
					} else {
						if (revtype!=null && revtype.equals("SINGLE")) {
	 		  				System.out.println("Army Number ---> " + employee.getArmyNo() + ".  Individual Is NOT Eligible For Increment !!! \n");
			  				return;
						}	
					}									
	  			}  		
	  		} else if (!paoCode.equals(Do2Manager.PAO_DSC) && employee.getInitialMusteringDate()==null
	  					&& employee.getEnrolmentDate()!=null && employee.getEnrolmentDate().after(getPaoCutOffDate(paoCode))) {
	  			if (employee.getGroupCategory()!=null && employee.getGroupCategory().getSoldierCategory().equals("RT")) {
					presentIncrDate = Utils.getNextIncrementDate(employee.getEnrolmentDate());
		  			correctDNI = DateManager.getNextYear(presentIncrDate);
		 			correctIncrementDateIn7cpcList.add(presentIncrDate); // Since increment date arrived here itself, it is added immediately.			
				} else {
		  			if (revtype!=null && revtype.equals("SINGLE")) {
	 	  				System.out.println("Army Number ---> " + employee.getArmyNo() + ".  Individual Is NOT Eligible For Increment !!! \n");
		  				musterDateNullList.add(employee.getArmyNo());
		  				return;	  	
		  			}
				}
	  		} else if (employee.getInitialMusteringDate()!=null && !employee.getInitialMusteringDate().before(sevencpcDate)) {
	  				if (employee.getAttestationDate()==null 
	  						|| (employee.getAttestationDate()!=null && !employee.getInitialMusteringDate().after(employee.getAttestationDate()))) {
	  					Calendar cal = new GregorianCalendar();
	  					cal.setTime(employee.getInitialMusteringDate());
	  					if (cal.get(Calendar.MONTH)==Calendar.JANUARY && cal.get(Calendar.DATE)==1) {
	  						try {
								presentIncrDate = DateManager.getDate(cal.get(Calendar.YEAR), 7, 1);
							} catch (Exception e) {
								Utils.handleException(e);
							}
			 			} else {
	  						try {
								presentIncrDate = DateManager.getDate(cal.get(Calendar.YEAR)+1, 7, 1);
							} catch (Exception e) {
								Utils.handleException(e);
							}
						}
		  				correctDNI = DateManager.getNextYear(presentIncrDate);
		  				correctIncrementDateIn7cpcList.add(presentIncrDate); // Since increment date arrived here itself, it is added immediately.
		  			} else {
		  				Calendar cal = new GregorianCalendar();
	  					cal.setTime(employee.getInitialMusteringDate());
	  					if (cal.get(Calendar.MONTH)==Calendar.JANUARY && cal.get(Calendar.DATE)==1) {
	  						try {
								presentIncrDate = DateManager.getDate(cal.get(Calendar.YEAR), 7, 1);
							} catch (Exception e) {
								Utils.handleException(e);
							}
	  						correctDNI = DateManager.getNextYear(presentIncrDate);  
			 			} else if (cal.get(Calendar.MONTH)<Calendar.JULY || (cal.get(Calendar.MONTH)==Calendar.JULY && cal.get(Calendar.DATE)==1)) {
			 				Date likelyDate =null;
			 				try {
								likelyDate = DateManager.getDate(cal.get(Calendar.YEAR), Calendar.JULY, 1);
							} catch (Exception e1) {
								Utils.handleException(e1);
							}
			 				if (DateManager.getNoOfDays(employee.getAttestationDate(),likelyDate) >= 365) {
			 					try {
									presentIncrDate = DateManager.getDate(cal.get(Calendar.YEAR), 7, 1);
								} catch (Exception e) {
									Utils.handleException(e);
								}
		  						correctDNI = DateManager.getNextYear(presentIncrDate); 			 					
			 				} else {
			 					try {
									presentIncrDate = DateManager.getDate(cal.get(Calendar.YEAR)+1, 7, 1);
								} catch (Exception e) {
									Utils.handleException(e);
								}
		  						correctDNI = DateManager.getNextYear(presentIncrDate); 
			 				}  					 
						} else {
							try {
								presentIncrDate = DateManager.getDate(cal.get(Calendar.YEAR)+1, 7, 1);
							} catch (Exception e) {
								Utils.handleException(e);
							}
	  						correctDNI = DateManager.getNextYear(presentIncrDate);  					
						}
	  					correctIncrementDateIn7cpcList.add(presentIncrDate); // Since increment date arrived here itself, it is added immediately.
		  			}	  					
	  		} else {  	
	  	  		get7cpcOptionDate(employee);
	  	 
	  	  		 System.out.println("xxxxxxxxxxxxxxxxxxxxxxx sevencpcdate "+ sevencpcDate+"  presentincrdt " + presentIncrDate);
			  	if (!sevencpcDate.equals(presentIncrDate)) {
			  		if (!Utils.getPao().getPaoCode().equals(Do2Manager.APS_NAGPUR)) {
			  		 	presentIncrDate = Utils.getNextIncrementDate(sevencpcDate);
			  		 	correctIncrementDateIn7cpcList.add(presentIncrDate); // Since increment date arrived here itself, it is added immediately.					  
			  		 	correctDNI = DateManager.getNextYear(presentIncrDate);
			  		}
			  	} else if (employee.getEnrolmentDate()!=null && !employee.getEnrolmentDate().before(DateManager.getSeventhCpcDate())) {
			  		correctDNI = Utils.getSevenPcNextIncrementDate(employee.getEnrolmentDate());
			  	} else {
			  	 	try {
						correctDNI = DateManager.getDate("2016-07-01");
						if (Utils.getPao().getPaoCode().equals(Do2Manager.APS_NAGPUR)) {
							correctDNI = DateManager.getDate("2017-07-01");
						}
					} catch (Exception e1) {
						Utils.handleException(e1);
					}
			  	}
	   		}
	  	}
  
	  	nqs180plus = false; nqsDuringCrucial6Months = false;
	  	setCutOffDate();
 		while (presentIncrDate.before(Utils.getLastDayOfPreviousQuarter())) {
			changedIncrDateDueToNqs = checkNQS(employee, presentIncrDate, correctDNI);      
		  System.out.println("jjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjj nqs180plus "+nqs180plus +" optfixexercised "+optfixExercised+" nqsdurin6m  "+nqsDuringCrucial6Months);
	   		if (nqs180plus==false) {
	  			changedIncrDateDueToFix = checkPromotionUpgradation(employee, changedIncrDateDueToNqs, correctDNI);
	  	 	 System.out.println("kkkkkkkkkkkkkkkkkkkkkkkk nqs180plus "+nqs180plus +" optfixexercised "+optfixExercised+" nqsdurin6m  "+nqsDuringCrucial6Months +"  changedincrdtduefix "+changedIncrDateDueToFix);
	  			 
	  			if (optfixExercised==false) {
	  		//		 System.out.println("changedincrduetonqs................. "+changedIncrDateDueToNqs);
	  		//		 System.out.println("changedincrdttofix.................. "+changedIncrDateDueToFix);
	  		//		 System.out.println("presentincrdt "+presentIncrDate+"  correctdni "+correctDNI);
	  				if (changedIncrDateDueToNqs!=null && changedIncrDateDueToNqs.after(presentIncrDate)) {	
	  		//			 System.out.println("aaaaaaaaaaaaaaaaaaaaaaaa presentincrdt " +presentIncrDate+"  correct dni "+correctDNI);
		  				if (changedIncrDateDueToFix!=null) {
		  //					 System.out.println("bbbbbbbbbbbbbbbbbbbb presentincrdt " +presentIncrDate+"  correct dni "+correctDNI);
		  					if (changedIncrDateDueToFix.after(changedIncrDateDueToNqs) && !changedIncrDateDueToFix.after(correctDNI)) {
		  	//					 System.out.println("cccccccccccccccccccccccc presentincrdt " +presentIncrDate+"  correct dni "+correctDNI);
		  						correctIncrementDateIn7cpcList.add(changedIncrDateDueToFix);			  			
					  			correctDNI = DateManager.getNextYear(changedIncrDateDueToFix);	
					  			presentIncrDate = changedIncrDateDueToFix;	  	
	 	  					} else {
	 	  						//System.out.println("dddddddddddddddddddddddddd presentincrdt " +presentIncrDate+"  correct dni "+correctDNI);
		  						correctIncrementDateIn7cpcList.add(changedIncrDateDueToNqs);			  			
				//	  			correctDNI = DateManager.getNextYear(DateManager.getNextIncrementInOptixFixation(presentIncrDate));  //modified on 22.03.2021
		  			  			correctDNI = DateManager.getNextYear(correctDNI);  //modified on 22.03.2022
					  			presentIncrDate = changedIncrDateDueToNqs;	  	
	 	  					} 					
		  				} else {
		  					//System.out.println("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee presentincrdt " +presentIncrDate+"  correct dni "+correctDNI);
		  					correctIncrementDateIn7cpcList.add(changedIncrDateDueToNqs);			  			
				  			correctDNI = DateManager.getNextYear(presentIncrDate);	
				  			presentIncrDate = changedIncrDateDueToNqs;	
	 	  				}
			     	} else if (changedIncrDateDueToFix!=null && changedIncrDateDueToFix.after(presentIncrDate)) {
			     		//System.out.println("ffffffffffffffffffffffffffff presentincrdt " +presentIncrDate+"  correct dni "+correctDNI);
				  			correctIncrementDateIn7cpcList.add(changedIncrDateDueToFix);	
				  			presentIncrDate = changedIncrDateDueToFix;
				  			correctDNI = DateManager.getNextYear(changedIncrDateDueToFix);	
	 			    } else {
	 			    	//System.out.println("kkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk presentincrdt " +presentIncrDate+"  correct dni "+correctDNI);
				     		presentIncrDate = correctDNI;	
				 			correctDNI = DateManager.getNextYear(presentIncrDate);
				      		if (!correctIncrementDateIn7cpcList.contains(presentIncrDate))
				     			correctIncrementDateIn7cpcList.add(presentIncrDate);
				   	}
	  			} else {
	  				optfixExercised=false;
	  				if (changedIncrDateDueToNqs.after(correctDNI)) {
	  					correctDNI=changedIncrDateDueToNqs;
	  				}
	  				changedIncrDateDueToNqs = checkNQS(employee, presentIncrDate, correctDNI); 
	  			System.out.println("$$$$$$$$$$$$$$$$$$$$$ presentincrdt "+presentIncrDate+"   correctdni "+correctDNI+"");
	   			System.out.println("changed incr date due to nqs ............"+changedIncrDateDueToNqs+" duetofix  " +changedIncrDateDueToFix+" nqsduring6m "+nqsDuringCrucial6Months);
	  				if (nqsDuringCrucial6Months==true) {
	  					nqsDuringCrucial6Months=false;
	  					changedIncrDateDueToNqs = checkNQS(employee, presentIncrDate, DateManager.getNextYear(presentIncrDate)); 
	  				}
	  				
	  				if (changedIncrDateDueToNqs!=null && changedIncrDateDueToNqs.after(presentIncrDate)) {	
		  				if (changedIncrDateDueToFix!=null) {
		  					if (changedIncrDateDueToFix.after(changedIncrDateDueToNqs) && !changedIncrDateDueToFix.after(correctDNI)) {
		  						correctIncrementDateIn7cpcList.add(changedIncrDateDueToFix);			  			
					  			correctDNI = DateManager.getNextIncrementInOptixFixation(changedIncrDateDueToFix);	
					  			presentIncrDate = changedIncrDateDueToFix;	  	
	 	  					} else {
		  						correctIncrementDateIn7cpcList.add(changedIncrDateDueToNqs);			  			
					  			correctDNI = DateManager.getNextIncrementInOptixFixation(presentIncrDate);	
					  			presentIncrDate = changedIncrDateDueToNqs;	  	
	 	  					} 					
		  				} else {
		  					correctIncrementDateIn7cpcList.add(changedIncrDateDueToNqs);			  			
				  			correctDNI = DateManager.getNextIncrementInOptixFixation(presentIncrDate);	
				  			presentIncrDate = changedIncrDateDueToNqs;	
	 	  				}
			     	} else if (changedIncrDateDueToFix!=null && changedIncrDateDueToFix.after(presentIncrDate)) {	
		      		//System.out.println("ppppppppppppppppppppppp presentincrdt "+presentIncrDate+" changeincrfix "+changedIncrDateDueToFix);
				  			correctIncrementDateIn7cpcList.add(changedIncrDateDueToFix);	
				  			presentIncrDate = changedIncrDateDueToFix;
				  			correctDNI = DateManager.getNextIncrementInOptixFixation(changedIncrDateDueToFix);	
	 			    } else {
	 	 		    	//System.out.println("qqqqqqqqqqqqqqqqqqqqqq correcdni "+correctDNI);
				     		presentIncrDate = correctDNI;	
				 			correctDNI = DateManager.getNextYear(presentIncrDate);
				      		if (!correctIncrementDateIn7cpcList.contains(presentIncrDate))
				     			correctIncrementDateIn7cpcList.add(presentIncrDate);
				   	} 
	   			//System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% presentincrdt "+presentIncrDate+"   correctdni "+correctDNI);	  				
	  			}	  			
	  		} else {
	  		//	System.out.println("............... correct incr dt list size " +correctIncrementDateIn7cpcList.size());
	  			if (correctIncrementDateIn7cpcList==null || (correctIncrementDateIn7cpcList!=null && correctIncrementDateIn7cpcList.size()==0)) {
	  				return;
	  			} else {
	  				break;
	  			}
	     	}
	  		changedIncrDateDueToFix=null; changedIncrDateDueToNqs=null;
	  	}
	 		
 	//	System.out.println("......................after loop ... correct incr dt list size " +correctIncrementDateIn7cpcList.size());
 	
 	//	if (nqs180plus==false && correctIncrementDateIn7cpcList!=null && correctIncrementDateIn7cpcList.size()>0) {
 		if (correctIncrementDateIn7cpcList!=null && correctIncrementDateIn7cpcList.size()>0) {
 			System.out.println("................. correctincrdt7cpclist size  " +correctIncrementDateIn7cpcList.size()+"  me " +monthEnding );
 			do2Repo.updateCancelledStatusToValidIncorrectIncrDo2(employee, monthEnding, correctIncrementDateIn7cpcList);
				
			for (Date d : correctIncrementDateIn7cpcList) {
				String colC="INCRREV";
				if (d.before(Utils.getLastDayOfPreviousQuarter())) {
				 	List<Do2> incrDo2List = do2Repo.getValidPendingIncrDo2List(employee.getId(), d);
					System.out.println("army no. "+armyNumber+"  correct incr date " +d+" incrdo2list size "+incrDo2List.size()+"  inserted do2 count " + insertedDo2Count);
		 			if (incrDo2List==null || (incrDo2List!=null && incrDo2List.size()==0)) {
		 				if ((insertedDo2Count/500)>=1) {
		 					batchNo++;
		 					insertedDo2Count=0;
		 				}
		 				Calendar cal = new GregorianCalendar();
		 				cal.setTime(d);
		 				if (cal.get(Calendar.DATE)>1) {
		 			 		colC="POSTPONED";
		 				}	
		 				do2Repo.insertIncrementDo2DuringReview(batchNo, employee.getUnit().getUnitCode(), employee.getUnit().getId(), 
		 						employee.getArmyNo(), employee.getCheckDigit(), employee.getId(), d, colC, monthEnding);			 		 
						insertedDo2Count++;	
					}
				} else {
					System.out.println("............army number "+armyNumber+"  "+d);
					employee.setIncrementDate(d);	
					empRepo.save(employee);
				} 
			} 
				 		
			Date incrCancelMinDate=do2Repo.getMinimumFromDateFromIncrCancelForGeneratingAtcorr(employee.getId(), monthEnding);
			Date insertedIncrMinDate=do2Repo.getMinimumFromDateFromInsertedIncrForGeneratingAtcorr(employee.getId(), monthEnding, incrCancelMinDate);
	 					 				 
		 	if (incrCancelMinDate!=null && insertedIncrMinDate!=null && insertedIncrMinDate.after(incrCancelMinDate)) { 
		 		List<Do2> pendingAtcorrDo2List = do2Repo.getPendingAtcorrDo2List(employee.getId(), incrCancelMinDate, monthEnding);
			 	if (pendingAtcorrDo2List==null || (pendingAtcorrDo2List!=null && pendingAtcorrDo2List.size()==0)) {
			 		do2Repo.insertAtcorrDo2DuringReview(batchNo, employee.getUnit().getUnitCode(), employee.getUnit().getId(), employee.getArmyNo(), 
			 				employee.getCheckDigit(), employee.getId(), incrCancelMinDate, monthEnding, "ATCORR Do2 Inserted After Increment Review In "+monthEnding);	 
			 		insertedDo2Count++;
			 	}
	 		} 		
 		}
 		setCutOffDate();	 
    }
     
    public void get7cpcOptionDate(Employee employee) { 
    	sevencpcDate = do2Repo.get7cpcOptionDate(employee.getId());
    	if (sevencpcDate==null) {
     		sevencpcDate=DateManager.getSeventhCpcDate();     		 
     	}
    }
   
   public Date checkPromotionUpgradation(Employee employee, Date presentIncrDate, Date correctDNI) {
 	   	Date tempDate = null;
	   	tempDate = presentIncrDate;
	   	Date currentMacpPromotDate = null, empIncrementDate = null, acpNaikDate = null, havDate = null, acpHavDate = null, nbsubDate = null,
					acpNbSubDate = null, acpSubDate = null, acpSubMajDate = null, enrollmentDate = null, attestationDate = null, musterDate = null,
					naikDate = null, subDate = null, submajDate = null, honyLtDate = null, fixationOptionDate = null;
	 	java.util.Date firstJanuary = null, firstJuly = null; 
	  	String armyNo = null, checkDigit = null, description = null, columnA = null, unitCode = null;
		BigInteger empId = BigInteger.ZERO;
		Integer unitId = 0;	
  						
	 		List<String> validSbDo2ColumnA = new ArrayList<String>();
	 	 	validSbDo2ColumnA.add("SBNK");
		 	validSbDo2ColumnA.add("SBHAV");
		 	validSbDo2ColumnA.add("SBSUB");
		 	validSbDo2ColumnA.add("SBSUBMAJ");
		 	validSbDo2ColumnA.add("SBLD");
		 	validSbDo2ColumnA.add("SBDFR");
		 	validSbDo2ColumnA.add("SBRIS");
		 	validSbDo2ColumnA.add("SBRISMAJ");
				
		 	List<Object[]> macpsPromotDataList = do2Repo.getMacpsPromotDos2BetweenTwoIncrDates(employee.getId(), presentIncrDate, correctDNI);
		 	if (macpsPromotDataList==null || (macpsPromotDataList!=null && macpsPromotDataList.size()==0)) {
		 		macpsPromotDataList = do2Repo.getMacpsPromotDos2BetweenTwoIncrDatesForAps(employee.getId(), presentIncrDate, correctDNI);
		 	}
		 	System.out.println("............presentincrdt " +presentIncrDate+"  tempdate "+tempDate+"  correctdni "+correctDNI+"  macpprom list "+macpsPromotDataList.size());
		 	if (macpsPromotDataList!=null && macpsPromotDataList.size()>0) {
	 			for (Object[] obj : macpsPromotDataList) {
	 				unitCode = (String) obj[0];
					unitId = (Integer) obj[1];				
					empId = (BigInteger) obj[2];  
					armyNo = (String) obj[3]; 
					checkDigit = (String) obj[4].toString();
					description = (String) obj[5];
					columnA = (String) obj[6];
					currentMacpPromotDate = (Date) obj[7];
					empIncrementDate = (Date) obj[8];
					acpNaikDate = (Date) obj[9];
					acpHavDate = (Date) obj[10];
					acpNbSubDate = (Date) obj[11];
					acpSubDate = (Date) obj[12];
					acpSubMajDate =(Date) obj[13];
					havDate = (Date) obj[14];
					nbsubDate = (Date) obj[15];
					enrollmentDate = (Date) obj[16];
					attestationDate = (Date) obj[17];
					musterDate = (Date) obj[18];
					naikDate = (Date) obj[19];
					subDate = (Date) obj[20];
					submajDate = (Date) obj[21];
					honyLtDate = (Date) obj[22];
					fixationOptionDate = (Date) obj[23];	 				
	 			 
	 			tempDate=null;
	  			
				optfixExercised=false;
				
				if (description.equals("STEPUP")) {
 					if (!presentIncrDate.equals(currentMacpPromotDate)) {
 						Calendar stepupdate = new GregorianCalendar();
 						stepupdate.setTime(currentMacpPromotDate);
 						if (stepupdate.get(Calendar.DATE)==1 
 								&& ((stepupdate.get(Calendar.MONTH)==Calendar.JANUARY) || (stepupdate.get(Calendar.MONTH)==Calendar.JULY))) {
 							tempDate=DateManager.getNextYear(currentMacpPromotDate);								
 						} else {
 							tempDate=Utils.getSevenPcNextIncrementDate(currentMacpPromotDate);
 						}
 						System.out.println ("  stepup condition   " +tempDate);
 						continue;
 					}
 				}
	 		
				if (!presentIncrDate.equals(currentMacpPromotDate)) {
					List<Object[]> validOptfixDo2ListValues = do2Repo.getValidOptfixDo2List(employee.getId(), description, currentMacpPromotDate);
					if (validOptfixDo2ListValues!=null && validOptfixDo2ListValues.size()>0) {
						optfixExercised=true;
						tempDate = correctDNI;
			 			return tempDate;
					} 
					if (Utils.getPao().getPaoCode().equals(Do2Manager.APS_NAGPUR)) {
						validOptfixDo2ListValues=null;
						validOptfixDo2ListValues = do2Repo.getValidOptfixDo2ListForAps(employee.getId(), description, currentMacpPromotDate);
						if (validOptfixDo2ListValues!=null && validOptfixDo2ListValues.size()>0) {
							optfixExercised=true;
							tempDate = correctDNI;
				 			return tempDate;
						} 
					}					
				}
				if (correctDNI.equals(currentMacpPromotDate)) {
		 			correctIncrementDateIn7cpcList.add(correctDNI);
				}
				
 	 		Calendar cal = new GregorianCalendar();	
				cal.setTime(currentMacpPromotDate);
		 		int year = cal.get(Calendar.YEAR);			
								
				try {
					firstJanuary = DateManager.getDate(year, 1, 1);
					firstJuly = DateManager.getDate(year, 7, 1);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
	System.out.println("}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}} promotmacpscheck "+firstJanuary+"  "+firstJuly+"  currentmacpprodt "+currentMacpPromotDate
					 +" optfixex "+optfixExercised +"  presentincrdt "+presentIncrDate+"  corrdni "+correctDNI);
				
				if (fixationOptionDate==null) {				
					if (description!=null && description.equals("MACPS")) {					
						tempDate = Utils.getSevenPcNextIncrementDate(currentMacpPromotDate);
					} else if (description!=null && description.trim().equals("PROMOT")) {
						if (columnA!=null && validSbDo2ColumnA.contains(columnA.trim())) {
							columnA="PA"+columnA.substring(2);					 
							List<Object[]> validPreviousPromotDo2List = do2Repo.getPreviousPromotDo2List(employee.getId(), columnA, currentMacpPromotDate);
							if (validPreviousPromotDo2List!=null & validPreviousPromotDo2List.size()>0) {
								;
							} else {											
								if (columnA!=null) {
									if (columnA.trim().contains("NK") || columnA.trim().contains("LD")) {
										if (acpNaikDate!=null && currentMacpPromotDate!=null && acpNaikDate.before(currentMacpPromotDate)) {
											;
										} else {
											tempDate = Utils.getSevenPcNextIncrementDate(currentMacpPromotDate);
										}
									} else if (columnA.trim().contains("HAV") || columnA.trim().contains("DFR")) {
										if (acpHavDate!=null && currentMacpPromotDate!=null && acpHavDate.before(currentMacpPromotDate)) {
		 									;							
										} else {
											tempDate = Utils.getSevenPcNextIncrementDate(currentMacpPromotDate);
										}
									} else if (columnA.trim().endsWith("SUB") || columnA.trim().endsWith("RIS")) {
										if (acpSubDate!=null && currentMacpPromotDate!=null && acpSubDate.before(currentMacpPromotDate)) {
											;
										} else {
											tempDate = Utils.getSevenPcNextIncrementDate(currentMacpPromotDate);
										}
									} else if (columnA.trim().contains("SUBMAJ") || columnA.trim().contains("RISMAJ")) {
										if (acpSubMajDate!=null && currentMacpPromotDate!=null && acpSubMajDate.before(currentMacpPromotDate)) {
											;
										} else {
											tempDate = Utils.getSevenPcNextIncrementDate(currentMacpPromotDate);
										}
									} 
								} else {
			//						//System.out.println("Column 7A in Promot DOII Is Null For Army No."+employee.getArmyNo());
								}
							}
						} else if (columnA.trim().equals("NBSUB") || columnA.trim().equals("NBRIS")) {
							if (acpNbSubDate!=null && currentMacpPromotDate!=null && acpNbSubDate.before(currentMacpPromotDate)) {
								;
							} else {
								tempDate = Utils.getSevenPcNextIncrementDate(currentMacpPromotDate);
			//					//System.out.println("[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[ temp date  " +tempDate);
							}				
						} else {
							System.out.println("Column 7A in Promot DOII Is Null (After NBSUB/RIS Check) For Army No."+employee.getArmyNo());
						}
					} else if (description!=null && description.trim().equalsIgnoreCase("MSTHAV")) {
							columnA = description;
							if (havDate!=null && havDate.before(currentMacpPromotDate)) {
								;
							} else if (acpHavDate!=null && acpHavDate.before(currentMacpPromotDate)) {
								;
							} else {
								tempDate = Utils.getSevenPcNextIncrementDate(currentMacpPromotDate);
							}
					} else if (description!=null && description.trim().equalsIgnoreCase("MSTJCO")) {
							columnA = description;
							if (nbsubDate!=null && nbsubDate.before(currentMacpPromotDate)) {
								;
							} else if (acpNbSubDate!=null && acpNbSubDate.before(currentMacpPromotDate)) {
								;
							} else {
								tempDate = Utils.getSevenPcNextIncrementDate(currentMacpPromotDate);
							}
					} else if (description!=null && (description.trim().equals("LATENR"))) {
						;
					} else if (description!=null && (description.trim().equals("HCOMMN"))) {
							List<Do2> hcommnDo2List = do2Repo.getHonyCommnDo2List(employee.getId(), presentIncrDate, correctDNI); 
							if (hcommnDo2List!=null && hcommnDo2List.size()>0) {
								if (hcommnDo2List.get(0).getColumnC()!=null) {
									tempDate = DateManager.getDateFromDdMmYyyy(hcommnDo2List.get(0).getColumnC());
								} else {
									tempDate = Utils.getSevenPcNextIncrementDate(hcommnDo2List.get(0).getFromDate());
								}
							} else {
								System.out.println("Honorary Commission DOII Incomplete For Army No."+employee.getArmyNo());
							}
					} 
		 		} else {
		 			List<Do2> optionDateDo2List = do2Repo.getOptionDatePayfixDo2List(employee.getId(), fixationOptionDate);
		   			if (optionDateDo2List!=null && optionDateDo2List.size()>0) {
		  				tempDate = fixationOptionDate;
		 		 		if (optionDateDo2List.get(0).getStatus().equals(Do2Manager.INVALID)) {
		 		 			jdbc.update("update do2 set status='P', reason=null where id = ?", optionDateDo2List.get(0).getId());	 
		 				} 				
		 			}
		 		}
	 		} 	 
		}
 	 	System.out.println("tempdate from checkprmotmacp method .......... "+tempDate);
  		return tempDate;
   }
   
   public Date checkNQS(Employee employee, Date startDate, Date endDate) {
	   List<AuditCage> acList = null;
		//System.out.println("33333333333333333333333333.....................  " +employee.getArmyNo()+"  "+cutOffDate +" start/end date "+startDate+"  "+endDate);
		nqsDuringCrucial6Months=false;
	 
		acList = acRepo.getAuditCagesForCheckingNqsInIncrReview(employee.getId(), startDate, endDate, cutOffDate); 				
	 
		//System.out.println("444444444444444444444444444444 .....................  " +employee.getArmyNo()+" aclistsize "+acList.size() +" start/end date "+startDate+"  "+endDate);
		
		if (acList==null || (acList!=null && acList.size()==0)) {
			return startDate;
		}
		
		double nqsDays = 0; 
		Date sDate = startDate;
		Date eDate = endDate;
		
		for (AuditCage ac : acList) {
			System.out.println("ggggggggggggggggggggggggggggggggggggggggggggggg fromdt  todt "+employee.getArmyNo()+"  "+ac.getFromDate()+"  "+ac.getToDate());
			if (ac.getFromDate().equals(ac.getToDate())) {
				if (optfixExercised==true) {
					nqsDuringCrucial6Months=true;
				}
				continue;
			}
			
		 	
			if (ac.getFromDate().before(sDate)) {
				sDate=startDate;
			} else {
				sDate=ac.getFromDate();
			}
			
			if (ac.getToDate()!=null) {
				eDate=ac.getToDate();	
			} else {
				if ((ac.getDo2()!=null && (ac.getDo2().getDescription().startsWith("FS") || ac.getDo2().getDescription().equals("DISCH"))
						|| ac.getFromDate().after(Utils.getLastDayOfPreviousQuarter()))) {
					continue;
				}
				eDate=endDate;
			} 
			//System.out.println("hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh sdate edate " +employee.getArmyNo()+"  "+sDate+"  "+eDate +"  "+nqsDays);
			if (!eDate.before(sDate))
				nqsDays += DateManager.getNoOfDays(sDate, eDate);			
		}	
		
		if (acList.get(0).getFromDate().before(startDate)) {  // arriving at sDate for Print Purpose
			sDate = startDate;
		} else {
			sDate = acList.get(0).getFromDate();
		}
		 
		
		if (nqsDays>180) { 		 		
			nqs180plus = true;				
		} else if (eDate!=null && eDate.after(endDate)) {
			if (optfixExercised==false) {
				startDate = DateManager.getNextDate(eDate);
			}  			
		}
		if (nqsDays<=180 && optfixExercised==true) {
			nqsDuringCrucial6Months=true;
			startDate = DateManager.getNextYear(startDate);
		}
		//System.out.println("iiiiiiiiiiiiiiiiiiiiiiiiiii nqsdays "+nqsDays+" nqs180plus  "+nqs180plus+" optfixexercised "+optfixExercised+" nqsduring6m "+nqsDuringCrucial6Months);
		nqsDays=0;
	//System.out.println("5555555555555555555555555555555555555.............................. "+employee.getArmyNo()+"   "+startDate );
		return startDate;
	}
	
	public boolean checkArmyNumberLength(String fedArmyNo) {
		String armyNumber = fedArmyNo;
		if (armyNumber==null || (armyNumber!=null && (armyNumber.trim().length()!=8 || armyNumber.trim().isEmpty()))) {		
 			armyNumber=null; 			
 			return false;
 		}
		
		for (int i=0; i<armyNumber.length(); i++) {
	 		if (Character.isDigit(armyNumber.charAt(i))==false) {
	 			if (i<2 && armyNumber.charAt(0)=='J' && armyNumber.charAt(1)=='C')
	 				continue;
	 			if (i<2 && armyNumber.charAt(1)=='T' && armyNumber.charAt(1)=='J')
	 				continue;
	  			System.out.println("Entered Army Number : "+employee.getArmyNo() +" is Incorrect.  Please Check !!! ");
	 			armyNumber=null;		 
				return false;
			}
		}	
		return true;
	}	 
	 
	public String padStringData(String value, int fieldLength) {
		String field = String.valueOf(value).trim();
		StringBuffer buffer = new StringBuffer();
		buffer.append(field);
		for (int i = 0; i < fieldLength-field.length(); i++) {
			buffer.append(" ");
		}
		return buffer.toString();
	}
	
	public Date getPaoCutOffDate(String paoCode) {
	 	
		if (paoCode.equals(Do2Manager.PAO_JKLI) || paoCode.equals(Do2Manager.PAO_LADAK_SCOUTS)) {
			return DateManager.getOtherCutOffDate();
		}
		
		if (paoCode.equals(Do2Manager.AO_ESTTNO22)) {
			return DateManager.getEstt22CutOffDate();
		}
		
		if (paoCode.equals(Do2Manager.APS_NAGPUR)) {
			return DateManager.getApsCutOffDate();
		}
		
		return DateManager.getCutOffDate();	
	} 
}  
