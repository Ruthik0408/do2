package itsdc.dolphin.do2;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.repositories.AuditCageRepository;
import itsdc.dolphin.repositories.Do2Repository;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream; 
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader; 
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar; 
import java.util.GregorianCalendar; 
import java.util.List;
import javax.transaction.SystemException;
import java.util.Date;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.core.env.Environment;
 
public class EnrollPre2016ReviewForReversionAll {
	Environment env;
	Do2Repository do2Repo;
	AuditCageRepository acRepo;

	public Do2Repository getDo2Repo() {
		return do2Repo;
	}

	public void setDo2Repo(Do2Repository do2Repo) {
		this.do2Repo = do2Repo;
	}
	
	public Environment getEnv() {
		return env;
	}

	public void setEnv(Environment env) {
		this.env = env;
	}

	public AuditCageRepository getAcRepo() {
		return acRepo;
	}

	public void setAcRepo(AuditCageRepository acRepo) {
		this.acRepo = acRepo;
	}

	static String url; 
	static String driver;
	static String user;
	static String password;
	static Connection con;
	static String bladeserver = "";   
	static String armyNumber;	
	static Date cutOffDate;
	Employee employee;
	String monthEnding = null;
	Date sevencpcDate = null;
	boolean nqs180plus = false, nqsDuringCrucial6Months = false;
	boolean optfixExercised = false;
	 
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
   
    public void verifyIncrementDo2AndDateForOneAmongAll(Employee employee) {
    	if (con==null) {
			con = getConnection();
		}
     	armyNumber = employee.getArmyNo();
    
    	if (monthEnding==null) {
    		monthEnding = Utils.getCurrentQuarter();
    	}
    	
     
    	get7cpcOptionDate(employee);   	
    	correctIncrementDateIn7cpcList = new ArrayList<Date>();
	  	Date presentIncrDate = DateManager.getSeventhCpcDate(), correctDNI = null, changedIncrDateDueToNqs = null, changedIncrDateDueToFix = null;
	  	try {
  			correctDNI = DateManager.getDate("2016-07-01");
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
  			if (employee.getStatusCode()==null || (employee.getStatusCode()!=null && employee.getStatusCode().getId()>1)) {
				System.out.println("Army Number ---> " + employee.getArmyNo() + ". Employee Status Is Not Effective !!! \n");
   				return;
  			}
  			if (!paoCode.equals(Do2Manager.PAO_DSC) && employee.getEnrolmentDate()!=null && employee.getEnrolmentDate().before(sevencpcDate)   
	  			   	&& employee.getInitialMusteringDate()!=null && !employee.getInitialMusteringDate().before(sevencpcDate)) {
	  			if (employee.getAttestationDate()==null 
	  					|| (employee.getAttestationDate()!=null && !employee.getAttestationDate().before(employee.getInitialMusteringDate()))) {
	  				Calendar cal = new GregorianCalendar();
	  				cal.setTime(employee.getInitialMusteringDate());
	  		 		try {
						if (cal.get(Calendar.MONTH)==Calendar.JANUARY && cal.get(Calendar.DATE)==1) {
							presentIncrDate = DateManager.getDate(cal.get(Calendar.YEAR) + "-07-01");
						} else {
							presentIncrDate = DateManager.getDate(cal.get(Calendar.YEAR)+1 + "-07-01");
						}
	  		 		} catch (Exception e) {
						Utils.handleException(e);
					}	  					
	  			} else if (employee.getAttestationDate()!=null && employee.getAttestationDate().before(employee.getInitialMusteringDate())) {
	  				Calendar cal = new GregorianCalendar();
	  				cal.setTime(employee.getAttestationDate());
	  		 		try {
						if (cal.get(Calendar.MONTH)==Calendar.JANUARY && cal.get(Calendar.DATE)==1) {
							presentIncrDate = DateManager.getDate(cal.get(Calendar.YEAR) + "-07-01");
						} else {
							presentIncrDate = DateManager.getDate(cal.get(Calendar.YEAR)+1 + "-07-01");
						}
						if (!employee.getInitialMusteringDate().before(presentIncrDate)) {
							presentIncrDate = DateManager.getDate(cal.get(Calendar.YEAR)+1 + "-07-01");
						}
	  		 		} catch (Exception e) {
						Utils.handleException(e);
					}	
	  			}
	  		} else if (!paoCode.equals(Do2Manager.PAO_DSC) && (employee.getInitialMusteringDate()==null || employee.getAttestationDate()==null)
  					&& employee.getEnrolmentDate()!=null && employee.getEnrolmentDate().after(getPaoCutOffDate(paoCode))) {
	  				if (employee.getGroupCategory()!=null && employee.getGroupCategory().getSoldierCategory().equals("RT")) {
	  					presentIncrDate = Utils.getNextIncrementDate(employee.getEnrolmentDate());
	  		 	 	}
	  		}
  			if (!presentIncrDate.equals(DateManager.getSeventhCpcDate())) {
  	  			correctDNI = DateManager.getNextYear(presentIncrDate);
  				correctIncrementDateIn7cpcList.add(presentIncrDate); 
  				System.out.println("..........$$$$$$$$$$$$$$$$$$$$$$$$$$$$ correct dni " +correctDNI+"  present incrdt "+presentIncrDate);
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
	  				System.out.println("changedincrduetonqs................. "+changedIncrDateDueToNqs);
	  				System.out.println("changedincrdttofix.................. "+changedIncrDateDueToFix);
	  				System.out.println("presentincrdt "+presentIncrDate+"  correctdni "+correctDNI);
	  				if (changedIncrDateDueToNqs!=null && changedIncrDateDueToNqs.after(presentIncrDate)) {	
	  					System.out.println("aaaaaaaaaaaaaaaaaaaaaaaa presentincrdt " +presentIncrDate+"  correct dni "+correctDNI);
		  				if (changedIncrDateDueToFix!=null) {
		  					System.out.println("bbbbbbbbbbbbbbbbbbbb presentincrdt " +presentIncrDate+"  correct dni "+correctDNI);
		  					if (changedIncrDateDueToFix.after(changedIncrDateDueToNqs) && !changedIncrDateDueToFix.after(correctDNI)) {
		  						System.out.println("cccccccccccccccccccccccc presentincrdt " +presentIncrDate+"  correct dni "+correctDNI);
		  						correctIncrementDateIn7cpcList.add(changedIncrDateDueToFix);			  			
					  			correctDNI = DateManager.getNextYear(changedIncrDateDueToFix);	
					  			presentIncrDate = changedIncrDateDueToFix;	  	
	 	  					} else {
	 	  						System.out.println("dddddddddddddddddddddddddd presentincrdt " +presentIncrDate+"  correct dni "+correctDNI);
		  						correctIncrementDateIn7cpcList.add(changedIncrDateDueToNqs);			  			
					  			correctDNI = DateManager.getNextYear(DateManager.getNextIncrementInOptixFixation(presentIncrDate));  //modified on 22.03.2021	
					  			presentIncrDate = changedIncrDateDueToNqs;	  	
	 	  					} 					
		  				} else {
		  					System.out.println("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee presentincrdt " +presentIncrDate+"  correct dni "+correctDNI);
		  					correctIncrementDateIn7cpcList.add(changedIncrDateDueToNqs);			  			
				  			correctDNI = DateManager.getNextYear(presentIncrDate);	
				  			presentIncrDate = changedIncrDateDueToNqs;	
	 	  				}
			     	} else if (changedIncrDateDueToFix!=null && changedIncrDateDueToFix.after(presentIncrDate)) {
			     		System.out.println("ffffffffffffffffffffffffffff presentincrdt " +presentIncrDate+"  correct dni "+correctDNI);
				  			correctIncrementDateIn7cpcList.add(changedIncrDateDueToFix);	
				  			presentIncrDate = changedIncrDateDueToFix;
				  			correctDNI = DateManager.getNextYear(changedIncrDateDueToFix);	
	 			    } else {
	 			    	System.out.println("kkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk presentincrdt " +presentIncrDate+"  correct dni "+correctDNI);
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
		      		System.out.println("ppppppppppppppppppppppp presentincrdt "+presentIncrDate+" changeincrfix "+changedIncrDateDueToFix);
				  			correctIncrementDateIn7cpcList.add(changedIncrDateDueToFix);	
				  			presentIncrDate = changedIncrDateDueToFix;
				  			correctDNI = DateManager.getNextIncrementInOptixFixation(changedIncrDateDueToFix);	
	 			    } else {
	 	 		    	System.out.println("qqqqqqqqqqqqqqqqqqqqqq correcdni "+correctDNI);
				     		presentIncrDate = correctDNI;	
				 			correctDNI = DateManager.getNextYear(presentIncrDate);
				      		if (!correctIncrementDateIn7cpcList.contains(presentIncrDate))
				     			correctIncrementDateIn7cpcList.add(presentIncrDate);
				   	} 
	   			System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% presentincrdt "+presentIncrDate+"   correctdni "+correctDNI);	  				
	  			}	  			
	  		} else {   	     	 
	     		return;
	     	}
	  		changedIncrDateDueToFix=null; changedIncrDateDueToNqs=null;
	  	}
	 			 
 		if (nqs180plus==false && correctIncrementDateIn7cpcList!=null && correctIncrementDateIn7cpcList.size()>0) {
 			PreparedStatement incrDo2, updateDNI;
 	 
 			try {
				incrDo2 = con.prepareStatement(" select * from do2 where description='INCR' and fk_employee = ?  and from_date = ? "
						+ " and status in ('V', 'P') order by status desc");
			
				PreparedStatement insertIncrementDo2 = con.prepareStatement(" insert into do2 (batch_no, unit_code, fk_unit, army_no, fk_employee, check_digit, "
		 				+ " do2_group, do2_type, do2_no, do2_item_no, do2_date, description, from_date, column_c, status, transcription_type, qe, audit_cage, "
		 				+ " input_qe, reason) values (?, ?, ?, ?, ?, ?, 'IA', '0', '6666', '666', ?, 'INCR', ?, ?, 'P', 'S', ?, 'true', ?, ?)"); 
	 		
				PreparedStatement updateCancelForIncr = null;
				 
				ResultSet result=null; 
				String updateCancelSqlString = " update do2 set status='C', accepted_qe = ? where description='INCR' and from_date > ? and "
			 			+ " status='V' and fk_employee = ? and column_c!='POSTPONED' and from_date NOT in (";
				
				int cancelledIncrDo2SqlReturnValue = 0;
				boolean incrDo2Cancelled = false;
				
				for (int i=0; i < correctIncrementDateIn7cpcList.size(); i++) {
					updateCancelSqlString=updateCancelSqlString+"?,";
	 		 	} 
					
				updateCancelSqlString =  updateCancelSqlString.substring(0, updateCancelSqlString.length()-1) +")";
					
				updateCancelForIncr = con.prepareStatement(updateCancelSqlString);
			 	
				updateCancelForIncr.setString(1, monthEnding);
				updateCancelForIncr.setDate(2, DateManager.convertUtilDtToSqlDt(employee.getEnrolmentDate()));
				updateCancelForIncr.setLong(3, employee.getId());
						
				int elementNo = 4;

				for (int i=0; i < correctIncrementDateIn7cpcList.size(); i++) {
					updateCancelForIncr.setDate(elementNo, DateManager.convertUtilDtToSqlDt(correctIncrementDateIn7cpcList.get(i)));
					elementNo++; 			 	
	 		 	} 
				
				cancelledIncrDo2SqlReturnValue = updateCancelForIncr.executeUpdate();
				updateCancelForIncr.clearBatch();
				updateCancelForIncr.clearParameters();
				updateCancelForIncr.close();
				 
				updateDNI = con.prepareStatement(" update employee set increment_date=? where current_record=true and qe=? and id=? ");
				 System.out.println(".............emp " +employee.getId()+" muster dt " +employee.getInitialMusteringDate()+" canceindrdo2sqlreturnvalue " +cancelledIncrDo2SqlReturnValue+" incrd2can " +incrDo2Cancelled);
				if (employee.getInitialMusteringDate()==null && cancelledIncrDo2SqlReturnValue==0 && incrDo2Cancelled==false) {
					return;
				} 
				 System.out.println(".............emp " +employee.getId() +" %%%%%%%%%%%  correctincr size   " + correctIncrementDateIn7cpcList.size());
						
				if (employee.getInitialMusteringDate()!=null) {
					for (Date d : correctIncrementDateIn7cpcList) {
						if (d.before(Utils.getLastDayOfPreviousQuarter())) {
							incrDo2.setLong(1,  employee.getId());
							incrDo2.setDate(2, DateManager.convertUtilDtToSqlDt(d));
							result = incrDo2.executeQuery();
							System.out.println("army no. "+armyNumber+"  correct incr date " +d);
				 			if (result.next()==false) {
				 				if ((insertedDo2Count/500)>=1) {
				 					batchNo++;
				 					insertedDo2Count=0;
				 				}
								insertIncrDo2(insertIncrementDo2, employee, d);
							}
						} else {
							System.out.println("............army number "+armyNumber+" old incr dt "+employee.getIncrementDate()+"  new incr dt "+d);
							//employee.setIncrementDate(d);
							//Utils.getEntityManager().flush();						
							updateDNI.setDate(1, DateManager.convertUtilDtToSqlDt(d));
							updateDNI.setString(2, monthEnding);
							updateDNI.setLong(3, employee.getId());
							updateDNI.executeUpdate();
							updateDNI.clearBatch();
							updateDNI.clearParameters();
							updateDNI.close();						
						}				
						result=null;
					}
					incrDo2.close();
					insertIncrementDo2.close();
				}
				
				if (cancelledIncrDo2SqlReturnValue>0 || incrDo2Cancelled==true) {
					PreparedStatement fromDateForAtcorr = con.prepareStatement("select min(from_date) from do2 where fk_employee = ? and description='INCR' and "
							+ " from_date>? and status='C' and accepted_qe = ?");

			 		fromDateForAtcorr.setLong(1, employee.getId());
			 		fromDateForAtcorr.setDate(2, DateManager.convertUtilDtToSqlDt(employee.getEnrolmentDate()));
			 		fromDateForAtcorr.setString(3, monthEnding);
			 		
					ResultSet minFromDateResult = fromDateForAtcorr.executeQuery();
					java.sql.Date atcorrFromDate=null;
			//		ResultSet fromDateIncrDo2Result=null; 
					System.out.println("...............1111111111111111111111111111111111111111");
			 		if (minFromDateResult!=null && minFromDateResult.next()) {
			 	 		
			 			atcorrFromDate = minFromDateResult.getDate(1);
				System.out.println("22222222222");
			 /*			PreparedStatement fromDateOfIncrDo2 = con.prepareStatement("select min(from_date) from do2 where fk_employee = ? and description='INCR' and "
								+ " from_date>? and status='P' and input_qe = ? and from_date <= ? ");
				  		
			   			fromDateOfIncrDo2.setLong(1, employee.getId());
			   			fromDateOfIncrDo2.setDate(2, DateManager.convertUtilDtToSqlDt(employee.getEnrolmentDate()));					 	
			   			fromDateOfIncrDo2.setString(3, monthEnding);
			   			fromDateOfIncrDo2.setDate(4, atcorrFromDate);
			   			fromDateIncrDo2Result = fromDateOfIncrDo2.executeQuery();			   
			*/ 		}
			 		fromDateForAtcorr.close();
minFromDateResult.close();
			// 		if ((fromDateIncrDo2Result==null || (fromDateIncrDo2Result!=null && !fromDateIncrDo2Result.next())) && minFromDateResult!=null && minFromDateResult.next()) {
			 	System.out.println("............ atcorrfromdt " +atcorrFromDate);
			 	//	if (fromDateIncrDo2Result==null && atcorrFromDate!=null) {
				if (atcorrFromDate!=null) {
			 			PreparedStatement atcorrDo2 = con.prepareStatement(" select * from do2 where description='ATCORR' and fk_employee=?  and from_date=? and status='P' "); 
						ResultSet atcorrDo2Result; 
	 					   			
	 		 			atcorrDo2.setLong(1, employee.getId());
			 			atcorrDo2.setDate(2, atcorrFromDate);
			 			atcorrDo2Result = atcorrDo2.executeQuery();
	
			 			if (!atcorrDo2Result.next()) {
			 				PreparedStatement insertAtcorrDo2 = con.prepareStatement(" insert into do2 (batch_no, unit_code, fk_unit, army_no, fk_employee, check_digit, "
			 		 				+ " do2_group, do2_type, do2_no, do2_item_no, do2_date, description, from_date,  status, transcription_type, qe, audit_cage, "
			 		 				+ " input_qe, reason) values (?, ?, ?, ?, ?, ?, 'IA', '0', '8888', '888', ?, 'ATCORR', ?, 'P', 'T', ?, 'true', ?, ?)"); 
			 			 
		 	 				insertAtcorrDo2(insertAtcorrDo2, employee, atcorrFromDate);	
			 				insertAtcorrDo2.close();
			 			}
			 			atcorrDo2.close();
			 			atcorrDo2Result.close();
			 		}
			 //		fromDateIncrDo2Result.close();
			 		
				} 
			
			} catch (SQLException e) {
				Utils.handleException(e);
			} 		
 		}
 		setCutOffDate(); 		
    }
    
   public void insertIncrDo2(PreparedStatement insertIncrementDo2, Employee employee, Date incrDate) {
	   	 
    	try {
			insertIncrementDo2.setInt(1, batchNo);
			insertIncrementDo2.setString(2, employee.getUnit().getUnitCode());
			insertIncrementDo2.setLong(3, employee.getUnit().getId());
			insertIncrementDo2.setString(4, employee.getArmyNo());
			insertIncrementDo2.setLong(5, employee.getId());
			insertIncrementDo2.setString(6, employee.getCheckDigit()+"");
			insertIncrementDo2.setDate(7, DateManager.convertUtilDtToSqlDt(incrDate));
			insertIncrementDo2.setDate(8, DateManager.convertUtilDtToSqlDt(incrDate));
			Calendar cal = new GregorianCalendar();
			cal.setTime(incrDate);
			if (cal.get(Calendar.DATE)==1) {
				insertIncrementDo2.setString(9, "INCRREV");	
			} else {
				insertIncrementDo2.setString(9, "POSTPONED");
			}	
			insertIncrementDo2.setString(10, monthEnding);
			insertIncrementDo2.setString(11, monthEnding);			
			insertIncrementDo2.setString(12, "Incr Do2 Inserted After Increment Review In 7CPC "+monthEnding);		  
			insertIncrementDo2.executeUpdate();
			insertedDo2Count++;	
		} catch (SQLException e) {
			Utils.handleException(e);
		}    	 
    }
   
   public void insertAtcorrDo2(PreparedStatement insertAtcorrDo2, Employee employee, Date atcorrDate) {
	 	try {
			insertAtcorrDo2.setInt(1, batchNo);
			insertAtcorrDo2.setString(2, employee.getUnit().getUnitCode());
			insertAtcorrDo2.setLong(3, employee.getUnit().getId());
			insertAtcorrDo2.setString(4, employee.getArmyNo());
			insertAtcorrDo2.setLong(5, employee.getId());
			insertAtcorrDo2.setString(6, employee.getCheckDigit()+"");
			insertAtcorrDo2.setDate(7, DateManager.convertUtilDtToSqlDt(atcorrDate));
			insertAtcorrDo2.setDate(8, DateManager.convertUtilDtToSqlDt(atcorrDate));
			insertAtcorrDo2.setString(9, monthEnding);
			insertAtcorrDo2.setString(10, monthEnding);
			insertAtcorrDo2.setString(11, ("ATCORR Do2 Inserted After Increment Review In "+monthEnding));		
			insertAtcorrDo2.executeUpdate();	
			insertedDo2Count++;
		} catch (SQLException e) {
			Utils.handleException(e);
		}		
   }
       
   public void get7cpcOptionDate(Employee employee) {
	   sevencpcDate = DateManager.getSeventhCpcDate();
	/*	List<Do2> do2List = Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.description='OPTCPC' "
		 		+ " and d.employee=:emp and d.columnB='SEVENPC' and d.columnA!='INCR' and d.status='V' ").setParameter("emp", employee).getResultList();
 	*/
	   List<Do2> do2List = do2Repo.findOptcpcNotOnIncrDateForEmployeeId(employee.getId());
	   if (do2List!=null && do2List.size()>0) {
 			if (do2List.get(0).getReason()==null || (do2List.get(0).getReason()!=null && !do2List.get(0).getReason().contains("Not Beneficial"))) {
	 			//System.out.println("-----------------");
				sevencpcDate = do2List.get(0).getFromDate();
			}			
		}
	   System.out.println("...........sevencpc date from increment review (enrol) .............. "+sevencpcDate);
	 }
   
   public Date checkPromotionUpgradation(Employee employee, Date presentIncrDate, Date correctDNI) {
 	   	Date tempDate = null;
	   	tempDate = presentIncrDate;
	   	java.sql.Date currentMacpPromotDate = null, empIncrementDate = null, acpNaikDate = null, havDate = null, acpHavDate = null, nbsubDate = null,
					acpNbSubDate = null, acpSubDate = null, acpSubMajDate = null, enrollmentDate = null, attestationDate = null, musterDate = null,
					naikDate = null, subDate = null, submajDate = null, honyLtDate = null, fixationOptionDate = null;
	 	java.util.Date firstJanuary = null, firstJuly = null; 
	  	String armyNo = null, checkDigit = null, description = null, columnA = null, unitCode = null;
		long empId = 0, unitId = 0;	

	    PreparedStatement getMacpsPromotDOs2=null, previousPromotDo2List=null, optionDateDo2List=null, optfixDo2List=null;
	    ResultSet result=null, previousPromotDo2Result, optionDateDo2Result, optfixDo2Result;  
 	 	try {
			getMacpsPromotDOs2 = con.prepareStatement("select  u.unit_code, e.fk_present_unit, d.fk_employee, d.army_no, d.check_digit, " +
					" d.description, d.column_a, d.from_date, e.increment_date, e.acpnaik_date, e.acphav_date, e.acpnbsub_date, e.acpsub_date, e.acpsubmaj_date, " +
					" e.hav_date, e.nbsub_date, e.enrolment_date, e.attestation_date, e.initial_mustering_date, e.naik_date, e.sub_date, e.submaj_date, " +
					" e.hony_lieu_date, d.to_date from do2 d join employee e on e.id=d.fk_employee join unit u on u.id=e.fk_present_unit where d.description " +
					" in ('PROMOT', 'MACPS', 'MSTHAV', 'MSTJCO','HCOMMN', 'LATENR') and d.status='V' " +
					" and d.from_date>'2015-12-31' and e.current_record=true and e.regular_ta_flag='R' and e.fk_status_code<5 and e.id=? " +
					" and d.from_date between ? and ? order by d.from_date limit 1");
			
			previousPromotDo2List = con.prepareStatement(" select fk_employee, description, column_a, from_date from do2 where fk_employee = ? and "
					+ " from_date < ? and status='V' and column_a = ? and description='PROMOT' order by from_date desc ");
			
			optfixDo2List = con.prepareStatement(" select fk_employee, description, column_a, from_date from do2 where fk_employee = ? and "
					+ " from_date = ? and status='V' and column_a = ? and description='OPTFIX'  ");

				optionDateDo2List = con.prepareStatement(" select id, status from do2 where fk_employee = ? and "
					+ " from_date = ? and status!='C'  and description in ('PROFIX', 'ACPFIX', 'HAVFIX') order by from_date desc ");
				
			getMacpsPromotDOs2.setLong(1, employee.getId());			
			getMacpsPromotDOs2.setDate(2, DateManager.convertUtilDtToSqlDt(presentIncrDate));
			getMacpsPromotDOs2.setDate(3, DateManager.convertUtilDtToSqlDt(correctDNI));
			
			result = getMacpsPromotDOs2.executeQuery();  
			List<String> validSbDo2ColumnA = new ArrayList<String>();
	 	 	validSbDo2ColumnA.add("SBNK");
		 	validSbDo2ColumnA.add("SBHAV");
		 	validSbDo2ColumnA.add("SBSUB");
		 	validSbDo2ColumnA.add("SBSUBMAJ");
		 	validSbDo2ColumnA.add("SBLD");
		 	validSbDo2ColumnA.add("SBDFR");
		 	validSbDo2ColumnA.add("SBRIS");
		 	validSbDo2ColumnA.add("SBRISMAJ");
		 	tempDate=null;
		 	optfixExercised=false;
	 		while (result.next()) {	 			
	  			unitCode = result.getString(1);
				unitId = result.getLong(2);				
				empId = result.getLong(3);  
				armyNo = result.getString(4); 
				checkDigit = result.getString(5);
				description = result.getString(6);
				columnA = result.getString(7);
				currentMacpPromotDate = result.getDate(8);
				empIncrementDate = result.getDate(9);
				acpNaikDate = result.getDate(10);
				acpHavDate = result.getDate(11);
				acpNbSubDate = result.getDate(12);
				acpSubDate = result.getDate(13);
				acpSubMajDate = result.getDate(14);
				havDate = result.getDate(15);
				nbsubDate = result.getDate(16);
				enrollmentDate = result.getDate(17);
				attestationDate = result.getDate(18);
				musterDate = result.getDate(19);
				naikDate = result.getDate(20);
				subDate = result.getDate(21);
				submajDate = result.getDate(22);
				honyLtDate = result.getDate(23);
				fixationOptionDate = result.getDate(24);
				
				/*			
				if (!presentIncrDate.equals(currentMacpPromotDate)) {
					optfixDo2List.setLong(1, employee.getId());
					optfixDo2List.setDate(2, DateManager.convertUtilDtToSqlDt(currentMacpPromotDate));
					optfixDo2List.setString(3, description);					
					optfixDo2Result = optfixDo2List.executeQuery(); 
					if (optfixDo2Result.next()) { 
						optfixExercised=true;
						tempDate = correctDNI; 
						optfixDo2List.close();
						return tempDate;
					} 
				}
				if (correctDNI.equals(currentMacpPromotDate)) {
		 			correctIncrementDateIn7cpcList.add(correctDNI);
				}
				
 	*/	 		Calendar cal = new GregorianCalendar();	
				cal.setTime(currentMacpPromotDate);
		 		int year = cal.get(Calendar.YEAR);			
								
				firstJanuary = DateManager.getDate(year, 1, 1);
				firstJuly = DateManager.getDate(year, 7, 1);
				
	 System.out.println("}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}} promotmacpscheck "+firstJanuary+"  "+firstJuly+"  currentmacpprodt "+currentMacpPromotDate
	 					 +" optfixex "+optfixExercised +"  presentincrdt "+presentIncrDate+"  corrdni "+correctDNI);
				
				if (fixationOptionDate==null) {				
					if (description!=null && description.equals("MACPS")) {					
						tempDate = Utils.getSevenPcNextIncrementDate(currentMacpPromotDate);
					} else if (description!=null && description.trim().equals("PROMOT")) {
						if (columnA!=null && validSbDo2ColumnA.contains(columnA.trim())) {
							columnA="PA"+columnA.substring(2);
							previousPromotDo2List.setLong(1, empId);
							previousPromotDo2List.setDate(2, currentMacpPromotDate);
							previousPromotDo2List.setString(3, columnA);
							previousPromotDo2Result = previousPromotDo2List.executeQuery();
							if (previousPromotDo2Result.next()) {
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
									} else if (columnA.trim().contains("SUB") || columnA.trim().contains("RIS")) {
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
			 						//System.out.println("Column 7A in Promot DOII Is Null For Army No."+employee.getArmyNo());
								}
							}
						} else if (columnA.trim().equals("NBSUB") || columnA.trim().equals("NBRIS")) {
							if (acpNbSubDate!=null && currentMacpPromotDate!=null && acpNbSubDate.before(currentMacpPromotDate)) {
								;
							} else {
								tempDate = Utils.getSevenPcNextIncrementDate(currentMacpPromotDate);
			 					 System.out.println("[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[ temp date  " +tempDate);
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
			/*				List<Do2> hcommnDo2List = Utils.getEntityManager().createQuery("select d from Do2 d where d.description='HCOMMN' and "
									+ " d.description='V' and d.employee=:emp and d.fromDate > :frdt and d.toDate < :todt order by d.fromDate ")
									.setParameter("emp", employee).setParameter("frdt", presentIncrDate).setParameter("todt", correctDNI).getResultList(); */
							List<Do2> hcommnDo2List = do2Repo.findHcommnDo2ForEmployeeId(employee.getId());
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
		 			optionDateDo2List.setLong(1, employee.getId());
		 			optionDateDo2List.setDate(2, fixationOptionDate);
		 			optionDateDo2Result=optionDateDo2List.executeQuery();
		 			tempDate=fixationOptionDate;
		 			fixationOptionDate=null;
		 			if (optionDateDo2Result.next()) {		  				
		 				long do2id = optionDateDo2Result.getLong(1);
		 				String do2Status = optionDateDo2Result.getString(2);
		 				if (do2Status.equals(Do2Manager.INVALID)) {
		 					PreparedStatement ps = con.prepareStatement("update do2 set status='P', reason=null where id = ? ");
		 					ps.setLong(1, do2id);
		 					ps.executeUpdate();
		 				} 				
		 			}
		 			optfixExercised=true;
		 			optionDateDo2List=null;
		 			optionDateDo2Result.close();
		 		}
	 		}
	 		getMacpsPromotDOs2.close();	 		 
		} catch (Exception e) {
			Utils.handleException(e);
		} 
 	  	System.out.println("tempdate from checkprmotmacp method .......... "+tempDate);
  		return tempDate;
   }
   
   public Date checkNQS(Employee employee, Date startDate, Date endDate) {
	   List<AuditCage> acList = null;
		//System.out.println("33333333333333333333333333.....................  " +employee.getArmyNo()+"  "+cutOffDate +" start/end date "+startDate+"  "+endDate);
		nqsDuringCrucial6Months=false;
/*		acList = Utils.getEntityManager().createQuery("select a from AuditCage a where a.employee=:emp and a.effective=true "
				+ " and a.cancelled=false and a.recordStatus='V' and a.absentFlag=true and a.deputationFlag=false and  "
				+ " (a.fromDate between :frdt and :todt OR (a.toDate is null OR a.toDate between :frdt and :todt)) and a.fromDate<=:currentME "
				+ " order by a.fromDate, a.toDate ").setParameter("emp", employee).setParameter("frdt", startDate).setParameter("currentME", cutOffDate)
				.setParameter("todt", endDate).getResultList();
	*/
		//System.out.println("444444444444444444444444444444 .....................  " +employee.getArmyNo()+" aclistsize "+acList.size() +" start/end date "+startDate+"  "+endDate);
		
		acList = acRepo.getAuditCageListForNQS(employee.getId(), startDate, cutOffDate, endDate);
		if (acList==null || (acList!=null && acList.size()==0)) {
			return startDate;
		}
		
		double nqsDays = 0; 
		Date sDate = startDate;
		Date eDate = endDate;
		
		for (AuditCage ac : acList) {
			//System.out.println("ggggggggggggggggggggggggggggggggggggggggggggggg fromdt  todt "+employee.getArmyNo()+"  "+ac.getFromDate()+"  "+ac.getToDate());
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
			try {
				writeNqsObject(employee, sDate, eDate, nqsDays);	
			} catch (Exception e) {
				Utils.handleException(e);
			}			
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
	 
	static public void JdbcDetails() {
		Document doc = null;
		List list = null;
        String rootName;       
		try {
			SAXReader reader = new SAXReader();
			String jbossHome = System.getenv("JBOSS_HOME");
			String filepath = jbossHome+"/server/default/deploy/dolphin-ds.xml";
			if (bladeserver.equalsIgnoreCase("y")) {
				File file = new File(jbossHome+"/server/");
				String[] names = file.list();
				for (String name : names) {
				    if (name.startsWith("node") && new File(jbossHome+"/server/" + name).isDirectory()) {
				        filepath = jbossHome+"/server/"+ name + "/deploy/dolphin-ds.xml";
				    }
				}
			} 
			doc = reader.read(new File(filepath));
			Element rootElement = doc.getRootElement();
			rootName = rootElement.getName() + "//local-tx-datasource//";
			list = doc.selectNodes(rootName+"password" );
			password = ((Element)list.get(0)).getText();
			list = doc.selectNodes(rootName+"connection-url" );
			url = ((Element)list.get(0)).getText();
			list = doc.selectNodes(rootName+"driver-class" );
			driver = ((Element)list.get(0)).getText();
			list = doc.selectNodes(rootName+"user-name" );
			user = ((Element)list.get(0)).getText();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	
		
	public void writeNqsObject(Employee employee, Date sDate, Date eDate, double nqsDays) throws java.io.NotSerializableException {
		try {
			String fileName = null;
			File objectsDirectory = null;
			objectsDirectory = new File("../reports/NqsDataIncrReview");		
		
			if (!objectsDirectory.exists())
				objectsDirectory.mkdirs();
		
			String startDate = DateManager.getStringDateWithoutSeparators(sDate);
		
			BufferedWriter writer = null;
			StringBuffer sb = new StringBuffer("                                                                                                                    ");
		
			fileName = "../reports/NqsDataIncrReview/Nqs_"+employee.getArmyNo()+"_"+startDate+".txt";
				
			File file = new File(fileName);
			writer = new BufferedWriter(new FileWriter(file));
			String name = null;
			if (employee!=null) {
				if (employee.getName()!=null) {
					if (employee.getName().length()>20) {
             			name = employee.getName().substring(0,20);
             		} else {
             			name = employee.getName();
             		}
	                } else {
	                	name="                       ";
	                }
             } else {
             	name="                       ";
             }
	    	 
		   	sb.replace(0, 10, employee.getArmyNo()+employee.getCheckDigit()+"~");
	    	sb.replace(11, 32, name+"~");
	    	sb.replace(32, 36, employee.getUnit().getTask_1()+"~");
	    	sb.replace(37, 45, DateManager.getStringDateWithoutSeparators(sDate)+"~");
	    	sb.replace(46, 54, DateManager.getStringDateWithoutSeparators(eDate)+"~");
	    	sb.replace(55, 61, Math.round(nqsDays)+"".trim());	
 			 		
	    	writer.write(sb.toString());
			writer.write("\r\n");
			writer.close();
		
		} catch(Exception e) {
			Utils.handleException(e);			 
		} 
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
	
	private Connection getConnection() {
		String url = "jdbc:postgresql://localhost:5432/dolphin";
		String driver = "org.postgresql.Driver";
 		String user = env.getProperty("spring.datasource.username");
 		String password = env.getProperty("spring.datasource.password");
		Connection con = null;		 
		try {
			Class.forName(driver).newInstance();
			con = DriverManager.getConnection(url, user, password);
		} catch (InstantiationException e) { 
			e.printStackTrace();
		} catch (IllegalAccessException e) { 
			e.printStackTrace();
		} catch (ClassNotFoundException e) { 
			e.printStackTrace();
		} catch (SQLException e) { 
			e.printStackTrace();
		}
		return con;
	}
}  
